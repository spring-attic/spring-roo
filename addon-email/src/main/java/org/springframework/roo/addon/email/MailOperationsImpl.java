package org.springframework.roo.addon.email;

import static org.springframework.roo.addon.email.MailProtocol.SMTP;
import static org.springframework.roo.model.SpringJavaType.ASYNC;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.MAIL_SENDER;
import static org.springframework.roo.model.SpringJavaType.SIMPLE_MAIL_MESSAGE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.PairList;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link MailOperationsImpl}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class MailOperationsImpl implements MailOperations {

	// Constants
	private static final int PRIVATE_TRANSIENT = Modifier.PRIVATE | Modifier.TRANSIENT;
	private static final AnnotatedJavaType STRING = new AnnotatedJavaType(JavaType.STRING);
	private static final String LOCAL_MESSAGE_VARIABLE = "mailMessage";
	private static final String SPRING_TASK_NS = "http://www.springframework.org/schema/task";
	private static final String SPRING_TASK_XSD = "http://www.springframework.org/schema/task/spring-task-3.0.xsd";
	private static final String TEMPLATE_MESSAGE_FIELD = "templateMessage";
	private static final Logger log = HandlerUtils.getLogger(MailOperationsImpl.class);

	// Fields
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	@Reference private TypeManagementService typeManagementService;
	@Reference private TypeLocationService typeLocationService;

	public boolean isInstallEmailAvailable() {
		return projectOperations.isFocusedProjectAvailable();
	}

	public boolean isManageEmailAvailable() {
		return projectOperations.isFocusedProjectAvailable() && fileManager.exists(getApplicationContextPath());
	}

	/**
	 * Returns the canonical path of the user project's applicationContext.xml
	 * file.
	 *
	 * @return a non-blank path
	 */
	private String getApplicationContextPath() {
		return pathResolver.getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
	}

	public void installEmail(final String hostServer, final MailProtocol protocol, final String port, final String encoding, final String username, final String password) {
		Assert.hasText(hostServer, "Host server name required");

		final String contextPath = getApplicationContextPath();
		final Document document = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = document.getDocumentElement();

		boolean installDependencies = true;
		final Map<String, String> props = new HashMap<String, String>();

		Element mailBean = XmlUtils.findFirstElement("/beans/bean[@class = 'org.springframework.mail.javamail.JavaMailSenderImpl']", root);
		if (mailBean != null) {
			root.removeChild(mailBean);
			installDependencies = false;
		}

		mailBean = document.createElement("bean");
		mailBean.setAttribute("class", "org.springframework.mail.javamail.JavaMailSenderImpl");
		mailBean.setAttribute("id", "mailSender");

		final Element property = document.createElement("property");
		property.setAttribute("name", "host");
		property.setAttribute("value", "${email.host}");
		mailBean.appendChild(property);
		root.appendChild(mailBean);
		props.put("email.host", hostServer);

		if (protocol != null) {
			final Element pElement = document.createElement("property");
			pElement.setAttribute("value", "${email.protocol}");
			pElement.setAttribute("name", "protocol");
			mailBean.appendChild(pElement);
			props.put("email.protocol", protocol.getProtocol());
		}

		if (StringUtils.hasText(port)) {
			final Element pElement = document.createElement("property");
			pElement.setAttribute("name", "port");
			pElement.setAttribute("value", "${email.port}");
			mailBean.appendChild(pElement);
			props.put("email.port", port);
		}

		if (StringUtils.hasText(encoding)) {
			final Element pElement = document.createElement("property");
			pElement.setAttribute("name", "defaultEncoding");
			pElement.setAttribute("value", "${email.encoding}");
			mailBean.appendChild(pElement);
			props.put("email.encoding", encoding);
		}

		if (StringUtils.hasText(username)) {
			final Element pElement = document.createElement("property");
			pElement.setAttribute("name", "username");
			pElement.setAttribute("value", "${email.username}");
			mailBean.appendChild(pElement);
			props.put("email.username", username);
		}

		if (StringUtils.hasText(password)) {
			final Element pElement = document.createElement("property");
			pElement.setAttribute("name", "password");
			pElement.setAttribute("value", "${email.password}");
			mailBean.appendChild(pElement);
			props.put("email.password", password);

			if (SMTP.equals(protocol)) {
				final Element javaMailProperties = document.createElement("property");
				javaMailProperties.setAttribute("name", "javaMailProperties");
				final Element securityProps = document.createElement("props");
				javaMailProperties.appendChild(securityProps);
				final Element prop = document.createElement("prop");
				prop.setAttribute("key", "mail.smtp.auth");
				prop.setTextContent("true");
				securityProps.appendChild(prop);
				final Element prop2 = document.createElement("prop");
				prop2.setAttribute("key", "mail.smtp.starttls.enable");
				prop2.setTextContent("true");
				securityProps.appendChild(prop2);
				mailBean.appendChild(javaMailProperties);
			}
		}

		DomUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(document), false);

		if (installDependencies) {
			updateConfiguration(projectOperations.getFocusedModuleName());
		}

		propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT.contextualize(projectOperations.getFocusedModuleName()), "email.properties", props, true, true);
	}

	public void configureTemplateMessage(final String from, final String subject) {
		final String contextPath = getApplicationContextPath();
		final Document document = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = document.getDocumentElement();

		final Map<String, String> props = new HashMap<String, String>();

		if (StringUtils.hasText(from) || StringUtils.hasText(subject)) {
			Element smmBean = getSimpleMailMessageBean(root);
			if (smmBean == null) {
				smmBean = document.createElement("bean");
				smmBean.setAttribute("class", "org.springframework.mail.SimpleMailMessage");
				smmBean.setAttribute("id", "templateMessage");
			}

			if (StringUtils.hasText(from)) {
				Element smmProperty = XmlUtils.findFirstElement("//property[@name='from']", smmBean);
				if (smmProperty != null) {
					smmBean.removeChild(smmProperty);
				}
				smmProperty = document.createElement("property");
				smmProperty.setAttribute("value", "${email.from}");
				smmProperty.setAttribute("name", "from");
				smmBean.appendChild(smmProperty);
				props.put("email.from", from);
			}

			if (StringUtils.hasText(subject)) {
				Element smmProperty = XmlUtils.findFirstElement("//property[@name='subject']", smmBean);
				if (smmProperty != null) {
					smmBean.removeChild(smmProperty);
				}
				smmProperty = document.createElement("property");
				smmProperty.setAttribute("value", "${email.subject}");
				smmProperty.setAttribute("name", "subject");
				smmBean.appendChild(smmProperty);
				props.put("email.subject", subject);
			}

			root.appendChild(smmBean);

			DomUtils.removeTextNodes(root);

			fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(document), false);
		}

		if (props.size() > 0) {
			propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT.contextualize(projectOperations.getPomManagementService().getFocusedModuleName()), "email.properties", props, true, true);
		}
	}

	/**
	 * Finds the SimpleMailMessage bean in the Spring XML file with the given
	 * root element
	 *
	 * @param root
	 * @return <code>null</code> if there is no such bean
	 */
	private Element getSimpleMailMessageBean(final Element root) {
		return XmlUtils.findFirstElement("/beans/bean[@class = 'org.springframework.mail.SimpleMailMessage']", root);
	}

	public void injectEmailTemplate(final JavaType targetType, final JavaSymbolName fieldName, final boolean async) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");

		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));

		// Obtain the physical type and its mutable class details
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType);
		ClassOrInterfaceTypeDetails existing = typeLocationService.getTypeDetails(targetType);
		if (existing == null) {
			log.warning("Aborting: Unable to find metadata for target type '" + targetType.getFullyQualifiedTypeName() + "'");
			return;
		}
		final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(existing);

		// Add the MailSender field
		final FieldMetadataBuilder mailSenderFieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, PRIVATE_TRANSIENT, annotations, fieldName, MAIL_SENDER);
		classOrInterfaceTypeDetailsBuilder.addField(mailSenderFieldBuilder.build());

		// Add the "sendMessage" method
		classOrInterfaceTypeDetailsBuilder.addMethod(getSendMethod(fieldName, async, declaredByMetadataId, classOrInterfaceTypeDetailsBuilder));
		typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
	}

	/**
	 * Generates the "send email" method to be added to the domain type
	 *
	 * @param mailSenderName the name of the MailSender field (required)
	 * @param async whether to send the email asynchronously
	 * @param targetClassMID the MID of the class to receive the method
	 * @param mutableTypeDetails the type to which the method is being added (required)
	 * @return a non-<code>null</code> method
	 */
	private MethodMetadata getSendMethod(final JavaSymbolName mailSenderName, final boolean async, final String targetClassMID, final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder) {
		final String contextPath = getApplicationContextPath();
		final Document document = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = document.getDocumentElement();

		// Make a builder for the created method's body
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		// Collect the types and names of the created method's parameters
		final PairList<AnnotatedJavaType, JavaSymbolName> parameters = new PairList<AnnotatedJavaType, JavaSymbolName>();

		if (getSimpleMailMessageBean(root) == null) {
			// There's no SimpleMailMessage bean; use a local variable
			bodyBuilder.appendFormalLine("org.springframework.mail.SimpleMailMessage " + LOCAL_MESSAGE_VARIABLE	+ " = new org.springframework.mail.SimpleMailMessage();");
			// Set the from address
			parameters.add(STRING, new JavaSymbolName("mailFrom"));
			bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE	+ ".setFrom(mailFrom);");
			// Set the subject
			parameters.add(STRING, new JavaSymbolName("subject"));
			bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE	+ ".setSubject(subject);");
		} else {
			// A SimpleMailMessage bean exists; auto-wire it into the entity and use it as a template
			final List<AnnotationMetadataBuilder> smmAnnotations = Arrays.asList(new AnnotationMetadataBuilder(AUTOWIRED));
			final FieldMetadataBuilder smmFieldBuilder = new FieldMetadataBuilder(targetClassMID, PRIVATE_TRANSIENT, smmAnnotations, new JavaSymbolName(TEMPLATE_MESSAGE_FIELD), SIMPLE_MAIL_MESSAGE);
			classOrInterfaceTypeDetailsBuilder.addField(smmFieldBuilder.build());
			// Use the injected bean as a template (for thread safety)
			bodyBuilder.appendFormalLine("org.springframework.mail.SimpleMailMessage " + LOCAL_MESSAGE_VARIABLE	+ " = new org.springframework.mail.SimpleMailMessage(" + TEMPLATE_MESSAGE_FIELD + ");");
		}

		// Set the to address
		parameters.add(STRING, new JavaSymbolName("mailTo"));
		bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE	+ ".setTo(mailTo);");

		// Set the message body
		parameters.add(STRING, new JavaSymbolName("message"));
		bodyBuilder.appendFormalLine(LOCAL_MESSAGE_VARIABLE	+ ".setText(message);");

		bodyBuilder.newLine();
		bodyBuilder.appendFormalLine(mailSenderName + ".send(" + LOCAL_MESSAGE_VARIABLE + ");");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(targetClassMID, Modifier.PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, parameters.getKeys(), parameters.getValues(), bodyBuilder);

		if (async) {
			if (DomUtils.findFirstElementByName("task:annotation-driven", root) == null) {
				// Add asynchronous email support to the application
				if (!StringUtils.hasText(root.getAttribute("xmlns:task"))) {
					// Add the "task" namespace to the Spring config file
					root.setAttribute("xmlns:task", SPRING_TASK_NS);
					root.setAttribute("xsi:schemaLocation", root.getAttribute("xsi:schemaLocation") + "  " + SPRING_TASK_NS + " " + SPRING_TASK_XSD);
				}
				root.appendChild(new XmlElementBuilder("task:annotation-driven", document).addAttribute("executor", "asyncExecutor").addAttribute("mode", "aspectj").build());
				root.appendChild(new XmlElementBuilder("task:executor", document).addAttribute("id", "asyncExecutor").addAttribute("pool-size", "${executor.poolSize}").build());
				// Write out the new Spring config file
				fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(document), false);
				// Update the email properties file
				propFileOperations.addPropertyIfNotExists(pathResolver.getFocusedPath(Path.SPRING_CONFIG_ROOT), "email.properties", "executor.poolSize", "10", true);
			}
			methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
		}
		return methodBuilder.build();
	}

	private void updateConfiguration(String moduleName) {
		final Element configuration = XmlUtils.getConfiguration(getClass());

		final List<Dependency> dependencies = new ArrayList<Dependency>();
		final List<Element> emailDependencies = XmlUtils.findElements("/configuration/email/dependencies/dependency", configuration);
		for (final Element dependencyElement : emailDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(moduleName, dependencies);
	}
}