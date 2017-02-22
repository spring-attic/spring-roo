package org.springframework.roo.addon.web.mvc.views;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dto.addon.EntityProjectionLocator;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
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
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;
import org.springframework.roo.support.util.XmlUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * This abstract class implements MVCViewGenerationService interface that
 * provides all necessary elements to generate views inside project.
 *
 * @param <DOC>
 * @param <T>
 * @author Juan Carlos García
 * @author Sergio Clares
 * @since 2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractViewGenerationService<DOC, T extends AbstractViewMetadata> implements
    MVCViewGenerationService<T> {

  // Max fields that will be included on generated view
  private static final int MAX_FIELDS_TO_ADD = 5;

  private final List<JavaType> STANDAR_TYPES = Arrays.asList(JavaType.BOOLEAN_OBJECT,
      JavaType.STRING, JavaType.LONG_OBJECT, JavaType.INT_OBJECT, JavaType.FLOAT_OBJECT,
      JavaType.DOUBLE_OBJECT);

  private final List<JavaType> DATE_TIME_TYPES = Arrays.asList(JdkJavaType.DATE,
      JdkJavaType.CALENDAR);

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

  protected abstract void writeDoc(DOC document, String viewPath);

  @Override
  public void addListView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, List<T> detailsControllers, ViewContext<T> ctx) {

    // Get the repository related with the entity to check the default return type
    RepositoryJpaMetadata repository =
        getRepositoryJpaLocator().getFirstRepositoryMetadata(entityMetadata.getAnnotatedEntity());

    // All views should have a repository
    Validate.notNull(repository,
        "ERROR: The provided entity should have an associated repository to be able "
            + "to generate the list view.");

    // Obtain the defaultReturnType
    JavaType defaultReturnType = repository.getDefaultReturnType();

    // The defaultReturnType must not be null. If it's not an entity projection,
    // it must be an entity
    Validate
        .notNull(defaultReturnType,
            "ERROR: The repository associated to the provided entity should define a defaultReturnType");

    // Obtain details of the provided defaultReturnType. If not exists as type, show an error
    ClassOrInterfaceTypeDetails defaultReturnTypeCid =
        getTypeLocationService().getTypeDetails(defaultReturnType);
    Validate.notNull(defaultReturnTypeCid,
        "ERROR: The provided defaultReturnType is not a valid type");
    MemberDetails defaultReturnTypeDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().toString(), defaultReturnTypeCid);
    Validate.notNull(defaultReturnTypeDetails,
        "ERROR: Is not possible to obtain any detail from the " + "provided defaultReturnType.");

    List<FieldMetadata> defaultReturnTypeFields = defaultReturnTypeDetails.getFields();

    // To prevent errors during generation, is defaultReturnTypeFields is empty,
    // all the entity fields will be used
    if (defaultReturnTypeFields.isEmpty()) {
      defaultReturnTypeFields = entity.getFields();
    }

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = getPersistentFields(defaultReturnTypeFields);
    List<FieldItem> fields =
        getFieldViewItems(entityMetadata, entityFields, ctx.getEntityName(), true, ctx,
            TABLE_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/list")
            .concat(getViewsExtension());

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    List<List<DetailEntityItem>> detailsLevels = new ArrayList<List<DetailEntityItem>>();
    if (detailsControllers != null && !detailsControllers.isEmpty()) {
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

      // Locates parent details for children, grandsons, etc and make groups by
      // levels
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

  protected List<FieldMetadata> getEditableFields(List<FieldMetadata> fields) {
    List<FieldMetadata> persistentFields = getPersistentFields(fields);
    Iterator<FieldMetadata> it = persistentFields.iterator();
    while (it.hasNext()) {
      FieldMetadata field = it.next();
      if (field.getAnnotation(SpringJavaType.LAST_MODIFIED_DATE) != null
          || field.getAnnotation(SpringJavaType.LAST_MODIFIED_BY) != null
          || field.getAnnotation(SpringJavaType.CREATED_BY) != null
          || field.getAnnotation(SpringJavaType.CREATED_DATE) != null) {
        it.remove();
      }
    }

    return persistentFields;
  }

  protected EntityItem createEntityItem(JpaEntityMetadata entityMetadata, ViewContext<T> ctx,
      String suffix) {
    return new EntityItem(ctx.getEntityName(), ctx.getIdentifierField(), ctx.getControllerPath(),
        suffix, entityMetadata.isReadOnly(), entityMetadata.getCurrentVersionField().getFieldName()
            .getSymbolName());
  }

  @Override
  public void addShowView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, List<T> detailsControllers, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = new ArrayList<FieldMetadata>();

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    Map<String, List<FieldItem>> compositeRelationFields =
        manageChildcompositionFields(entityMetadata, entityDetails, ctx);

    // Remove one-to-one fields from composite relations and create EntityItems
    // for each referenced entity field
    Set<String> compositeRelationFieldNames = compositeRelationFields.keySet();
    for (FieldMetadata field : getPersistentFields(entityDetails.getFields())) {
      if (!compositeRelationFieldNames.contains(field.getFieldName().getSymbolName())) {
        entityFields.add(field);
      }
    }
    List<FieldItem> fields =
        getFieldViewItems(entityMetadata, entityFields, ctx.getEntityName(), false, ctx,
            FIELD_SUFFIX);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("compositeRelationFields", compositeRelationFields);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/show")
            .concat(getViewsExtension());

    // Including details if needed
    List<List<DetailEntityItem>> detailsLevels = new ArrayList<List<DetailEntityItem>>();
    if (detailsControllers != null && !detailsControllers.isEmpty()) {
      List<DetailEntityItem> details = new ArrayList<DetailEntityItem>();
      for (T detailController : detailsControllers) {
        DetailEntityItem detailItem =
            createDetailEntityItem(detailController, entityDetails, entityMetadata,
                ctx.getEntityName(), ctx, DETAIL_SUFFIX, entityItem);
        details.add(detailItem);
      }

      // Sort details by path
      Collections.sort(details, new Comparator<DetailEntityItem>() {

        @Override
        public int compare(DetailEntityItem o1, DetailEntityItem o2) {
          return o1.getPathString().compareTo(o2.getPathString());
        }
      });

      // Locates parent details for children, grandsons, etc and make groups by
      // levels
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

    ctx.addExtraParameter("detailsLevels", detailsLevels);

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
  public void addShowInlineView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, ViewContext<T> ctx) {

    // Getting entity fields that should be included on view
    List<FieldMetadata> entityFields = new ArrayList<FieldMetadata>();

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    Map<String, List<FieldItem>> compositeRelationFields =
        manageChildcompositionFields(entityMetadata, entityDetails, ctx);

    // Remove one-to-one fields from composite relations and create EntityItems
    // for each referenced entity field
    Set<String> compositeRelationFieldNames = compositeRelationFields.keySet();
    for (FieldMetadata field : getPersistentFields(entityDetails.getFields())) {
      if (!compositeRelationFieldNames.contains(field.getFieldName().getSymbolName())) {
        entityFields.add(field);
      }
    }
    List<FieldItem> fields =
        getFieldViewItems(entityMetadata, entityFields, ctx.getEntityName(), false, ctx,
            FIELD_SUFFIX);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("compositeRelationFields", compositeRelationFields);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/")
            .concat("/showInline").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("showInline", loadExistingDoc(viewName), ctx, fields);
    } else {
      newDoc = process("showInline", ctx);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addCreateView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entityDetails, ViewContext<T> ctx) {

    // Create void list for adding main entity fields
    List<FieldMetadata> entityFields = new ArrayList<FieldMetadata>();

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/create")
            .concat(getViewsExtension());

    Map<String, List<FieldItem>> compositeRelationFields =
        manageChildcompositionFields(entityMetadata, entityDetails, ctx);

    // Remove one-to-one fields from composite relations and create EntityItems
    // for each referenced entity field
    Set<String> compositeRelationFieldNames = compositeRelationFields.keySet();
    for (FieldMetadata field : getEditableFields(entityDetails.getFields())) {
      if (!compositeRelationFieldNames.contains(field.getFieldName().getSymbolName())) {
        entityFields.add(field);
      }
    }

    List<FieldItem> fields =
        getFieldViewItems(entityMetadata, entityFields, ctx.getEntityName(), false, ctx,
            FIELD_SUFFIX);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("compositeRelationFields", compositeRelationFields);

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
    List<FieldMetadata> entityFields = new ArrayList<FieldMetadata>();

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/edit")
            .concat(getViewsExtension());

    // Check and manage detail composition fields
    Map<String, List<FieldItem>> compositeRelationFields =
        manageChildcompositionFields(entityMetadata, entityDetails, ctx);

    // Remove one-to-one fields from composite relations and create EntityItems
    // for each referenced entity field
    Set<String> compositeRelationFieldNames = compositeRelationFields.keySet();
    for (FieldMetadata field : getEditableFields(entityDetails.getFields())) {
      if (!compositeRelationFieldNames.contains(field.getFieldName().getSymbolName())) {
        entityFields.add(field);
      }
    }

    List<FieldItem> fields =
        getFieldViewItems(entityMetadata, entityFields, ctx.getEntityName(), false, ctx,
            FIELD_SUFFIX);

    EntityItem entityItem = createEntityItem(entityMetadata, ctx, TABLE_SUFFIX);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("entity", entityItem);
    ctx.addExtraParameter("compositeRelationFields", compositeRelationFields);

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
      T viewMetadata, JavaType formBean, String finderName, ViewContext<T> ctx) {
    // To be implemented by View Provider
  }

  @Override
  public void addFinderListView(String moduleName, JpaEntityMetadata entityMetadata,
      MemberDetails entity, T viewMetadata, JavaType formBean, JavaType returnType,
      String finderName, List<T> detailsControllers, ViewContext<T> ctx) {
    // To be implemented by View Provider
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
  public void addHomeLayout(String moduleName, ViewContext<T> ctx) {

    // Process elements to generate
    DOC newDoc = null;

    // Getting new viewName
    String viewName =
        getLayoutsFolder(moduleName).concat("/home-layout").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge("layouts/home-layout", loadExistingDoc(viewName), ctx);
    } else {
      newDoc = process("layouts/home-layout", ctx);
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

    Map<String, MenuEntry> mapMenuEntries = new TreeMap<String, MenuEntry>();

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

      // Obtain the entityMetadata
      JpaEntityMetadata entityMetadata =
          getMetadataService().get(
              JpaEntityMetadata.createIdentifier(getTypeLocationService().getTypeDetails(entity)));

      boolean isReadOnly = false;
      if (entityMetadata != null && entityMetadata.isReadOnly()) {
        isReadOnly = true;
      }

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
      if (controllerSearchAnnotation != null) {
        // Prevent that /search will be included in every menu entry
        // The /search is already included in the step before only for
        // finder entries
        path = path.replace("/search", "");
      }

      // Create new menuEntry element for controller
      String keyThatRepresentsEntry = pathPrefix.concat(entity.getSimpleTypeName());

      // Add new menu entry to menuEntries list if doesn't exist
      MenuEntry menuEntry = null;
      if (controllerValues.getType() == ControllerType.SEARCH) {

        // Only add finder entry
        menuEntry =
            createMenuEntry(entity.getSimpleTypeName(), path, pathPrefix,
                FieldItem.buildLabel(entity.getSimpleTypeName(), ""),
                FieldItem.buildLabel(entity.getSimpleTypeName(), "plural"), finderNamesAndPaths,
                false, false, false);
      } else {

        // Add default menu entries
        menuEntry =
            createMenuEntry(entity.getSimpleTypeName(), path, pathPrefix,
                FieldItem.buildLabel(entity.getSimpleTypeName(), ""),
                FieldItem.buildLabel(entity.getSimpleTypeName(), "plural"), finderNamesAndPaths,
                false, true, isReadOnly);
      }

      if (mapMenuEntries.containsKey(keyThatRepresentsEntry)) {
        MenuEntry menuEntryInserted = mapMenuEntries.get(keyThatRepresentsEntry);
        if (menuEntryInserted.getFinderNamesAndPaths().isEmpty()
            && !menuEntry.getFinderNamesAndPaths().isEmpty()) {
          menuEntryInserted.setFinderNamesAndPaths(menuEntry.getFinderNamesAndPaths());
        }

        // Check the 'addDefaultEntries' attribute and add it if needed
        if (!menuEntryInserted.isAddDefaultEntries() && menuEntry.isAddDefaultEntries()) {
          menuEntryInserted.setAddDefaultEntries(menuEntry.isAddDefaultEntries());
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
              FieldItem.buildLabel(webFlowView, "plural"), null, true, false, false);

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
      boolean simple, boolean addDefaultEntries, boolean readOnly) {
    return new MenuEntry(entityName, path, pathPrefix, entityLabel, entityPluralLabel,
        finderNamesAndPaths, simple, addDefaultEntries, readOnly);
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
   * This method obtains all necessary information about fields from entity and
   * returns a List of FieldItem. If provided entity has more than 5 fields,
   * only the first 5 ones will be included on generated view.
   *
   * @param entityMetadata
   * @param fields
   * @param entityName
   * @param checkMaxFields
   * @param ctx
   * @param suffixId
   * @return List that contains FieldMetadata that will be added to the view.
   */
  protected List<FieldItem> getFieldViewItems(JpaEntityMetadata entityMetadata,
      List<FieldMetadata> entityFields, String entityName, boolean checkMaxFields,
      ViewContext<T> ctx, String suffixId) {

    // Get the MAX_FIELDS_TO_ADD
    List<FieldItem> fieldViewItems = new ArrayList<FieldItem>();
    for (FieldMetadata entityField : entityFields) {

      FieldItem fieldItem =
          createFieldItem(entityMetadata, entityField, entityName, suffixId, ctx, null);

      if (fieldItem != null) {
        fieldViewItems.add(fieldItem);
      }

      if (fieldViewItems.size() >= MAX_FIELDS_TO_ADD && checkMaxFields) {
        break;
      }
    }

    return fieldViewItems;
  }

  protected FieldItem createFieldItem(JpaEntityMetadata entityMetadata, FieldMetadata entityField,
      String entityName, String suffixId, ViewContext<T> ctx, String referencedFieldName) {

    // Getting current identifier field
    FieldMetadata identifierField = entityMetadata.getCurrentIndentifierField();
    FieldMetadata versionField = entityMetadata.getCurrentVersionField();

    // Exclude id and version fields
    if (entityField.getAnnotation(JpaJavaType.ID) != null
        || entityField.getAnnotation(JpaJavaType.VERSION) != null
        || identifierField.getFieldName().equals(entityField.getFieldName())
        || versionField.getFieldName().equals(entityField.getFieldName())) {
      return null;
    }

    // Generating new FieldItem element
    FieldItem fieldItem = null;
    if (referencedFieldName != null) {
      fieldItem =
          new FieldItem(entityField.getFieldName().getSymbolName(), ctx.getEntityName(),
              referencedFieldName, entityName, suffixId);
    } else {
      fieldItem = new FieldItem(entityField.getFieldName().getSymbolName(), entityName, suffixId);
    }

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
      // Referenced field is a relation field      

      // If is child part field of a composition relation, specify it if view context
      if (entityMetadata.getCompositionRelationField() != null
          && entityMetadata.getCompositionRelationField().getFieldName()
              .equals(entityField.getFieldName())) {
        fieldItem.addConfigurationElement("isCompositionChildField", true);
      } else {
        fieldItem.addConfigurationElement("isCompositionChildField", false);
      }

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
      // ROO-3810: Getting @Min and @Max annotations to add validations if
      // necessary
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

      // ROO-3872: Support numeric input validation from client and server side
      // Add digits constraints
      AnnotationMetadata digitsAnnotation = entityField.getAnnotation(Jsr303JavaType.DIGITS);
      if (digitsAnnotation != null) {

        // Fraction digits
        AnnotationAttributeValue<Object> digitsFraction = digitsAnnotation.getAttribute("fraction");
        fieldItem.addConfigurationElement("digitsFraction", digitsFraction.getValue().toString());

        // Integer digits
        AnnotationAttributeValue<Object> digitsInteger = digitsAnnotation.getAttribute("integer");
        fieldItem.addConfigurationElement("digitsInteger", digitsInteger.getValue().toString());
      } else {

        // Add default values for decimals
        if (type.equals(JavaType.INT_OBJECT) || type.equals(JavaType.INT_PRIMITIVE)
            || type.equals(JdkJavaType.BIG_INTEGER)) {
          fieldItem.addConfigurationElement("digitsFraction", "0");
        } else {
          fieldItem.addConfigurationElement("digitsFraction", "2");
        }
        fieldItem.addConfigurationElement("digitsInteger", "NULL");
      }

      fieldItem.setType(FieldTypes.NUMBER.toString());
    } else {
      // ROO-3810: Getting @Size annotation
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
   * Create a new instance of {@link DetailEntityItem}. Implementation can
   * override this method to include it own information or extend defaults.
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
    List<FieldMetadata> referencedFields = null;
    // Get the repository related with the child entity to check the default return type
    RepositoryJpaMetadata repository =
        getRepositoryJpaLocator().getFirstRepositoryMetadata(
            childEntityMetadata.getAnnotatedEntity());

    // All views should have a repository
    Validate.notNull(repository,
        "ERROR: The provided child entity should have an associated repository to be able "
            + "to generate the list view.");

    // Obtain the defaultReturnType
    JavaType defaultReturnType = repository.getDefaultReturnType();

    // The defaultReturnType must not be null. If it's not an entity projection,
    // it must be an entity
    Validate
        .notNull(defaultReturnType,
            "ERROR: The repository associated to the provided entity should define a defaultReturnType");

    // Obtain details of the provided defaultReturnType. If not exists as type, show an error
    ClassOrInterfaceTypeDetails defaultReturnTypeCid =
        getTypeLocationService().getTypeDetails(defaultReturnType);
    Validate.notNull(defaultReturnTypeCid,
        "ERROR: The provided defaultReturnType is not a valid type");
    MemberDetails defaultReturnTypeDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().toString(), defaultReturnTypeCid);
    Validate.notNull(defaultReturnTypeDetails,
        "ERROR: Is not possible to obtain any detail from the " + "provided defaultReturnType.");

    List<FieldMetadata> defaultReturnTypeFields = defaultReturnTypeDetails.getFields();
    if (defaultReturnTypeFields.isEmpty()) {
      referencedFields =
          getEditableFields(getMemberDetailsScanner().getMemberDetails(getClass().toString(),
              childEntityDetails).getFields());
    } else {
      referencedFields = getEditableFields(defaultReturnTypeFields);
    }

    detailItem.addConfigurationElement(
        "referenceFieldFields",
        getFieldViewItems(childEntityMetadata, referencedFields, entityName + "." + last.fieldName,
            true, ctx, StringUtils.EMPTY));
    detailItem.addConfigurationElement(
        "fields",
        getFieldViewItems(childEntityMetadata, referencedFields, detailItem.getEntityName(), true,
            ctx, StringUtils.EMPTY));
    return detailItem;
  }

  protected MetadataService getMetadataService() {
    return serviceInstaceManager.getServiceInstance(this, MetadataService.class);
  }

  /**
   * This method obtains all necessary configuration to be able to work with
   * reference fields. Complete provided FieldItem with extra fields. If some
   * extra configuration is not available, returns false to prevent that this
   * field will be added. If everything is ok, returns true to add this field to
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

      // ROO-3865: Add suport for enum internationalization
      // If field is enum, add each enum value as label with proper format
      JavaType fieldType = field.getFieldType();
      final ClassOrInterfaceTypeDetails javaTypeDetails =
          getTypeLocationService().getTypeDetails(fieldType);
      if (javaTypeDetails != null
          && javaTypeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
        List<JavaSymbolName> enumConstants = javaTypeDetails.getEnumConstants();
        for (JavaSymbolName constant : enumConstants) {
          String enumLabelCode =
              XmlUtils.convertId("enum.".concat(fieldType.getSimpleTypeName()).concat(".")
                  .concat(constant.getSymbolName()));
          String enumLabelValue = StringUtils.capitalize(constant.getSymbolName().toLowerCase());
          properties.put(enumLabelCode, enumLabelValue);
        }
      }

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

    // ROO-3868: Add label for localized SpEL to show entity in presentation view
    // Check for @EntityFormat at entity type-level
    addI18nLabelsFromEntityFormatExpressions(properties,
        getTypeLocationService().getTypeDetails(entity));

    // Check for @EntityFormat at all existing projections for this entity
    Collection<ClassOrInterfaceTypeDetails> entityProjectionsForEntity =
        getEntityProjectionLocator().getEntityProjectionsForEntity(entity);
    for (ClassOrInterfaceTypeDetails details : entityProjectionsForEntity) {

      addI18nLabelsFromEntityFormatExpressions(properties, details);
    }

    return properties;
  }

  /**
   * Locates `@EntityFormat` annotations at type-level and relation fields and
   * adds a new message to provided map if the annotation has the `message` 
   * attribute.
   * 
   * @param properties the Map<String, String> to be added to message bundles.
   * @param details the ClassOrInterfaceTypeDetails to check.
   */
  private void addI18nLabelsFromEntityFormatExpressions(final Map<String, String> properties,
      ClassOrInterfaceTypeDetails details) {
    // Get all projection member details
    if (details != null) {
      MemberDetails memberDetails =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), details);

      // Search for @EntityFormat at type-level
      AnnotationMetadata entityFormatTypeLevelAnnotation =
          memberDetails.getAnnotation(SpringletsJavaType.SPRINGLETS_ENTITY_FORMAT);
      if (entityFormatTypeLevelAnnotation != null) {
        AnnotationAttributeValue<Object> messageAttribute =
            entityFormatTypeLevelAnnotation.getAttribute("message");
        if (messageAttribute != null) {
          properties.put((String) messageAttribute.getValue(), "TO BE MODIFIED BY DEVELOPER");
        }
      }

      // Search for @EntityFormat at relation fields
      for (FieldMetadata field : memberDetails.getFields()) {
        AnnotationMetadata entityFormatAnnotation =
            field.getAnnotation(SpringletsJavaType.SPRINGLETS_ENTITY_FORMAT);
        if (entityFormatAnnotation != null) {
          AnnotationAttributeValue<Object> messageAttribute =
              entityFormatAnnotation.getAttribute("message");
          if (messageAttribute != null) {
            properties.put((String) messageAttribute.getValue(), "TO BE MODIFIED BY DEVELOPER");
          }
        }
      }
    }
  }

  /**
   * This method obtains all necessary information about fields from each
   * one-to-one referenced entity.
   * 
   * @param fieldList the List with main entity fields.
   * @param checkMaxFields whether field number is restricted.
   * @param ctx the context with all the necessary information for view
   *          generation.
   * @param suffixId the suffix for items.
   * @param compositeReferencedFields the entity fields with one-to-one and
   *          composite relation
   * @param parentEntity the parent entity type
   * @return the Map where the key is the current entity field and the value is
   *         the referenced entity editable field list.
   */
  private Map<String, List<FieldItem>> getRelationFieldItems(List<FieldMetadata> fieldList,
      boolean checkMaxFields, ViewContext<T> ctx, String suffixId,
      List<String> compositeReferencedFields, JpaEntityMetadata parentEntity) {

    // Create the Map to return
    Map<String, List<FieldItem>> relationFieldsMap = new HashMap<String, List<FieldItem>>();

    for (FieldMetadata entityField : fieldList) {

      // If one-to-one composition field, get referenced entity fields instead
      // of parent field
      if (compositeReferencedFields.contains(entityField.getFieldName().getSymbolName())) {

        // Create the list for related entity fields
        List<FieldItem> referencedEntityFieldItems = new ArrayList<FieldItem>();

        // Get referenced entity fields
        List<FieldMetadata> referencedEntityFields =
            getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
                getTypeLocationService().getTypeDetails(entityField.getFieldType())).getFields();
        List<FieldMetadata> referencedEntityEditableFields =
            getEditableFields(referencedEntityFields);

        // Add referenced entity field view items
        for (FieldMetadata referencedEntityField : referencedEntityEditableFields) {

          // If references the parent entity, don't add it
          if (referencedEntityField.getFieldType().equals(parentEntity.getAnnotatedEntity())) {
            continue;
          }

          FieldItem fieldItem =
              createFieldItem(parentEntity, referencedEntityField, entityField.getFieldType()
                  .getSimpleTypeName(), suffixId, ctx, entityField.getFieldName().getSymbolName());

          if (fieldItem != null) {
            referencedEntityFieldItems.add(fieldItem);
          }

          if (referencedEntityFieldItems.size() >= MAX_FIELDS_TO_ADD && checkMaxFields) {
            break;
          }
        }

        // Add entity to referenced entities map
        if (!relationFieldsMap.containsKey(entityField.getFieldName().getSymbolName())) {
          relationFieldsMap.put(entityField.getFieldName().getSymbolName(),
              referencedEntityFieldItems);
        }
      }
    }
    return relationFieldsMap;
  }

  /**
   * Checks if entity has child one-to-one composition relations and gets 
   * referenced fields info for adding it to the view context.
   * 
   * @param entityMetadata the JpaEntityMetadata from parent entity 
   * @param entityDetails the MemeberDetails from parent entity
   * @param ctx the ViewContext of view to create
   * @return a Map<String, List<FieldItem>> where the keys are the referenced 
   *            field names on parent entity and the values are the referenced 
   *            entity fields info to add to ViewContext
   */
  private Map<String, List<FieldItem>> manageChildcompositionFields(
      JpaEntityMetadata entityMetadata, MemberDetails entityDetails, ViewContext<T> ctx) {
    // Manage referenced fields from one-to-one composite relations
    Map<String, RelationInfo> relationInfos = entityMetadata.getRelationInfos();
    List<String> compositeReferencedFields = new ArrayList<String>();
    for (Entry<String, RelationInfo> entry : relationInfos.entrySet()) {
      if (entry.getValue().type.name().equals(JpaRelationType.COMPOSITION.name())
          && entry.getValue().cardinality.name().equals(Cardinality.ONE_TO_ONE.name())) {
        compositeReferencedFields.add(entry.getKey());
      }
    }
    Map<String, List<FieldItem>> compositeRelationFields =
        getRelationFieldItems(getEditableFields(entityDetails.getFields()), false, ctx,
            FIELD_SUFFIX, compositeReferencedFields, entityMetadata);
    return compositeRelationFields;
  }

  /**
   * Builds the label of the specified field by joining its names and adding it
   * to the entity label
   *
   * @param entity the entity name
   * @param fieldNames list of fields
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

  protected RepositoryJpaLocator getRepositoryJpaLocator() {
    return serviceInstaceManager.getServiceInstance(this, RepositoryJpaLocator.class);
  }

  protected ControllerOperations getControllerOperations() {
    return serviceInstaceManager.getServiceInstance(this, ControllerOperations.class);
  }

  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  protected EntityProjectionLocator getEntityProjectionLocator() {
    return serviceInstaceManager.getServiceInstance(this, EntityProjectionLocator.class);
  }
}
