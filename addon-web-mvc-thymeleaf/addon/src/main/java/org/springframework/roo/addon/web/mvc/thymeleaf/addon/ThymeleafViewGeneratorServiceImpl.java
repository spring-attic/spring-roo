package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.addon.web.mvc.views.components.DetailEntityItem;
import org.springframework.roo.addon.web.mvc.views.components.EntityItem;
import org.springframework.roo.addon.web.mvc.views.components.FieldItem;
import org.springframework.roo.addon.web.mvc.views.components.MenuEntry;
import org.springframework.roo.addon.web.mvc.views.template.engines.AbstractFreeMarkerViewGenerationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.settings.project.ProjectSettingsService;

/**
 *
 * This class implements AbstractFreeMarkerViewGenerationService.
 *
 * As a result, view generation services could delegate on this one to generate
 * views with Thymeleaf components and structure.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
@Component
@Service
public class ThymeleafViewGeneratorServiceImpl extends
    AbstractFreeMarkerViewGenerationService<Document, ThymeleafMetadata> implements
    ThymeleafViewGeneratorService {

  private static final String SPRING_ROO_THYMELEAF_TEMPLATES_LOCATION =
      "spring.roo.thymeleaf.templates-location";

  @Reference
  private PathResolver pathResolver;

  @Reference
  private ProjectSettingsService projectSettings;

  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  @Override
  public JavaType getType() {
    return RooJavaType.ROO_THYMELEAF;
  }

  @Override
  public String getName() {
    return "THYMELEAF";
  }

  @Override
  public String getViewsFolder(String moduleName) {
    return pathResolver.getIdentifier(moduleName, Path.SRC_MAIN_RESOURCES, "/templates");
  }

  @Override
  public String getViewsExtension() {
    return ".html";
  }

  @Override
  public String getLayoutsFolder(String moduleName) {
    return getViewsFolder(moduleName).concat("/").concat("layouts");
  }

  @Override
  public String getFragmentsFolder(String moduleName) {
    return getViewsFolder(moduleName).concat("/").concat("fragments");
  }

  @Override
  public Class<?> getResourceLoaderClass() {
    return ThymeleafViewGeneratorServiceImpl.class;
  }

  @Override
  public Document parse(String content) {
    Document doc = Jsoup.parse(content, "", Parser.xmlParser());
    doc.outputSettings().prettyPrint(false);
    return doc;
  }

  /**
   * Get ids and codes of old document that have 'user-managed' value in data-z attribute.
   *
   * @param loadExistingDoc
   * @return
   */
  private Map<String, String> mergeStructure(Document loadExistingDoc) {
    Map<String, String> structureUserManaged = new HashMap<String, String>();
    Elements elementsUserManaged =
        loadExistingDoc.getElementsByAttributeValue("data-z", "user-managed");
    for (Element elementUserManaged : elementsUserManaged) {
      String id = elementUserManaged.attr("id");
      if (id != null) {
        String code = elementsUserManaged.outerHtml();
        structureUserManaged.put(id, code);
      }
    }
    return structureUserManaged;
  }

  @Override
  public Document merge(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, List<FieldItem> fields) {
    for (FieldItem field : fields) {
      // Get field code if data-z attribute value is equals to
      // user-managed
      Element elementField = loadExistingDoc.getElementById(field.getFieldId());
      if (elementField != null && elementField.hasAttr("data-z")
          && elementField.attr("data-z").equals("user-managed")) {
        field.setUserManaged(true);
        field.setCodeManaged(elementField.outerHtml());
      }
    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    ctx.addExtraParameter("fields", fields);
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }

  @Override
  public Document mergeListView(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, EntityItem entity, List<FieldItem> fields,
      List<List<DetailEntityItem>> detailsLevels) {

    // Get field code if data-z attribute value is equals to user-managed
    Element elementField =
        loadExistingDoc.getElementById(entity.getEntityItemId().concat("-table"));
    if (elementField != null && elementField.hasAttr("data-z")
        && elementField.attr("data-z").equals("user-managed")) {
      entity.setUserManaged(true);
      entity.setCodeManaged(elementField.outerHtml());
    }

    for (List<DetailEntityItem> detailsLevel : detailsLevels) {
      for (DetailEntityItem detail : detailsLevel) {

        // Get detail code if data-z attribute value is equals to
        // user-managed
        Element elementDetail =
            loadExistingDoc.getElementById(detail.getEntityItemId().concat("-table"));
        if (elementDetail != null && elementDetail.hasAttr("data-z")
            && elementDetail.attr("data-z").equals("user-managed")) {
          detail.setUserManaged(true);
          detail.setCodeManaged(elementDetail.outerHtml());
        }

        // Check tab code if data-z attribute value is equals to
        // user-managed
        Element elementTabCodeDetailField =
            loadExistingDoc.getElementById(detail.getEntityItemId().concat("-table-tab"));
        if (elementTabCodeDetailField != null && elementTabCodeDetailField.hasAttr("data-z")
            && elementTabCodeDetailField.attr("data-z").equals("user-managed")) {
          detail.setTabLinkCode(elementTabCodeDetailField.outerHtml());
        }

      }
    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    ctx.addExtraParameter("entity", entity);
    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("detailsLevels", detailsLevels);
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }

  @Override
  public Document mergeMenu(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, List<MenuEntry> menuEntries) {

    for (MenuEntry menuEntry : menuEntries) {

      // Get field code if data-z attribute value is equals to
      // user-managed
      Element elementMenu = loadExistingDoc.getElementById(menuEntry.getId().concat("-entry"));
      if (elementMenu != null && elementMenu.hasAttr("data-z")
          && elementMenu.attr("data-z").equals("user-managed")) {
        menuEntry.setUserManaged(true);
        menuEntry.setCodeManaged(elementMenu.outerHtml());
      }
    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    ctx.addExtraParameter("menuEntries", menuEntries);
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }

  @Override
  public Document merge(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx) {
    // TODO: TO BE FIXED WITH NEW COMMAND 'web mvc view update'
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    Document newDoc = process(templateName, ctx);
    return newDoc;
  }



  @Override
  public String getTemplatesLocation() {
    String thymeleafTemplatesLocation =
        projectSettings.getProperty(SPRING_ROO_THYMELEAF_TEMPLATES_LOCATION);
    if (thymeleafTemplatesLocation != null) {
      return pathResolver.getIdentifier("", Path.ROOT_ROO_CONFIG, thymeleafTemplatesLocation);
    }
    return pathResolver.getIdentifier("", Path.ROOT_ROO_CONFIG, "templates/thymeleaf/default");
  }

  @Override
  public void writeDoc(Document document, String viewPath) {
    // Write doc on disk
    if (document != null && StringUtils.isNotBlank(viewPath)) {
      getFileManager().createOrUpdateTextFileIfRequired(viewPath, document.html(), false);
    }
  }

  @Override
  public void installTemplates() {
    // Getting destination where FreeMarker templates should be installed.
    // This will allow developers to customize his own templates.
    copyDirectoryContents("templates/*.ftl", getTemplatesLocation(), true);
    copyDirectoryContents("templates/fragments/*.ftl",
        getTemplatesLocation().concat("/").concat("fragments"), true);
    copyDirectoryContents("templates/layouts/*.ftl",
        getTemplatesLocation().concat("/").concat("layouts"), true);
    copyDirectoryContents("templates/fields/*.ftl",
        getTemplatesLocation().concat("/").concat("fields"), true);
  }

  @Override
  public void addModalConfirmDelete(String moduleName, ViewContext<ThymeleafMetadata> ctx) {
    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getFragmentsFolder(moduleName).concat("/modal-confirm-delete").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/modal-confirm-delete", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/modal-confirm-delete", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  protected DetailEntityItem createDetailEntityItem(ThymeleafMetadata detailController,
      MemberDetails entityMembers, JpaEntityMetadata entityMetadata, String entityName,
      ViewContext<ThymeleafMetadata> ctx, String detailSuffix, EntityItem rootEntity) {


    JavaType detailItemController;
    JavaType detailCollectionController;
    final ControllerType type = detailController.getControllerMetadata().getType();
    if (type == ControllerType.DETAIL) {
      detailCollectionController = detailController.getDestination();
      detailItemController = detailController.getDetailItemController();
    } else {
      detailCollectionController = detailController.getDetailCollectionController();
      detailItemController = detailController.getDestination();
    }
    JavaType itemController = detailController.getItemController();
    JavaType relatedCollectionController = detailController.getRelatedCollectionController();
    JavaType relatedItemController = detailController.getRelatedItemController();


    DetailEntityItem item =
        super.createDetailEntityItem(detailController, entityMembers, entityMetadata, entityName,
            ctx, detailSuffix, rootEntity);
    item.addConfigurationElement("mvnDetailControllerName", detailController.getMvcControllerName());
    item.addConfigurationElement("mvcUrl_datatablesDetails", ThymeleafMetadata.getMvcUrlNameFor(
        detailController.getDestination(), ThymeleafMetadata.LIST_DATATABLES_DETAILS_METHOD_NAME));
    item.addConfigurationElement("mvcUrl_createForm", ThymeleafMetadata.getMvcUrlNameFor(
        detailCollectionController, ThymeleafMetadata.CREATE_FORM_METHOD_NAME));
    item.addConfigurationElement("mvcUrl_create", ThymeleafMetadata.getMvcUrlNameFor(
        detailCollectionController, ThymeleafMetadata.CREATE_METHOD_NAME));
    if (detailController.getControllerMetadata().getLastDetailsInfo().type == JpaRelationType.AGGREGATION) {
      item.addConfigurationElement(
          "mvcUrl_delete",
          ThymeleafMetadata.getMvcUrlNameFor(detailCollectionController,
              detailController.getCurrentRemoveFromDetailsMethod()));
      item.addConfigurationElement("mvcUrl_delete_dt_ext",
          "arg(1,'_ID_').buildAndExpand('_PARENTID_')");
      item.addConfigurationElement("mvcUrl_show", ThymeleafMetadata.getMvcUrlNameFor(
          relatedItemController, ThymeleafMetadata.SHOW_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_editForm", ThymeleafMetadata.getMvcUrlNameFor(
          relatedItemController, ThymeleafMetadata.EDIT_FORM_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_select2", ThymeleafMetadata.getMvcUrlNameFor(
          relatedCollectionController, ThymeleafMetadata.SELECT2_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_itemExpandBuilderExp", "'_ID_'");

    } else {
      item.addConfigurationElement("mvcUrl_delete", ThymeleafMetadata.getMvcUrlNameFor(
          detailItemController, ThymeleafMetadata.DELETE_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_show", ThymeleafMetadata.getMvcUrlNameFor(
          detailItemController, ThymeleafMetadata.SHOW_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_editForm", ThymeleafMetadata.getMvcUrlNameFor(
          detailItemController, ThymeleafMetadata.EDIT_FORM_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_update", ThymeleafMetadata.getMvcUrlNameFor(
          detailItemController, ThymeleafMetadata.UPDATE_METHOD_NAME));
      item.addConfigurationElement("mvcUrl_itemExpandBuilderExp", "'_PARENTID_','_ID_'");
      item.addConfigurationElement("mvcUrl_delete_dt_ext", "buildAndExpand('_PARENTID_','_ID_')");
    }

    return item;
  }

  @Override
  public Map<String, String> getI18nLabels(MemberDetails entityMemberDetails, JavaType entity,
      JpaEntityMetadata entityMetadata, ControllerMetadata controllerMetadata, String module,
      ViewContext<ThymeleafMetadata> ctx) {
    Map<String, String> labels =
        super.getI18nLabels(entityMemberDetails, entity, entityMetadata, controllerMetadata,
            module, ctx);
    if (controllerMetadata.getType() == ControllerType.DETAIL) {
      String key = getCreateDetailsSelect2PlaceholderLabelKey(controllerMetadata, ctx);

      String value = "Select " + controllerMetadata.getLastDetailsInfo().fieldName + " to add";

      labels.put(key, value);
    }

    return labels;
  }

  private String getCreateDetailsSelect2PlaceholderLabelKey(ControllerMetadata controllerMetadata,
      ViewContext<ThymeleafMetadata> ctx) {
    return "info_select_" + controllerMetadata.getDetailsPathAsString("_");
  }

  @Override
  public void addDetailsViews(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, ControllerMetadata controllerMetadata, ThymeleafMetadata viewMetadata,
      ViewContext<ThymeleafMetadata> ctx) {
    super
        .addDetailsViews(moduleName, entityMetadata, entity, controllerMetadata, viewMetadata, ctx);


    if (controllerMetadata.getLastDetailsInfo().type == JpaRelationType.AGGREGATION) {
      addCreateDetailsView(moduleName, entityMetadata, entity, controllerMetadata, viewMetadata,
          ctx);
    } else {

      addCreateDetailsCompositionView(moduleName, entityMetadata, entity, controllerMetadata,
          viewMetadata, ctx);
    }
  }


  @Override
  public void addDetailsItemViews(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, ControllerMetadata controllerMetadata, ThymeleafMetadata viewMetadata,
      ViewContext<ThymeleafMetadata> ctx) {
    super.addDetailsItemViews(moduleName, entityMetadata, entity, controllerMetadata, viewMetadata,
        ctx);

    // Get root entity metadata
    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    DetailEntityItem detail =
        createDetailEntityItem(viewMetadata, entity, entityMetadata, ctx.getEntityName(), ctx,
            DETAIL_SUFFIX, entityItem);


    detail.addConfigurationElement("entityLabel",
        StringUtils.uncapitalize(FieldItem.buildLabel(detail.getEntityName(), "")));

    ViewContext<ThymeleafMetadata> childCtx =
        createViewContext(controllerMetadata, controllerMetadata.getLastDetailEntity(),
            controllerMetadata.getLastDetailsInfo().childEntityMetadata, viewMetadata);

    // TODO
    addShowDetailsCompositionView(moduleName, entityMetadata, viewMetadata, entityItem, detail,
        childCtx);
    addUpdateDetailsCompositionView(moduleName, entityMetadata, viewMetadata, entityItem, detail,
        childCtx);
  }

  @Override
  public void addFinderFormView(String moduleName, JpaEntityMetadata entityMetadata,
      ThymeleafMetadata viewMetadata, JavaType formBean, String finderName,
      ViewContext<ThymeleafMetadata> ctx) {

    // Getting formBean details
    MemberDetails formBeanDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(),
            getTypeLocationService().getTypeDetails(formBean));

    // Getting entity fields that should be included on view
    List<FieldMetadata> formBeanFields = getPersistentFields(formBeanDetails.getFields());
    List<FieldItem> fields =
        getFieldViewItems(formBeanFields, ctx.getEntityName(), true, ctx, TABLE_SUFFIX);

    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat(finderName)
            .concat("Form").concat(getViewsExtension());

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    final JavaType searchController = viewMetadata.getDestination();

    Map<String, MethodMetadata> finderListMethods = viewMetadata.getFinderListMethods();
    MethodMetadata finderMethod = finderListMethods.get(finderName);

    ctx.addExtraParameter("mvcUrl_search",
        ThymeleafMetadata.getMvcUrlNameFor(searchController, finderMethod));

    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("fields", fields);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeListView("finderForm", loadExistingDoc(viewName), ctx, entityItem, fields,
              new ArrayList<List<DetailEntityItem>>());
    } else {
      newDoc = process("finderForm", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFinderListView(String moduleName, JpaEntityMetadata entityMetadata,
      ThymeleafMetadata viewMetadata, JavaType formBean, JavaType returnType, String finderName,
      ViewContext<ThymeleafMetadata> ctx) {
    // Getting returnType details
    MemberDetails returnTypeDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(),
            getTypeLocationService().getTypeDetails(returnType));

    // Getting entity fields that should be included on view
    List<FieldMetadata> returnFields = getPersistentFields(returnTypeDetails.getFields());
    List<FieldItem> fields =
        getFieldViewItems(returnFields, ctx.getEntityName(), true, ctx, TABLE_SUFFIX);


    // Getting formBean details
    MemberDetails formBeanDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(),
            getTypeLocationService().getTypeDetails(formBean));

    // Getting entity fields that should be included on view
    List<FieldMetadata> formBeanFieldsMetadata = getPersistentFields(formBeanDetails.getFields());
    List<FieldItem> formBeanFields =
        getFieldViewItems(formBeanFieldsMetadata, ctx.getEntityName(), true, ctx, TABLE_SUFFIX);


    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat(finderName)
            .concat(getViewsExtension());

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    final JavaType searchController = viewMetadata.getDestination();

    Map<String, MethodMetadata> finderDatatablesMethods = viewMetadata.getFinderDatatableMethods();
    MethodMetadata finderDtMethod = finderDatatablesMethods.get(finderName);
    Map<String, MethodMetadata> finderListMethods = viewMetadata.getFinderListMethods();
    MethodMetadata finderListMethod = finderListMethods.get(finderName);
    Map<String, MethodMetadata> finderFormMethods = viewMetadata.getFinderFormMethods();
    MethodMetadata finderFormMethod = finderFormMethods.get(finderName);

    ctx.addExtraParameter("mvcUrl_search_datatables",
        ThymeleafMetadata.getMvcUrlNameFor(searchController, finderDtMethod));
    ctx.addExtraParameter("mvcUrl_search_list",
        ThymeleafMetadata.getMvcUrlNameFor(searchController, finderListMethod));
    ctx.addExtraParameter("mvcUrl_search_form",
        ThymeleafMetadata.getMvcUrlNameFor(searchController, finderFormMethod));

    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("fields", fields);

    // Adding formBean fields
    ctx.addExtraParameter("formbeanfields", formBeanFields);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeListView("finderList", loadExistingDoc(viewName), ctx, entityItem, fields,
              new ArrayList<List<DetailEntityItem>>());
    } else {
      newDoc = process("finderList", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }


  private void addShowDetailsCompositionView(String moduleName, JpaEntityMetadata entityMetadata,
      ThymeleafMetadata viewMetadata, EntityItem entityItem, DetailEntityItem detail,
      ViewContext<ThymeleafMetadata> ctx) {

    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/")
            .concat(viewMetadata.getControllerMetadata().getDetailsPathAsString("/"))
            .concat("/show").concat(getViewsExtension());

    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("detail", detail);

    // TODO
    ctx.addExtraParameter("details", Collections.EMPTY_LIST);
    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeDetailsCompositionView("showDetailComposition", loadExistingDoc(viewName), ctx,
              entityItem, detail, (List<FieldItem>) detail.getConfiguration().get("fields"));
    } else {
      ctx.addExtraParameter("fields", detail.getConfiguration().get("fields"));
      newDoc = process("showDetailComposition", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }

  private void addUpdateDetailsCompositionView(String moduleName, JpaEntityMetadata entityMetadata,
      ThymeleafMetadata viewMetadata, EntityItem entityItem, DetailEntityItem detail,
      ViewContext<ThymeleafMetadata> ctx) {

    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/")
            .concat(viewMetadata.getControllerMetadata().getDetailsPathAsString("/"))
            .concat("/edit").concat(getViewsExtension());

    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("detail", detail);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeDetailsCompositionView("editDetailComposition", loadExistingDoc(viewName), ctx,
              entityItem, detail, (List<FieldItem>) detail.getConfiguration().get("fields"));
    } else {
      ctx.addExtraParameter("fields", detail.getConfiguration().get("fields"));
      newDoc = process("editDetailComposition", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }


  protected void addCreateDetailsView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, ControllerMetadata controllerMetadata, ThymeleafMetadata viewMetadata,
      ViewContext<ThymeleafMetadata> ctx) {

    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/")
            .concat(controllerMetadata.getDetailsPathAsString("/")).concat("/create")
            .concat(getViewsExtension());

    // Get root entity metadata
    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    DetailEntityItem detail =
        createDetailEntityItem(viewMetadata, entity, entityMetadata, ctx.getEntityName(), ctx,
            DETAIL_SUFFIX, entityItem);


    detail.addConfigurationElement("entityLabel",
        StringUtils.uncapitalize(FieldItem.buildLabel(detail.getEntityName(), "")));

    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("detail", detail);
    ctx.addExtraParameter("select2_placeholder",
        getCreateDetailsSelect2PlaceholderLabelKey(controllerMetadata, ctx));

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeCreateDetailsView("createDetail", loadExistingDoc(viewName), ctx, entityItem, detail);
    } else {
      newDoc = process("createDetail", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);


  }

  protected void addCreateDetailsCompositionView(String moduleName,
      JpaEntityMetadata entityMetadata, MemberDetails entity,
      ControllerMetadata controllerMetadata, ThymeleafMetadata viewMetadata,
      ViewContext<ThymeleafMetadata> ctx) {
    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/")
            .concat(controllerMetadata.getDetailsPathAsString("/")).concat("/create")
            .concat(getViewsExtension());

    // Get root entity metadata
    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    DetailEntityItem detail =
        createDetailEntityItem(viewMetadata, entity, entityMetadata, ctx.getEntityName(), ctx,
            DETAIL_SUFFIX, entityItem);

    ViewContext<ThymeleafMetadata> childCtx =
        createViewContext(controllerMetadata, controllerMetadata.getLastDetailEntity(),
            controllerMetadata.getLastDetailsInfo().childEntityMetadata, viewMetadata);

    detail.addConfigurationElement("entityLabel",
        StringUtils.uncapitalize(FieldItem.buildLabel(detail.getEntityName(), "")));

    childCtx.addExtraParameter("entity", entityItem);
    childCtx.addExtraParameter("detail", detail);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeDetailsCompositionView("createDetailComposition", loadExistingDoc(viewName),
              childCtx, entityItem, detail,
              (List<FieldItem>) detail.getConfiguration().get("fields"));
    } else {
      childCtx.addExtraParameter("fields", detail.getConfiguration().get("fields"));
      newDoc = process("createDetailComposition", childCtx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }



  private Document mergeDetailsCompositionView(String templateName, Document loadExistingDoc,
      ViewContext ctx, EntityItem entityItem, DetailEntityItem detail, List<FieldItem> fields) {
    for (FieldItem field : fields) {
      // Get field code if data-z attribute value is equals to
      // user-managed
      Element elementField = loadExistingDoc.getElementById(field.getFieldId());
      if (elementField != null && elementField.hasAttr("data-z")
          && elementField.attr("data-z").equals("user-managed")) {
        field.setUserManaged(true);
        field.setCodeManaged(elementField.outerHtml());
      } else {
        field.setUserManaged(false);
        field.setCodeManaged("");
      }
    }
    ctx.addExtraParameter("fields", fields);

    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));
    return process(templateName, ctx);
  }

  private Document mergeCreateDetailsView(String templateName, Document loadExistingDoc,
      ViewContext ctx, EntityItem entityItem, DetailEntityItem detail) {
    Element elementDetail = loadExistingDoc.getElementById(detail.getEntityItemId());
    if (elementDetail != null && elementDetail.hasAttr("data-z")
        && elementDetail.attr("data-z").equals("user-managed")) {
      detail.setUserManaged(true);
      detail.setCodeManaged(elementDetail.outerHtml());
      ctx.addExtraParameter("detail", detail);
    }
    ctx.addExtraParameter("userManagedComponents", mergeStructure(loadExistingDoc));

    return process(templateName, ctx);
  }

  @Override
  public ViewContext<ThymeleafMetadata> createViewContext(
      final ControllerMetadata controllerMetadata, final JavaType entity,
      final JpaEntityMetadata entityMetadata, ThymeleafMetadata metadata) {
    ViewContext<ThymeleafMetadata> ctx =
        super.createViewContext(controllerMetadata, entity, entityMetadata, metadata);

    final JavaType itemCtrl;
    final JavaType collCtrl;
    ctx.addExtraParameter("mvcControllerName", metadata.getMvcControllerName());
    if (controllerMetadata.getType() == ControllerType.COLLECTION) {
      collCtrl = controllerMetadata.getDestination();
    } else {
      collCtrl = metadata.getCollectionController();
    }
    ctx.addExtraParameter("mvcCollectionControllerName",
        ThymeleafMetadata.getMvcControllerName(collCtrl));
    if (controllerMetadata.getType() == ControllerType.ITEM) {
      itemCtrl = controllerMetadata.getDestination();
    } else {
      itemCtrl = metadata.getItemController();
    }
    ctx.addExtraParameter("mvcItemControllerName", ThymeleafMetadata.getMvcControllerName(itemCtrl));
    ctx.addExtraParameter("mvcUrl_datatables",
        ThymeleafMetadata.getMvcUrlNameFor(collCtrl, ThymeleafMetadata.LIST_DATATABLES_METHOD_NAME));
    ctx.addExtraParameter("mvcUrl_createForm",
        ThymeleafMetadata.getMvcUrlNameFor(collCtrl, ThymeleafMetadata.CREATE_FORM_METHOD_NAME));
    ctx.addExtraParameter("mvcUrl_create",
        ThymeleafMetadata.getMvcUrlNameFor(collCtrl, ThymeleafMetadata.CREATE_METHOD_NAME));
    ctx.addExtraParameter("mvcUrl_list",
        ThymeleafMetadata.getMvcUrlNameFor(collCtrl, ThymeleafMetadata.LIST_METHOD_NAME));
    ctx.addExtraParameter("mvcUrl_show",
        ThymeleafMetadata.getMvcUrlNameFor(itemCtrl, ThymeleafMetadata.SHOW_METHOD_NAME));
    ctx.addExtraParameter("mvcUrl_editForm",
        ThymeleafMetadata.getMvcUrlNameFor(itemCtrl, ThymeleafMetadata.EDIT_FORM_METHOD_NAME));
    ctx.addExtraParameter("mvcUrl_remove",
        ThymeleafMetadata.getMvcUrlNameFor(itemCtrl, ThymeleafMetadata.DELETE_METHOD_NAME));

    ctx.addExtraParameter("mvcUrl_update",
        ThymeleafMetadata.getMvcUrlNameFor(itemCtrl, ThymeleafMetadata.UPDATE_METHOD_NAME));

    // TODO finder names

    return ctx;
  }


  @Override
  protected boolean getReferenceField(FieldItem fieldItem, ClassOrInterfaceTypeDetails typeDetails,
      ViewContext<ThymeleafMetadata> ctx) {
    // TODO Auto-generated method stub
    boolean found = super.getReferenceField(fieldItem, typeDetails, ctx);
    if (!found) {
      return false;
    }
    ClassOrInterfaceTypeDetails referenceController =
        (ClassOrInterfaceTypeDetails) fieldItem.getConfiguration().get("referencedController");
    fieldItem.addConfigurationElement("referecedMvcUrl_select2", ThymeleafMetadata
        .getMvcUrlNameFor(referenceController.getType(), ThymeleafMetadata.SELECT2_METHOD_NAME));


    return true;

  }

  @Override
  public void addDefaultListLayout(String moduleName, ViewContext<ThymeleafMetadata> ctx) {
    // Process elements to generate
    Document newDoc = null;

    // Getting new viewName
    String viewName =
        getLayoutsFolder(moduleName).concat("/default-list-layout").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("layouts/default-list-layout", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("layouts/default-list-layout", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

}
