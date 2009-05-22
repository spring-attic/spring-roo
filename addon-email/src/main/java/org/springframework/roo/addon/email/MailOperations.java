package org.springframework.roo.addon.email;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
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
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Provides email configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class MailOperations {
	
	Logger logger = Logger.getLogger(MailOperations.class.getName());
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	
	public MailOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.projectOperations = projectOperations;
	}
	
	public boolean isInstallEmailAvailable() {		
		return getPathResolver() != null;
	}
	
	public boolean isManageEmailAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext.xml"));
	}
	
	public void installEmail(String hostServer, MailProtocol protocol, String port, String encoding, String username, String password) {
		Assert.hasText(hostServer, "Host server name required");		
		
		String emailPropsPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "email.properties");
		MutableFile databaseMutableFile = null;
		
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(emailPropsPath)) {
				databaseMutableFile = fileManager.updateFile(emailPropsPath);
				props.load(databaseMutableFile.getInputStream());
			} else {
				databaseMutableFile = fileManager.createFile(emailPropsPath);
				props.load(databaseMutableFile.getInputStream());
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		String contextPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext.xml");
		MutableFile contextMutableFile = null;
		
		Document appCtx = null;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				new IllegalStateException("Could not aquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		Element root = (Element) appCtx.getFirstChild();
		
		boolean installDependencies = true;
		
		Element mailBean = XmlUtils.findFirstElement("//bean[@class='org.springframework.mail.javamail.JavaMailSenderImpl']", root);
		
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
		
		if (port != null && port.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "port");
			pElement.setAttribute("value", "${email.port}");		
			mailBean.appendChild(pElement);
			props.put("email.port", port);
		}
		
		if (encoding != null && encoding.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "encoding");
			pElement.setAttribute("value", "${email.encoding}");		
			mailBean.appendChild(pElement);
			props.put("email.encoding", encoding);
		}
		
		if (username != null && username.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "username");
			pElement.setAttribute("value", "${email.username}");		
			mailBean.appendChild(pElement);
			props.put("email.username", username);
		}
		
		if (password != null && password.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "password");
			pElement.setAttribute("value", "${email.password}");		
			mailBean.appendChild(pElement);
			props.put("email.password", password);
			
			if(MailProtocol.SMTP.equals(protocol)) {
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
			updateDependencies();
		}
		
		try {
			props.store(databaseMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}	
	
	public void configureTemplateMessage(String from, String subject) {		
		
		String emailPropsPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "email.properties");
		MutableFile databaseMutableFile = null;
		
		Properties props = new Properties();
		
		try {
			if (fileManager.exists(emailPropsPath)) {
				databaseMutableFile = fileManager.updateFile(emailPropsPath);
				props.load(databaseMutableFile.getInputStream());
			} else {
				databaseMutableFile = fileManager.createFile(emailPropsPath);
				props.load(databaseMutableFile.getInputStream());
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}	
		
		String contextPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext.xml");
		MutableFile contextMutableFile = null;
		
		Document appCtx = null;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				new IllegalStateException("Could not aquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		Element root = (Element) appCtx.getFirstChild();
		
		if ((null != from && from.length() > 0) || (null != subject && subject.length() > 0)) {
			Element smmBean = XmlUtils.findFirstElement("//bean[@class='org.springframework.mail.SimpleMailMessage']", root);
			
			if (smmBean == null) {
				smmBean = appCtx.createElement("bean");
				smmBean.setAttribute("class", "org.springframework.mail.SimpleMailMessage");
				smmBean.setAttribute("id", "templateMessage");
			}
			
			if (null != from && from.length() > 0) {
				Element smmProperty = XmlUtils.findFirstElement("//property[@name='from']", root);
				if (smmProperty != null) {
					smmBean.removeChild(smmProperty);
				}
				smmProperty = appCtx.createElement("property");
				smmProperty.setAttribute("value", "${email.from}");
				smmProperty.setAttribute("name", "from");
				smmBean.appendChild(smmProperty);
				props.put("email.from", from);
			}
			
			if (null != subject && subject.length() > 0) {
				Element smmProperty = XmlUtils.findFirstElement("//property[@name='subject']", root);
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
		
		try {
			props.store(databaseMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
	
	public void injectEmailTemplate(JavaType targetType, JavaSymbolName fieldName) {
		Assert.notNull(targetType, "Java type required");
		Assert.notNull(fieldName, "Field name required");
		
		int modifier = Modifier.PRIVATE;
		modifier |= Modifier.TRANSIENT;
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));		
		FieldMetadata fieldMetadata = new DefaultFieldMetadata(PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA), modifier, fieldName, new JavaType("org.springframework.mail.MailSender"), null, annotations);
		
		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(fieldMetadata.getDeclaredByMetadataId());
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;
		
		mutableTypeDetails.addField(fieldMetadata);
		
		String contextPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext.xml");
		MutableFile contextMutableFile = null;
		
		Document appCtx = null;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				new IllegalStateException("Could not aquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		Element root = (Element) appCtx.getFirstChild();		
		
		Element smmBean = XmlUtils.findFirstElement("//bean[@class='org.springframework.mail.SimpleMailMessage']", root);
		
		//create some method content to get the user started			
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA);
		List<AnnotationMetadata> smmAnnotations = new ArrayList<AnnotationMetadata>();
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		
		if (smmBean != null) {			
			smmAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));		
			FieldMetadata smmFieldMetadata = new DefaultFieldMetadata(PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA), modifier, new JavaSymbolName("simpleMailMessage"), new JavaType("org.springframework.mail.SimpleMailMessage"), null, smmAnnotations);
			mutableTypeDetails.addField(smmFieldMetadata);			
		} else {							
			bodyBuilder.appendFormalLine("org.springframework.mail.SimpleMailMessage simpleMailMessage = new org.springframework.mail.SimpleMailMessage();");
			paramTypes.add(new AnnotatedJavaType(new JavaType(String.class.getName()), new ArrayList<AnnotationMetadata>()));
			paramNames.add(new JavaSymbolName("mailFrom"));
			bodyBuilder.appendFormalLine("simpleMailMessage.setFrom(mailFrom);");
			
			paramTypes.add(new AnnotatedJavaType(new JavaType(String.class.getName()), new ArrayList<AnnotationMetadata>()));
			paramNames.add(new JavaSymbolName("subject"));			
			bodyBuilder.appendFormalLine("simpleMailMessage.setSubject(subject);");
		}
		
		paramTypes.add(new AnnotatedJavaType(new JavaType(String.class.getName()), new ArrayList<AnnotationMetadata>()));
		paramNames.add(new JavaSymbolName("mailTo"));
		bodyBuilder.appendFormalLine("simpleMailMessage.setTo(mailTo);");
		
		paramTypes.add(new AnnotatedJavaType(new JavaType(String.class.getName()), new ArrayList<AnnotationMetadata>()));
		paramNames.add(new JavaSymbolName("message"));		
		bodyBuilder.appendFormalLine("simpleMailMessage.setText(message);");
		
		bodyBuilder.newLine();
		bodyBuilder.appendFormalLine(fieldName + ".send(simpleMailMessage);");				
		
		mutableTypeDetails.addMethod(new DefaultMethodMetadata(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("sendMessage"), JavaType.VOID_PRIMITIVE, paramTypes, paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput()));
	}
	
	private void updateDependencies() {		

		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dependenciesElement = (Element) dependencyDoc.getFirstChild();
		
		List<Element> dependencies = XmlUtils.findElements("/dependencies/email/dependency", dependenciesElement);
		for(Element dependency : dependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
	}	
	
	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
}