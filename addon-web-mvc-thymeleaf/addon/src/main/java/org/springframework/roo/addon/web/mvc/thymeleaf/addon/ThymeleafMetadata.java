package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.SpringJavaType.DELETE_MAPPING;
import static org.springframework.roo.model.SpringJavaType.GET_MAPPING;
import static org.springframework.roo.model.SpringJavaType.INIT_BINDER;
import static org.springframework.roo.model.SpringJavaType.POST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_ENTITY;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_DATATABLES;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_DATATABLES_DATA;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_NOT_FOUND_EXCEPTION;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.helper.Validate;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.RelationInfoExtended;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.addon.web.mvc.views.AbstractViewMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooThymeleaf}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class ThymeleafMetadata extends AbstractViewMetadata {

  // Method names
  protected static final JavaSymbolName CREATE_METHOD_NAME = new JavaSymbolName("create");
  protected static final JavaSymbolName LIST_METHOD_NAME = new JavaSymbolName("list");
  protected static final JavaSymbolName DELETE_METHOD_NAME = new JavaSymbolName("delete");
  protected static final JavaSymbolName LIST_URI_METHOD_NAME = new JavaSymbolName("listURI");
  protected static final JavaSymbolName LIST_DATATABLES_METHOD_NAME = new JavaSymbolName(
      "datatables");
  protected static final JavaSymbolName LIST_DATATABLES_DETAILS_METHOD_NAME = new JavaSymbolName(
      "datatables");
  protected static final JavaSymbolName SELECT2_METHOD_NAME = new JavaSymbolName("select2");
  protected static final JavaSymbolName SHOW_URI_METHOD_NAME = new JavaSymbolName("showURI");
  protected static final JavaSymbolName SHOW_METHOD_NAME = new JavaSymbolName("show");
  protected static final JavaSymbolName CREATE_FORM_METHOD_NAME = new JavaSymbolName("createForm");
  protected static final JavaSymbolName EDIT_FORM_METHOD_NAME = new JavaSymbolName("editForm");
  protected static final JavaSymbolName UPDATE_METHOD_NAME = new JavaSymbolName("update");


  private static final AnnotatedJavaType PAGEABLE_PARAM = new AnnotatedJavaType(
      SpringJavaType.PAGEABLE);
  private static final JavaSymbolName PAGEABLE_PARAM_NAME = new JavaSymbolName("pageable");
  private static final AnnotatedJavaType DATATABLES_PAGEABLE_PARAM = new AnnotatedJavaType(
      SpringletsJavaType.SPRINGLETS_DATATABLES_PAGEABLE);
  private static final JavaSymbolName DATATABLES_PAGEABLE_PARAM_NAME = new JavaSymbolName(
      "pageable");
  private static final AnnotatedJavaType GLOBAL_SEARCH_PARAM = new AnnotatedJavaType(
      SPRINGLETS_GLOBAL_SEARCH);
  private static final JavaSymbolName GLOBAL_SEARCH_PARAM_NAME = new JavaSymbolName("search");
  private static final AnnotatedJavaType MODEL_PARAM = new AnnotatedJavaType(SpringJavaType.MODEL);
  private static final JavaSymbolName MODEL_PARAM_NAME = new JavaSymbolName("model");
  private static final JavaSymbolName FORM_BEAN_PARAM_NAME = new JavaSymbolName("formBean");
  private static final AnnotatedJavaType LOCALE_PARAM = new AnnotatedJavaType(JdkJavaType.LOCALE);
  private static final AnnotationMetadataBuilder RESPONSE_BODY_ANNOTATION =
      new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
  private static final JavaType JODA_DATETIME_FORMAT_JAVA_TYPE = new JavaType(
      "org.joda.time.format.DateTimeFormat");
  private static final JavaSymbolName MESSAGE_SOURCE = new JavaSymbolName("messageSource");
  private static final AnnotationMetadata ANN_METADATA_MODEL_ATTRIBUTE = AnnotationMetadataBuilder
      .getInstance(SpringJavaType.MODEL_ATTRIBUTE);

  private static final AnnotationMetadata ANN_METADATA_VALID = AnnotationMetadataBuilder
      .getInstance(Jsr303JavaType.VALID);

  private static final String PROVIDES_TYPE_STRING = ThymeleafMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaSymbolName DRAW_PARAM_NAME = new JavaSymbolName("draw");
  private static final String LINK_BUILDER_ARGUMENT_NAME = "linkBuilder";

  private final boolean readOnly;
  private final ControllerMetadata controllerMetadata;
  private final ControllerType type;
  private final ConstructorMetadata constructor;
  private final ServiceMetadata serviceMetadata;
  private final JavaType entity;
  private final String entityPlural;
  private final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne;
  private final String entityItemName;
  private final JpaEntityMetadata entityMetadata;
  private final JavaType itemController;
  private final JavaType detailItemController;
  private final JavaType detailCollectionController;
  private final String entityIdentifierPlural;
  private final String entityIdentifier;
  private final Map<String, String> mvcMethodNames;
  private final JavaType relatedCollectionController;
  private final JavaType relatedItemController;

  // Common method
  private final MethodMetadata initBinderMethod;

  // Collection Methods
  private final MethodMetadata createFormMethod;
  private final MethodMetadata createMethod;
  private final MethodMetadata listMethod;
  private final MethodMetadata listDatatablesMethod;
  private final MethodMetadata select2Method;

  // Item Methods
  private final MethodMetadata modelAttributeMethod;
  private final MethodMetadata editFormMethod;
  private final MethodMetadata updateMethod;
  private final MethodMetadata deleteMethod;
  private final MethodMetadata showMethod;
  private final MethodMetadata populateFormMethod;
  private final MethodMetadata populateFormatsMethod;

  // Details Methods
  private final Map<RelationInfo, MethodMetadata> modelAttributeDetailsMethod;
  private final MethodMetadata listDatatablesDetailsMethod;
  private final MethodMetadata createFormDetailsMethod;
  private final MethodMetadata createDetailsMethod;
  private final MethodMetadata removeFromDetailsMethod;


  // Finder Methods
  private final Map<String, MethodMetadata> finderFormMethods;
  private final Map<String, MethodMetadata> finderListMethods;
  private final Map<String, MethodMetadata> finderDatatableMethods;

  // TODO
  // private final Map<String, MethodMetadata> finderListMethods;
  // ????

  private final List<MethodMetadata> allMethods;
  private final FieldMetadata messageSourceField;
  private final FieldMetadata methodLinkBuilderFactoryField;
  private final String viewsPath;
  private final JavaType collectionController;
  private final List<FieldMetadata> dateTimeFields;
  private final List<FieldMetadata> enumFields;
  private final Map<JavaType, List<FieldMetadata>> formBeansDateTimeFields;
  private final Map<JavaType, List<FieldMetadata>> formBeansEnumFields;
  private final MethodMetadata editFormDetailMethod;
  private final MethodMetadata updateDetailMethod;
  private final MethodMetadata deleteDetailMethod;
  private final MethodMetadata showDetailMethod;
  private final String ITEM_LINK = "itemLink";
  private final String COLLECTION_LINK = "collectionLink";

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Constructor
   *
   * @param identifier
   *            the identifier for this item of metadata (required)
   * @param aspectName
   *            the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata
   *            the governor, which is expected to contain a
   *            {@link ClassOrInterfaceTypeDetails} (required)
   * @param collectionController
   * @param formBeansEnumFields
   * @param formBeansDateTimeFields
   * @param detailsCollectionController
   * @param relatedCollectionController
  
   */
  public ThymeleafMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      ControllerMetadata controllerMetadata, ServiceMetadata serviceMetadata,
      JpaEntityMetadata entityMetadata, String entityPlural, String entityIdentifierPlural,
      final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne,
      final JavaType itemController, final JavaType collectionController,
      final List<FieldMetadata> dateTimeFields, final List<FieldMetadata> enumFields,
      final Map<String, MethodMetadata> findersToAdd,
      final Map<JavaType, List<FieldMetadata>> formBeansDateTimeFields,
      final Map<JavaType, List<FieldMetadata>> formBeansEnumFields,
      final JavaType detailItemController, JavaType detailsCollectionController,
      final JavaType relatedCollectionController, final JavaType relatedItemController) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);


    this.readOnly = entityMetadata.isReadOnly();
    this.controllerMetadata = controllerMetadata;
    this.type = this.controllerMetadata.getType();
    this.serviceMetadata = serviceMetadata;
    this.entity = serviceMetadata.getEntity();
    this.entityMetadata = entityMetadata;
    this.entityIdentifier =
        entityMetadata.getCurrentIndentifierField().getFieldName().getSymbolName();
    this.entityIdentifierPlural = entityIdentifierPlural;
    this.entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());
    this.entityPlural = entityPlural;
    this.compositionRelationOneToOne = compositionRelationOneToOne;
    this.itemController = itemController;
    this.detailItemController = detailItemController;
    this.detailCollectionController = detailsCollectionController;
    this.collectionController = collectionController;
    this.dateTimeFields = Collections.unmodifiableList(dateTimeFields);
    this.enumFields = Collections.unmodifiableList(enumFields);
    this.formBeansDateTimeFields = formBeansDateTimeFields;
    this.formBeansEnumFields = formBeansEnumFields;
    this.relatedCollectionController = relatedCollectionController;
    this.relatedItemController = relatedItemController;
    this.viewsPath =
        controllerMetadata.getPath().startsWith("/") ? controllerMetadata.getPath().substring(1)
            : controllerMetadata.getPath();

    this.mvcMethodNames = new HashMap<String, String>();

    //Add @Controller
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.CONTROLLER));
    // Add @RequestMapping
    ensureGovernorIsAnnotated(getRequestMappingAnnotation());

    this.messageSourceField = getMessageSourceField();
    ensureGovernorHasField(new FieldMetadataBuilder(this.messageSourceField));

    List<MethodMetadata> allMethods = new ArrayList<MethodMetadata>();

    switch (this.type) {
      case COLLECTION: {

        // Add MethodLinkBuilderFactory field  
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(ITEM_LINK, this.itemController);
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        // Build constructor
        String linkBuilderLine =
            String.format("this.%s = linkBuilder.of(%s.class);", ITEM_LINK,
                getNameOfJavaType(this.itemController));
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        this.listMethod = addAndGet(getListMethod(), allMethods);
        this.listDatatablesMethod = addAndGet(getListDatatablesMethod(), allMethods);

        boolean generateSelect2 = true;
        // XXX To Be Analyzed
        //        for (FieldMetadata relationField : entityMetadata.getRelationsAsChild().values()) {
        //          if (relationField.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
        //              || relationField.getAnnotation(JpaJavaType.MANY_TO_MANY) != null) {
        //            generateSelect2 = true;
        //            break;
        //          }
        //        }
        if (generateSelect2) {
          this.select2Method = addAndGet(getSelect2Method(), allMethods);
        } else {
          this.select2Method = null;
        }

        if (readOnly) {
          this.initBinderMethod = null;
          this.populateFormMethod = null;
          this.populateFormatsMethod = null;
          this.createMethod = null;
          this.createFormMethod = null;
        } else {
          this.initBinderMethod = addAndGet(getInitBinderMethod(entity), allMethods);
          this.populateFormatsMethod = addAndGet(getPopulateFormatsMethod(), allMethods);
          this.populateFormMethod = addAndGet(getPopulateFormMethod(), allMethods);
          this.createMethod = addAndGet(getCreateMethod(), allMethods);
          this.createFormMethod = addAndGet(getCreateFormMethod(), allMethods);
        }


        this.modelAttributeMethod = null;
        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.modelAttributeDetailsMethod = null;
        this.listDatatablesDetailsMethod = null;
        this.finderListMethods = null;
        this.finderDatatableMethods = null;
        this.finderFormMethods = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;
        this.removeFromDetailsMethod = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;

        break;
      }
      case ITEM: {

        // Add MethodLinkBuilderFactory field  
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(ITEM_LINK, this.governorTypeDetails.getType());
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        // Build constructor
        String linkBuilderLine =
            String.format("this.%s = linkBuilder.of(%s.class);", ITEM_LINK,
                getNameOfJavaType(this.governorTypeDetails.getType()));
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        this.showMethod = addAndGet(getShowMethod(), allMethods);
        if (readOnly) {
          this.editFormMethod = null;
          this.updateMethod = null;
          this.deleteMethod = null;
          this.populateFormatsMethod = null;
          this.populateFormMethod = null;
          this.initBinderMethod = null;
        } else {
          this.initBinderMethod = addAndGet(getInitBinderMethod(entity), allMethods);
          this.populateFormatsMethod = addAndGet(getPopulateFormatsMethod(), allMethods);
          this.populateFormMethod = addAndGet(getPopulateFormMethod(), allMethods);
          this.editFormMethod = addAndGet(getEditFormMethod(), allMethods);
          this.updateMethod = addAndGet(getUpdateMethod(), allMethods);
          this.deleteMethod = addAndGet(getDeleteMethod(), allMethods);
        }


        this.listMethod = null;
        this.listDatatablesMethod = null;
        this.createMethod = null;
        this.createFormMethod = null;
        this.modelAttributeDetailsMethod = null;
        this.listDatatablesDetailsMethod = null;
        this.finderListMethods = null;
        this.finderDatatableMethods = null;
        this.finderFormMethods = null;
        this.select2Method = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;
        this.removeFromDetailsMethod = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        break;
      }
      case SEARCH: {

        this.methodLinkBuilderFactoryField = null;

        // Build constructor
        String linkBuilderLine = "";
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        Map<String, MethodMetadata> tmpFindersDtt = new TreeMap<String, MethodMetadata>();
        Map<String, MethodMetadata> tmpFinderLists = new TreeMap<String, MethodMetadata>();
        Map<String, MethodMetadata> tmpFinderForms = new TreeMap<String, MethodMetadata>();
        MethodMetadata finderFormMethod, finderMethod, finderDtMethod;
        for (Entry<String, MethodMetadata> finder : findersToAdd.entrySet()) {
          finderFormMethod =
              getFinderFormMethodForFinderInService(finder.getKey(), finder.getValue());
          tmpFinderForms.put(finder.getKey(), addAndGet(finderFormMethod, allMethods));

          finderMethod = getFinderMethodForFinderInService(finder.getKey(), finder.getValue());
          tmpFinderLists.put(finder.getKey(), addAndGet(finderMethod, allMethods));

          finderDtMethod =
              getFinderDatatablesMethodForFinderInService(finder.getKey(), finder.getValue());
          tmpFindersDtt.put(finder.getKey(), addAndGet(finderDtMethod, allMethods));


        }
        this.finderDatatableMethods = Collections.unmodifiableMap(tmpFindersDtt);
        this.finderListMethods = Collections.unmodifiableMap(tmpFinderLists);
        this.finderFormMethods = Collections.unmodifiableMap(tmpFinderForms);
        // FIXME We need more method to handle it... To Be Defined!!!

        this.listMethod = null;
        this.listDatatablesMethod = null;
        this.createMethod = null;
        this.createFormMethod = null;
        this.initBinderMethod = null;

        this.modelAttributeMethod = null;
        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.populateFormMethod = null;
        this.populateFormatsMethod = null;
        this.modelAttributeDetailsMethod = null;
        this.listDatatablesDetailsMethod = null;
        this.select2Method = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;
        this.removeFromDetailsMethod = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        break;
      }
      case DETAIL: {

        // Add MethodLinkBuilderFactory field  
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(COLLECTION_LINK, this.collectionController);
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        // Build constructor
        String linkBuilderLine =
            String.format("this.%s = linkBuilder.of(%s.class);", COLLECTION_LINK,
                getNameOfJavaType(this.collectionController));
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        Map<RelationInfo, MethodMetadata> modelAtributeDetailsMethod =
            new TreeMap<RelationInfo, MethodMetadata>();
        for (int i = 0; i < controllerMetadata.getDetailsFieldInfo().size() - 1; i++) {
          RelationInfo info = controllerMetadata.getDetailsFieldInfo().get(i);
          JavaType entityType = info.childType;
          MethodMetadata method =
              addAndGet(
                  getModelAttributeMethod(info.fieldName,
                      controllerMetadata.getServiceMetadataForEntity(entityType),
                      controllerMetadata.getDetailsServiceFields().get(entityType)), allMethods);
          modelAtributeDetailsMethod.put(info, method);
        }
        this.modelAttributeDetailsMethod = Collections.unmodifiableMap(modelAtributeDetailsMethod);
        this.populateFormatsMethod = addAndGet(getPopulateFormatsMethod(), allMethods);
        this.populateFormMethod = addAndGet(getPopulateFormMethod(), allMethods);
        this.listDatatablesDetailsMethod = addAndGet(getListDatatablesDetailMethod(), allMethods);
        this.createFormDetailsMethod = addAndGet(getCreateFormDetailsMethod(), allMethods);

        if (controllerMetadata.getLastDetailsInfo().type == JpaRelationType.AGGREGATION) {
          this.removeFromDetailsMethod = addAndGet(getRemoveFromDetailsMethod(), allMethods);
          this.createDetailsMethod = addAndGet(getCreateDetailsMethod(), allMethods);
          this.initBinderMethod = null;
        } else {
          this.initBinderMethod =
              addAndGet(getInitBinderMethod(controllerMetadata.getLastDetailEntity()), allMethods);
          this.createDetailsMethod = addAndGet(getCreateDetailsCompositionMethod(), allMethods);
          this.removeFromDetailsMethod = null;
        }

        this.listMethod = null;
        this.listDatatablesMethod = null;
        this.createMethod = null;
        this.createFormMethod = null;

        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.finderListMethods = null;
        this.finderDatatableMethods = null;
        this.finderFormMethods = null;
        this.select2Method = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        break;
      }
      case DETAIL_ITEM: {

        // Add MethodLinkBuilderFactory field  
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(COLLECTION_LINK, this.collectionController);
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        // Build constructor
        String linkBuilderLine =
            String.format("this.%s = linkBuilder.of(%s.class);", COLLECTION_LINK,
                getNameOfJavaType(this.collectionController));
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        Map<RelationInfo, MethodMetadata> modelAtributeDetailsMethod =
            new TreeMap<RelationInfo, MethodMetadata>();
        for (RelationInfo info : controllerMetadata.getDetailsFieldInfo()) {
          JavaType entityType = info.childType;
          MethodMetadata method =
              addAndGet(
                  getModelAttributeMethod(info.fieldName,
                      controllerMetadata.getServiceMetadataForEntity(entityType),
                      controllerMetadata.getDetailsServiceFields().get(entityType)), allMethods);
          modelAtributeDetailsMethod.put(info, method);
        }
        this.initBinderMethod =
            addAndGet(getInitBinderMethod(controllerMetadata.getLastDetailEntity()), allMethods);
        this.modelAttributeDetailsMethod = Collections.unmodifiableMap(modelAtributeDetailsMethod);

        this.populateFormatsMethod = addAndGet(getPopulateFormatsMethod(), allMethods);
        this.populateFormMethod = addAndGet(getPopulateFormMethod(), allMethods);

        this.editFormDetailMethod = addAndGet(getEditFormDetailMethod(), allMethods);
        this.updateDetailMethod = addAndGet(getUpdateDetailMethod(), allMethods);
        this.deleteDetailMethod = addAndGet(getDeleteDetailMethod(), allMethods);
        this.showDetailMethod = addAndGet(getShowDetailMethod(), allMethods);
        this.listMethod = null;
        this.listDatatablesMethod = null;
        this.createMethod = null;
        this.createFormMethod = null;

        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.finderDatatableMethods = null;
        this.finderListMethods = null;
        this.finderFormMethods = null;
        this.select2Method = null;
        this.listDatatablesDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;

        break;

      }
      default:
        throw new IllegalArgumentException("Unsupported Controller type: " + this.type.name());
    }

    this.allMethods = Collections.unmodifiableList(allMethods);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private FieldMetadata getMessageSourceField() {
    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), MESSAGE_SOURCE, SpringJavaType.MESSAGE_SOURCE)
        .build();
  }

  private MethodMetadata addAndGet(MethodMetadata method, List<MethodMetadata> allMethods) {
    allMethods.add(method);
    ensureGovernorHasMethod(new MethodMetadataBuilder(method));
    return method;
  }

  private ConstructorMetadata addAndGetConstructor(ConstructorMetadata constructor) {
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(constructor));
    return constructor;
  }

  private AnnotationMetadataBuilder getRequestMappingAnnotation() {
    AnnotationMetadataBuilder annotationBuilder =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_MAPPING);

    // Adding path attribute
    annotationBuilder.addStringAttribute("value", controllerMetadata.getRequestMappingValue());

    // Add name attribute
    annotationBuilder.addStringAttribute("name", this.getMvcControllerName());

    //Add produces
    annotationBuilder.addEnumAttribute("produces", SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE);

    return annotationBuilder;
  }


  private ConstructorMetadata getConstructor(String linkBuilderLine) {
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.setModifier(Modifier.PUBLIC);
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    // Getting serviceFieldName
    String serviceFieldName = controllerMetadata.getServiceField().getFieldName().getSymbolName();
    constructor.addParameter(serviceFieldName, controllerMetadata.getService());
    bodyBuilder
        .appendFormalLine(String.format("this.%s = %s;", serviceFieldName, serviceFieldName));


    if (this.type == ControllerType.DETAIL || this.type == ControllerType.DETAIL_ITEM) {

      for (FieldMetadata serviceField : controllerMetadata.getDetailsServiceFields().values()) {

        // Getting parentServiceFieldName
        String childServiceFieldName = serviceField.getFieldName().getSymbolName();

        // Adding parameters
        constructor.addParameter(childServiceFieldName, serviceField.getFieldType());

        // Generating body
        bodyBuilder.appendFormalLine(String.format("this.%s = %s;", childServiceFieldName,
            childServiceFieldName));
      }
    }


    String messageSourceName = messageSourceField.getFieldName().getSymbolName();
    constructor.addParameter(messageSourceName, messageSourceField.getFieldType());
    bodyBuilder.appendFormalLine("this.%1$s = %1$s;", messageSourceName);

    // Add ControllerMethodLinkBuilderFactory argument
    if (StringUtils.isNotBlank(linkBuilderLine)) {
      constructor.addParameter(LINK_BUILDER_ARGUMENT_NAME,
          SpringletsJavaType.SPRINGLETS_CONTROLLER_METHOD_LINK_BUILDER_FACTORY);
      bodyBuilder.appendFormalLine(linkBuilderLine);
      this.builder.getImportRegistrationResolver().addImport(
          SpringletsJavaType.SPRINGLETS_CONTROLLER_METHOD_LINK_BUILDER_FACTORY);
    }

    // Adding body
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();

  }


  /**
   * Generates a finder method which delegates on entity service to get result
   *
   * @param finderName
   * @param serviceFinderMethod
   * @return
   */
  private MethodMetadata getFinderFormMethodForFinderInService(String finderName,
      MethodMetadata serviceFinderMethod) {

    // Define methodName
    String pathName = finderName;
    if (pathName.startsWith("findBy")) {
      pathName = pathName.replace("findBy", "by");
    }

    final JavaSymbolName methodName = new JavaSymbolName(pathName.concat("Form"));

    // Form Bean is always the first parameter of finder
    final JavaType formBean = serviceFinderMethod.getParameterTypes().get(0).getJavaType();
    List<AnnotationMetadata> formBeanAnnotations = new ArrayList<AnnotationMetadata>();
    AnnotationMetadataBuilder formBeanAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    formBeanAnnotation.addStringAttribute("value", FORM_BEAN_PARAM_NAME.getSymbolName());
    formBeanAnnotations.add(formBeanAnnotation.build());
    AnnotatedJavaType annotatedFormBean = new AnnotatedJavaType(formBean, formBeanAnnotations);

    // Including annotated formBean parameter and Model parameter
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(annotatedFormBean);
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(FORM_BEAN_PARAM_NAME);
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    // TODO Delegates on ControllerOperations to obtain the URL for this finder
    getMappingAnnotation.addStringAttribute("value", "/" + pathName + "/search-form");
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    buildPopulateFormatBody(bodyBuilder, formBeansDateTimeFields.get(formBean));

    final List<FieldMetadata> enumFileds = formBeansEnumFields.get(formBean);
    if (enumFileds != null && !enumFileds.isEmpty()) {
      buildPopulateEnumsBody(bodyBuilder, formBeansEnumFields.get(formBean));
    }

    // return new ModelAndView("customers/findByFirstNameLastNameForm");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath, finderName.concat("Form"));


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Generates a finder method which delegates on entity service to get result
   *
   * @param finderName
   * @param serviceFinderMethod
   * @return
   */
  private MethodMetadata getFinderMethodForFinderInService(String finderName,
      MethodMetadata serviceFinderMethod) {

    // Define methodName
    String pathName = finderName;
    if (pathName.startsWith("findBy")) {
      pathName = pathName.replace("findBy", "by");
    }

    final JavaSymbolName methodName = new JavaSymbolName(pathName);

    // Form Bean is always the first parameter of finder
    final JavaType formBean = serviceFinderMethod.getParameterTypes().get(0).getJavaType();
    List<AnnotationMetadata> formBeanAnnotations = new ArrayList<AnnotationMetadata>();
    AnnotationMetadataBuilder formBeanAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    formBeanAnnotation.addStringAttribute("value", FORM_BEAN_PARAM_NAME.getSymbolName());
    formBeanAnnotations.add(formBeanAnnotation.build());
    AnnotatedJavaType annotatedFormBean = new AnnotatedJavaType(formBean, formBeanAnnotations);

    // Including annotated formBean parameter and Model parameter
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(annotatedFormBean);
    parameterTypes.add(MODEL_PARAM);


    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(FORM_BEAN_PARAM_NAME);
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    // TODO Delegates on ControllerOperations to obtain the URL for this finder
    getMappingAnnotation.addStringAttribute("value", "/" + pathName);
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    buildPopulateFormatBody(bodyBuilder, formBeansDateTimeFields.get(formBean));

    final List<FieldMetadata> enumFileds = formBeansEnumFields.get(formBean);
    if (enumFileds != null && !enumFileds.isEmpty()) {
      buildPopulateEnumsBody(bodyBuilder, formBeansEnumFields.get(formBean));
    }

    // return new ModelAndView("customers/findByFirstNameLastName");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath, finderName);


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }



  /**
   * Generates a finder method which delegates on entity service to get result
   *
   * @param finderName
   * @param serviceFinderMethod
   * @return
   */
  private MethodMetadata getFinderDatatablesMethodForFinderInService(String finderName,
      MethodMetadata serviceFinderMethod) {

    // Define methodName
    String pathName = finderName;
    if (pathName.startsWith("findBy")) {
      pathName = pathName.replace("findBy", "by");
    }
    final JavaSymbolName methodName = new JavaSymbolName(pathName.concat("Dt"));

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Form Bean is always the first parameter of finder
    final JavaType formBean = serviceFinderMethod.getParameterTypes().get(0).getJavaType();
    List<AnnotationMetadata> formBeanAnnotations = new ArrayList<AnnotationMetadata>();
    AnnotationMetadataBuilder formBeanAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    formBeanAnnotation.addStringAttribute("value", FORM_BEAN_PARAM_NAME.getSymbolName());
    formBeanAnnotations.add(formBeanAnnotation.build());
    AnnotatedJavaType annotatedFormBean = new AnnotatedJavaType(formBean, formBeanAnnotations);
    parameterTypes.add(annotatedFormBean);

    // Including GlobalSearch parameter and DatatablesPageable parameter
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    parameterTypes.add(DATATABLES_PAGEABLE_PARAM);

    // Including Draw parameter
    List<AnnotationMetadata> drawAnnotations = new ArrayList<AnnotationMetadata>();
    AnnotationMetadataBuilder drawAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_PARAM);
    drawAnnotation.addEnumAttribute("value", SpringletsJavaType.SPRINGLETS_DATATABLES,
        "PARAMETER_DRAW");
    drawAnnotations.add(drawAnnotation.build());
    AnnotatedJavaType annotatedDraw = new AnnotatedJavaType(JavaType.INT_OBJECT, drawAnnotations);
    parameterTypes.add(annotatedDraw);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final List<String> parameterStrings = new ArrayList<String>();
    parameterNames.add(FORM_BEAN_PARAM_NAME);
    parameterStrings.add(FORM_BEAN_PARAM_NAME.getSymbolName());
    parameterNames.add(GLOBAL_SEARCH_PARAM_NAME);
    parameterStrings.add(GLOBAL_SEARCH_PARAM_NAME.getSymbolName());
    parameterNames.add(DATATABLES_PAGEABLE_PARAM_NAME);
    parameterStrings.add(DATATABLES_PAGEABLE_PARAM_NAME.getSymbolName());
    parameterNames.add(DRAW_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    // TODO Delegates on ControllerOperations to obtain the URL for this finder
    getMappingAnnotation.addStringAttribute("value", "/" + pathName + "/dt");
    getMappingAnnotation.addEnumAttribute("produces", SPRINGLETS_DATATABLES, "MEDIA_TYPE");
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Adding @ResoponseBody
    annotations.add(RESPONSE_BODY_ANNOTATION);

    // Generating returnType
    JavaType serviceReturnType = serviceFinderMethod.getReturnType();
    JavaType datatablesDataReturnType =
        serviceReturnType.getParameters().isEmpty() ? serviceReturnType.getBaseType()
            : serviceReturnType.getParameters().get(0);
    JavaType returnType =
        JavaType.wrapperOf(RESPONSE_ENTITY, JavaType.wrapperOf(
            SpringletsJavaType.SPRINGLETS_DATATABLES_DATA, datatablesDataReturnType));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemNames = StringUtils.uncapitalize(this.entityPlural);

    // Page<Customer> customers = customerService.findAll(formBean, globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(serviceReturnType),
        itemNames, controllerMetadata.getServiceField().getFieldName(),
        serviceFinderMethod.getMethodName(), StringUtils.join(parameterStrings, ","));

    // long totalProductsCount = products.getTotalElements();
    String totalItemNamesCount = String.format("total%sCount", StringUtils.capitalize(itemNames));
    bodyBuilder.appendFormalLine(String.format("long %s = %s.getTotalElements();",
        totalItemNamesCount, itemNames));

    // if (search != null && StringUtils.hasText(search.getText())) {
    bodyBuilder.appendFormalLine(String.format("if (%s != null && %s.hasText(%s.getText())) {",
        GLOBAL_SEARCH_PARAM_NAME, getNameOfJavaType(SpringJavaType.STRING_UTILS),
        GLOBAL_SEARCH_PARAM_NAME));

    // Getting count method
    MethodMetadata countMethod = null;
    Map<JavaSymbolName, MethodMetadata> repositoryCustomFindersAndCounts =
        serviceMetadata.getRepositoryCustomFindersAndCounts();
    for (Entry<JavaSymbolName, MethodMetadata> entry : repositoryCustomFindersAndCounts.entrySet()) {
      if (entry.getKey().getSymbolName().equals(finderName)) {
        countMethod = entry.getValue();
      }
    }

    Validate
        .notNull(
            countMethod,
            String
                .format(
                    "ERROR: Is not possible to obtain the count method related with finder '%s'. Please, "
                        + "generate the count method before continue publishing this finder to the web layer.",
                    finderName));

    // totalProductsCount = productService.countByNameAndDescription(formBean);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("%s = %s.%s(%s);", totalItemNamesCount,
        controllerMetadata.getServiceField().getFieldName().getSymbolName(),
        countMethod.getMethodName(), FORM_BEAN_PARAM_NAME));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // DatatablesData<Product> datatablesData = new DatatablesData<Product>(products, totalProductsCount, draw);
    bodyBuilder.appendFormalLine(String.format(
        "%s<%s> datatablesData = new DatatablesData<%s>(%s, %s, %s);",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_DATATABLES_DATA),
        getNameOfJavaType(datatablesDataReturnType), getNameOfJavaType(datatablesDataReturnType),
        itemNames, totalItemNamesCount, DRAW_PARAM_NAME));

    // return ResponseEntity.ok(datatablesData);
    bodyBuilder.appendFormalLine(String.format("return %s.ok(datatablesData);",
        getNameOfJavaType(RESPONSE_ENTITY)));


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates getInitBinderMethod method
   *
   * @return
   */
  private MethodMetadata getInitBinderMethod(JavaType entity) {
    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName("init" + entity.getSimpleTypeName() + "Binder");

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.WEB_DATA_BINDER));
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("dataBinder"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    InvocableMemberBodyBuilder body = new InvocableMemberBodyBuilder();

    // dataBinder.setDisallowedFields("id");
    body.appendFormalLine("dataBinder.setDisallowedFields(\"%s\");", entityIdentifier);

    for (Pair<RelationInfo, JpaEntityMetadata> item : compositionRelationOneToOne) {
      body.appendFormalLine("dataBinder.setDisallowedFields(\"%s.%s\");", item.getKey().fieldName,
          item.getValue().getCurrentIndentifierField().getFieldName());
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, body);

    // Adding annotation
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @InitBinder annotation
    AnnotationMetadataBuilder getInitBinderAnnotation = new AnnotationMetadataBuilder(INIT_BINDER);
    getInitBinderAnnotation.addStringAttribute("value",
        StringUtils.uncapitalize(entity.getSimpleTypeName()));
    annotations.add(getInitBinderAnnotation);

    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Build a MethodLinkBuilderFactory field of provided type
   * 
   * @param fieldName
   * @return FieldMetadata
   */
  private FieldMetadata getMethodLinkBuilderFactoryField(String fieldName, JavaType controller) {
    FieldMetadataBuilder field =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName(fieldName),
            JavaType.wrapperOf(SpringletsJavaType.SPRINGLETS_METHOD_LINK_BUILDER_FACTORY,
                controller), null);
    return field.build();
  }

  /**
   * This method provides the getModelAttributeMethod() method
   *
   * @return MethodMetadata
   */
  private MethodMetadata getModelAttributeMethod() {
    return getModelAttributeMethod(StringUtils.uncapitalize(entity.getSimpleTypeName()),
        serviceMetadata, controllerMetadata.getServiceField());
  }

  /**
   * This method provides the method to add to manage a model attribute from a
   * path variable
   *
   * @param pathVariable
   * @param serviceMetadata
   * @param serviceField
   * @return MethodMetadata
   */
  private MethodMetadata getModelAttributeMethod(String pathVariable,
      ServiceMetadata serviceMetadata, FieldMetadata serviceField) {
    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName("get" + StringUtils.capitalize(pathVariable));

    final JavaType idType =
        serviceMetadata.getCurrentFindOneMethod().getParameterTypes().get(0).getJavaType();
    final JavaSymbolName idName =
        serviceMetadata.getCurrentFindOneMethod().getParameterNames().get(0);
    final JavaType entityType = serviceMetadata.getCurrentFindOneMethod().getReturnType();

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", StringUtils.uncapitalize(pathVariable));

    parameterTypes.add(new AnnotatedJavaType(idType, pathVariableAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(idName);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Customer customer = customerService.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(entityType), pathVariable,
        serviceField.getFieldName(), serviceMetadata.getCurrentFindOneMethod().getMethodName(),
        idName);

    // if (customer == null) {
    //   throw new NotFoundException("Customer not found");
    // }
    bodyBuilder.appendFormalLine("if (%s == null) {", pathVariable);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "throw new %s(String.format(\"%s with identifier '%%s' not found\",%s));",
        getNameOfJavaType(SPRINGLETS_NOT_FOUND_EXCEPTION), entityType.getSimpleTypeName(), idName);
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // return customer;
    bodyBuilder.appendFormalLine("return %s;", pathVariable);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, parameterTypes,
            parameterNames, bodyBuilder);

    methodBuilder.addAnnotation(ANN_METADATA_MODEL_ATTRIBUTE);

    return methodBuilder.build();
  }

  /*
   * ===================================================================================
   */

  /**
   * This method provides the "list" form method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListMethod() {
    // Define methodName
    final JavaSymbolName methodName = LIST_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    annotations.add(RESPONSE_BODY_ANNOTATION);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new ModelAndView("customers/list");
    bodyBuilder.appendFormalLine("return new %s(\"%s/list\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), controllerMetadata.getPath());

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" form method using Thymeleaf view
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateFormMethod() {
    // Define methodName
    final JavaSymbolName methodName = CREATE_FORM_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMapping = new AnnotationMetadataBuilder(GET_MAPPING);
    getMapping.addStringAttribute("value", "/create-form");
    getMapping.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMapping);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateForm(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // model.addAttribute(new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(new %s());",
        this.entity.getSimpleTypeName()));

    // return "path/create";
    bodyBuilder.appendFormalLine("return new %s(\"%s/create\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod() {
    // Define methodName
    final JavaSymbolName methodName = CREATE_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_VALID,
        ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(POST_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // return new ModelAndView("customers/create");
    bodyBuilder.appendFormalLine("return new %s(\"%s/create\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), controllerMetadata.getPath());
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    final String newValueVar = "new" + entity.getSimpleTypeName();
    // Customer newCustomer = customerService.save(customer);
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(entity), newValueVar,
        controllerMetadata.getServiceField().getFieldName(), serviceMetadata.getCurrentSaveMethod()
            .getMethodName(), entityItemName);

    // UriComponents showURI = itemLink.to("show").with("category", newCategory.getId()).toUri();
    bodyBuilder.appendFormalLine("%s showURI = %s.to(\"show\").with(\"%s\", %s.getId()).toUri();",
        getNameOfJavaType(SpringJavaType.URI_COMPONENTS), ITEM_LINK, this.entityItemName,
        newValueVar);

    // return new ModelAndView("redirect:" + showURI.toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + showURI.toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateMethod() {
    // Define methodName
    final JavaSymbolName methodName = UPDATE_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_VALID,
        ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PutMapping annotation
    AnnotationMetadataBuilder putMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PUT_MAPPING);
    putMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(putMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // return "path/create";
    bodyBuilder.appendFormalLine("return new %s(\"%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    String savedVarName = "saved" + entity.getSimpleTypeName();
    // Customer savedCustomer = customerService.save(customer);;
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(entity), savedVarName,
        controllerMetadata.getServiceField().getFieldName(), serviceMetadata.getCurrentSaveMethod()
            .getMethodName(), entityItemName);

    // UriComponents showURI = itemLink.to("show").with("category", savedCategory.getId()).toUri();
    bodyBuilder.appendFormalLine("%s showURI = %s.to(\"show\").with(\"%s\", %s.getId()).toUri();",
        getNameOfJavaType(SpringJavaType.URI_COMPONENTS), ITEM_LINK, this.entityItemName,
        savedVarName);

    // return new ModelAndView("redirect:" + showURI.toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + showURI.toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateDetailMethod() {
    // Define methodName
    final JavaSymbolName methodName = UPDATE_METHOD_NAME;

    final RelationInfoExtended info = controllerMetadata.getLastDetailsInfo();
    final JavaType parentEntity = info.entityType;
    final JavaType entity = info.childType;
    final String entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());
    final ServiceMetadata serviceMetadata = controllerMetadata.getServiceMetadataForEntity(entity);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_METADATA_VALID,
        ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(
        StringUtils.uncapitalize(parentEntity.getSimpleTypeName())));
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PutMapping annotation
    AnnotationMetadataBuilder putMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PUT_MAPPING);
    putMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(putMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // return new ModelAndView("customerorders/details/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath,
        controllerMetadata.getDetailsPathAsString("/"));;

    // }
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Customer savedCustomer = customerService.save(customer);
    bodyBuilder.appendFormalLine("%s.%s(%s);", controllerMetadata.getLastDetailServiceField()
        .getFieldName(), serviceMetadata.getCurrentSaveMethod().getMethodName(), entityItemName);

    // return new ModelAndView("redirect:" + collectionLink.to("list").toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + %s.to(\"list\").toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), COLLECTION_LINK);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "delete" method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteMethod() {
    // Define methodName
    final JavaSymbolName methodName = DELETE_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_MODEL_ATTRIBUTE));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    AnnotationMetadataBuilder deleteMapping = new AnnotationMetadataBuilder(DELETE_MAPPING);
    deleteMapping.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(RESPONSE_BODY_ANNOTATION);
    annotations.add(deleteMapping);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.delete(customer);
    bodyBuilder.appendFormalLine("%s.%s(%s);", controllerMetadata.getServiceField().getFieldName(),
        serviceMetadata.getCurrentDeleteMethod().getMethodName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides a finder method using THYMELEAF response type
   *
   * @param finderMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getFinderMethod(MethodMetadata finderMethod) {
    // TODO
    return null;
    /*
    final List<AnnotatedJavaType> originalParameterTypes = finderMethod.getParameterTypes();
    
    // Get finder parameter names
    final List<JavaSymbolName> originalParameterNames = finderMethod.getParameterNames();
    List<String> stringParameterNames = new ArrayList<String>();
    for (JavaSymbolName parameterName : originalParameterNames) {
      stringParameterNames.add(parameterName.getSymbolName());
    }
    
    // Define methodName
    final JavaSymbolName methodName = finderMethod.getMethodName();
    
    // Define path
    String path = "";
    if (StringUtils.startsWith(methodName.getSymbolName(), "count")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "count");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "find")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "find");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "query")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "query");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(path);
    
    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod = getControllerMVCService().getMVCMethodByRequestMapping(
        controller.getType(), SpringEnumDetails.REQUEST_METHOD_GET, "/" + path,
        stringParameterNames, null, "application/vnd.datatables+json", "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }
    
    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    StringBuffer finderParamsString = new StringBuffer();
    for (int i = 0; i < originalParameterTypes.size(); i++) {
    
      // Add @ModelAttribute for search param
      if (i == 0) {
        AnnotationMetadataBuilder requestParamAnnotation =
            new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
        requestParamAnnotation.addStringAttribute("value",
            originalParameterNames.get(i).getSymbolName());
        parameterTypes.add(new AnnotatedJavaType(originalParameterTypes.get(i).getJavaType(),
            requestParamAnnotation.build()));
        parameterNames.add(originalParameterNames.get(i));
        finderParamsString.append(originalParameterNames.get(i).getSymbolName());
      } else if (originalParameterTypes.get(i).getJavaType()
          .equals(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH)) {
        parameterTypes.add(originalParameterTypes.get(i));
        addTypeToImport(originalParameterTypes.get(i).getJavaType());
        parameterNames.add(originalParameterNames.get(i));
    
        // Build finder parameters String
        finderParamsString.append(", ".concat(originalParameterNames.get(i).getSymbolName()));
      }
    }
    
    // Add DatatablesPageable param
    parameterTypes
        .add(AnnotatedJavaType.convertFromJavaType(addTypeToImport(this.datatablesPageable)));
    parameterNames.add(new JavaSymbolName("pageable"));
    finderParamsString.append(", pageable");
    
    // Add additional 'draw' param
    AnnotationMetadataBuilder requestParamAnnotation =
        new AnnotationMetadataBuilder(addTypeToImport(SpringJavaType.REQUEST_PARAM));
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));
    parameterNames.add(new JavaSymbolName("draw"));
    
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    
    // Adding @RequestMapping annotation
    annotations.add(
        getControllerMVCService().getRequestMappingAnnotation(SpringEnumDetails.REQUEST_METHOD_GET,
            "/" + path, stringParameterNames, null, "application/vnd.datatables+json", ""));
    
    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);
    
    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    
    // Generating returnType
    JavaType returnType = finderMethod.getReturnType();
    List<JavaType> returnParameterTypes = returnType.getParameters();
    StringBuffer returnTypeParamsString = new StringBuffer();
    for (int i = 0; i < returnParameterTypes.size(); i++) {
      addTypeToImport(returnParameterTypes.get(i));
      if (i > 0) {
        returnTypeParamsString.append(",");
      }
      returnTypeParamsString.append(returnParameterTypes.get(i).getSimpleTypeName());
    
      // Add module dependency
      getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
          returnParameterTypes.get(i));
    }
    
    // ReturnType<ReturnTypeParams> entity =
    // ENTITY_SERVICE_FIELD.FINDER_NAME(SEARCH_PARAMS);
    String returnParameterName =
        StringUtils.uncapitalize(returnParameterTypes.get(0).getSimpleTypeName());
    bodyBuilder.newLine();
    if (StringUtils.isEmpty(returnTypeParamsString)) {
      bodyBuilder.appendFormalLine(String.format("%s %s = %s.%s(%s);",
          addTypeToImport(returnType).getSimpleTypeName(), returnParameterName,
          getServiceField().getFieldName(), methodName, finderParamsString));
    } else {
      bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(%s);",
          addTypeToImport(returnType).getSimpleTypeName(), returnTypeParamsString,
          returnParameterName, getServiceField().getFieldName(), methodName, finderParamsString));
    }
    
    // long allAvailableEntity/Projection =
    // ENTITY_SERVICE_FIELD.COUNT_METHOD(formBean);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("long allAvailable%s = %s.%s(%s);",
        StringUtils.capitalize(Noun.pluralOf(returnParameterName, Locale.ENGLISH)),
        getServiceField().getFieldName(), "count".concat(StringUtils.capitalize(path)),
        parameterNames.get(0)));
    bodyBuilder.newLine();
    
    // return new DatatablesData<Entity/Projection>(entity/projection,
    // allAvailableEntity/Projection, draw);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s, allAvailable%s, draw);",
        addTypeToImport(this.datatablesDataType).getSimpleTypeName(), returnTypeParamsString,
        returnParameterName,
        StringUtils.capitalize(Noun.pluralOf(returnParameterName, Locale.ENGLISH))));
    
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            addTypeToImport(new JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0,
                DataType.TYPE, null, returnParameterTypes)),
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);
    
    return methodBuilder.build();
    */
  }

  /**
   * This method provides a finder redirect method using THYMELEAF response
   * type
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderRedirectMethod(MethodMetadata finderMethod) {
    // TODO
    return null;
    /*
    
    // Get finder parameter names
    final List<JavaSymbolName> originalParameterNames = finderMethod.getParameterNames();
    List<String> stringParameterNames = new ArrayList<String>();
    for (JavaSymbolName parameterName : originalParameterNames) {
      stringParameterNames.add(parameterName.getSymbolName());
    }
    
    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(finderMethod.getMethodName().getSymbolName().concat("Redirect"));
    
    // Define path
    String path = "";
    if (StringUtils.startsWith(methodName.getSymbolName(), "count")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "count");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "find")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "find");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "query")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "query");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(StringUtils.removeEnd(path, "Redirect"));
    
    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod = getControllerMVCService().getMVCMethodByRequestMapping(
        controller.getType(), SpringEnumDetails.REQUEST_METHOD_POST, "/" + path,
        stringParameterNames, null, SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }
    
    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    
    // Check if finder parameter is a DTO
    JavaType formBean = finderMethod.getParameterTypes().get(0).getJavaType();
    if (getTypeLocationService().getTypeDetails(formBean) != null && getTypeLocationService()
        .getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) == null) {
    
      // Finder parameter are entity fields
      formBean = this.entity;
    }
    
    // Add form bean parameter
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    modelAttributeAnnotation.addStringAttribute("value", "formBean");
    parameterTypes.add(new AnnotatedJavaType(formBean, modelAttributeAnnotation.build()));
    parameterNames.add(new JavaSymbolName("formBean"));
    
    // Add redirect parameter
    parameterTypes.add(
        AnnotatedJavaType.convertFromJavaType(addTypeToImport(SpringJavaType.REDIRECT_ATTRIBUTES)));
    parameterNames.add(new JavaSymbolName("redirect"));
    
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    
    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_POST, "/" + path, stringParameterNames, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));
    
    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();
    
    // redirect.addFlashAttribute(entity/dtoSearch);
    bodyBuilder.appendFormalLine(
        String.format("redirect.addFlashAttribute(\"formBean\", %s);", parameterNames.get(0)));
    bodyBuilder.newLine();
    
    // return "redirect:PATH_PREFIX/ENTITY_PLURAL/FINDER_NAME";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:%s/%s/search/%s\";",
        this.pathPrefix, this.entityPlural, path));
    
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);
    
    return methodBuilder.build();
    */
  }

  /**
   * This method provides a finder form method using THYMELEAF response type
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderFormMethod(MethodMetadata finderMethod) {
    // TODO
    return null;
    /*
    
    // Get finder parameter names
    List<String> stringParameterNames = new ArrayList<String>();
    stringParameterNames.add("model");
    
    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(finderMethod.getMethodName().getSymbolName().concat("Form"));
    
    // Define path
    String path = "";
    if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "count");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "find")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "find");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "query")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "query");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "read")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(path).concat("/search-form");
    
    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod = getControllerMVCService().getMVCMethodByRequestMapping(
        controller.getType(), SpringEnumDetails.REQUEST_METHOD_GET, "/" + path,
        stringParameterNames, null, SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }
    
    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    
    // Add model parameter
    parameterTypes
        .add(AnnotatedJavaType.convertFromJavaType(addTypeToImport(SpringJavaType.MODEL)));
    parameterNames.add(new JavaSymbolName("model"));
    
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    
    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));
    
    // Check if finder parameter is a DTO
    JavaType formBean = finderMethod.getParameterTypes().get(0).getJavaType();
    if (getTypeLocationService().getTypeDetails(formBean) != null && getTypeLocationService()
        .getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) == null) {
    
      // Finder parameter are entity fields
      formBean = this.entity;
    }
    
    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();
    
    // Entity/DTO search = new Entity/DTO();
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = new %1$s();",
        addTypeToImport(formBean).getSimpleTypeName(), "formBean"));
    bodyBuilder.newLine();
    
    // model.addAttribute("search", search);
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(\"%1$s\", %1$s);", "formBean"));
    bodyBuilder.newLine();
    
    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");
    bodyBuilder.newLine();
    
    // return "PATH_PREFIX/ENTITY_PLURAL/FINDER_NAMEForm";
    String pathPrefix = "";
    if (StringUtils.isBlank(this.pathPrefix)) {
      pathPrefix = this.pathPrefix;
    } else {
      pathPrefix = this.pathPrefix.concat("/");
    }
    bodyBuilder.appendFormalLine(String.format("return \"%s%s/%s\";",
        StringUtils.removeStart(pathPrefix, "/"), this.entityPlural, methodName.getSymbolName()));
    
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);
    
    return methodBuilder.build();
    */
  }

  /**
   * This method provides populateFormats method that allows to configure date
   * time format for every entity
   *
   * @return
   */
  private MethodMetadata getPopulateFormatsMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("populateFormats");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    buildPopulateFormatBody(bodyBuilder, dateTimeFields);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Build method body which populate in model all required formats for a form based on dateTimeField
   * of a type.
   *
   * Also populate current locale in model.
   *
   * @param bodyBuilder
   * @param dateTimeFields dateTime fields (optional could be empty or null)
   */
  private void buildPopulateFormatBody(InvocableMemberBodyBuilder bodyBuilder,
      List<FieldMetadata> dateTimeFields) {
    // Always save locale
    bodyBuilder.appendFormalLine(
        "model.addAttribute(\"application_locale\", %s.getLocale().getLanguage());",
        getNameOfJavaType(SpringJavaType.LOCALE_CONTEXT_HOLDER));

    if (dateTimeFields == null || dateTimeFields.isEmpty()) {
      // All done;
      return;
    }

    // Getting all enum types from provided entity
    for (FieldMetadata field : dateTimeFields) {

      // Getting annotation format
      AnnotationMetadata dateTimeFormatAnnotation =
          field.getAnnotation(SpringJavaType.DATE_TIME_FORMAT);

      if (dateTimeFormatAnnotation != null
          && (dateTimeFormatAnnotation.getAttribute("style") != null || dateTimeFormatAnnotation
              .getAttribute("pattern") != null)) {

        AnnotationAttributeValue<String> formatAttr =
            dateTimeFormatAnnotation.getAttribute("style");
        if (formatAttr != null) {
          String format = formatAttr.getValue();
          // model.addAttribute("field_date_format",
          // DateTimeFormat.patternForStyle("M-",
          // LocaleContextHolder.getLocale()));
          bodyBuilder
              .appendFormalLine(
                  "model.addAttribute(\"%s_date_format\", %s.patternForStyle(\"%s\", %s.getLocale()));",
                  field.getFieldName(), getNameOfJavaType(JODA_DATETIME_FORMAT_JAVA_TYPE), format,
                  getNameOfJavaType(SpringJavaType.LOCALE_CONTEXT_HOLDER));
        } else {
          formatAttr = dateTimeFormatAnnotation.getAttribute("pattern");
          String format = formatAttr.getValue();
          // model.addAttribute("field_date_format", "pattern");
          bodyBuilder.appendFormalLine(String.format(
              "model.addAttribute(\"%s_date_format\", \"%s\");", field.getFieldName(), format));
        }
      }

    }
  }


  /**
   * This method provides the "list" Datatables JSON method using JSON
   * response type and returns Datatables element
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDatatablesMethod() {
    // Define methodName
    final JavaSymbolName methodName = LIST_DATATABLES_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    parameterTypes.add(DATATABLES_PAGEABLE_PARAM);
    AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    final AnnotationMetadataBuilder getMappingAnnotation =
        new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addEnumAttribute("produces", SPRINGLETS_DATATABLES, "MEDIA_TYPE");
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    getMappingAnnotation.addStringAttribute("value", "/dt");
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Adding @ResponseBody annotation
    annotations.add(RESPONSE_BODY_ANNOTATION);

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();


    final String itemNames = StringUtils.uncapitalize(this.entityPlural);

    // Page<Customer> customers = customerService.findAll(search, pageable);
    bodyBuilder.appendFormalLine("%s<%s> %s = %s.%s(search, pageable);",
        getNameOfJavaType(SpringJavaType.PAGE), getNameOfJavaType(entity), itemNames,
        controllerMetadata.getServiceField().getFieldName(), serviceMetadata
            .getCurrentFindAllMethod().getMethodName());

    final String totalVarName = "total" + StringUtils.capitalize(this.entityPlural) + "Count";
    // long totalCustomersCount = customers.getTotalElements();
    bodyBuilder.appendFormalLine("long %s = %s.getTotalElements();", totalVarName, itemNames);

    //  if (search != null && StringUtils.hasText(search.getText())) {
    //      totalCustomersCount = customerService.count();
    //  }
    bodyBuilder.appendFormalLine("if (search != null && %s.hasText(search.getText())) {",
        getNameOfJavaType(SpringJavaType.STRING_UTILS));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("%s = %s.%s();", totalVarName, controllerMetadata
        .getServiceField().getFieldName(), serviceMetadata.getCurrentCountMethod().getMethodName());
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");



    // DatatablesData<Entity> datatablesData = new DatatablesData<Entity>(entityNamePlural,
    // allAvailableentityNamePlural, draw);
    bodyBuilder.appendFormalLine("%s<%s> datatablesData = new %s<%s>(%s, %s, draw);",
        getNameOfJavaType(SPRINGLETS_DATATABLES_DATA), getNameOfJavaType(entity),
        getNameOfJavaType(SPRINGLETS_DATATABLES_DATA), getNameOfJavaType(entity), itemNames,
        totalVarName);

    // return ResponseEntity.ok(datatablesData);
    bodyBuilder.appendFormalLine("return  %s.ok(datatablesData);",
        getNameOfJavaType(RESPONSE_ENTITY));

    // Generating returnType
    JavaType returnType =
        JavaType.wrapperOf(RESPONSE_ENTITY, JavaType.wrapperOf(SPRINGLETS_DATATABLES_DATA, entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "select2" Select2 JSON method using JSON
   * response type and returns Select2Data element
   *
   * @return MethodMetadata
   */
  private MethodMetadata getSelect2Method() {
    // Define methodName
    final JavaSymbolName methodName = SELECT2_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    parameterTypes.add(PAGEABLE_PARAM);
    parameterTypes.add(LOCALE_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("locale"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    final AnnotationMetadataBuilder getMappingAnnotation =
        new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addEnumAttribute("produces", SpringJavaType.MEDIA_TYPE,
        "APPLICATION_JSON_VALUE");
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    getMappingAnnotation.addStringAttribute("value", "/s2");
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Adding @ResponseBody annotation
    annotations.add(RESPONSE_BODY_ANNOTATION);

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Customer> customers = customerService.findAll(search, pageable);
    bodyBuilder.appendFormalLine("%s<%s> %s = %s.%s(search, pageable);",
        getNameOfJavaType(SpringJavaType.PAGE), getNameOfJavaType(entity), this.entityPlural,
        controllerMetadata.getServiceField().getFieldName(), serviceMetadata
            .getCurrentFindAllMethod().getMethodName());

    // String idExpression = "#{id}";
    bodyBuilder.appendFormalLine("String idExpression = \"#{%s}\";", this.entityIdentifier);

    // String textExpression = messageSource.getMessage("expression_product", null, "#{toString()}", locale);
    bodyBuilder
        .appendFormalLine(
            "String textExpression = messageSource.getMessage(\"expression_%s\", null, \"#{toString()}\", locale);",
            this.entityItemName);

    // Select2Data<Entity> datatablesData = new Select2Data<Entity>(entityNamePlural,
    // idExpression, textExpression);
    bodyBuilder.appendFormalLine(
        "%s<%s> select2Data = new %s<%s>(%s, idExpression, textExpression);",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_SELECT2_DATA), getNameOfJavaType(entity),
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_SELECT2_DATA), getNameOfJavaType(entity),
        this.entityPlural);

    // return ResponseEntity.ok(select2Data);
    bodyBuilder.appendFormalLine("return  %s.ok(select2Data);", getNameOfJavaType(RESPONSE_ENTITY));

    // Generating returnType
    JavaType returnType =
        JavaType.wrapperOf(RESPONSE_ENTITY,
            JavaType.wrapperOf(SpringletsJavaType.SPRINGLETS_SELECT2_DATA, entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }


  /**
   * This method provides the "edit" form method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getEditFormMethod() {
    // Define methodName
    final JavaSymbolName methodName = EDIT_FORM_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    final AnnotationMetadataBuilder getMapping = new AnnotationMetadataBuilder(GET_MAPPING);
    getMapping.addStringAttribute("value", "/edit-form");
    getMapping.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMapping);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // return new ModelAndView("customers/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "edit" details form method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getEditFormDetailMethod() {
    // Define methodName
    final JavaSymbolName methodName = EDIT_FORM_METHOD_NAME;

    final RelationInfoExtended info = controllerMetadata.getLastDetailsInfo();
    final JavaType parentEntity = info.entityType;
    final JavaType entity = info.childType;
    final String entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());


    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(
        StringUtils.uncapitalize(parentEntity.getSimpleTypeName())));
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    final AnnotationMetadataBuilder getMapping = new AnnotationMetadataBuilder(GET_MAPPING);
    getMapping.addStringAttribute("value", "/edit-form");
    getMapping.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMapping);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // return new ModelAndView("customerorders/details/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath,
        controllerMetadata.getDetailsPathAsString("/"));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" method using Thymeleaf view response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod() {
    // Define methodName
    final JavaSymbolName methodName = SHOW_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new ModelAndView("customers/show");
    bodyBuilder.appendFormalLine("return new %s(\"%s/show\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" detail method using Thymeleaf view response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowDetailMethod() {
    // Define methodName
    final JavaSymbolName methodName = SHOW_METHOD_NAME;

    final RelationInfoExtended info = controllerMetadata.getLastDetailsInfo();
    final JavaType parentEntity = info.entityType;
    final JavaType entity = info.childType;
    final String entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(
        StringUtils.uncapitalize(parentEntity.getSimpleTypeName())));
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new ModelAndView("customerorders/details/show");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/show\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath,
        controllerMetadata.getDetailsPathAsString("/"));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }


  /**
   * This method provides the "populateForm" method
   *
   * @return MethodMetadata
   */
  private MethodMetadata getPopulateFormMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("populateForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormatsMethod.getMethodName());

    buildPopulateEnumsBody(bodyBuilder, enumFields);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Build method body which populate enum field of a entity into model
   *
   * @param bodyBuilder
   * @param enumFields
   */
  private void buildPopulateEnumsBody(InvocableMemberBodyBuilder bodyBuilder,
      List<FieldMetadata> enumFields) {
    // Getting all enum types from provided entity
    for (FieldMetadata field : enumFields) {
      // model.addAttribute("enumField",
      // Arrays.asList(Enum.values()));
      bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s.asList(%s.values()));",
          field.getFieldName(), getNameOfJavaType(JavaType.ARRAYS),
          getNameOfJavaType(field.getFieldType()));
    }
  }

  /**
   * This method provides detail datatables list method using Thymeleaf
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDatatablesDetailMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.childType);
    final MethodMetadata findAllMethod =
        detailsServiceMetadata.getRefencedFieldFindAllDefinedMethod(detailsInfo.mappedBy);
    final MethodMetadata countByDetailMethod =
        detailsServiceMetadata.getCountByReferenceFieldDefinedMethod(detailsInfo.mappedBy);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.childType);

    // Define methodName
    final JavaSymbolName methodName = LIST_DATATABLES_DETAILS_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(
        findAllMethod.getParameterTypes().get(0).getJavaType(), ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    parameterTypes.add(DATATABLES_PAGEABLE_PARAM);
    AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final JavaSymbolName parentParamName = findAllMethod.getParameterNames().get(0);
    parameterNames.add(parentParamName);
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    getMappingAnnotation.addEnumAttribute("produces", SPRINGLETS_DATATABLES, "MEDIA_TYPE");
    getMappingAnnotation.addStringAttribute("value", "/dt");
    annotations.add(getMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Adding @ResponseBody annotation
    annotations.add(RESPONSE_BODY_ANNOTATION);

    // Generating returnType
    final JavaType serviceReturnType = findAllMethod.getReturnType();
    final JavaType dataReturnType =
        JavaType.wrapperOf(SPRINGLETS_DATATABLES_DATA, serviceReturnType.getParameters().get(0));
    final JavaType returnType = JavaType.wrapperOf(RESPONSE_ENTITY, dataReturnType);

    // TODO
    // Add module dependency
    //getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
    //    returnParameterTypes.get(i));

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemsName = StringUtils.uncapitalize(detailsInfo.fieldName);

    // Page<CustomerOrder> orders = customerOrderService.findByCustomer(customer, globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s, search, pageable);",
        getNameOfJavaType(serviceReturnType), itemsName, detailsServiceField.getFieldName(),
        findAllMethod.getMethodName(), parentParamName);

    final String totalVarName = "total" + StringUtils.capitalize(itemsName) + "Count";


    //  totalOrdersCount = customerOrderService.countByCustomer(customer);
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);",
        getNameOfJavaType(countByDetailMethod.getReturnType()), totalVarName,
        detailsServiceField.getFieldName(), countByDetailMethod.getMethodName(), parentParamName);


    // DatatablesData<CustomerOrder> data =  new DatatablesData<CustomerOrder>(orders, totalOrderCount, draw);
    bodyBuilder.appendFormalLine("%s data =  new %s(%s, %s, draw);",
        getNameOfJavaType(dataReturnType), getNameOfJavaType(dataReturnType), itemsName,
        totalVarName);

    // return ResponseEntity.ok(datatablesData);
    bodyBuilder.appendFormalLine("return %s.ok(data);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method for details association relationship
   * using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateDetailsCompositionMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata addToRelationMethod =
        detailsServiceMetadata.getAddToRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    final RelationInfoExtended info = controllerMetadata.getLastDetailsInfo();
    final JavaType parentEntity = info.entityType;
    final JavaType entity = info.childType;
    final String entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());
    final String parentItemName = StringUtils.uncapitalize(parentEntity.getSimpleTypeName());

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_METADATA_VALID,
        ANN_METADATA_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(MODEL_PARAM);

    // Define methodName
    final JavaSymbolName methodName = CREATE_METHOD_NAME;

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PostMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.POST_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(parentItemName));
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(MODEL_PARAM_NAME);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateFormats(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // return new ModelAndView("customerorders/details/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/create\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath,
        controllerMetadata.getDetailsPathAsString("/"));

    bodyBuilder.indentRemove();
    // }
    bodyBuilder.appendFormalLine("}");

    // customerOrderService.addToDetails(customerOrder, Collections.singleton(orderDetail));
    bodyBuilder.appendFormalLine("%s.%s(%s,%s.singleton(%s));", detailsServiceField.getFieldName(),
        addToRelationMethod.getMethodName(), parentItemName,
        getNameOfJavaType(JavaType.COLLECTIONS), entityItemName);

    // return new ModelAndView("redirect:" + collectionLink.to("list").toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + %s.to(\"list\").toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), COLLECTION_LINK);


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method for details association relationship
   * using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateDetailsMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata setMethod =
        detailsServiceMetadata.getSetRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final JavaSymbolName methodName = CREATE_METHOD_NAME;
    JavaSymbolName itemsName = setMethod.getParameterNames().get(1);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    parameterTypes.add(new AnnotatedJavaType(setMethod.getParameterTypes().get(0).getJavaType(),
        ANN_METADATA_MODEL_ATTRIBUTE));

    AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", itemsName.getSymbolName().concat("Ids"));
    JavaType identifierType =
        setMethod.getParameterTypes().get(1).getJavaType().getParameters().get(0);
    parameterTypes.add(new AnnotatedJavaType(JavaType.wrapperOf(JavaType.LIST, identifierType),
        requestParamAnnotation.build()));

    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PostMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.POST_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(setMethod.getParameterNames());
    parameterNames.add(MODEL_PARAM_NAME);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    /*
      // Remove empty values
      for (Iterator<Long> iterator = products.iterator(); iterator.hasNext();) {
        if (iterator.next() == null) {
          iterator.remove();
        }
      }
     */
    bodyBuilder.appendFormalLine("// Remove empty values");
    bodyBuilder.appendFormalLine("for (%s<%s> iterator = %s.iterator(); iterator.hasNext();) {",
        getNameOfJavaType(JavaType.ITERATOR), getNameOfJavaType(identifierType), itemsName);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("if (iterator.next() == null) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("iterator.remove();");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // categoryService.addToProducts(category, products);
    bodyBuilder.appendFormalLine("%s.%s(%s,%s);", detailsServiceField.getFieldName(),
        setMethod.getMethodName(), setMethod.getParameterNames().get(0), itemsName);

    // return new ModelAndView("redirect:" + collectionLink.to("list").toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + %s.to(\"list\").toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), COLLECTION_LINK);


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }


  /**
   * This method provides the "create" form method for details using Thymeleaf view
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateFormDetailsMethod() {
    // Define methodName
    final JavaSymbolName methodName = CREATE_FORM_METHOD_NAME;

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    final RelationInfoExtended info = controllerMetadata.getLastDetailsInfo();
    final JavaType parentEntity = info.entityType;
    final JavaType entity = info.childType;

    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_METADATA_MODEL_ATTRIBUTE));

    parameterTypes.add(MODEL_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(parentEntity.getSimpleTypeName()));
    parameterNames.add(MODEL_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMapping = new AnnotationMetadataBuilder(GET_MAPPING);
    getMapping.addStringAttribute("value", "/create-form");
    getMapping.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMapping);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateForm(model);
    bodyBuilder.appendFormalLine("%s(model);", populateFormMethod.getMethodName());
    bodyBuilder.newLine();

    // model.addAttribute(new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(new %s());",
        entity.getSimpleTypeName()));


    // return new ModelAndView("path/create");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/create\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath,
        controllerMetadata.getDetailsPathAsString("/"));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getDeleteDetailMethod() {
    RelationInfoExtended detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata removeFromMethod =
        detailsServiceMetadata.getRemoveFromRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    JavaSymbolName methodName = DELETE_METHOD_NAME;
    JavaSymbolName itemsName = detailsInfo.fieldMetadata.getFieldName();

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(0)
        .getJavaType(), ANN_METADATA_MODEL_ATTRIBUTE));

    parameterTypes.add(new AnnotatedJavaType(detailsInfo.childType, ANN_METADATA_MODEL_ATTRIBUTE));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.DELETE_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(removeFromMethod.getParameterNames().get(0));
    parameterNames.add(itemsName);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.addToOrders(customer, order.getId());
    bodyBuilder.appendFormalLine("%s.%s(%s,%s.singleton(%s.%s()));", detailsServiceField
        .getFieldName(), removeFromMethod.getMethodName(), removeFromMethod.getParameterNames()
        .get(0), getNameOfJavaType(JavaType.COLLECTIONS), itemsName,
        detailsInfo.childEntityMetadata.getCurrentIdentifierAccessor().getMethodName());

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getRemoveFromDetailsMethod() {
    RelationInfoExtended detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata removeFromMethod =
        detailsServiceMetadata.getRemoveFromRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);


    // Define methodName
    final JavaSymbolName methodName = removeFromMethod.getMethodName();
    JavaSymbolName itemsName = removeFromMethod.getParameterNames().get(1);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(0)
        .getJavaType(), AnnotationMetadataBuilder.getInstance(SpringJavaType.MODEL_ATTRIBUTE)));
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", itemsName.getSymbolName());

    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(1)
        .getJavaType().getParameters().get(0), pathVariableAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.DELETE_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    postMappingAnnotation.addStringAttribute("value", "/{" + itemsName.getSymbolName() + "}");
    annotations.add(postMappingAnnotation);
    this.mvcMethodNames.put(methodName.getSymbolName(), methodName.getSymbolName());

    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(removeFromMethod.getParameterNames());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.addToOrders(customer, Collections.singleton(order));
    bodyBuilder.appendFormalLine("%s.%s(%s,%s.singleton(%s));", detailsServiceField.getFieldName(),
        removeFromMethod.getMethodName(), removeFromMethod.getParameterNames().get(0),
        getNameOfJavaType(JavaType.COLLECTIONS), itemsName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /*
   * ==============================================================================
   */

  /**
   * Method that returns list Datatables JSON method
   *
   * @return
   */
  public MethodMetadata getCurrentListDatatablesMethod() {
    return this.listDatatablesMethod;
  }

  /**
   * Method that returns create form Thymeleaf method
   *
   * @return
   */
  public MethodMetadata getCurrentCreateFormMethod() {
    return this.createFormMethod;
  }

  /**
   * Method that returns create Thymeleaf method
   *
   * @return
   */
  public MethodMetadata getCurrentCreateMethod() {
    return this.createMethod;
  }

  /**
   * Method that returns edit form Thymeleaf method
   *
   * @return
   */
  public MethodMetadata getCurrentEditForm() {
    return this.editFormMethod;
  }

  /**
   * Method that returns update Thymeleaf method
   *
   * @return
   */
  public MethodMetadata getCurrentUpdateMethod() {
    return this.updateMethod;
  }

  /**
   * Method that returns delete Thymeleaf method
   *
   * @return
   */
  public MethodMetadata getCurrentDeleteMethod() {
    return this.deleteMethod;
  }

  /**
   * Method that returns show Thymeleaf method
   *
   * @return
   */
  public MethodMetadata getCurrentShowMethod() {
    return this.showMethod;
  }

  /**
   * Method that returns populateForm method
   *
   * @return
   */
  public MethodMetadata getCurrentPopulateFormMethod() {
    return this.populateFormMethod;
  }

  /**
   * Method that returns if related entity is readOnly or not.
   *
   * @return
   */
  @Override
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /**
   * gets Mvc URL name for a controller method
   * @param controller
   * @param methodName
   * @return
   */
  public static String getMvcUrlNameFor(JavaType controller, JavaSymbolName methodName) {
    return getMvcControllerName(controller) + "#" + getMvcMethodName(methodName);
  }

  /**
   * gets Mvc URL name for a controller method
   * @param controller
   * @param methodName
   * @return
   */
  public static String getMvcUrlNameFor(JavaType controller, MethodMetadata method) {
    return getMvcUrlNameFor(controller, method.getMethodName());
  }

  /**
   * @return @RequestMapping.name annotation value
   */
  public String getMvcControllerName() {
    return getMvcControllerName(getDestination());
  }

  /**
   * @return value to use in @RequestMapping.name
   */
  public static String getMvcControllerName(JavaType thymeleaftController) {
    return thymeleaftController.getSimpleTypeName();
  }

  public static String getMvcMethodName(JavaSymbolName methodName) {
    return methodName.getSymbolName();
  }

  public MethodMetadata getCurrentListMethod() {
    return listMethod;
  }

  public MethodMetadata getListDatatablesDetailsMethod() {
    return listDatatablesDetailsMethod;
  }

  @Override
  public JavaType getEntity() {
    return entity;
  }

  public ControllerMetadata getControllerMetadata() {
    return controllerMetadata;
  }

  public JavaType getDetailItemController() {
    return detailItemController;
  }

  public JavaType getDetailCollectionController() {
    return detailCollectionController;
  }

  public JavaType getItemController() {
    return itemController;
  }

  public JavaType getCollectionController() {
    return collectionController;
  }

  public MethodMetadata getCurrentCreateFormDetailsMethod() {
    return createFormDetailsMethod;
  }

  public MethodMetadata getCurrentRemoveFromDetailsMethod() {
    return removeFromDetailsMethod;
  }

  public MethodMetadata getCurrentCreateDetailsMethod() {
    return createDetailsMethod;
  }

  public MethodMetadata getCurrentDeleteDetailsMethod() {
    return deleteDetailMethod;
  }

  public MethodMetadata getCurrentEditFormDetailsMethod() {
    return editFormDetailMethod;
  }


  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }

  public JavaType getRelatedCollectionController() {
    return relatedCollectionController;
  }

  public JavaType getRelatedItemController() {
    return relatedItemController;
  }

  public Map<String, MethodMetadata> getFinderFormMethods() {
    return finderFormMethods;
  }

  public Map<String, MethodMetadata> getFinderDatatableMethods() {
    return finderDatatableMethods;
  }

  public Map<String, MethodMetadata> getFinderListMethods() {
    return finderListMethods;
  }
}
