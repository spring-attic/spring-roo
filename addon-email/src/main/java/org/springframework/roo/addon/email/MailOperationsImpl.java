package org.springframework.roo.addon.email;

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
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides email configuration operations.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component 
@Service 
public class MailOperationsImpl implements MailOperations {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;

	public boolean isInstallEmailAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public boolean isManageEmailAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml"));
	}

	public void installEmail(String hostServer, MailProtocol protocol, String port, String encoding, String username, String password) {
		Assert.hasText(hostServer, "Host server name required");

		Map<String, String> props = new HashMap<String, String>();
		
		String contextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		MutableFile contextMutableFile;
		Document appCtx;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) appCtx.getFirstChild();

		boolean installDependencies = true;

		Element mailBean = XmlUtils.findFirstElement("/beans/bean[@class = 'org.springframework.mail.javamail.JavaMailSenderImpl']", root);
		if (mailBean != null) {
			root.removeChild(mailBean);
			installDependencies = false;
		}

		mailBean = appCtx.createElement("bean");
		mailBean.setAttribute("class", "org.springframework.mail.javamail.JavaMailSenderImpl");
		mailBean.setAttribute("id", "mailSender");

		Element property = appCtx.createElement("property");
		property.setAttribute("name", "host");
		property.setAttribute("value", "${email.host}");
		mailBean.appendChild(property);
		root.appendChild(mailBean);
		props.put("email.host", hostServer);

		if (protocol != null) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("value", "${email.protocol}");
			pElement.setAttribute("name", "protocol");
			mailBean.appendChild(pElement);
			props.put("email.protocol", protocol.getProtocol());
		}

		if (StringUtils.hasText(port)) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "port");
			pElement.setAttribute("value", "${email.port}");
			mailBean.appendChild(pElement);
			props.put("email.port", port);
		}

		if (StringUtils.hasText(encoding)) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "defaultEncoding");
			pElement.setAttribute("value", "${email.encoding}");
			mailBean.appendChild(pElement);
			props.put("email.encoding", encoding);
		}

		if (StringUtils.hasText(username)) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "username");
			pElement.setAttribute("value", "${email.username}");
			mailBean.appendChild(pElement);
			props.put("email.username", username);
		}

		if (StringUtils.hasText(password)) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "password");
			pElement.setAttribute("value", "${email.password}");
			mailBean.appendChild(pElement);
			props.put("email.password", password);

			if (MailProtocol.SMTP.equals(protocol)) {
				Element javaMailProperties = appCtx.createElement("property");
				javaMailProperties.setAttribute("name", "javaMailProperties");
				Element securityProps = appCtx.createElement("props");
				javaMailProperties.appendChild(securityProps);
				Element prop = appCtx.createElement("prop");
				prop.setAttribute("key", "mail.smtp.auth");
				prop.setTextContent("true");
				securityProps.appendChild(prop);
				Element prop2 = appCtx.createElement("prop");
				prop2.setAttribute("key", "mail.smtp.starttls.enable");
				prop2.setTextContent("true");
				securityProps.appendChild(prop2);
				mailBean.appendChild(javaMailProperties);
			}
		}

		XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);

		if (installDependencies) {
			updateConfiguration();
		}

		propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT, "email.properties", props, true, false);
	}

	public void configureTemplateMessage(String from, String subject) {
		Map<String, String> props = new HashMap<String, String>();
		
		String contextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		MutableFile contextMutableFile;
		Document appCtx;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not aquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) appCtx.getFirstChild();

		if (StringUtils.hasText(from) || StringUtils.hasText(subject)) {
			Element smmBean = XmlUtils.findFirstElement("/beans/bean[@class = 'org.springframework.mail.SimpleMailMessage']", root);
			if (smmBean == null) {
				smmBean = appCtx.createElement("bean");
				smmBean.setAttribute("class", "org.springframework.mail.SimpleMailMessage");
				smmBean.setAttribute("id", "templateMessage");
			}

			if (StringUtils.hasText(from)) {
				Element smmProperty = XmlUtils.findFirstElement("//property[@name='from']", smmBean);
				if (smmProperty != null) {
					smmBean.removeChild(smmProperty);
				}
				smmProperty = appCtx.createElement("property");
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
				smmProperty = appCtx.createElement("property");
				smmProperty.setAttribute("value", "${email.subject}");
				smmProperty.setAttribute("name", "subject");
				smmBean.appendChild(smmProperty);
				props.put("email.subject", subject);
			}

			root.appendChild(smmBean);

			XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);
		}

		if (props.size() > 0) {
			propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT, "email.properties", props, true, false);
		}
	}

	public void injectEmailTemplate(JavaType targetType, JavaSymbolName fieldName, boolean async) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));

		// Obtain the physical type and itd mutable details
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(declaredByMetadataId));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(declaredByMetadataId));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(declaredByMetadataId));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		int modifier = Modifier.PRIVATE | Modifier.TRANSIENT;
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, modifier, annotations, fieldName, new JavaType("org.springframework.mail.MailSender"));
		mutableTypeDetails.addField(fieldBuilder.build());

		String contextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		MutableFile contextMutableFile;
		Document appCtx;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not aquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) appCtx.getFirstChild();

		Element smmBean = XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.mail.SimpleMailMessage']", root);

		// Create some method content to get the user started
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		List<AnnotationMetadataBuilder> smmAnnotations = new ArrayList<AnnotationMetadataBuilder>();

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();

		if (smmBean != null) {
			smmAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
			FieldMetadataBuilder smmFieldBuilder = new FieldMetadataBuilder(declaredByMetadataId, modifier, smmAnnotations, new JavaSymbolName("simpleMailMessage"), new JavaType("org.springframework.mail.SimpleMailMessage"));
			mutableTypeDetails.addField(smmFieldBuilder.build());
		} else {
			bodyBuilder.appendFormalLine("org.springframework.mail.SimpleMailMessage simpleMailMessage = new org.springframework.mail.SimpleMailMessage();");
			paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, new ArrayList<AnnotationMetadata>()));
			paramNames.add(new JavaSymbolName("mailFrom"));
			bodyBuilder.appendFormalLine("simpleMailMessage.setFrom(mailFrom);");

			paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, new ArrayList<AnnotationMetadata>()));
			paramNames.add(new JavaSymbolName("subject"));
			bodyBuilder.appendFormalLine("simpleMailMessage.setSubject(subject);");
		}

		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, new ArrayList<AnnotationMetadata>()));
		paramNames.add(new JavaSymbolName("mailTo"));
		bodyBuilder.appendFormalLine("simpleMailMessage.setTo(mailTo);");

		paramTypes.add(new AnnotatedJavaType(JavaType.STRING_OBJECT, new ArrayList<AnnotationMetadata>()));
		paramNames.add(new JavaSymbolName("message"));
		bodyBuilder.appendFormalLine("simpleMailMessage.setText(message);");

		bodyBuilder.newLine();
		bodyBuilder.appendFormalLine(fieldName + ".send(simpleMailMessage);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		
		if (async) {
			if (XmlUtils.findFirstElementByName("task:annotation-driven", root) == null) {
				if (root.getAttribute("xmlns:task").length() == 0) {
					root.setAttribute("xmlns:task", "http://www.springframework.org/schema/task");
					root.setAttribute("xsi:schemaLocation", root.getAttribute("xsi:schemaLocation") + "  http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd");
				}
				root.appendChild(new XmlElementBuilder("task:annotation-driven", appCtx).addAttribute("executor", "asyncExecutor").addAttribute("mode", "aspectj").build());
				root.appendChild(new XmlElementBuilder("task:executor", appCtx).addAttribute("id", "asyncExecutor").addAttribute("pool-size", "${executor.poolSize}").build());
				XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), contextMutableFile.getOutputStream(), appCtx);
				propFileOperations.addPropertyIfNotExists(Path.SPRING_CONFIG_ROOT, "email.properties", "executor.poolSize", "10", true);
			}
			methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.scheduling.annotation.Async")));
		}
		
		mutableTypeDetails.addMethod(methodBuilder.build());
	}

	private void updateConfiguration() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> emailDependencies = XmlUtils.findElements("/configuration/email/dependencies/dependency", configuration);
		for (Element dependencyElement : emailDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(dependencies);
	}
}