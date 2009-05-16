package org.springframework.roo.addon.email;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
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
		property.setAttribute("value", hostServer);		
		mailBean.appendChild(property);
		root.appendChild(mailBean);	
		
		if (protocol != null) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "protocol");
			pElement.setAttribute("value", protocol.getProtocol());		
			mailBean.appendChild(pElement);
		}
		
		if (port != null && port.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "port");
			pElement.setAttribute("value", port);		
			mailBean.appendChild(pElement);
		}
		
		if (encoding != null && encoding.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "encoding");
			pElement.setAttribute("value", encoding);		
			mailBean.appendChild(pElement);
		}
		
		if (username != null && username.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "username");
			pElement.setAttribute("value", username);		
			mailBean.appendChild(pElement);
		}
		
		if (password != null && password.length() > 0) {
			Element pElement = appCtx.createElement("property");
			pElement.setAttribute("name", "password");
			pElement.setAttribute("value", password);		
			mailBean.appendChild(pElement);
			
			if(MailProtocol.SMTP.equals(protocol)) {
				Element javaMailProperties = appCtx.createElement("property");
				javaMailProperties.setAttribute("name", "javaMailProperties");
				Element props = appCtx.createElement("props");
				javaMailProperties.appendChild(props);
				Element prop = appCtx.createElement("prop");
				prop.setAttribute("key", "mail.smtp.auth");
				prop.setTextContent("true");
				props.appendChild(prop);
				Element prop2 = appCtx.createElement("prop");
				prop2.setAttribute("key", "mail.smtp.starttls.enable");
				prop2.setTextContent("true");
				props.appendChild(prop2);			
				mailBean.appendChild(javaMailProperties);
			}
		}
				
		XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);			
		
		if (installDependencies) {
			updateDependencies();
		}
	}	
	
	public void configureTemplateMessage(String from, String subject) {		
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
				smmProperty.setAttribute("value", from);
				smmProperty.setAttribute("name", "from");
				smmBean.appendChild(smmProperty);
			}
			
			if (null != subject && subject.length() > 0) {
				Element smmProperty = XmlUtils.findFirstElement("//property[@name='subject']", root);
				if (smmProperty != null) {
					smmBean.removeChild(smmProperty);
				}
				smmProperty = appCtx.createElement("property");
				smmProperty.setAttribute("value", subject);
				smmProperty.setAttribute("name", "subject");
				smmBean.appendChild(smmProperty);
			}
			
			root.appendChild(smmBean);
			
			XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);
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
		
		List<AnnotationMetadata> smmAnnotations = new ArrayList<AnnotationMetadata>();
		if (smmBean != null) {			
			smmAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));		
		}
		FieldMetadata smmFieldMetadata = new DefaultFieldMetadata(PhysicalTypeIdentifier.createIdentifier(targetType, Path.SRC_MAIN_JAVA), modifier, new JavaSymbolName("message"), new JavaType("org.springframework.mail.SimpleMailMessage"), null, smmAnnotations);
		mutableTypeDetails.addField(smmFieldMetadata);
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