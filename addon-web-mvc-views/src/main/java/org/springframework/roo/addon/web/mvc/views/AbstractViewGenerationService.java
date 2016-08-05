package org.springframework.roo.addon.web.mvc.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

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
import org.springframework.roo.addon.web.mvc.views.components.FieldItem;
import org.springframework.roo.addon.web.mvc.views.components.FieldTypes;
import org.springframework.roo.addon.web.mvc.views.components.MenuEntry;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * 
 * This abstract class implements MVCViewGenerationService interface
 * that provides all necessary elements to generate views inside project.
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

  protected abstract DOC merge(DOC existingDoc, DOC newDoc, List<String> requiredIds);

  protected abstract String getTemplatesLocation();

  protected abstract void writeDoc(DOC document, String viewPath);

  @Override
  public void addListView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldItem> fields = getFieldViewItems(entityDetails, ctx.getEntityName(), true, ctx);
    List<FieldItem> details = getDetailsFieldViewItems(entityDetails, ctx.getEntityName(), ctx);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("details", details);

    // Process elements to generate 
    DOC newDoc = process("list", ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/list")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc =
          merge(loadExistingDoc(viewName), newDoc, Arrays.asList(ctx.getEntityName() + "Table"));
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addShowView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldItem> fields = getFieldViewItems(entityDetails, ctx.getEntityName(), false, ctx);
    List<FieldItem> details = getDetailsFieldViewItems(entityDetails, ctx.getEntityName(), ctx);

    ctx.addExtraParameter("fields", fields);
    ctx.addExtraParameter("details", details);

    // Process elements to generate 
    DOC newDoc = process("show", ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/show")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {

      List<String> requiredIds = new ArrayList<String>();
      for (FieldItem field : fields) {
        requiredIds.add(field.getFieldName());
      }
      newDoc = merge(loadExistingDoc(viewName), newDoc, requiredIds);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addCreateView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldItem> fields = getFieldViewItems(entityDetails, ctx.getEntityName(), false, ctx);

    ctx.addExtraParameter("fields", fields);

    // Process elements to generate 
    DOC newDoc = process("create", ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/create")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      List<String> requiredIds = new ArrayList<String>();
      for (FieldItem field : fields) {
        requiredIds.add(field.getFieldName());
      }
      newDoc = merge(loadExistingDoc(viewName), newDoc, requiredIds);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addUpdateView(String moduleName, MemberDetails entityDetails, ViewContext ctx) {

    // Getting entity fields that should be included on view
    List<FieldItem> fields = getFieldViewItems(entityDetails, ctx.getEntityName(), false, ctx);

    ctx.addExtraParameter("fields", fields);

    // Process elements to generate 
    DOC newDoc = process("edit", ctx);

    // Getting new viewName
    String viewName =
        getViewsFolder(moduleName).concat(ctx.getControllerPath()).concat("/").concat("/edit")
            .concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      List<String> requiredIds = new ArrayList<String>();
      for (FieldItem field : fields) {
        requiredIds.add(field.getFieldName());
      }
      newDoc = merge(loadExistingDoc(viewName), newDoc, requiredIds);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFinderView(String moduleName, MemberDetails entity, String finderName,
      ViewContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addIndexView(String moduleName, ViewContext ctx) {

    // Process elements to generate 
    DOC newDoc = process("index", ctx);

    // Getting new viewName
    String viewName = getViewsFolder(moduleName).concat("/index").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addErrorView(String moduleName, ViewContext ctx) {

    // Process elements to generate 
    DOC newDoc = process("error", ctx);

    // Getting new viewName
    String viewName = getViewsFolder(moduleName).concat("/error").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addDefaultLayout(String moduleName, ViewContext ctx) {

    // Process elements to generate 
    DOC newDoc = process("layouts/default-layout", ctx);

    // Getting new viewName
    String viewName =
        getLayoutsFolder(moduleName).concat("/default-layout").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addFooter(String moduleName, ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/footer", ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/footer").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addHeader(String moduleName, ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/header", ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/header").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addMenu(String moduleName, ViewContext ctx) {
    // First of all, generate a list of MenuEntries based on existing controllers
    List<MenuEntry> menuEntries = new ArrayList<MenuEntry>();

    Set<ClassOrInterfaceTypeDetails> existingControllers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_CONTROLLER);

    Iterator<ClassOrInterfaceTypeDetails> it = existingControllers.iterator();

    while (it.hasNext()) {
      // Create new menuEntry element for every controller
      MenuEntry menuEntry = new MenuEntry();
      // Getting controller and its information
      ClassOrInterfaceTypeDetails controller = it.next();
      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
      JavaType entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();
      String path = (String) controllerAnnotation.getAttribute("path").getValue();

      // Include info inside menuEntry
      menuEntry.setEntityName(entity.getSimpleTypeName());
      menuEntry.setPath(path);
      menuEntry.setEntityLabel(FieldItem.buildLabel(entity.getSimpleTypeName(), ""));
      menuEntry.setEntityPluralLabel(FieldItem.buildLabel(entity.getSimpleTypeName(), "plural"));

      // Add new menu entry to menuEntries list
      menuEntries.add(menuEntry);
    }

    ctx.addExtraParameter("menuEntries", menuEntries);

    // Generate ids to search when merge new and existing doc
    List<String> requiredIds = new ArrayList<String>();
    for (MenuEntry entry : menuEntries) {
      requiredIds.add(entry.getEntityName() + "Entry");
    }

    // Process elements to generate 
    DOC newDoc = process("fragments/menu", ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/menu").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, requiredIds);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addModal(String moduleName, ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/modal", ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/modal").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addSession(String moduleName, ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/session", ctx);

    // Getting new viewName
    String viewName = getFragmentsFolder(moduleName).concat("/session").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
    }

    // Write newDoc on disk
    writeDoc(newDoc, viewName);

  }

  @Override
  public void addSessionLinks(String moduleName, ViewContext ctx) {
    // Process elements to generate 
    DOC newDoc = process("fragments/session-links", ctx);

    // Getting new viewName
    String viewName =
        getFragmentsFolder(moduleName).concat("/session-links").concat(getViewsExtension());

    // Check if new view to generate exists or not
    if (existsFile(viewName)) {
      newDoc = merge(loadExistingDoc(viewName), newDoc, null);
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
    DOC newDoc = process("fragments/languages", ctx);

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
      newDoc = merge(loadExistingDoc(viewName), newDoc, requiredIds);
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
   * @param entityDetails
   * @param entityName
   * @param checkMaxFields
   * @param ctx
   * 
   * @return List that contains FieldMetadata that will be added to the view.
   */
  protected List<FieldItem> getFieldViewItems(MemberDetails entityDetails, String entityName,
      boolean checkMaxFields, ViewContext ctx) {
    // Getting entity fields
    List<FieldMetadata> entityFields = entityDetails.getFields();
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
        FieldItem fieldItem = new FieldItem(entityField.getFieldName().getSymbolName(), entityName);

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
          // Saving enum and items to display. Same name as populateForm method
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
          // Ignore details. To obtain details uses getDetailsFieldViewItems method
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
   * This method obtains all necessary information about details fields from entity
   * and returns a List of FieldItem.
   * 
   * @param entityDetails
   * @param entityName
   * @param ctx
   * 
   * @return List that contains FieldMetadata that will be added to the view.
   */
  protected List<FieldItem> getDetailsFieldViewItems(MemberDetails entityDetails,
      String entityName, ViewContext ctx) {
    // Getting entity fields
    List<FieldMetadata> entityFields = entityDetails.getFields();

    List<FieldItem> detailFieldViewItems = new ArrayList<FieldItem>();
    for (FieldMetadata entityField : entityFields) {
      // Exclude id and version fields
      if (entityField.getAnnotation(JpaJavaType.ID) == null
          && entityField.getAnnotation(JpaJavaType.VERSION) == null) {

        // Generating new FieldItem element
        FieldItem fieldItem = new FieldItem(entityField.getFieldName().getSymbolName(), entityName);

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

            fieldItem.setType(FieldTypes.LIST.toString());

            // Saving necessary configuration
            fieldItem.addConfigurationElement("referencedFieldType",
                referencedField.getSimpleTypeName());

            // Getting identifier field
            List<FieldMetadata> identifierFields =
                getPersistenceMemberLocator().getIdentifierFields(referencedField);
            fieldItem.addConfigurationElement("identifierField", identifierFields.get(0)
                .getFieldName().getSymbolName());


            fieldItem.addConfigurationElement("controllerPath", "/"
                + entityField.getFieldName().getSymbolName().toLowerCase());

            // Getting referencedfield label plural
            fieldItem.addConfigurationElement("referencedFieldLabel",
                FieldItem.buildLabel(entityName, entityField.getFieldName().getSymbolName()));

            // Getting all referenced fields
            fieldItem.addConfigurationElement(
                "referenceFieldFields",
                getFieldViewItems(
                    getMemberDetailsScanner().getMemberDetails(getClass().toString(),
                        referencedFieldDetails), entityName + "."
                        + entityField.getFieldName().getSymbolName(), true, ctx));

          } else {
            // Ignore set or list which base types are not entity field
            continue;
          }

        } else {
          // Ignore not details fields
          continue;
        }

        detailFieldViewItems.add(fieldItem);
      }
    }

    return detailFieldViewItems;
  }

  /**
   * This method obtains all necessary configuration to be able to work
   * with reference fields.
   * 
   * Complete provided FieldItem with extra fields. If some extra configuration 
   * is not available, returns false to prevent that this field will be added.
   * If everything is ok, returns true to add this field to generated view.
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

    // Add the controllerPath related to the referencedEntity to configuration
    Iterator<ClassOrInterfaceTypeDetails> it = allControllers.iterator();
    String referencedPath = "";
    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails controller = it.next();
      AnnotationMetadata controllerAnnotation =
          controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
      AnnotationAttributeValue<JavaType> entityAttr = controllerAnnotation.getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(typeDetails.getType())) {
        AnnotationAttributeValue<String> controllerPath = controllerAnnotation.getAttribute("path");
        referencedPath = controllerPath.getValue();
      }
    }

    fieldItem.addConfigurationElement("referencedPath", referencedPath);

    // Add one or more fields to configuration, to be able to display content
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
