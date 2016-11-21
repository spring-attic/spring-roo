package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

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

    // Get javascript associated to field if data-z attribute value is
    // equals to user-managed
    Map<String, String> javascriptCode = new HashMap<String, String>();
    Element elementJavascriptField =
        loadExistingDoc.getElementById(entity.getEntityItemId().concat("-table-javascript"));
    if (elementJavascriptField != null && elementJavascriptField.hasAttr("data-z")
        && elementJavascriptField.attr("data-z").equals("user-managed")) {
      javascriptCode.put(entity.getEntityItemId().concat("-table-javascript"),
          elementJavascriptField.outerHtml());
    }

    // Check if has details because in this case it has a special javascript
    if (!detailsLevels.isEmpty()) {
      Element elementJavascriptDetailField =
          loadExistingDoc.getElementById(entity.getEntityItemId().concat(
              "-table-javascript-firstdetail"));
      if (elementJavascriptDetailField != null && elementJavascriptDetailField.hasAttr("data-z")
          && elementJavascriptDetailField.attr("data-z").equals("user-managed")) {
        javascriptCode.put(entity.getEntityItemId().concat("-table-javascript-firstdetail"),
            elementJavascriptDetailField.outerHtml());
      }
    }

    entity.setJavascriptCode(javascriptCode);

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

        // Get javascript associated to field if data-z attribute value is
        // equals to user-managed
        Map<String, String> javascriptDetailCode = new HashMap<String, String>();
        Element elementJavascriptDetailField =
            loadExistingDoc.getElementById(detail.getEntityItemId().concat("-table-javascript"));
        if (elementJavascriptDetailField != null && elementJavascriptDetailField.hasAttr("data-z")
            && elementJavascriptDetailField.attr("data-z").equals("user-managed")) {
          javascriptDetailCode.put(detail.getEntityItemId().concat("-table-javascript"),
              elementJavascriptDetailField.outerHtml());
        }

        detail.setJavascriptCode(javascriptDetailCode);

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
    ctx.addExtraParameter("fields", detail.getConfiguration().get("fields"));

    // TODO
    ctx.addExtraParameter("details", Collections.EMPTY_LIST);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeShowDetailsCompositionView("showDetailComposition", loadExistingDoc(viewName), ctx,
              entityItem, detail);
    } else {
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
    ctx.addExtraParameter("fields", detail.getConfiguration().get("fields"));

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeUpdateDetailsCompositionView("editDetailComposition", loadExistingDoc(viewName),
              ctx, entityItem, detail);
    } else {
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
    childCtx.addExtraParameter("fields", detail.getConfiguration().get("fields"));

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeCreateDetailsCompositionView("createDetailComposition", loadExistingDoc(viewName),
              childCtx, entityItem, detail);
    } else {
      newDoc = process("createDetailComposition", childCtx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }



  private Document mergeCreateDetailsCompositionView(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, EntityItem entityItem, DetailEntityItem detail) {
    // TODO
    return process(templateName, ctx);
  }

  private Document mergeCreateDetailsView(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, EntityItem entityItem, DetailEntityItem detail) {
    // TODO
    return process(templateName, ctx);
  }

  private Document mergeUpdateDetailsCompositionView(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, EntityItem entityItem, DetailEntityItem detail) {
    // TODO
    return process(templateName, ctx);
  }

  private Document mergeShowDetailsCompositionView(String templateName, Document loadExistingDoc,
      ViewContext<ThymeleafMetadata> ctx, EntityItem entityItem, DetailEntityItem detail) {
    // TODO
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

}
