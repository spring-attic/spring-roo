package org.springframework.roo.addon.web.mvc.views;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerLocator;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerOperations;
import org.springframework.roo.addon.web.mvc.controller.addon.RelationInfoExtended;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.components.I18n;
import org.springframework.roo.addon.web.mvc.views.components.DetailEntityItem;
import org.springframework.roo.addon.web.mvc.views.components.EntityItem;
import org.springframework.roo.addon.web.mvc.views.components.FieldItem;
import org.springframework.roo.addon.web.mvc.views.components.FieldTypes;
import org.springframework.roo.addon.web.mvc.views.components.MenuEntry;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.XmlUtils;

/**
 *
 * This abstract class implements MVCViewGenerationService interface that
 * provides all necessary elements to generate views inside project.
 *
 * @param <DOC>
 * @param <T>
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractViewGenerationService<DOC, T extends AbstractViewMetadata> implements
    MVCViewGenerationService<T> {

  // Max fields that will be included on generated view
  private static final int MAX_FIELDS_TO_ADD = 5;

  private static Logger LOGGER = HandlerUtils.getLogger(AbstractViewGenerationService.class);

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  // ------------ OSGi component attributes ----------------
  protected BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  protected abstract DOC process(String templateName, ViewContext<T> ctx);

  protected abstract DOC parse(String content);

  protected abstract DOC merge(String templateName, DOC loadExistingDoc, ViewContext<T> ctx);


  protected abstract DOC merge(String templateName, DOC existingDoc, ViewContext<T> ctx,
      List<FieldItem> fields);

  protected abstract DOC mergeListView(String templateName, DOC loadExistingDoc,
      ViewContext<T> ctx, EntityItem entity, List<FieldItem> fields,
      List<List<DetailEntityItem>> detailsLevels);

  protected abstract DOC mergeMenu(String templateName, DOC loadExistingDoc, ViewContext<T> ctx,
      List<MenuEntry> menuEntries);

  protected abstract String getTemplatesLocation();

  protected abstract void writeDoc(DOC document, String viewPath);



  @Override
  public void addListView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, List<T> detailsControllers, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = getPersistentFields(entity.getFields());
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), true, ctx, TABLE_SUFFIX);



    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/list")
            .concat(getViewsExtension());

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    List<List<DetailEntityItem>> detailsLevels = new ArrayList<List<DetailEntityItem>>();
    if (detailsControllers != null) {
      List<DetailEntityItem> details = new ArrayList<DetailEntityItem>();
      for (T detailController : detailsControllers) {
        DetailEntityItem detailItem =
            createDetailEntityItem(detailController, entity, entityMetadata, ctx.getEntityName(),
                ctx, DETAIL_SUFFIX, entityItem);
        details.add(detailItem);
      }

      // Sort details by path
      Collections.sort(details, new Comparator<DetailEntityItem>() {
        @Override
        public int compare(DetailEntityItem o1, DetailEntityItem o2) {
          return o1.getPathString().compareTo(o2.getPathString());
        }
      });

      // Locates parent details for children, grandsons, etc and make groups by levels
      for (DetailEntityItem detail : details) {
        // Create group until item level
        while (detailsLevels.size() < detail.getLevel()) {
          detailsLevels.add(new ArrayList<DetailEntityItem>());
        }
        // Include detail in its group
        detailsLevels.get(detail.getLevel() - 1).add(detail);
        if (detail.getLevel() < 1) {
          // Nothing more to do with detail
          continue;
        }
        // look for parent
        for (DetailEntityItem parent : details) {
          if (detail.isTheParentEntity(parent)) {
            // set parent
            detail.setParentEntity(parent);
            break;
          }
        }
      }
    }

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeListView("list", loadExistingDoc(viewName), ctx, entityItem, fields, detailsLevels);
    } else {
      ctx.addExtraParameter("entity", entityItem);
      ctx.addExtraParameter("fields", fields);
      ctx.addExtraParameter("detailsLevels", detailsLevels);
      newDoc = process("list", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  protected List<FieldMetadata> getPersistentFields(List<FieldMetadata> fields) {
    List<FieldMetadata> result = new ArrayList<FieldMetadata>(fields.size());
    for (FieldMetadata field : fields) {
      int modifier = field.getModifier();
      if (Modifier.isStatic(modifier) || Modifier.isFinal(modifier)
          || Modifier.isTransient(modifier)) {
        continue;
      }
      if (field.getAnnotation(JpaJavaType.TRANSIENT) != null) {
        continue;
      }
      result.add(field);
    }
    return result;
  }

  protected EntityItem createEntityItem(JpaEntityMetadata entityMetadata, ViewContext<T> ctx,
      String suffix) {
    return new EntityItem(ctx.getEntityName(), ctx.getIdentifierField(), ctx.getControllerPath(),
        suffix, entityMetadata.isReadOnly());
  }

  @Override
  public void addShowView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = getPersistentFields(entityDetails.getFields());
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    // TODO: TO BE FIXED when implements details
    List<FieldItem> details = new ArrayList<FieldItem>();
    // getDetailsFieldViewItems(entityDetails, ctx.getEntityName(), ctx);

    ctx.addExtraParameter("details", details);
    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/show")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("show", loadExistingDoc(viewName), ctx, fields);
    } else {
      newDoc = process("show", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addCreateView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = getPersistentFields(entityDetails.getFields());
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/create")
            .concat(getViewsExtension());

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("create", loadExistingDoc(viewName), ctx, fields);
    } else {
      newDoc = process("create", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addUpdateView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = getPersistentFields(entityDetails.getFields());
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/edit")
            .concat(getViewsExtension());

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("edit", loadExistingDoc(viewName), ctx, fields);
    } else {
      newDoc = process("edit", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFinderFormView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, String finderName, List<FieldMetadata> fieldsToAdd,
      ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldItem> fields =
        getFieldViewItems(fieldsToAdd, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    ctx.addExtraParameter("fields", fields);

    // Build action path
    String path = "";
    if (StringUtils.startsWith(finderName, "count")) {
      path = StringUtils.removeStart(finderName, "count");
    } else if (StringUtils.startsWith(finderName, "find")) {
      path = StringUtils.removeStart(finderName, "find");
    } else if (StringUtils.startsWith(finderName, "query")) {
      path = StringUtils.removeStart(finderName, "query");
    } else if (StringUtils.startsWith(finderName, "read")) {
      path = StringUtils.removeStart(finderName, "read");
    } else {
      path = finderName;
    }
    path = StringUtils.uncapitalize(path);
    ctx.addExtraParameter("action", path);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat(finderName)
            .concat("Form").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("finderForm", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("finderForm", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }

  @Override
  public void addFinderListView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails returnTypeDetails, String finderName, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = getPersistentFields(returnTypeDetails.getFields());
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, StringUtils.EMPTY);

    // Build URL path to get data
    String path = "";
    if (StringUtils.startsWith(finderName, "count")) {
      path = StringUtils.removeStart(finderName, "count");
    } else if (StringUtils.startsWith(finderName, "find")) {
      path = StringUtils.removeStart(finderName, "find");
    } else if (StringUtils.startsWith(finderName, "query")) {
      path = StringUtils.removeStart(finderName, "query");
    } else if (StringUtils.startsWith(finderName, "read")) {
      path = StringUtils.removeStart(finderName, "read");
    } else {
      path = finderName;
    }
    path = StringUtils.uncapitalize(path);
    ctx.addExtraParameter("finderPath", path);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat(finderName)
            .concat("List").concat(getViewsExtension());

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, FINDER_SUFFIX);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeListView("finderList", loadExistingDoc(viewName), ctx, entityItem, fields,
              new ArrayList<List<DetailEntityItem>>());
    } else {
      ctx.addExtraParameter("fields", fields);
      newDoc = process("finderList", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }

  @Override
  public void addIndexView(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getViewsFolder(moduleName).concat("/index").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("index", loadExistingDoc(viewName), ctx);

    } else {
      newDoc = process("index", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addLoginView(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getViewsFolder(moduleName).concat("/login").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("login", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("login", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addAccessibilityView(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat("/accessibility").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("accessibility", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("accessibility", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addErrorView(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getViewsFolder(moduleName).concat("/error").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("error", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("error", ctx);
    }


    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addDefaultLayout(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getLayoutsFolder(moduleName).concat("/default-layout").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("layouts/default-layout", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("layouts/default-layout", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addDefaultLayoutNoMenu(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getLayoutsFolder(moduleName).concat("/default-layout-no-menu").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("layouts/default-layout-no-menu", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("layouts/default-layout-no-menu", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFooter(String moduleName, ViewContext<T> ctx) {
    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/footer").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/footer", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/footer", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addHeader(String moduleName, ViewContext<T> ctx) {
    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/header").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/header", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/header", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addMenu(String moduleName, ViewContext<T> ctx) {

    Map<String, MenuEntry> mapMenuEntries = new HashMap<String, MenuEntry>();

    Set<ClassOrInterfaceTypeDetails> existingControllers =
        new HashSet<ClassOrInterfaceTypeDetails>();
    existingControllers.addAll(getControllerLocator().getControllers(null,
        ControllerType.COLLECTION, getType()));
    existingControllers.addAll(getControllerLocator().getControllers(null, ControllerType.SEARCH,
        getType()));


    Iterator<ClassOrInterfaceTypeDetails> it = existingControllers.iterator();

    while (it.hasNext()) {
      // Getting controller and its information
      ClassOrInterfaceTypeDetails controller = it.next();

      ControllerAnnotationValues controllerValues = new ControllerAnnotationValues(controller);
      JavaType entity = controllerValues.getEntity();

      // Get finders for each controller
      AnnotationMetadata controllerSearchAnnotation =
          controller.getAnnotation(RooJavaType.ROO_SEARCH);
      Map<String, String> finderNamesAndPaths = new HashMap<String, String>();
      if (controllerSearchAnnotation != null
          && controllerSearchAnnotation.getAttribute("finders") != null) {
        List<?> finders = (List<?>) controllerSearchAnnotation.getAttribute("finders").getValue();
        Iterator<?> iterator = finders.iterator();
        while (iterator.hasNext()) {
          StringAttributeValue attributeValue = (StringAttributeValue) iterator.next();
          String finderName = attributeValue.getValue();

          // Build URL path to get data
          String finderPath = "";
          if (StringUtils.startsWith(finderName, "count")) {
            finderPath = StringUtils.removeStart(finderName, "count");
          } else if (StringUtils.startsWith(finderName, "find")) {
            finderPath = StringUtils.removeStart(finderName, "find");
          } else if (StringUtils.startsWith(finderName, "query")) {
            finderPath = StringUtils.removeStart(finderName, "query");
          } else if (StringUtils.startsWith(finderName, "read")) {
            finderPath = StringUtils.removeStart(finderName, "read");
          } else {
            finderPath = finderName;
          }
          finderPath = String.format("search/%s/search-form", StringUtils.uncapitalize(finderPath));
          finderNamesAndPaths.put(finderName, finderPath);
        }
      }

      // Getting pathPrefix
      String pathPrefix = StringUtils.defaultString(controllerValues.getPathPrefix(), "");

      // Generate path
      String path = getControllerOperations().getBaseUrlForController(controller);

      // Create new menuEntry element for controller
      MenuEntry menuEntry =
          createMenuEntry(entity.getSimpleTypeName(), path, pathPrefix,
              FieldItem.buildLabel(entity.getSimpleTypeName(), ""),
              FieldItem.buildLabel(entity.getSimpleTypeName(), "plural"), finderNamesAndPaths,
              false);
      String keyThatRepresentsEntry = pathPrefix.concat(entity.getSimpleTypeName());

      // Add new menu entry to menuEntries list if doesn't exist
      if (mapMenuEntries.containsKey(keyThatRepresentsEntry)) {
        MenuEntry menuEntryInserted = mapMenuEntries.get(keyThatRepresentsEntry);
        if (menuEntryInserted.getFinderNamesAndPaths().isEmpty()
            && !menuEntry.getFinderNamesAndPaths().isEmpty()) {
          menuEntryInserted.setFinderNamesAndPaths(menuEntry.getFinderNamesAndPaths());
        }
      } else {
        mapMenuEntries.put(keyThatRepresentsEntry, menuEntry);
      }
    }

    // Also, check web flow views in the views folder
    String viewsFolder = getViewsFolder(moduleName);
    List<String> webFlowViews = getWebFlowViewsFromDir(viewsFolder, null);

    // After obtain the webFlow views, add them to the menu
    for (String webFlowView : webFlowViews) {

      // Creating the menu entry
      MenuEntry menuEntry =
          createMenuEntry(webFlowView, webFlowView, "", FieldItem.buildLabel(webFlowView, ""),
              FieldItem.buildLabel(webFlowView, "plural"), null, true);

      mapMenuEntries.put(webFlowView, menuEntry);
    }

    // First of all, generate a list of MenuEntries based on existing
    // controllers
    List<MenuEntry> menuEntries = new ArrayList<MenuEntry>(mapMenuEntries.values());

    // Generate ids to search when merge new and existing doc
    List<String> requiredIds = new ArrayList<String>();
    for (MenuEntry entry : menuEntries) {
      requiredIds.add(entry.getPathPrefix().concat(entry.getEntityName()).concat("Entry"));
    }

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/menu").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = mergeMenu("fragments/menu", loadExistingDoc(viewName), ctx, menuEntries);
    } else {
      ctx.addExtraParameter("menuEntries", menuEntries);
      newDoc = process("fragments/menu", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  protected MenuEntry createMenuEntry(String entityName, String path, String pathPrefix,
      String entityLabel, String entityPluralLabel, Map<String, String> finderNamesAndPaths,
      boolean simple) {
    return new MenuEntry(entityName, path, pathPrefix, entityLabel, entityPluralLabel,
        finderNamesAndPaths, false);
  }

  /**
   * THis method obtains the WebFlow views generated in the project.
   *
   * @param viewsFolder
   * @param views
   * @return
   */
  private List<String> getWebFlowViewsFromDir(String viewsFolder, List<String> views) {

    if (views == null) {
      views = new ArrayList<String>();
    }

    File viewsDir = new File(viewsFolder);
    File[] allElements = viewsDir.listFiles();
    for (File element : allElements) {
      if (element.isDirectory()) {
        getWebFlowViewsFromDir(element.getAbsolutePath(), views);
      } else if (element.getName().endsWith("-flow.xml")) {
        String flowName = element.getName().replaceAll("-flow.xml", "");
        views.add(flowName);
      }
    }

    return views;
  }

  @Override
  public void addModal(String moduleName, ViewContext<T> ctx) {
    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/modal").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/modal", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/modal", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addModalConfirm(String moduleName, ViewContext<T> ctx) {
    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getFragmentsFolder(moduleName).concat("/modal-confirm").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/modal-confirm", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/modal-confirm", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addSessionLinks(String moduleName, ViewContext<T> ctx) {
    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getFragmentsFolder(moduleName).concat("/session-links").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/session-links", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/session-links", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addLanguages(String moduleName, ViewContext<T> ctx) {

    // Add installed languages
    List<I18n> installedLanguages = getI18nOperations().getInstalledLanguages(moduleName);
    ctx.addExtraParameter("languages", installedLanguages);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getFragmentsFolder(moduleName).concat("/languages").concat(getViewsExtension());

    // Generate ids to search when merge new and existing doc
    List<String> requiredIds = new ArrayList<String>();
    for (I18n language : installedLanguages) {
      requiredIds.add(language.getLocale().getLanguage() + "Flag");
    }

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/languages", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/languages", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }

  @Override
  public void updateMenuView(String moduleName, ViewContext<T> ctx) {
    // TODO: This method should update menu view with the new
    // controller to include, instead of regenerate menu view page.
    addMenu(moduleName, ctx);
    addLanguages(moduleName, ctx);
  }

  @Override
  public String getLayoutsFolder(String moduleName) {
    // Default implementation
    return getViewsFolder(moduleName);
  }

  @Override
  public String getFragmentsFolder(String moduleName) {
    // Default implementation
    return getViewsFolder(moduleName);
  }

  /**
   * This method obtains all necessary information about fields from entity
   * and returns a List of FieldItem.
   *
   * If provided entity has more than 5 fields, only the first 5 ones will be
   * included on generated view.
   *
   * @param fields
   * @param entityName
   * @param checkMaxFields
   * @param ctx
   * @param suffixId
   *
   * @return List that contains FieldMetadata that will be added to the view.
   */
  protected List<FieldItem> getFieldViewItems(List<FieldMetadata> entityFields, String entityName,
      boolean checkMaxFields, ViewContext<T> ctx, String suffixId) {

    // Get the MAX_FIELDS_TO_ADD
    List<FieldItem> fieldViewItems = new ArrayList<FieldItem>();
    for (FieldMetadata entityField : entityFields) {
      FieldItem fieldItem = createFieldItem(entityField, entityName, suffixId, ctx);

      if (fieldItem != null) {
        fieldViewItems.add(fieldItem);
      }

      if (fieldViewItems.size() >= MAX_FIELDS_TO_ADD && checkMaxFields) {
        break;
      }
    }

    return fieldViewItems;
  }


  protected FieldItem createFieldItem(FieldMetadata entityField, String entityName,
      String suffixId, ViewContext<T> ctx) {

    // Exclude id and version fields
    if (entityField.getAnnotation(JpaJavaType.ID) != null
        || entityField.getAnnotation(JpaJavaType.VERSION) != null) {
      return null;
    }

    FieldItem fieldItem =
        new FieldItem(entityField.getFieldName().getSymbolName(), entityName, suffixId);
    // Generating new FieldItem element

    // Calculate fieldType
    JavaType type = entityField.getFieldType();
    ClassOrInterfaceTypeDetails typeDetails = getTypeLocationService().getTypeDetails(type);

    // ROO-3810: Getting @NotNull annotation to include required attr
    AnnotationMetadata notNullAnnotation = entityField.getAnnotation(Jsr303JavaType.NOT_NULL);
    if (notNullAnnotation != null) {
      fieldItem.addConfigurationElement("required", true);
    } else {
      fieldItem.addConfigurationElement("required", false);
    }

    // Check if is a referenced field
    if (typeDetails != null && typeDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
      boolean shouldBeAdded = getReferenceField(fieldItem, typeDetails, ctx);
      if (!shouldBeAdded) {
        return null;
      }
    } else if (type.isBoolean()) {
      // Check if is a boolean field
      fieldItem.setType(FieldTypes.BOOLEAN.toString());
    } else if (typeDetails != null
        && typeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
      // Saving enum and items to display. Same name as
      // populateForm method
      fieldItem.setType(FieldTypes.ENUM.toString());
      fieldItem.addConfigurationElement("items", fieldItem.getFieldName());

    } else if (type.getFullyQualifiedTypeName().equals(Date.class.getName())
        || type.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
      // Check if is a date field
      fieldItem.setType(FieldTypes.DATE.toString());
      // Getting datetime format to use
      AnnotationMetadata dateTimeFormatAnnotation =
          entityField.getAnnotation(SpringJavaType.DATE_TIME_FORMAT);
      String format = "d/m/Y";
      if (dateTimeFormatAnnotation != null) {
        AnnotationAttributeValue<String> styleAttribute =
            dateTimeFormatAnnotation.getAttribute("style");
        if (styleAttribute != null) {
          String annotationFormat = styleAttribute.getValue();
          if (annotationFormat.equals("M-")) {
            format = "d-M-Y";
          } else {
            format = annotationFormat;
          }
        }
      }
      fieldItem.addConfigurationElement("format", format);
    } else if (type.getFullyQualifiedTypeName().equals("java.util.Set")
        || type.getFullyQualifiedTypeName().equals("java.util.List")) {
      // Ignore details. To obtain details uses
      // getDetailsFieldViewItems method
      return null;
    } else if (type.isNumber()) {
      // ROO-3810: Getting @Min and @Max annotations to add validations if necessary
      AnnotationMetadata minAnnotation = entityField.getAnnotation(Jsr303JavaType.MIN);
      if (minAnnotation != null) {
        AnnotationAttributeValue<Object> min = minAnnotation.getAttribute("value");
        if (min != null) {
          fieldItem.addConfigurationElement("min", min.getValue().toString());
        } else {
          fieldItem.addConfigurationElement("min", "NULL");
        }
      } else {
        fieldItem.addConfigurationElement("min", "NULL");
      }
      AnnotationMetadata maxAnnotation = entityField.getAnnotation(Jsr303JavaType.MAX);
      if (maxAnnotation != null) {
        AnnotationAttributeValue<Object> max = maxAnnotation.getAttribute("value");
        if (max != null) {
          fieldItem.addConfigurationElement("max", max.getValue().toString());
        } else {
          fieldItem.addConfigurationElement("max", "NULL");
        }
      } else {
        fieldItem.addConfigurationElement("max", "NULL");
      }
      fieldItem.setType(FieldTypes.NUMBER.toString());
    } else {
      // ROO-3810:  Getting @Size annotation
      AnnotationMetadata sizeAnnotation = entityField.getAnnotation(Jsr303JavaType.SIZE);
      if (sizeAnnotation != null) {
        AnnotationAttributeValue<Object> maxLength = sizeAnnotation.getAttribute("max");
        if (maxLength != null) {
          fieldItem.addConfigurationElement("maxLength", maxLength.getValue().toString());
        } else {
          fieldItem.addConfigurationElement("maxLength", "NULL");
        }
      } else {
        fieldItem.addConfigurationElement("maxLength", "NULL");
      }
      fieldItem.setType(FieldTypes.TEXT.toString());
    }
    return fieldItem;
  }

  @Override
  public void addDetailsViews(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, ControllerMetadata controllerMetadata, T viewMetadata,
      ViewContext<T> ctx) {
    // Nothing to do here

  }


  @Override
  public void addDetailsItemViews(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, ControllerMetadata controllerMetadata, T viewMetadata,
      ViewContext<T> ctx) {
    // Nothing to do here

  }

  /**
   * Create a new instance of {@link DetailEntityItem}.
   *
   * Implementation can override this method to include it own information or
   * extend defaults.
   *
   * @param detailController
   * @param detailSuffix
   * @param ctx
   * @param string
   * @param entityMetadata
   * @param entity
   * @return
   */
  protected DetailEntityItem createDetailEntityItem(T detailController,
      MemberDetails entityMembers, JpaEntityMetadata entityMetadata, String entityName,
      ViewContext<T> ctx, String detailSuffix, EntityItem rootEntity) {
    ControllerMetadata controllerMetadata = detailController.getControllerMetadata();

    RelationInfoExtended last = controllerMetadata.getLastDetailsInfo();
    ClassOrInterfaceTypeDetails childEntityDetails =
        getTypeLocationService().getTypeDetails(last.childType);
    JpaEntityMetadata childEntityMetadata = last.childEntityMetadata;



    DetailEntityItem detailItem =
        new DetailEntityItem(childEntityMetadata, controllerMetadata, detailSuffix, rootEntity);
    // Saving necessary configuration
    detailItem.addConfigurationElement("referencedFieldType", last.childType.getSimpleTypeName());

    // Getting identifier field
    detailItem.addConfigurationElement("identifierField", childEntityMetadata
        .getCurrentIndentifierField().getFieldName().getSymbolName());

    // Getting referencedfield label plural
    detailItem.addConfigurationElement("referencedFieldLabel",
        FieldItem.buildLabel(entityName, last.fieldName));

    // Getting all referenced fields
    List<FieldMetadata> referencedFields =
        getPersistentFields(getMemberDetailsScanner().getMemberDetails(getClass().toString(),
            childEntityDetails).getFields());
    detailItem.addConfigurationElement(
        "referenceFieldFields",
        getFieldViewItems(referencedFields, entityName + "." + last.fieldName, true, ctx,
            StringUtils.EMPTY));
    detailItem.addConfigurationElement(
        "fields",
        getFieldViewItems(referencedFields, detailItem.getEntityName(), true, ctx,
            StringUtils.EMPTY));
    return detailItem;
  }

  protected MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

  /**
   * This method obtains all necessary configuration to be able to work with
   * reference fields.
   *
   * Complete provided FieldItem with extra fields. If some extra
   * configuration is not available, returns false to prevent that this field
   * will be added. If everything is ok, returns true to add this field to
   * generated view.
   *
   * @param fieldItem
   * @param typeDetails
   * @param allControllers
   * @return
   */
  protected boolean getReferenceField(FieldItem fieldItem, ClassOrInterfaceTypeDetails typeDetails,
      ViewContext<T> viewContext) {
    // Set type as REFERENCE
    fieldItem.setType(FieldTypes.REFERENCE.toString());

    // Add referencedEntity to configuration
    fieldItem
        .addConfigurationElement("referencedEntity", typeDetails.getType().getSimpleTypeName());

    // Add the controllerPath related to the referencedEntity to
    // configuration
    final String controllerPrefix =
        viewContext.getViewMetadata().getControllerMetadata().getAnnotationValues().getPathPrefix();
    Collection<ClassOrInterfaceTypeDetails> allControllers =
        getControllerLocator().getControllers(typeDetails.getType(), ControllerType.COLLECTION,
            getType());
    Iterator<ClassOrInterfaceTypeDetails> it = allControllers.iterator();
    String referencedPath = "";
    ClassOrInterfaceTypeDetails referencedController = null;
    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails controller = it.next();
      ControllerAnnotationValues values = new ControllerAnnotationValues(controller);

      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
      AnnotationAttributeValue<String> prefixAttr = controllerAnnotation.getAttribute("pathPrefix");
      if (StringUtils.equals(values.getPathPrefix(), controllerPrefix)) {
        // Get path
        referencedPath = getControllerOperations().getBaseUrlForController(controller);
        referencedController = controller;

        // Get target entity metadata to get identifier field
        ClassOrInterfaceTypeDetails relatedEntityCid =
            getTypeLocationService().getTypeDetails(typeDetails.getType());
        JpaEntityMetadata relatedEntityMetadata =
            getMetadataService().get(JpaEntityMetadata.createIdentifier(relatedEntityCid));
        fieldItem.addConfigurationElement("identifierField", relatedEntityMetadata
            .getCurrentIndentifierField().getFieldName().getSymbolName());
        break;
      }
    }

    if (referencedController == null) {
      return false;
    }

    fieldItem.addConfigurationElement("referencedPath", referencedPath);
    fieldItem.addConfigurationElement("referencedController", referencedController);

    return true;

  }

  @Override
  public Map<String, String> getI18nLabels(MemberDetails entityMemberDetails, JavaType entity,
      JpaEntityMetadata entityMetadata, ControllerMetadata controllerMetadata, String module,
      ViewContext<T> ctx) {
    final Map<String, String> properties = new LinkedHashMap<String, String>();

    final String entityName = entity.getSimpleTypeName();

    properties.put(buildLabel(entityName), new JavaSymbolName(entity.getSimpleTypeName()
        .toLowerCase()).getReadableSymbolName());

    final String pluralResourceId = buildLabel(entity.getSimpleTypeName(), "plural");
    final String plural = getPluralService().getPlural(entity);
    properties.put(pluralResourceId, new JavaSymbolName(plural).getReadableSymbolName());

    final List<FieldMetadata> javaTypePersistenceMetadataDetails =
        getPersistenceMemberLocator().getIdentifierFields(entity);

    if (!javaTypePersistenceMetadataDetails.isEmpty()) {
      for (final FieldMetadata idField : javaTypePersistenceMetadataDetails) {
        properties.put(buildLabel(entityName, idField.getFieldName().getSymbolName()), idField
            .getFieldName().getReadableSymbolName());
      }
    }

    for (final FieldMetadata field : entityMemberDetails.getFields()) {
      final String fieldResourceId = buildLabel(entityName, field.getFieldName().getSymbolName());

      properties.put(fieldResourceId, field.getFieldName().getReadableSymbolName());

      // Add related entity fields
      if (field.getFieldType().getFullyQualifiedTypeName().equals(Set.class.getName())
          || field.getFieldType().getFullyQualifiedTypeName().equals(List.class.getName())) {

        // Getting inner type
        JavaType referencedEntity = field.getFieldType().getBaseType();

        ClassOrInterfaceTypeDetails referencedEntityDetails =
            getTypeLocationService().getTypeDetails(referencedEntity);

        if (referencedEntityDetails != null
            && referencedEntityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {

          for (final FieldMetadata referencedEntityField : getMemberDetailsScanner()
              .getMemberDetails(this.getClass().getName(), referencedEntityDetails).getFields()) {

            final String referenceEntityFieldResourceId =
                buildLabel(entityName, field.getFieldName().getSymbolName(), referencedEntityField
                    .getFieldName().getSymbolName());
            properties.put(referenceEntityFieldResourceId, referencedEntityField.getFieldName()
                .getReadableSymbolName());
          }
        }
      }
    }
    return properties;
  }



  /**
   * Builds the label of the specified field by joining its names and adding
   * it to the entity label
   *
   * @param entity
   *            the entity name
   * @param fieldNames
   *            list of fields
   * @return label
   */
  private static String buildLabel(String entityName, String... fieldNames) {
    String label = XmlUtils.convertId("label." + entityName.toLowerCase());

    for (String fieldName : fieldNames) {
      label = XmlUtils.convertId(label.concat(".").concat(fieldName.toLowerCase()));
    }
    return label;
  }


  /**
   * This method load the provided file and get its content in String format.
   *
   * After that, uses parse method to generate a valid DOC object.
   *
   * @param path
   * @return
   */
  protected DOC loadExistingDoc(String path) {
    String content = "";
    try {
      // Load file and get STRING content
      content = FileUtils.readFileToString(new File(path));

    } catch (IOException e) {
      throw new RuntimeException(String.format("ERROR: Error trying to load existing doc %s", path));
    }

    // Parse String content to obtain the same type of object
    return parse(content);
  }

  /**
   * This method check if the provided viewPath file exists
   *
   * @param viewName
   * @return true if exists the provided view path
   */
  protected boolean existsFile(String viewPath) {
    return getFileManager().exists(viewPath);
  }

  @Override
  public ViewContext<T> createViewContext(final ControllerMetadata controllerMetadata,
      final JavaType entity, final JpaEntityMetadata entityMetadata, T viewMetadata) {
    ViewContext<T> ctx = new ViewContext<T>();
    ctx.setControllerPath(controllerMetadata.getPath());
    ctx.setProjectName(getProjectOperations().getProjectName(""));
    ctx.setVersion(getProjectOperations().getPomFromModuleName("").getVersion());
    ctx.setEntityName(entity.getSimpleTypeName());
    ctx.setModelAttribute(StringUtils.uncapitalize(entity.getSimpleTypeName()));
    ctx.setModelAttributeName(StringUtils.uncapitalize(entity.getSimpleTypeName()));
    ctx.setIdentifierField(entityMetadata.getCurrentIndentifierField().getFieldName()
        .getSymbolName());
    ctx.setViewMetadata(viewMetadata);
    ctx.setControllerMetadata(controllerMetadata);
    return ctx;
  }



  // Getting OSGi Services

  protected FileManager getFileManager() {
    return serviceInstaceManager.getServiceInstance(this, FileManager.class);
  }

  protected TypeLocationService getTypeLocationService() {
    return serviceInstaceManager.getServiceInstance(this, TypeLocationService.class);
  }

  protected PersistenceMemberLocator getPersistenceMemberLocator() {
    return serviceInstaceManager.getServiceInstance(this, PersistenceMemberLocator.class);
  }

  protected MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }

  protected I18nOperations getI18nOperations() {
    return serviceInstaceManager.getServiceInstance(this, I18nOperations.class);
  }

  protected PluralService getPluralService() {
    return serviceInstaceManager.getServiceInstance(this, PluralService.class);
  }

  protected ControllerLocator getControllerLocator() {
    return serviceInstaceManager.getServiceInstance(this, ControllerLocator.class);
  }

  protected ControllerOperations getControllerOperations() {
    return serviceInstaceManager.getServiceInstance(this, ControllerOperations.class);
  }

  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }
}
