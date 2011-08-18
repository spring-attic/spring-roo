package org.springframework.roo.addon.email;

import static org.springframework.roo.addon.email.MailProtocol.SMTP;
import static org.springframework.roo.model.SpringJavaType.ASYNC;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;
import static org.springframework.roo.model.SpringJavaType.MAIL_SENDER;
import static org.springframework.roo.model.SpringJavaType.SIMPLE_MAIL_MESSAGE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
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
	private static final AnnotatedJavaType STRING = new AnnotatedJavaType(JavaType.STRING_OBJECT);
	
	// Fields
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;

	public boolean isInstallEmailAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public boolean isManageEmailAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(getApplicationContextPath());
	}

	/**
	 * Returns the canonical path of the user project's applicationContext.xml
	 * file.
	 * 
	 * @return a non-blank path
	 */
	private String getApplicationContextPath() {
		return projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
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

		XmlUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(document), false);
		
		if (installDependencies) {
			updateConfiguration();
		}

		propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT, "email.properties", props, true, true);
	}

	public void configureTemplateMessage(final String from, final String subject) {		
		final String contextPath = getApplicationContextPath();
		final Document document = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = document.getDocumentElement();

		final Map<String, String> props = new HashMap<String, String>();

		if (StringUtils.hasText(from) || StringUtils.hasText(subject)) {
			Element smmBean = XmlUtils.findFirstElement("/beans/bean[@class = 'org.springframework.mail.SimpleMailMessage']", root);
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
			
			XmlUtils.removeTextNodes(root);
			
			fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(document), false);
		}

		if (props.size() > 0) {
			propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT, "email.properties", props, true, true);
		}
	}

	public void injectEmailTemplate(final JavaType targetType, final JavaSymbolName fieldName, final boolean async) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");

		final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(AUTOWIRED));

		// Obtain the physical type and its mutable class details
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType);
		final MutableClassOrInterfaceTypeDetails mutableTypeDetails = getMutableClass(declaredByMetadataId);

		// Add the MailSender field
		final FieldMetadataBuilder mailSenderFieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, PRIVATE_TRANSIENT, annotations, fieldName, MAIL_SENDER);
		mutableTypeDetails.addField(mailSenderFieldBuilder.build());
		
		// Add the "sendMessage" method
		mutableTypeDetails.addMethod(getSendMethod(fieldName, async, declaredByMetadataId, mutableTypeDetails));
	}

	/**
	 * Returns the mutable class of the given physical type
	 * 
	 * @param classMetadataId
	 * @return a non-<code>null</code> instance
	 */
	private MutableClassOrInterfaceTypeDetails getMutableClass(final String classMetadataId) {
		final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(classMetadataId);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(classMetadataId));
		final PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(classMetadataId));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(classMetadataId));
		return (MutableClassOrInterfaceTypeDetails) ptd;
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
	private MethodMetadata getSendMethod(final JavaSymbolName mailSenderName, final boolean async, final String targetClassMID, final MutableClassOrInterfaceTypeDetails mutableTypeDetails) {
		final String contextPath = getApplicationContextPath();
		final Document document = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = document.getDocumentElement();

		// Find the existing SimpleMailMessage bean (if any) in applicationContext.xml
		final Element smmBean = XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.mail.SimpleMailMessage']", root);

		// Create some method content to get the user started
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		final List<AnnotationMetadataBuilder> smmAnnotations = new ArrayList<AnnotationMetadataBuilder>();

		// Build a list of the types and names of the "send message" method's parameters
		final PairList<AnnotatedJavaType, JavaSymbolName> parameters = new PairList<AnnotatedJavaType, JavaSymbolName>();

		if (smmBean == null) {
			// Use a local variable for the SimpleMailMessage
			bodyBuilder.appendFormalLine("org.springframework.mail.SimpleMailMessage simpleMailMessage = new org.springframework.mail.SimpleMailMessage();");
			// "From"
			parameters.add(STRING, new JavaSymbolName("mailFrom"));
			bodyBuilder.appendFormalLine("simpleMailMessage.setFrom(mailFrom);");
			// "Subject"
			parameters.add(STRING, new JavaSymbolName("subject"));
			bodyBuilder.appendFormalLine("simpleMailMessage.setSubject(subject);");
		} else {
			smmAnnotations.add(new AnnotationMetadataBuilder(AUTOWIRED));
			final FieldMetadataBuilder smmFieldBuilder = new FieldMetadataBuilder(targetClassMID, PRIVATE_TRANSIENT, smmAnnotations, new JavaSymbolName("simpleMailMessage"), SIMPLE_MAIL_MESSAGE);
			mutableTypeDetails.addField(smmFieldBuilder.build());
		}

		// "To"
		parameters.add(STRING, new JavaSymbolName("mailTo"));
		bodyBuilder.appendFormalLine("simpleMailMessage.setTo(mailTo);");

		// "Message"
		parameters.add(STRING, new JavaSymbolName("message"));
		bodyBuilder.appendFormalLine("simpleMailMessage.setText(message);");

		bodyBuilder.newLine();
		bodyBuilder.appendFormalLine(mailSenderName + ".send(simpleMailMessage);");

		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(targetClassMID, Modifier.PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, parameters.getKeys(), parameters.getValues(), bodyBuilder);
		
		if (async) {
			if (XmlUtils.findFirstElementByName("task:annotation-driven", root) == null) {
				if (root.getAttribute("xmlns:task").length() == 0) {
					root.setAttribute("xmlns:task", "http://www.springframework.org/schema/task");
					root.setAttribute("xsi:schemaLocation", root.getAttribute("xsi:schemaLocation") + "  http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd");
				}
				root.appendChild(new XmlElementBuilder("task:annotation-driven", document).addAttribute("executor", "asyncExecutor").addAttribute("mode", "aspectj").build());
				root.appendChild(new XmlElementBuilder("task:executor", document).addAttribute("id", "asyncExecutor").addAttribute("pool-size", "${executor.poolSize}").build());
				
				fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(document), false);

				propFileOperations.addPropertyIfNotExists(Path.SPRING_CONFIG_ROOT, "email.properties", "executor.poolSize", "10", true);
			}
			methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
		}
		return methodBuilder.build();
	}

	private void updateConfiguration() {
		final Element configuration = XmlUtils.getConfiguration(getClass());

		final List<Dependency> dependencies = new ArrayList<Dependency>();
		final List<Element> emailDependencies = XmlUtils.findElements("/configuration/email/dependencies/dependency", configuration);
		for (final Element dependencyElement : emailDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);
	}
}