package org.springframework.roo.addon.jms;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides JMS configuration operations.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component 
@Service 
public class JmsOperationsImpl implements JmsOperations {
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ProjectOperations projectOperations;

	public boolean isInstallJmsAvailable() {
		return getPathResolver() != null;
	}

	public boolean isManageJmsAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml"));
	}

	public void installJms(JmsProvider jmsProvider, String name, JmsDestinationType destinationType) {
		Assert.notNull(jmsProvider, "Jms provider required");
		String jmsContextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml");
		MutableFile jmsContextMutableFile = null;

		Document appCtx;
		try {
			if (fileManager.exists(jmsContextPath)) {
				jmsContextMutableFile = fileManager.updateFile(jmsContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(jmsContextMutableFile.getInputStream());
			} else {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-jms-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml")).getOutputStream());
				jmsContextMutableFile = fileManager.updateFile(jmsContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(jmsContextMutableFile.getInputStream());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) appCtx.getFirstChild();

		boolean needsPersisted = false;

		if (name != null && name.length() > 0) {
			Element destination = appCtx.createElement("amq:" + destinationType.getType().toLowerCase());
			destination.setAttribute("physicalName", name);
			destination.setAttribute("id", name);
			root.appendChild(destination);
			addDefaultDestination(appCtx, name);
			needsPersisted = true;
		}

		Element listenerContainer = XmlUtils.findFirstElement("/beans/listener-container[@destination-type='" + destinationType.getType().toLowerCase() + "']", root);

		if (listenerContainer == null) {
			listenerContainer = appCtx.createElement("jms:listener-container");
			listenerContainer.setAttribute("connection-factory", "jmsFactory");
			listenerContainer.setAttribute("destination-type", destinationType.getType().toLowerCase());
			root.appendChild(listenerContainer);
			needsPersisted = true;
		}

		if (needsPersisted) {
			XmlUtils.writeXml(jmsContextMutableFile.getOutputStream(), appCtx);
		}

		updateConfiguration(jmsProvider);
	}

	public void injectJmsTemplate(JavaType targetType, JavaSymbolName fieldName) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(declaredByMetadataId));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(declaredByMetadataId));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(declaredByMetadataId));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		// Create some method content to get people started
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType(Object.class.getName()), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("messageObject"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName + ".convertAndSend(messageObject);");

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, Modifier.PRIVATE | Modifier.TRANSIENT, annotations, fieldName, new JavaType("org.springframework.jms.core.JmsTemplate"));
		mutableTypeDetails.addField(fieldBuilder.build());

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		mutableTypeDetails.addMethod(methodBuilder.build());
	}

	public void addJmsListener(JavaType targetType, String name, JmsDestinationType destinationType) {
		Assert.notNull(targetType, "Java type required");

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);

		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Object"), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("message"));

		// create some method content to get people started
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("System.out.println(\"JMS message received: \" + message);");
		methods.add(new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("onMessage"), JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder));

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, targetType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setDeclaredMethods(methods);

		// Determine the canonical filename
		String physicalLocationCanonicalPath = getPhysicalLocationCanonicalPath(declaredByMetadataId);

		// Check the file doesn't already exist
		Assert.isTrue(!fileManager.exists(physicalLocationCanonicalPath), getPathResolver().getFriendlyName(physicalLocationCanonicalPath) + " already exists");

		// Compute physical location
		PhysicalTypeMetadata toCreate = new DefaultPhysicalTypeMetadata(declaredByMetadataId, physicalLocationCanonicalPath, typeDetailsBuilder.build());

		physicalTypeMetadataProvider.createPhysicalType(toCreate);

		String jmsContextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml");
		MutableFile jmsContextMutableFile = null;

		Document appCtx;
		try {
			if (fileManager.exists(jmsContextPath)) {
				jmsContextMutableFile = fileManager.updateFile(jmsContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(jmsContextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not find applicationContext-jms.xml");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) appCtx.getFirstChild();
		Element listenerContainer = XmlUtils.findFirstElementByName("jms:listener-container", root);
		if (listenerContainer != null && destinationType.getType().toLowerCase().equals(listenerContainer.getAttribute("destination-type"))) {
			listenerContainer = appCtx.createElement("jms:listener-container");
			listenerContainer.setAttribute("connection-factory", "jmsFactory");
			listenerContainer.setAttribute("destination-type", destinationType.getType().toLowerCase());
			root.appendChild(listenerContainer);
		}

		Element jmsListener = appCtx.createElement("jms:listener");
		jmsListener.setAttribute("ref", StringUtils.uncapitalize(targetType.getSimpleTypeName()));
		jmsListener.setAttribute("method", "onMessage");
		jmsListener.setAttribute("destination", name);

		Element bean = appCtx.createElement("bean");
		bean.setAttribute("class", targetType.getFullyQualifiedTypeName());
		bean.setAttribute("id", StringUtils.uncapitalize(targetType.getSimpleTypeName()));
		root.appendChild(bean);

		listenerContainer.appendChild(jmsListener);

		XmlUtils.writeXml(jmsContextMutableFile.getOutputStream(), appCtx);
	}

	private void updateConfiguration(JmsProvider jmsProvider) {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Element> springDependencies = XmlUtils.findElements("/configuration/springJms/dependencies/dependency", configuration);
		for (Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}

		List<Element> dependencies = XmlUtils.findElements("/configuration/jmsProviders/provider[@id='" + jmsProvider.getKey() + "']/dependencies/dependency", configuration);
		for (Element dependency : dependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
	}

	private void addDefaultDestination(Document appCtx, String name) {
		// If we do already have a default destination configured then do nothing
		Element root = (Element) appCtx.getFirstChild();
		if (null != XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.jms.core.JmsTemplate']/property[@name='defaultDestination']", root)) {
			return;
		}
		// Otherwise add it
		Element jmsTemplate = XmlUtils.findRequiredElement("/beans/bean[@class='org.springframework.jms.core.JmsTemplate']", root);
		Element defaultDestination = appCtx.createElement("property");
		defaultDestination.setAttribute("ref", name);
		defaultDestination.setAttribute("name", "defaultDestination");
		jmsTemplate.appendChild(defaultDestination);
	}

	private String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		PathResolver pathResolver = getPathResolver();
		Assert.notNull(pathResolver, "Cannot computed metadata ID of a type because the path resolver is presently unavailable");
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
		Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		String physicalLocationCanonicalPath = pathResolver.getIdentifier(path, relativePath);
		return physicalLocationCanonicalPath;
	}

	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
}