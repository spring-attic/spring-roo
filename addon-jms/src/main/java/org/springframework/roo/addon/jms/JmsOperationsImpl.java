package org.springframework.roo.addon.jms;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.TRANSIENT;
import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.SpringJavaType.ASYNC;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.JMS_OPERATIONS;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
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

	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private TypeManagementService typeManagementService;
	@Reference private TypeLocationService typeLocationService;

	public boolean isInstallJmsAvailable() {
		return projectOperations.isProjectAvailable() && !hasJmsContext();
	}

	public boolean isManageJmsAvailable() {
		return projectOperations.isProjectAvailable() && hasJmsContext();
	}

	private boolean hasJmsContext() {
		return fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml"));
	}

	public void installJms(final JmsProvider jmsProvider, final String name, final JmsDestinationType destinationType) {
		Assert.isTrue(isInstallJmsAvailable(), "Project not available");
		Assert.notNull(jmsProvider, "JMS provider required");

		final String jmsContextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml");
		final InputStream in;
		if (fileManager.exists(jmsContextPath)) {
			in = fileManager.getInputStream(jmsContextPath);
		} else {
			in = TemplateUtils.getTemplate(getClass(), "applicationContext-jms-template.xml");
			Assert.notNull(in, "Could not acquire applicationContext-jms.xml template");
		}
		final Document document = XmlUtils.readXml(in);

		final Element root = document.getDocumentElement();

		if (StringUtils.hasText(name)) {
			final Element destination = document.createElement("amq:" + destinationType.name().toLowerCase());
			destination.setAttribute("physicalName", name);
			destination.setAttribute("id", name);
			root.appendChild(destination);
			addDefaultDestination(document, name);
		}

		Element listenerContainer = XmlUtils.findFirstElement("/beans/listener-container[@destination-type = '" + destinationType.name().toLowerCase() + "']", root);
		if (listenerContainer == null) {
			listenerContainer = document.createElement("jms:listener-container");
			listenerContainer.setAttribute("connection-factory", "jmsFactory");
			listenerContainer.setAttribute("destination-type", destinationType.name().toLowerCase());
			root.appendChild(listenerContainer);
		}

		DomUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(jmsContextPath, XmlUtils.nodeToString(document), false);

		updateConfiguration(jmsProvider);
	}

	public void injectJmsTemplate(final JavaType targetType, final JavaSymbolName fieldName, final boolean asynchronous) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");

		final ClassOrInterfaceTypeDetails targetTypeDetails = typeLocationService.findClassOrInterface(targetType);
		Assert.isTrue(targetTypeDetails != null, "Cannot locate source for '" + targetType.getFullyQualifiedTypeName() + "'");

		final String declaredByMetadataId = targetTypeDetails.getDeclaredByMetadataId();
		final ClassOrInterfaceTypeDetailsBuilder targetTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(targetTypeDetails);
		
		// Create the field
		addJmsOperationsField(fieldName, declaredByMetadataId, targetTypeBuilder);

		// Create the method
		targetTypeBuilder.addMethod(createSendMessageMethod(fieldName, declaredByMetadataId, asynchronous));

		if (asynchronous) {
			ensureSpringAsynchronousSupportEnabled();
		}

		typeManagementService.createOrUpdateTypeOnDisk(targetTypeBuilder.build());
	}

	/**
	 * Creates the injected method that sends a JMS message
	 * 
	 * @param fieldName
	 * @param declaredByMetadataId
	 * @param asynchronous whether the JMS message should be sent asynchronously
	 * @return a non-<code>null</code> builder
	 */
	private MethodMetadataBuilder createSendMessageMethod(final JavaSymbolName fieldName, final String declaredByMetadataId, final boolean asynchronous) {
		final List<JavaType> parameterTypes = Arrays.asList(JavaType.OBJECT);
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("messageObject"));

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(fieldName + ".convertAndSend(messageObject);");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		if (asynchronous) {
			methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
		}
		return methodBuilder;
	}

	/**
	 * Ensures that the Spring config files contain the necessary elements and
	 * properties to support asynchronous tasks.
	 */
	private void ensureSpringAsynchronousSupportEnabled() {
		final String contextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		final Document appContext = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = appContext.getDocumentElement();

		if (DomUtils.findFirstElementByName("task:annotation-driven", root) == null) {
			if (root.getAttribute("xmlns:task").length() == 0) {
				root.setAttribute("xmlns:task", "http://www.springframework.org/schema/task");
				root.setAttribute("xsi:schemaLocation", root.getAttribute("xsi:schemaLocation") + "  http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd");
			}
			root.appendChild(new XmlElementBuilder("task:annotation-driven", appContext).addAttribute("executor", "asyncExecutor").addAttribute("mode", "aspectj").build());
			root.appendChild(new XmlElementBuilder("task:executor", appContext).addAttribute("id", "asyncExecutor").addAttribute("pool-size", "${executor.poolSize}").build());

			fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(appContext), false);

			propFileOperations.addPropertyIfNotExists(Path.SPRING_CONFIG_ROOT, "jms.properties", "executor.poolSize", "10", true);
		}
	}

	private void addJmsOperationsField(final JavaSymbolName fieldName,
			final String declaredByMetadataId,
			final ClassOrInterfaceTypeDetailsBuilder targetTypeBuilder) {
		final List<AnnotationMetadataBuilder> annotations = Arrays.asList(new AnnotationMetadataBuilder(AUTOWIRED));
		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, PRIVATE | TRANSIENT, annotations, fieldName, JMS_OPERATIONS);
		targetTypeBuilder.addField(fieldBuilder);
	}

	public void addJmsListener(final JavaType targetType, final String name, final JmsDestinationType destinationType) {
		Assert.notNull(targetType, "Java type required");

		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);

		final List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		final List<JavaType> parameterTypes = Arrays.asList(OBJECT);
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("message"));

		// Create some method content to get people started
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("System.out.println(\"JMS message received: \" + message);");
		methods.add(new MethodMetadataBuilder(declaredByMetadataId, PUBLIC, new JavaSymbolName("onMessage"), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder));

		final ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, PUBLIC, targetType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setDeclaredMethods(methods);

		// Determine the canonical filename
		final String physicalLocationCanonicalPath = getPhysicalLocationCanonicalPath(declaredByMetadataId);

		// Check the file doesn't already exist
		Assert.isTrue(!fileManager.exists(physicalLocationCanonicalPath), projectOperations.getPathResolver().getFriendlyName(physicalLocationCanonicalPath) + " already exists");

		typeManagementService.createOrUpdateTypeOnDisk(typeDetailsBuilder.build());

		final String jmsContextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jms.xml");
		final Document document = XmlUtils.readXml(fileManager.getInputStream(jmsContextPath));
		final Element root = document.getDocumentElement();

		Element listenerContainer = DomUtils.findFirstElementByName("jms:listener-container", root);
		if (listenerContainer != null && destinationType.name().equalsIgnoreCase(listenerContainer.getAttribute("destination-type"))) {
			listenerContainer = document.createElement("jms:listener-container");
			listenerContainer.setAttribute("connection-factory", "jmsFactory");
			listenerContainer.setAttribute("destination-type", destinationType.name().toLowerCase());
			root.appendChild(listenerContainer);
		}

		if (listenerContainer != null) {
			final Element jmsListener = document.createElement("jms:listener");
			jmsListener.setAttribute("ref", StringUtils.uncapitalize(targetType.getSimpleTypeName()));
			jmsListener.setAttribute("method", "onMessage");
			jmsListener.setAttribute("destination", name);

			final Element bean = document.createElement("bean");
			bean.setAttribute("class", targetType.getFullyQualifiedTypeName());
			bean.setAttribute("id", StringUtils.uncapitalize(targetType.getSimpleTypeName()));
			root.appendChild(bean);

			listenerContainer.appendChild(jmsListener);
		}

		fileManager.createOrUpdateTextFileIfRequired(jmsContextPath, XmlUtils.nodeToString(document), false);
	}

	private void updateConfiguration(final JmsProvider jmsProvider) {
		final Element configuration = XmlUtils.getConfiguration(getClass());

		final List<Dependency> dependencies = new ArrayList<Dependency>();

		final List<Element> springDependencies = XmlUtils.findElements("/configuration/springJms/dependencies/dependency", configuration);
		for (final Element dependencyElement : springDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		final List<Element> jmsDependencies = XmlUtils.findElements("/configuration/jmsProviders/provider[@id = '" + jmsProvider.name() + "']/dependencies/dependency", configuration);
		for (final Element dependencyElement : jmsDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		projectOperations.addDependencies(dependencies);
	}

	private void addDefaultDestination(final Document appCtx, final String name) {
		// If we do already have a default destination configured then do nothing
		final Element root = appCtx.getDocumentElement();
		if (null != XmlUtils.findFirstElement("/beans/bean[@class = 'org.springframework.jms.core.JmsTemplate']/property[@name = 'defaultDestination']", root)) {
			return;
		}
		// Otherwise add it
		final Element jmsTemplate = XmlUtils.findRequiredElement("/beans/bean[@class = 'org.springframework.jms.core.JmsTemplate']", root);
		final Element defaultDestination = appCtx.createElement("property");
		defaultDestination.setAttribute("ref", name);
		defaultDestination.setAttribute("name", "defaultDestination");
		jmsTemplate.appendChild(defaultDestination);
	}

	private String getPhysicalLocationCanonicalPath(final String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		final JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
		final Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		final String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return projectOperations.getPathResolver().getIdentifier(path, relativePath);
	}
}