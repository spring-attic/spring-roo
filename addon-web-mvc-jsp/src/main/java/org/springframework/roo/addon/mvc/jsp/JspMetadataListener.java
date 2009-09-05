package org.springframework.roo.addon.mvc.jsp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
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
	private JspOperations jspOperations;
	private PathResolver pathResolver;
	
	public JspMetadataListener(MetadataService metadataService, MetadataDependencyRegistry metadataDependencyRegistry, FileManager fileManager, PathResolver pathResolver, MenuOperations menuOperations) {
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(metadataDependencyRegistry, "Metadata dependency registry required");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(menuOperations, "Menu Operations required");
		Assert.notNull(pathResolver, "Path resolver required");
		this.metadataService = metadataService;
		this.metadataDependencyRegistry = metadataDependencyRegistry;
		this.fileManager = fileManager;
		this.menuOperations = menuOperations;		
		this.pathResolver = pathResolver;
		
		metadataService.register(this);
		
		// Ensure we're notified of all metadata related to web scaffold metadata, in particular their initial creation
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
	
		jspOperations = new JspOperations(fileManager, metadataService);
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
		
		String finderMetadataKey = FinderMetadata.createIdentifier(EntityMetadata.getJavaType(entityMetadataKey), path);
		FinderMetadata finderMetadata = (FinderMetadata) metadataService.get(finderMetadataKey);

		this.beanInfoMetadata = beanInfoMetadata;
		// We need to be informed if our dependent metadata changes
		// Shouldn't need this, as notifications should trickle down through WebScaffoldMetadataKey
//		metadataDependencyRegistry.registerDependency(beanInfoMetadataKey, metadataIdentificationString);
//		metadataDependencyRegistry.registerDependency(webScaffoldMetadataKey, metadataIdentificationString);
		
		jspOperations.installCommonViewArtefacts();
		
		JspMetadata md = new JspMetadata(metadataIdentificationString, beanInfoMetadata, webScaffoldMetadata);
		
		if (!md.getAnnotationValues().isAutomaticallyMaintainView()) {
			// we're not maintaining the view, so we have nothing to do
			return md;
		}

//		if (webScaffoldMetadata.getAnnotationValues().isShow()) { 
			installImage("images/show.png");
//		}
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) { 
			installImage("images/update.png");
		}
		if (webScaffoldMetadata.getAnnotationValues().isDelete()) { 
			installImage("images/delete.png");
		}
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");
		
		List<FieldMetadata> elegibleFields = getElegibleFields();
		String simpleBeanName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		JspDocumentHelper helper = new JspDocumentHelper(metadataService, elegibleFields, beanInfoMetadata, entityMetadata, finderMetadata, webScaffoldMetadata.getAnnotationValues());
		// Make the holding directory for this controller
		String destinationDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/" + simpleBeanName);
		if (!fileManager.exists(destinationDirectory)) {
			fileManager.createDirectory(destinationDirectory);
		} else {
			File file = new File(destinationDirectory);
			Assert.isTrue(file.isDirectory(), destinationDirectory + " is a file, when a directory was expected");
		}
		
		TilesOperations tilesOperations = new TilesOperations(simpleBeanName, fileManager, pathResolver, "config/webmvc-config.xml");
		
//		if (webScaffoldMetadata.getAnnotationValues().isList()) {
			// By now we have a directory to put the JSPs inside
			String listPath1 = destinationDirectory + "/list.jspx";
			writeToDiskIfNecessary(listPath1, helper.getListDocument().getChildNodes());
			tilesOperations.addViewDefinition(simpleBeanName + "/" + "list", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + simpleBeanName + "/list.jspx");
			
//		} 
//		if (webScaffoldMetadata.getAnnotationValues().isShow()) {
			String showPath = destinationDirectory + "/show.jspx";
			writeToDiskIfNecessary(showPath, helper.getShowDocument().getChildNodes());
			tilesOperations.addViewDefinition(simpleBeanName + "/" + "show", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + simpleBeanName + "/show.jspx");
//		}
		if (webScaffoldMetadata.getAnnotationValues().isCreate()) {
			String listPath = destinationDirectory + "/create.jspx";
			writeToDiskIfNecessary(listPath, helper.getCreateDocument().getChildNodes());
			//add 'create new' menu item
			menuOperations.addMenuItem(
					"web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", 
					beanInfoMetadata.getJavaBean().getSimpleTypeName(), 
					"web_mvc_jsp_create_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_menu_item", 
					beanInfoMetadata.getJavaBean().getSimpleTypeName(),
					"global.menu.new",
					"/" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "/form");
			tilesOperations.addViewDefinition(simpleBeanName + "/" + "create", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + simpleBeanName + "/create.jspx");
		} else {
			menuOperations.cleanUpMenuItem("web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", "web_mvc_jsp_create_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_menu_item");
			tilesOperations.removeViewDefinition(simpleBeanName + "/" + "create");
		}
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			String listPath = destinationDirectory + "/update.jspx";
			writeToDiskIfNecessary(listPath, helper.getUpdateDocument().getChildNodes());
			tilesOperations.addViewDefinition(simpleBeanName + "/" + "update", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + simpleBeanName + "/update.jspx");
		} else {
			tilesOperations.removeViewDefinition(simpleBeanName + "/" + "update");
		}

		//Add 'list all' menu item
		menuOperations.addMenuItem(
				"web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", 
				beanInfoMetadata.getJavaBean().getSimpleTypeName(), 
				"web_mvc_jsp_list_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_menu_item", 
				entityMetadata.getPlural(),
				"global.menu.list",
				"/" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "?offset=0&size=10");
	
		List<String> allowedMenuItems = new ArrayList<String>();
		if (webScaffoldMetadata.getAnnotationValues().isExposeFinders()) {
			for (String finderName : entityMetadata.getDynamicFinders()) {
				String listPath = destinationDirectory + "/" + finderName + ".jspx";
				writeToDiskIfNecessary(listPath, helper.getFinderDocument(finderName).getChildNodes());
				//Add 'Find by' menu item
				menuOperations.addMenuItem(
						"web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", 
						beanInfoMetadata.getJavaBean().getSimpleTypeName(), 
						"finder_" + finderName.toLowerCase() + "_menu_item", 
						"Find by " + new JavaSymbolName(finderName.replace("find" + entityMetadata.getPlural() + "By", "")).getReadableSymbolName(),
						"global.menu.find",
						"/" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "/find/" + finderName.replace("find" + entityMetadata.getPlural(), "") + "/form");
				allowedMenuItems.add("finder_" + finderName.toLowerCase() + "_menu_item");
				tilesOperations.addViewDefinition(simpleBeanName + "/" + finderName, TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + simpleBeanName + "/" + finderName +".jspx");
			}
		}
		
		//clean up links to finders which are removed by now
		menuOperations.cleanUpMenuItems("web_mvc_jsp_" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "_category", "finder_", allowedMenuItems);
		
		//finally write the tiles definition if necessary
		tilesOperations.writeToDiskIfNecessary();
		
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
	
	private void installImage(String imagePath) {
		String imageFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, imagePath);
		if (!fileManager.exists(imageFile)) {
			try {				
					FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), imagePath), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, imagePath)).getOutputStream());		
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		} 
	}
}