package org.springframework.roo.addon.web.mvc.jsp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypePersistenceMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.mvc.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
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
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Listens for {@link WebScaffoldMetadata} and produces JSPs when requested by that metadata.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true) 
@Service 
public final class JspMetadataListener implements MetadataProvider, MetadataNotificationListener {
	@Reference private FileManager fileManager;
	@Reference private JspOperations jspOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PropFileOperations propFileOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private TilesOperations tilesOperations;
	@Reference private WebMetadataService webMetadataService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		// Work out the MIDs of the other metadata we depend on
		// NB: The JavaType and Path are to the corresponding web scaffold controller class

		String webScaffoldMetadataKey = WebScaffoldMetadata.createIdentifier(JspMetadata.getJavaType(metadataIdentificationString), JspMetadata.getPath(metadataIdentificationString));
		WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataKey);

		if (webScaffoldMetadata == null || !webScaffoldMetadata.isValid()) {
			// Can't get the corresponding scaffold, so we certainly don't need to manage any JSPs at this time
			return null;
		}
		
		JavaType formbackingType = webScaffoldMetadata.getAnnotationValues().getFormBackingObject();
		MemberDetails memberDetails = webMetadataService.getMemberDetails(formbackingType);
		JavaTypeMetadataDetails formBackingTypeMetadataDetails = webMetadataService.getJavaTypeMetadataDetails(formbackingType, memberDetails, metadataIdentificationString);
		Assert.notNull(formBackingTypeMetadataDetails, "Unable to obtain metadata for type " + formbackingType.getFullyQualifiedTypeName());

		// Install web artifacts only if Spring MVC config is missing
		// TODO: remove this call when 'controller' commands are gone
		PathResolver pathResolver = projectOperations.getPathResolver();
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views"))) {
			jspOperations.installCommonViewArtefacts();
		}

		installImage("images/show.png");
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			installImage("images/update.png");
		}
		if (webScaffoldMetadata.getAnnotationValues().isDelete()) {
			installImage("images/delete.png");
		}

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");
		
		List<FieldMetadata> elegibleFields = webMetadataService.getScaffoldEligibleFieldMetadata(formbackingType, memberDetails, metadataIdentificationString);

		SortedMap<JavaType, JavaTypeMetadataDetails> relatedTypeMd = webMetadataService.getRelatedApplicationTypeMetadata(formbackingType, memberDetails, metadataIdentificationString);
		
		JavaTypeMetadataDetails formbackingTypeMetadata = relatedTypeMd.get(formbackingType);
		Assert.notNull(formbackingTypeMetadata, "Form backing type metadata required");
		JavaTypePersistenceMetadataDetails formbackingTypePersistenceMetadata = formbackingTypeMetadata.getPersistenceDetails();
		if (formbackingTypePersistenceMetadata == null) {
			return null;
		}

		JspViewManager viewManager = new JspViewManager(elegibleFields, webScaffoldMetadata.getAnnotationValues(), relatedTypeMd);

		String controllerPath = webScaffoldMetadata.getAnnotationValues().getPath();

		if (controllerPath.startsWith("/")) {
			controllerPath = controllerPath.substring(1);
		}
		
		// Make the holding directory for this controller
		String destinationDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/" + controllerPath);
		if (!fileManager.exists(destinationDirectory)) {
			fileManager.createDirectory(destinationDirectory);
		} else {
			File file = new File(destinationDirectory);
			Assert.isTrue(file.isDirectory(), destinationDirectory + " is a file, when a directory was expected");
		}

		// By now we have a directory to put the JSPs inside
		String listPath1 = destinationDirectory + "/list.jspx";
		writeToDiskIfNecessary(listPath1, viewManager.getListDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "list", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/list.jspx");

		String showPath = destinationDirectory + "/show.jspx";
		writeToDiskIfNecessary(showPath, viewManager.getShowDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "show", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/show.jspx");

		JavaSymbolName categoryName = new JavaSymbolName(formbackingType.getSimpleTypeName());

		Map<String, String> properties = new HashMap<String, String>();
		properties.put("menu_category_" + categoryName.getSymbolName().toLowerCase() + "_label", categoryName.getReadableSymbolName());
		
		if (webScaffoldMetadata.getAnnotationValues().isCreate()) {
			String listPath = destinationDirectory + "/create.jspx";
			writeToDiskIfNecessary(listPath, viewManager.getCreateDocument());
			JavaSymbolName menuItemId = new JavaSymbolName("new");
			// add 'create new' menu item
			menuOperations.addMenuItem(categoryName, menuItemId, "global_menu_new", "/" + controllerPath + "?form", MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", new JavaSymbolName(formbackingType.getSimpleTypeName()).getReadableSymbolName());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "create", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/create.jspx");
		} else {
			menuOperations.cleanUpMenuItem(categoryName, new JavaSymbolName("new"), MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			tilesOperations.removeViewDefinition(controllerPath + "/" + "create", controllerPath);
		}
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			String listPath = destinationDirectory + "/update.jspx";
			writeToDiskIfNecessary(listPath, viewManager.getUpdateDocument());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "update", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/update.jspx");
		} else {
			tilesOperations.removeViewDefinition(controllerPath + "/" + "update", controllerPath);
		}
		// setup labels for i18n support
		String resourceId = XmlUtils.convertId("label." + formbackingType.getFullyQualifiedTypeName().toLowerCase());
		properties.put(resourceId, new JavaSymbolName(formbackingType.getSimpleTypeName()).getReadableSymbolName());

		String pluralResourceId = XmlUtils.convertId(resourceId + ".plural");
		properties.put(pluralResourceId, new JavaSymbolName(formBackingTypeMetadataDetails.getPlural()).getReadableSymbolName());
		
		if (formBackingTypeMetadataDetails.getPersistenceDetails() != null && formBackingTypeMetadataDetails.getPersistenceDetails().getRooIdentifierFields().size() > 0) {
			for (FieldMetadata idField: formBackingTypeMetadataDetails.getPersistenceDetails().getRooIdentifierFields()) {
				properties.put(XmlUtils.convertId(resourceId + "." + formBackingTypeMetadataDetails.getPersistenceDetails().getIdentifierField().getFieldName().getSymbolName() + "." + idField.getFieldName().getSymbolName().toLowerCase()), idField.getFieldName().getReadableSymbolName());
			}
		}
		
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = webMetadataService.getJavaTypePersistenceMetadataDetails(formbackingType, memberDetails, metadataIdentificationString);
		Assert.notNull(javaTypePersistenceMetadataDetails, "Unable to determine persistence metadata for type " + formbackingType.getFullyQualifiedTypeName());
		
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			JavaSymbolName fieldName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, fieldName);
			if (field == null) {
				continue; 
			}
			String fieldResourceId = XmlUtils.convertId(resourceId + "." + fieldName.getSymbolName().toLowerCase());
			if (webMetadataService.isApplicationType(method.getReturnType()) && webMetadataService.isRooIdentifier(method.getReturnType(), webMetadataService.getMemberDetails(method.getReturnType()))) {
				JavaTypePersistenceMetadataDetails typePersistenceMetadataDetails = webMetadataService.getJavaTypePersistenceMetadataDetails(method.getReturnType(), webMetadataService.getMemberDetails(method.getReturnType()), metadataIdentificationString);
				if (typePersistenceMetadataDetails != null) {
					for (FieldMetadata f : typePersistenceMetadataDetails.getRooIdentifierFields()) {
						String sb = f.getFieldName().getReadableSymbolName();
						properties.put(XmlUtils.convertId(resourceId + "." + javaTypePersistenceMetadataDetails.getIdentifierField().getFieldName().getSymbolName() + "." + f.getFieldName().getSymbolName().toLowerCase()), (sb == null || sb.length() == 0) ? fieldName.getSymbolName() : sb);
					}
				}
			} else if (!method.getMethodName().equals(javaTypePersistenceMetadataDetails.getIdentifierAccessorMethod().getMethodName()) || (javaTypePersistenceMetadataDetails.getVersionAccessorMethod() != null && !method.getMethodName().equals(javaTypePersistenceMetadataDetails.getVersionAccessorMethod().getMethodName()))) {
				String sb = fieldName.getReadableSymbolName();
				properties.put(fieldResourceId, (sb == null || sb.length() == 0) ? fieldName.getSymbolName() : sb);
			}
		}

		if (javaTypePersistenceMetadataDetails.getFindAllMethod() != null) {
			// Add 'list all' menu item
			JavaSymbolName menuItemId = new JavaSymbolName("list");
			menuOperations.addMenuItem(categoryName, menuItemId, "global_menu_list", "/" + controllerPath + "?page=1&size=${empty param.size ? 10 : param.size}", MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", new JavaSymbolName(formBackingTypeMetadataDetails.getPlural()).getReadableSymbolName());
		} else {
			menuOperations.cleanUpMenuItem(categoryName, new JavaSymbolName("list"), MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
		}
		
		List<String> allowedMenuItems = new ArrayList<String>();
		if (webScaffoldMetadata.getAnnotationValues().isExposeFinders()) {
			Set<FinderMetadataDetails> finderMethodsDetails = webMetadataService.getDynamicFinderMethodsAndFields(formbackingType, memberDetails, metadataIdentificationString);
			for (FinderMetadataDetails finderDetails : finderMethodsDetails) {
				String finderName = finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName();
				String listPath = destinationDirectory + "/" + finderName + ".jspx";
				// finders only get scaffolded if the finder name is not too long (see ROO-1027)
				if (listPath.length() > 244) {
					continue;
				}
				writeToDiskIfNecessary(listPath, viewManager.getFinderDocument(finderDetails));
				JavaSymbolName finderLabel = new JavaSymbolName(finderName.replace("find" + formBackingTypeMetadataDetails.getPlural() + "By", ""));
				// Add 'Find by' menu item
				menuOperations.addMenuItem(categoryName, finderLabel, "global_menu_find", "/" + controllerPath + "?find=" + finderName.replace("find" + formBackingTypeMetadataDetails.getPlural(), "") + "&form", MenuOperations.FINDER_MENU_ITEM_PREFIX);
				properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + finderLabel.getSymbolName().toLowerCase() + "_label", finderLabel.getReadableSymbolName());
				allowedMenuItems.add(MenuOperations.FINDER_MENU_ITEM_PREFIX + categoryName.getSymbolName().toLowerCase() + "_" + finderLabel.getSymbolName().toLowerCase());
				for (JavaSymbolName paramName : finderDetails.getFinderMethodMetadata().getParameterNames()) {
					properties.put(XmlUtils.convertId(resourceId + "." + paramName.getSymbolName().toLowerCase()), paramName.getReadableSymbolName());
				}
				tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + finderName, TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + controllerPath + "/" + finderName + ".jspx");
			}
		}
		
		propFileOperations.addProperties(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", properties, true, false);

		// clean up links to finders which are removed by now
		menuOperations.cleanUpFinderMenuItems(categoryName, allowedMenuItems);

		return new JspMetadata(metadataIdentificationString, webScaffoldMetadata);
	}

	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(String jspFilename, Document proposed) {
		Document original = null;

		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(jspFilename)) {
			try {
				original = XmlUtils.getDocumentBuilder().parse(fileManager.getInputStream(jspFilename));
			} catch (Exception e) {
				throw new IllegalStateException("Could not parse file: " + jspFilename);
			}
			Assert.notNull(original, "Unable to parse " + jspFilename);
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				mutableFile = fileManager.updateFile(jspFilename);
			}
		} else {
			original = proposed;
			mutableFile = fileManager.createFile(jspFilename);
			Assert.notNull(mutableFile, "Could not create JSP file '" + jspFilename + "'");
		}

		if (mutableFile != null) {
			try {
				// Build a string representation of the JSP
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, original);
				String jspContent = byteArrayOutputStream.toString();
				byteArrayOutputStream.close();
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
			}
		}

		// A file existed, but it contained the same content, so we return false
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

		metadataService.get(downstreamDependency, true);
	}

	public String getProvidesType() {
		return JspMetadata.getMetadataIdentiferType();
	}

	private void installImage(String imagePath) {
		PathResolver pathResolver = projectOperations.getPathResolver();
		String imageFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, imagePath);
		if (!fileManager.exists(imageFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), imagePath), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, imagePath)).getOutputStream());
			} catch (Exception e) {
				throw new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}
}