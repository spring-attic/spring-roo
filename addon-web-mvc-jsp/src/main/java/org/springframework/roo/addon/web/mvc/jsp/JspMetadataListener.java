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
import org.springframework.roo.addon.web.mvc.controller.finder.WebFinderMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.roundtrip.XmlRoundTripFileManager;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
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
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;

/**
 * Listens for {@link WebScaffoldMetadata} and produces JSPs when requested by that metadata.
 *
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class JspMetadataListener implements MetadataProvider, MetadataNotificationListener {

	// Constants
	private static final String WEB_INF_VIEWS = "/WEB-INF/views/";

	// Fields
	@Reference private FileManager fileManager;
	@Reference private JspOperations jspOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PropFileOperations propFileOperations;
	@Reference private ProjectOperations projectOperations;
	@Reference private TilesOperations tilesOperations;
	@Reference private WebMetadataService webMetadataService;
	@Reference private XmlRoundTripFileManager xmlRoundTripFileManager;

	private final Map<JavaType, String> formBackingObjectTypesToLocalMids = new HashMap<JavaType, String>();

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(WebFinderMetadata.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(WebScaffoldMetadata.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(WebFinderMetadata.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.removeNotificationListener(this);
	}

	public MetadataItem get(final String metadataIdentificationString) {
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
		PathResolver pathResolver = projectOperations.getPathResolver();
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
		xmlRoundTripFileManager.writeToDiskIfNecessary(listPath1, viewManager.getListDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "list", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/list.jspx");

		String showPath = destinationDirectory + "/show.jspx";
		xmlRoundTripFileManager.writeToDiskIfNecessary(showPath, viewManager.getShowDocument());
		tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "show", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/show.jspx");

		JavaSymbolName categoryName = new JavaSymbolName(formBackingType.getSimpleTypeName());

		Map<String, String> properties = new LinkedHashMap<String, String>();
		properties.put("menu_category_" + categoryName.getSymbolName().toLowerCase() + "_label", categoryName.getReadableSymbolName());

		if (webScaffoldMetadata.getAnnotationValues().isCreate()) {
			String listPath = destinationDirectory + "/create.jspx";
			xmlRoundTripFileManager.writeToDiskIfNecessary(listPath, viewManager.getCreateDocument());
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
			xmlRoundTripFileManager.writeToDiskIfNecessary(listPath, viewManager.getUpdateDocument());
			tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + "update", TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/update.jspx");
		} else {
			tilesOperations.removeViewDefinition(controllerPath + "/" + "update", controllerPath);
		}

		// Setup labels for i18n support
		String resourceId = XmlUtils.convertId("label." + formBackingType.getFullyQualifiedTypeName().toLowerCase());
		properties.put(resourceId, new JavaSymbolName(formBackingType.getSimpleTypeName()).getReadableSymbolName());

		String pluralResourceId = XmlUtils.convertId(resourceId + ".plural");
		final String plural = formBackingTypeMetadataDetails.getPlural();
		properties.put(pluralResourceId, new JavaSymbolName(plural).getReadableSymbolName());

		JavaTypePersistenceMetadataDetails javaTypePersistenceMetadataDetails = formBackingTypeMetadataDetails.getPersistenceDetails();
		Assert.notNull(javaTypePersistenceMetadataDetails, "Unable to determine persistence metadata for type " + formBackingType.getFullyQualifiedTypeName());

		if (!javaTypePersistenceMetadataDetails.getRooIdentifierFields().isEmpty()) {
			for (FieldMetadata idField: javaTypePersistenceMetadataDetails.getRooIdentifierFields()) {
				properties.put(XmlUtils.convertId(resourceId + "." + javaTypePersistenceMetadataDetails.getIdentifierField().getFieldName().getSymbolName() + "." + idField.getFieldName().getSymbolName().toLowerCase()), idField.getFieldName().getReadableSymbolName());
			}
		}

		for (MethodMetadata method : memberDetails.getMethods()) {
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
			properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + menuItemId.getSymbolName().toLowerCase() + "_label", new JavaSymbolName(plural).getReadableSymbolName());
		} else {
			menuOperations.cleanUpMenuItem(categoryName, new JavaSymbolName("list"), MenuOperations.DEFAULT_MENU_ITEM_PREFIX);
		}

		List<String> allowedMenuItems = new ArrayList<String>();
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(JspMetadata.getJavaType(metadataIdentificationString), JspMetadata.getPath(metadataIdentificationString)));
		if (ptm != null) {
			MemberHoldingTypeDetails mhtd = ptm.getMemberHoldingTypeDetails();
			if (mhtd != null && MemberFindingUtils.getAnnotationOfType(mhtd.getAnnotations(), RooJavaType.ROO_WEB_FINDER) != null) {
				Set<FinderMetadataDetails> finderMethodsDetails = webMetadataService.getDynamicFinderMethodsAndFields(formBackingType, memberDetails, metadataIdentificationString);
				for (FinderMetadataDetails finderDetails : finderMethodsDetails) {
					String finderName = finderDetails.getFinderMethodMetadata().getMethodName().getSymbolName();
					String listPath = destinationDirectory + "/" + finderName + ".jspx";
					// Finders only get scaffolded if the finder name is not too long (see ROO-1027)
					if (listPath.length() > 244) {
						continue;
					}
					xmlRoundTripFileManager.writeToDiskIfNecessary(listPath, viewManager.getFinderDocument(finderDetails));
					JavaSymbolName finderLabel = new JavaSymbolName(finderName.replace("find" + plural + "By", ""));
					// Add 'Find by' menu item
					menuOperations.addMenuItem(categoryName, finderLabel, "global_menu_find", "/" + controllerPath + "?find=" + finderName.replace("find" + plural, "") + "&form", MenuOperations.FINDER_MENU_ITEM_PREFIX);
					properties.put("menu_item_" + categoryName.getSymbolName().toLowerCase() + "_" + finderLabel.getSymbolName().toLowerCase() + "_label", finderLabel.getReadableSymbolName());
					allowedMenuItems.add(MenuOperations.FINDER_MENU_ITEM_PREFIX + categoryName.getSymbolName().toLowerCase() + "_" + finderLabel.getSymbolName().toLowerCase());
					for (JavaSymbolName paramName : finderDetails.getFinderMethodMetadata().getParameterNames()) {
						properties.put(XmlUtils.convertId(resourceId + "." + paramName.getSymbolName().toLowerCase()), paramName.getReadableSymbolName());
					}
					tilesOperations.addViewDefinition(controllerPath, controllerPath + "/" + finderName, TilesOperations.DEFAULT_TEMPLATE, WEB_INF_VIEWS + controllerPath + "/" + finderName + ".jspx");
				}
			}
		}

		propFileOperations.addProperties(Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties", properties, true, false);

		// Clean up links to finders which are removed by now
		menuOperations.cleanUpFinderMenuItems(categoryName, allowedMenuItems);

		return new JspMetadata(metadataIdentificationString, webScaffoldMetadata);
	}

	public void notify(final String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			if (WebScaffoldMetadata.isValid(upstreamDependency)) {
				JavaType javaType = WebScaffoldMetadata.getJavaType(upstreamDependency);
				Path path = WebScaffoldMetadata.getPath(upstreamDependency);
				downstreamDependency = JspMetadata.createIdentifier(javaType, path);
			} else if (WebFinderMetadata.isValid(upstreamDependency)) {
				JavaType javaType = WebFinderMetadata.getJavaType(upstreamDependency);
				Path path = WebFinderMetadata.getPath(upstreamDependency);
				downstreamDependency = JspMetadata.createIdentifier(javaType, path);
			}

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

	private void installImage(final String imagePath) {
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