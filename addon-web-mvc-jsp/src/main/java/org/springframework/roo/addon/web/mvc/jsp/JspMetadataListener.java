package org.springframework.roo.addon.web.mvc.jsp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
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
	private static final String WEB_INF_VIEWS = "/WEB-INF/views/";
	@Reference private FileManager fileManager;
	@Reference private JspOperations jspOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PropFileOperations propFileOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private TilesOperations tilesOperations;
	@Reference private WebMetadataService webMetadataService;
	private Map<JavaType, String> formBackingObjectTypesToLocalMids = new HashMap<JavaType, String>();

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.addNotificationListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.removeNotificationListener(this);
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
		
		JavaType formBackingType = webScaffoldMetadata.getAnnotationValues().getFormBackingObject();
		MemberDetails memberDetails = webMetadataService.getMemberDetails(formBackingType);
		JavaTypeMetadataDetails formBackingTypeMetadataDetails = webMetadataService.getJavaTypeMetadataDetails(formBackingType, memberDetails, metadataIdentificationString);
		Assert.notNull(formBackingTypeMetadataDetails, "Unable to obtain metadata for type " + formBackingType.getFullyQualifiedTypeName());
		
		formBackingObjectTypesToLocalMids.put(formBackingType, metadataIdentificationString);
		
		SortedMap<JavaType, JavaTypeMetadataDetails> relatedTypeMd = webMetadataService.getRelatedApplicationTypeMetadata(formBackingType, memberDetails, metadataIdentificationString);
		JavaTypeMetadataDetails formbackingTypeMetadata = relatedTypeMd.get(formBackingType);
		Assert.notNull(formbackingTypeMetadata, "Form backing type metadata required");
		JavaTypePersistenceMetadataDetails formBackingTypePersistenceMetadata = formbackingTypeMetadata.getPersistenceDetails();
		if (formBackingTypePersistenceMetadata == null) {
			return null;
		}

		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(formBackingType, Path.SRC_MAIN_JAVA), JspMetadata.createIdentifier(formBackingType, Path.SRC_MAIN_JAVA));
		
		// Install web artifacts only if Spring MVC config is missing
		// TODO: Remove this call when 'controller' commands are gone
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		Assert.notNull(projectMetadata, "Project metadata required");
		PathResolver pathResolver = projectMetadata.getPathResolver();
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, WEB_INF_VIEWS))) {
			jspOperations.installCommonViewArtefacts();
		}

		installImage("images/show.png");
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			installImage("images/update.png");
		}
		if (webScaffoldMetadata.getAnnotationValues().isDelete()) {
			installImage("images/delete.png");
		}

		List<FieldMetadata> eligibleFields = webMetadataService.getScaffoldEligibleFieldMetadata(formBackingType, memberDetails, metadataIdentificationString);
		if (eligibleFields.isEmpty() && formBackingTypePersistenceMetadata.getRooIdentifierFields().isEmpty()) {
			return null;
		}
		JspViewManager viewManager = new JspViewManager(eligibleFields, webScaffoldMetadata.getAnnotationValues(), relatedTypeMd);

		String controllerPath = webScaffoldMetadata.getAnnotationValues().getPath();
		if (controllerPath.startsWith("/")) {
			controllerPath = controllerPath.substring(1);
		}
		
		// Make the holding directory for this controller
		String destinationDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, WEB_INF_VIEWS + controllerPath);
		if (!fileManager.exists(destinationDirectory)) {
			fileManager.createDirectory(destinationDirectory);
		} else {
			File file = new File(destinationDirectory);
			Assert.isTrue(file.isDirectory(), destinationDirectory + " is a file, when a directory was expected");
		}

		// By now we have a directory to put the JSPs inside
		String listPath1 = destinationDirectory + "/list.jspx";
		writeToDiskIfNecessary(listPath1, viewManager.getListDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "list", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/list.jspx");

		String showPath = destinationDirectory + "/show.jspx";
		writeToDiskIfNecessary(showPath, viewManager.getShowDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "show", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/show.jspx");

		JavaSymbolName categoryName = new JavaSymbolName(formBackingType.getSimpleTypeName());

		Map<String, String> properties = new LinkedHashMap<String, String>();
		properties.put("menu_category_" + categoryName.getSymbolName().toLowerCase() + "_label", categoryName.getReadableSymbolName());
		
		if (webScaffoldMetadata.getAnnotationValues().isCreate()) {
			String listPath = destinationDirectory + "/create.jspx";
			writeToDiskIfNecessary(listPath, viewManager.getCreateDocument());
			JavaSymbolName menuItemId = new JavaSymbolName("new");
			// Add 'create new' menu item
			menuOperations.addMenuItem(categoryName, menuItemId, "global_menu_new", "/" + controllerPath + "?form", MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", new JavaSymbolName(formBackingType.getSimpleTypeName()).getReadableSymbolName());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "create", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/create.jspx");
		} else {
			menuOperations.cleanUpMenuItem(categoryName, new JavaSymbolName("new"), MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
			tilesOperations.removeViewDefinition(controllerPath + "/" + "create", controllerPath);
		}
		if (webScaffoldMetadata.getAnnotationValues().isUpdate()) {
			String listPath = destinationDirectory + "/update.jspx";
			writeToDiskIfNecessary(listPath, viewManager.getUpdateDocument());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "update", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/update.jspx");
		} else {
			tilesOperations.removeViewDefinition(controllerPath + "/" + "update", controllerPath);
		}
		
		// Setup labels for i18n support
		String resourceId = XmlUtils.convertId("label." + formBackingType.getFullyQualifiedTypeName().toLowerCase());
		properties.put(resourceId, new JavaSymbolName(formBackingType.getSimpleTypeName()).getReadableSymbolName());

		String pluralResourceId = XmlUtils.convertId(resourceId + ".plural");
		properties.put(pluralResourceId, new JavaSymbolName(formBackingTypeMetadataDetails.getPlural()).getReadableSymbolName());
		
		if (formBackingTypeMetadataDetails.getPersistenceDetails() != null && !formBackingTypeMetadataDetails.getPersistenceDetails().getRooIdentifierFields().isEmpty()) {
			for (FieldMetadata idField: formBackingTypeMetadataDetails.getPersistenceDetails().getRooIdentifierFields()) {
				properties.put(XmlUtils.convertId(resourceId + "." + formBackingTypeMetadataDetails.getPersistenceDetails().getIdentifierField().getFieldName().getSymbolName() + "." + idField.getFieldName().getSymbolName().toLowerCase()), idField.getFieldName().getReadableSymbolName());
			}
		}
		
		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = webMetadataService.getJavaTypePersistenceMetadataDetails(formBackingType, memberDetails, metadataIdentificationString);
		Assert.notNull(javaTypePersistenceMetadataDetails, "Unable to determine persistence metadata for type " + formBackingType.getFullyQualifiedTypeName());
		
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
			Set<FinderMetadataDetails> finderMethodsDetails = webMetadataService.getDynamicFinderMethodsAndFields(formBackingType, memberDetails, metadataIdentificationString);
			for (FinderMetadataDetails finderDetails : finderMethodsDetails) {
				String finderName = finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName();
				String listPath = destinationDirectory + "/" + finderName + ".jspx";
				// Finders only get scaffolded if the finder name is not too long (see ROO-1027)
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
				tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + finderName, TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/" + finderName + ".jspx");
			}
		}
		
		propFileOperations.addProperties(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/application.properties", properties, true, false);

		// Clean up links to finders which are removed by now
		menuOperations.cleanUpFinderMenuItems(categoryName, allowedMenuItems);

		return new JspMetadata(metadataIdentificationString, webScaffoldMetadata);
	}

	/** return indicates if disk was changed (ie updated or created) */
	private void writeToDiskIfNecessary(String jspFilename, Document proposed) {
		Document original = null;
		if (fileManager.exists(jspFilename)) {
			original = XmlUtils.readXml(fileManager.getInputStream(jspFilename));
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				XmlUtils.removeTextNodes(original);
				fileManager.createOrUpdateTextFileIfRequired(jspFilename, XmlUtils.nodeToString(original), false);
			}
		} else {
			fileManager.createOrUpdateTextFileIfRequired(jspFilename, XmlUtils.nodeToString(proposed), false);
		}
	}
	
	public void notify(String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType javaType = WebScaffoldMetadata.getJavaType(upstreamDependency);
			Path path = WebScaffoldMetadata.getPath(upstreamDependency);
			downstreamDependency = JspMetadata.createIdentifier(javaType, path);

			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		} else {
			// This is the generic fallback listener, ie from MetadataDependencyRegistry.addListener(this) in the activate() method
			
			// Get the metadata that just changed
			MetadataItem metadataItem = metadataService.get(upstreamDependency);
			
			// We don't have to worry about physical type metadata, as we monitor the relevant .java once the DOD governor is first detected
			if (metadataItem == null || !metadataItem.isValid() || !(metadataItem instanceof ItdTypeDetailsProvidingMetadataItem)) {
				// There's something wrong with it or it's not for an ITD, so let's gracefully abort
				return;
			}
			
			// Let's ensure we have some ITD type details to actually work with
			ItdTypeDetailsProvidingMetadataItem itdMetadata = (ItdTypeDetailsProvidingMetadataItem) metadataItem;
			ItdTypeDetails itdTypeDetails = itdMetadata.getMemberHoldingTypeDetails();
			if (itdTypeDetails == null) {
				return;
			}

			String localMid = formBackingObjectTypesToLocalMids.get(itdTypeDetails.getGovernor().getName());
			if (localMid != null) {
				metadataService.get(localMid, true);
			}
			return;
		}

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