package org.springframework.roo.addon.web.mvc.views;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.jvnet.inflector.Noun;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperationsImpl;
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
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * This abstract class implements MVCViewGenerationService interface that
 * provides all necessary elements to generate views inside project.
 *
 * @param <DOC>
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractViewGenerationService<DOC> implements MVCViewGenerationService {

  // Max fields that will be included on generated view
  private static final int MAX_FIELDS_TO_ADD = 5;

  private static Logger LOGGER = HandlerUtils.getLogger(AbstractViewGenerationService.class);

  private TypeLocationService typeLocationService;
  private FileManager fileManager;
  private PersistenceMemberLocator persistenceMemberLocator;
  private MemberDetailsScanner memberDetailsScanner;
  private I18nOperationsImpl i18nOperationsImpl;

  // ------------ OSGi component attributes ----------------
  protected BundleContext context;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  protected abstract DOC process(String templateName, ViewContext ctx);

  protected abstract DOC parse(String content);

  protected abstract DOC merge(String templateName, DOC loadExistingDoc, ViewContext ctx);


  protected abstract DOC merge(String templateName, DOC existingDoc, ViewContext ctx,
      List<FieldItem> fields);

  protected abstract DOC mergeListView(String templateName, DOC loadExistingDoc, ViewContext ctx,
      EntityItem entity, List<FieldItem> fields, List<DetailEntityItem> details);

  protected abstract DOC mergeMenu(String templateName, DOC loadExistingDoc, ViewContext ctx,
      List<MenuEntry> menuEntries);

  protected abstract String getTemplatesLocation();

  protected abstract void writeDoc(DOC document, String viewPath);

  // Id of each container element by page type
  private static final String CRU_FINDER_LIST_ID_CONTAINER_ELEMENT = "containerFields";
  private static final String MENU_ID_CONTAINER_ELEMENT = "entitiesMenuEntries";
  private static final String LANGUAGES_ID_CONTAINER_ELEMENT = "languageFlags";

  private static final String FIELD_SUFFIX = "field";
  private static final String TABLE_SUFFIX = "entity";
  private static final String DETAIL_SUFFIX = "detail";
  private static final String FINDER_SUFFIX = "finder";

  @Override
  public void addListView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = entityDetails.getFields();
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), true, ctx, TABLE_SUFFIX);
    List<DetailEntityItem> details =
        getDetailsFieldViewItems(entityDetails, ctx.getEntityName(), ctx, DETAIL_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/list")
            .concat(getViewsExtension());

    EntityItem entityItem =
        new EntityItem(ctx.getEntityName(), ctx.getIdentifierField(), ctx.getControllerPath(),
            TABLE_SUFFIX);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = mergeListView("list", loadExistingDoc(viewName), ctx, entityItem, fields, details);
    } else {
      ctx.addExtraParameter("entity", entityItem);
      ctx.addExtraParameter("fields", fields);
      ctx.addExtraParameter("details", details);
      newDoc = process("list", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addShowView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = entityDetails.getFields();
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    // TODO: TO BE FIXED when implements details
    List<FieldItem> details = new ArrayList<FieldItem>();
    // getDetailsFieldViewItems(entityDetails, ctx.getEntityName(), ctx);

    ctx.addExtraParameter("details", details);

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
      ctx.addExtraParameter("fields", fields);
      newDoc = process("show", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addCreateView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = entityDetails.getFields();
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/create")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("create", loadExistingDoc(viewName), ctx, fields);
    } else {
      ctx.addExtraParameter("fields", fields);
      newDoc = process("create", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addUpdateView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = entityDetails.getFields();
    List<FieldItem> fields =
        getFieldViewItems(entityFields, ctx.getEntityName(), false, ctx, FIELD_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/edit")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("edit", loadExistingDoc(viewName), ctx, fields);
    } else {
      ctx.addExtraParameter("fields", fields);
      newDoc = process("edit", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFinderFormView(String moduleName, MemberDetails entityDetails, String finderName,
      List<FieldMetadata> fieldsToAdd, ViewContext ctx) {

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
  public void addFinderListView(String moduleName, MemberDetails returnTypeDetails,
      String finderName, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = returnTypeDetails.getFields();
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

    EntityItem entityItem =
        new EntityItem(ctx.getEntityName(), ctx.getIdentifierField(), ctx.getControllerPath(),
            FINDER_SUFFIX);

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          mergeListView("finderList", loadExistingDoc(viewName), ctx, entityItem, fields,
              new ArrayList<DetailEntityItem>());
    } else {
      ctx.addExtraParameter("fields", fields);
      newDoc = process("finderList", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);
  }

  @Override
  public void addIndexView(String moduleName, ViewContext ctx) {

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
  public void addLoginView(String moduleName, ViewContext ctx) {

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
  public void addAccessibilityView(String moduleName, ViewContext ctx) {

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
  public void addErrorView(String moduleName, ViewContext ctx) {

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
  public void addDefaultLayout(String moduleName, ViewContext ctx) {

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
  public void addDefaultLayoutNoMenu(String moduleName, ViewContext ctx) {

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
  public void addFooter(String moduleName, ViewContext ctx) {
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
  public void addHeader(String moduleName, ViewContext ctx) {
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
  public void addMenu(String moduleName, ViewContext ctx) {

    Map<String, MenuEntry> mapMenuEntries = new HashMap<String, MenuEntry>();

    Set<ClassOrInterfaceTypeDetails> existingControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    Iterator<ClassOrInterfaceTypeDetails> it = existingControllers.iterator();

    while (it.hasNext()) {
      // Getting controller and its information
      ClassOrInterfaceTypeDetails controller = it.next();
      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
      JavaType entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();

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
      AnnotationAttributeValue<Object> pathPrefixAttr =
          controllerAnnotation.getAttribute("pathPrefix");
      String pathPrefix = "";
      if (pathPrefixAttr != null) {
        pathPrefix = (String) pathPrefixAttr.getValue();
      }
      // Generate path
      String path =
          "/".concat(Noun.pluralOf(StringUtils.uncapitalize(entity.getSimpleTypeName()),
              Locale.ENGLISH));
      if (StringUtils.isNotEmpty(pathPrefix)) {
        if (pathPrefix.startsWith("/")) {
          path = pathPrefix.concat(path);

        } else {
          path = "/".concat(pathPrefix).concat(path);
        }
      }

      // Create new menuEntry element for controller
      MenuEntry menuEntry =
          new MenuEntry(entity.getSimpleTypeName(), path, pathPrefix, FieldItem.buildLabel(
              entity.getSimpleTypeName(), ""), FieldItem.buildLabel(entity.getSimpleTypeName(),
              "plural"), finderNamesAndPaths);
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

  @Override
  public void addModal(String moduleName, ViewContext ctx) {
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
  public void addSession(String moduleName, ViewContext ctx) {
    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/session").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("fragments/session", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("fragments/session", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addSessionLinks(String moduleName, ViewContext ctx) {
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
  public void addLanguages(String moduleName, ViewContext ctx) {

    // Add installed languages
    List<I18n> installedLanguages = getI18nOperationsImpl().getInstalledLanguages(moduleName);
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
  public void updateMenuView(String moduleName, ViewContext ctx) {
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
      boolean checkMaxFields, ViewContext ctx, String suffixId) {
    int addedFields = 0;

    // Getting all controllers
    Set<ClassOrInterfaceTypeDetails> allControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    // Get the MAX_FIELDS_TO_ADD
    List<FieldItem> fieldViewItems = new ArrayList<FieldItem>();
    for (FieldMetadata entityField : entityFields) {
      // Exclude id and version fields
      if (entityField.getAnnotation(JpaJavaType.ID) == null
          && entityField.getAnnotation(JpaJavaType.VERSION) == null) {

        // Generating new FieldItem element
        FieldItem fieldItem =
            new FieldItem(entityField.getFieldName().getSymbolName(), entityName, suffixId);

        // Calculate fieldType
        JavaType type = entityField.getFieldType();
        ClassOrInterfaceTypeDetails typeDetails = getTypeLocationService().getTypeDetails(type);

        // Check if is a referenced field
        if (typeDetails != null && typeDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {
          boolean shouldBeAdded = getReferenceField(fieldItem, typeDetails, allControllers);
          if (!shouldBeAdded) {
            continue;
          }
        } else if (type.isBoolean()) {
          // Check if is a boolean field
          fieldItem.setType(FieldTypes.BOOLEAN.toString());
        } else if (typeDetails != null
            && typeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
          // Saving enum and items to display. Same name as
          // populateForm method
          fieldItem.setType(FieldTypes.ENUM.toString());
          fieldItem.addConfigurationElement("items",
              Noun.pluralOf(entityField.getFieldName().getSymbolName(), Locale.ENGLISH));

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
          continue;
        } else {
          fieldItem.setType(FieldTypes.TEXT.toString());
        }

        fieldViewItems.add(fieldItem);
        addedFields++;
      }

      if (addedFields == MAX_FIELDS_TO_ADD && checkMaxFields) {
        break;
      }
    }

    return fieldViewItems;
  }

  /**
   * This method obtains all necessary information about details fields from
   * entity and returns a List of FieldItem.
   *
   * @param entityDetails
   * @param entityName
   * @param ctx
   *
   * @return List that contains FieldMetadata that will be added to the view.
   */
  protected List<DetailEntityItem> getDetailsFieldViewItems(MemberDetails entityDetails,
      String entityName, ViewContext ctx, String suffixId) {
    // Getting entity fields
    List<FieldMetadata> entityFields = entityDetails.getFields();

    List<DetailEntityItem> detailFieldViewItems = new ArrayList<DetailEntityItem>();
    for (FieldMetadata entityField : entityFields) {
      // Exclude id and version fields
      if (entityField.getAnnotation(JpaJavaType.ID) == null
          && entityField.getAnnotation(JpaJavaType.VERSION) == null) {

        // Generating new FieldItem element
        DetailEntityItem detailItem =
            new DetailEntityItem(entityField.getFieldName().getSymbolName(), suffixId);

        // Calculate fieldType
        JavaType type = entityField.getFieldType();

        // Check if is a referenced field
        if (type.getFullyQualifiedTypeName().equals("java.util.Set")
            || type.getFullyQualifiedTypeName().equals("java.util.List")) {
          // Getting base type
          JavaType referencedField = type.getBaseType();
          ClassOrInterfaceTypeDetails referencedFieldDetails =
              getTypeLocationService().getTypeDetails(referencedField);

          if (referencedFieldDetails != null
              && referencedFieldDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY) != null) {

            //fieldItem.setType(FieldTypes.LIST.toString());

            // Saving necessary configuration
            detailItem.addConfigurationElement("referencedFieldType",
                referencedField.getSimpleTypeName());

            // Getting identifier field
            List<FieldMetadata> identifierFields =
                getPersistenceMemberLocator().getIdentifierFields(referencedField);
            detailItem.addConfigurationElement("identifierField", identifierFields.get(0)
                .getFieldName().getSymbolName());

            detailItem.addConfigurationElement("controllerPath", "/"
                + entityField.getFieldName().getSymbolName().toLowerCase());

            // Getting referencedfield label plural
            detailItem.addConfigurationElement("referencedFieldLabel",
                FieldItem.buildLabel(entityName, entityField.getFieldName().getSymbolName()));

            // Getting all referenced fields
            List<FieldMetadata> referencedFields =
                getMemberDetailsScanner().getMemberDetails(getClass().toString(),
                    referencedFieldDetails).getFields();
            detailItem.addConfigurationElement(
                "referenceFieldFields",
                getFieldViewItems(referencedFields, entityName + "."
                    + entityField.getFieldName().getSymbolName(), true, ctx, StringUtils.EMPTY));

          } else {
            // Ignore set or list which base types are not entity
            // field
            continue;
          }

        } else {
          // Ignore not details fields
          continue;
        }

        detailFieldViewItems.add(detailItem);
      }
    }

    return detailFieldViewItems;
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
  private boolean getReferenceField(FieldItem fieldItem, ClassOrInterfaceTypeDetails typeDetails,
      Set<ClassOrInterfaceTypeDetails> allControllers) {
    // Set type as REFERENCE
    fieldItem.setType(FieldTypes.REFERENCE.toString());

    // Add referencedEntity to configuration
    fieldItem
        .addConfigurationElement("referencedEntity", typeDetails.getType().getSimpleTypeName());

    // Add identifierField to configuration
    List<FieldMetadata> identifierFields =
        getPersistenceMemberLocator().getIdentifierFields(typeDetails.getType());
    if (identifierFields.isEmpty()) {
      return false;
    }
    fieldItem.addConfigurationElement("identifierField", identifierFields.get(0).getFieldName()
        .getSymbolName());

    // Add the controllerPath related to the referencedEntity to
    // configuration
    Iterator<ClassOrInterfaceTypeDetails> it = allControllers.iterator();
    String referencedPath = "";
    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails controller = it.next();
      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
      AnnotationAttributeValue<JavaType> entityAttr = controllerAnnotation.getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(typeDetails.getType())) {
        AnnotationAttributeValue<String> pathPrefixAttr =
            controllerAnnotation.getAttribute("pathPrefix");
        String pathPrefix = "";
        if (pathPrefixAttr != null) {
          pathPrefix = (String) pathPrefixAttr.getValue();
        }
        // Generate path
        String path =
            "/".concat(Noun.pluralOf(entityAttr.getValue().getSimpleTypeName(), Locale.ENGLISH));
        if (StringUtils.isNotEmpty(pathPrefix)) {
          if (!pathPrefix.startsWith("/")) {
            pathPrefix = "/".concat(pathPrefix);
          }
          path = pathPrefix.concat(path);
        }
        referencedPath = path;
      }
    }

    fieldItem.addConfigurationElement("referencedPath", referencedPath);

    // Add one or more fields to configuration, to be able to display
    // content
    String fieldOne = "";
    String fieldTwo = "";
    MemberDetails referencedEntityDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().toString(), typeDetails);
    List<FieldMetadata> referencedEntityFields = referencedEntityDetails.getFields();
    for (FieldMetadata referencedEntityField : referencedEntityFields) {
      // Exclude id and version fields
      // TODO: Check audit fields
      if (referencedEntityField.getAnnotation(JpaJavaType.ID) == null
          && referencedEntityField.getAnnotation(JpaJavaType.VERSION) == null) {
        if (StringUtils.isBlank(fieldOne)) {
          fieldOne = referencedEntityField.getFieldName().getSymbolName();
        } else {
          fieldTwo = referencedEntityField.getFieldName().getSymbolName();
          break;
        }
      }
    }

    // At least, one field is necessary.
    if (StringUtils.isBlank(fieldOne)) {
      return false;
    }

    fieldItem.addConfigurationElement("fieldOne", fieldOne);
    fieldItem.addConfigurationElement("fieldTwo", fieldTwo);

    return true;

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

  // Getting OSGi Services

  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on AbstractViewGenerationService.");
        return null;
      }
    } else {
      return fileManager;
    }
  }

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on AbstractViewGenerationService.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

  public PersistenceMemberLocator getPersistenceMemberLocator() {
    if (persistenceMemberLocator == null) {
      // Get all Services implement PersistenceMemberLocator interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PersistenceMemberLocator.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          persistenceMemberLocator = (PersistenceMemberLocator) this.context.getService(ref);
          return persistenceMemberLocator;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PersistenceMemberLocator on AbstractViewGenerationService.");
        return null;
      }
    } else {
      return persistenceMemberLocator;
    }
  }

  public MemberDetailsScanner getMemberDetailsScanner() {
    if (memberDetailsScanner == null) {
      // Get all Services implement MemberDetailsScanner interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          memberDetailsScanner = (MemberDetailsScanner) this.context.getService(ref);
          return memberDetailsScanner;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MemberDetailsScanner on AbstractViewGenerationService.");
        return null;
      }
    } else {
      return memberDetailsScanner;
    }
  }

  public I18nOperationsImpl getI18nOperationsImpl() {
    if (i18nOperationsImpl == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(I18nOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          i18nOperationsImpl = (I18nOperationsImpl) this.context.getService(ref);
          return i18nOperationsImpl;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on AbstractViewGeneratorMetadataProvider.");
        return null;
      }
    } else {
      return i18nOperationsImpl;
    }
  }
}
