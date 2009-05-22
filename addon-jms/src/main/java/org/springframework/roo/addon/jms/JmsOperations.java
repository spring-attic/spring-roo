package org.springframework.roo.addon.jms;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
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
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
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
 * @since 1.0
 */
@ScopeDevelopment
public class JmsOperations {
	
	Logger logger = Logger.getLogger(JmsOperations.class.getName());
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	private ProjectOperations projectOperations;
	
	public JmsOperations(MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider, FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.physicalTypeMetadataProvider = physicalTypeMetadataProvider;
		this.projectOperations = projectOperations;
	}
	
	public boolean isInstallJmsAvailable() {		
		return getPathResolver() != null;
	}
	
	public boolean isManageJmsAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-jms.xml"));
	}
	
	public void installJms(JmsProvider jmsProvider, String name, JmsDestinationType destinationType) {
		Assert.notNull(jmsProvider, "Jms provider required");
		String jmsContextPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-jms.xml");
		MutableFile jmsContextMutableFile = null;
		
		Document appCtx;
		try {
			if (fileManager.exists(jmsContextPath)) {
				jmsContextMutableFile = fileManager.updateFile(jmsContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(jmsContextMutableFile.getInputStream());
			} else {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-jms-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-jms.xml")).getOutputStream());
				jmsContextMutableFile = fileManager.updateFile(jmsContextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(jmsContextMutableFile.getInputStream());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		Element root = (Element) appCtx.getFirstChild();
		
		boolean needsPersisted = false;
		
		if(name!=null && name.length() > 0) {
			Element destination = appCtx.createElement("amq:" + destinationType.getType().toLowerCase());
			destination.setAttribute("physicalName", name);
			destination.setAttribute("id", name);			
			root.appendChild(destination);
			addDefaultDestination(appCtx, name);
			needsPersisted = true;
		}
		
		Element listenerContainer = XmlUtils.findFirstElement("/beans/jms:listener-container[@destination-type='" + destinationType.getType().toLowerCase() + "']", root);
		
		if (listenerContainer == null) {
			listenerContainer = appCtx.createElement("jms:listener-container");
			listenerContainer.setAttribute("connection-factory", "jmsFactory");
			listenerContainer.setAttribute("destination-type", destinationType.getType().toLowerCase());
			root.appendChild(listenerContainer);
			needsPersisted = true;
		}
				
		if(needsPersisted) {
			XmlUtils.writeXml(jmsContextMutableFile.getOutputStream(), appCtx);
		}		
		
		updateDependencies(jmsProvider);
	}	
	
	public void injectJmsTemplate(JavaType targetType, JavaSymbolName fieldName) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");
		
		int modifier = Modifier.PRIVATE;
		modifier |= Modifier.TRANSIENT;
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);
		FieldMetadata fieldMetadata = new DefaultFieldMetadata(declaredByMetadataId, modifier, fieldName, new JavaType("org.springframework.jms.core.JmsTemplate"), null, annotations);
		
		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(fieldMetadata.getDeclaredByMetadataId());
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;
		
		//create some method content to get people started
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType(Object.class.getName()), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("messageObject"));
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName + ".convertAndSend(messageObject);");

		mutableTypeDetails.addField(fieldMetadata);
		mutableTypeDetails.addMethod(new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, paramTypes, paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput()));
	}
	
	public void addJmsListener(JavaType targetType, String name, JmsDestinationType destinationType) {
		Assert.notNull(targetType, "Java type required");
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);
		
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("java.lang.Object"), annotations));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("message"));
		
		//create some method content to get people started
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("System.out.println(\"JMS message received: \" + message);");
		methods.add(new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("onMessage"), JavaType.VOID_PRIMITIVE, paramTypes, paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput()));
		
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, targetType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, methods, null, null, null, null, null);
		
		// Determine the canonical filename
		String physicalLocationCanonicalPath = getPhysicalLocationCanonicalPath(details.getDeclaredByMetadataId());
		
		// Check the file doesn't already exist
		Assert.isTrue(!fileManager.exists(physicalLocationCanonicalPath), getPathResolver().getFriendlyName(physicalLocationCanonicalPath) + " already exists");
		
		// Compute physical location
		PhysicalTypeMetadata toCreate = new DefaultPhysicalTypeMetadata(details.getDeclaredByMetadataId(), physicalLocationCanonicalPath, details);
		
		physicalTypeMetadataProvider.createPhysicalType(toCreate);
		
		String jmsContextPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext-jms.xml");
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
				
		if (listenerContainer == null && destinationType.getType().toLowerCase().equals(listenerContainer.getAttribute("destination-type"))) {
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
	
	private void updateDependencies(JmsProvider jmsProvider) {		

		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dependenciesElement = (Element) dependencyDoc.getFirstChild();
		
		List<Element> springDependencies = XmlUtils.findElements("/dependencies/springJms/dependency", dependenciesElement);
		for(Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}

		List<Element> dependencies = XmlUtils.findElements("/dependencies/jmsProviders/provider[@id='" + jmsProvider.getKey() + "']/dependency", dependenciesElement);
		for(Element dependency : dependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}	
	}	
	
	private void addDefaultDestination(Document appCtx, String name) {
		//if we do already have a default destination configured then do nothing
		Element root = (Element) appCtx.getFirstChild();
		if (null != XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.jms.core.JmsTemplate']/property[@name='defaultDestination']", root)) {
			return;
		}
		//otherwise add it
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