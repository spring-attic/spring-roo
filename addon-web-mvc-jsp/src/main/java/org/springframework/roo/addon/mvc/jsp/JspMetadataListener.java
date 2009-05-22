package org.springframework.roo.addon.mvc.jsp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.web.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.controller.WebScaffoldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.NodeList;

/**
 * Listens for {@link WebScaffoldMetadata} and produces JSPs when requested by that metadata.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public final class JspMetadataListener implements MetadataProvider, MetadataNotificationListener {

	private MetadataDependencyRegistry metadataDependencyRegistry;
	private FileManager fileManager;
	private MetadataService metadataService;
	private BeanInfoMetadata beanInfoMetadata;
	private MenuOperations menuOperations;
	
	public JspMetadataListener(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, MenuOperations menuOperations) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(metadataDependencyRegistry, "Metadata dependency registry required");
		Assert.notNull(fileManager, "File manager required");
		this.metadataService = metadataService;
		this.metadataDependencyRegistry = metadataDependencyRegistry;
		this.fileManager = fileManager;
		this.menuOperations = menuOperations;

		metadataService.register(this);
		
		// Ensure we're notified of all metadata related to web scaffold metadata, in particular their initial creation
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		// Work out the MIDs of the other metadata we depend on
		// NB: The JavaType and Path are to the corresponding web scaffold controller class
		JavaType javaType = JspMetadata.getJavaType(metadataIdentificationString);
		Path path = JspMetadata.getPath(metadataIdentificationString);
		String webScaffoldMetadataKey = WebScaffoldMetadata.createIdentifier(javaType, path);
		WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataKey);

		if (webScaffoldMetadata == null || !webScaffoldMetadata.isValid()) {
			// Can't get the corresponding scaffold, so we certainly don't need to manage any JSPs at this time
			return null;
		}

		// Shouldn't be needed, as we get notified for every change to web scaffold metadata anyway
		//metadataDependencyRegistry.registerDependency(webScaffoldMetadataKey, metadataIdentificationString);

		// We need to lookup the metadata for the entity we are creating
		String beanInfoMetadataKey = webScaffoldMetadata.getIdentifierForBeanInfoMetadata();
		String entityMetadataKey = webScaffoldMetadata.getIdentifierForEntityMetadata();
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMetadataKey);
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);

		// We need to abort if we couldn't find dependent metadata
		if (beanInfoMetadata == null || !beanInfoMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid()) {
			// Can't get hold of the entity we are needing to build JSPs for
			return null;
		}
		
		this.beanInfoMetadata = beanInfoMetadata;
		// We need to be informed if our dependent metadata changes
		// Shouldn't need this, as notifications should trickle down through WebScaffoldMetadataKey
//		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
//		metadataDependencyRegistry.registerDependency(webScaffoldMetadataKey, metadataIdentificationString);
		
		JspMetadata md = new JspMetadata(metadataIdentificationString, beanInfoMetadata, webScaffoldMetadata);
		
		if (!md.getAnnotationValues().isAutomaticallyMaintainView()) {
			// we're not maintaining the view, so we have nothing to do
			return md;
		}
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");
		
		PathResolver pathResolver = projectMetadata.getPathResolver();
		
		String imagesDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images");
		if (!fileManager.exists(imagesDirectory)) {
			fileManager.createDirectory(imagesDirectory);
		} 
		String imageFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/banner-graphic.png");
		if (!fileManager.exists(imageFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/banner-graphic.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/banner-graphic.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/springsource-logo.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/springsource-logo.png")).getOutputStream());
				if (webScaffoldMetadata.getAnnotationValues().isList()) { 
					FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/list.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/list.png")).getOutputStream());
				}
				if (webScaffoldMetadata.getAnnotationValues().isShow()) { 
					FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/show.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/show.png")).getOutputStream());
				}
				if (webScaffoldMetadata.getAnnotationValues().isCreate()) { 
					FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/create.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/create.png")).getOutputStream());
				}
				if (webScaffoldMetadata.getAnnotationValues().isUpdate()) { 
					FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/update.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/update.png")).getOutputStream());
				}
				if (webScaffoldMetadata.getAnnotationValues().isDelete()) { 
					FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/delete.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/delete.png")).getOutputStream());
				}
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		} 
		
		String cssDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles");
		if (!fileManager.exists(cssDirectory)) {
			fileManager.createDirectory(cssDirectory);
		} 
		String cssFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles/roo.css");
		if (!fileManager.exists(cssFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/roo.css"), fileManager.createFile(cssFile).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}			
		}
		
		String jspDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp");
		if (!fileManager.exists(jspDirectory)) {
			fileManager.createDirectory(jspDirectory);
		} 
		String headerFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/header.jsp");
		if (!fileManager.exists(headerFile)) {
			try {				
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "header.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/header.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "footer.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/footer.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "includes.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/includes.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "dataAccessFailure.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/dataAccessFailure.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "uncaughtException.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/uncaughtException.jsp")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}			
		}

		List<FieldMetadata> elegibleFields = getElegibleFields();
		
		JspDocumentHelper helper = new JspDocumentHelper(metadataService, elegibleFields, beanInfoMetadata, entityMetadata, projectMetadata.getProjectName());
		// Make the holding directory for this controller
		String destinationDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase());
		if (!fileManager.exists(destinationDirectory)) {
			fileManager.createDirectory(destinationDirectory);
		} else {
			File file = new File(destinationDirectory);
			Assert.isTrue(file.isDirectory(), destinationDirectory + " is a file, when a directory was expected");
		}
		
		if (webScaffoldMetadata.getAnnotationValues().isList()) {
			// By now we have a directory to put the JSPs inside
			String listPath = destinationDirectory + "/list.jsp";
			writeToDiskIfNecessary(listPath, helper.getListDocument().getFirstChild().getChildNodes());
		} 
		if (webScaffoldMetadata.getAnnotationValues().isShow()) {
			// By now we have a directory to put the JSPs inside
			String listPath = destinationDirectory + "/show.jsp";
			writeToDiskIfNecessary(listPath, helper.getShowDocument().getFirstChild().getChildNodes());
		}
		if (webScaffoldMetadata.getAnnotationValues().isCreate()) {
			// By now we have a directory to put the JSPs inside
			String listPath = destinationDirectory + "/create.jsp";
			writeToDiskIfNecessary(listPath, helper.getCreateDocument().getFirstChild().getChildNodes());
		}
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			// By now we have a directory to put the JSPs inside
			String listPath = destinationDirectory + "/update.jsp";
			writeToDiskIfNecessary(listPath, helper.getUpdateDocument().getFirstChild().getChildNodes());
		}

		//Add 'list all' menu item
		menuOperations.addMenuItem(
				"web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", 
				beanInfoMetadata.getJavaBean().getSimpleTypeName(), 
				"web_mvc_jsp_list_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_menu_item", 
				"List all " + entityMetadata.getPlural(),
				"/" + projectMetadata.getProjectName() + "/" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase());
		
		//add 'create new' menu item
		menuOperations.addMenuItem(
				"web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", 
				beanInfoMetadata.getJavaBean().getSimpleTypeName(), 
				"web_mvc_jsp_create_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_menu_item", 
				"Create new " + beanInfoMetadata.getJavaBean().getSimpleTypeName(),
				"/" + projectMetadata.getProjectName() + "/" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "/form");
		
		return md;
	}
	
	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(String jspFilename, NodeList toWrite) {
		// Build a string representation of the JSP
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		XmlUtils.writeMalformedXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, toWrite);
		String jspContent = byteArrayOutputStream.toString();
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(jspFilename)) {
			// First verify if the file has even changed
			File f = new File(jspFilename);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {}
			
			if (!jspContent.equals(existing)) {
				mutableFile = fileManager.updateFile(jspFilename);
			}
			
		} else {
			mutableFile = fileManager.createFile(jspFilename);
			Assert.notNull(mutableFile, "Could not create JSP file '" + jspFilename + "'");
		}
		
		try {
			if (mutableFile != null) {
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
		
		// A file existed, but it contained the same content, so we return false
		return false;

	}
	
	private List<FieldMetadata> getElegibleFields() {
		List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		for (MethodMetadata method : beanInfoMetadata.getPublicAccessors(false)) {
			JavaSymbolName propertyName = beanInfoMetadata.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);
			
			if(field != null && hasMutator(field)) {
				
				// Never include id field (it shouldn't normally have a mutator anyway, but the user might have added one)
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Id")) != null) {
					continue;
				}
				// Never include version field (it shouldn't normally have a mutator anyway, but the user might have added one)
				if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Version")) != null) {
					continue;
				}				
				fields.add(field);
			}
		}
		return fields;
	}	
	
	private boolean hasMutator(FieldMetadata fieldMetadata) {
		for (MethodMetadata mutator : beanInfoMetadata.getPublicMutators()) {
			if (fieldMetadata.equals(beanInfoMetadata.getFieldForPropertyName(beanInfoMetadata.getPropertyNameForJavaBeanMethod(mutator)))) return true;
		}
		return false;
	}
	
	public void notify(String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(WebScaffoldMetadata.getMetadataIdentiferType())), "Expected class-level notifications only for web scaffold metadata (not '" + upstreamDependency + "')");
			
			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType javaType = WebScaffoldMetadata.getJavaType(upstreamDependency);
			Path path = WebScaffoldMetadata.getPath(upstreamDependency);
			downstreamDependency = JspMetadata.createIdentifier(javaType, path);
			
			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");
		
		metadataService.evict(downstreamDependency);
		if (get(downstreamDependency) != null) {
			metadataDependencyRegistry.notifyDownstream(downstreamDependency);
		}
	}
	
	public String getProvidesType() {
		return JspMetadata.getMetadataIdentiferType();
	}
}