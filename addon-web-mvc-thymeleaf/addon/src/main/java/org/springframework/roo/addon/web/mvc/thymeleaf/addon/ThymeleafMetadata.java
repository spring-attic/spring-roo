package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.SpringJavaType.DELETE_MAPPING;
import static org.springframework.roo.model.SpringJavaType.GET_MAPPING;
import static org.springframework.roo.model.SpringJavaType.INIT_BINDER;
import static org.springframework.roo.model.SpringJavaType.POST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.REQUEST_PARAM;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_ENTITY;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_DATATABLES;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_NOT_FOUND_EXCEPTION;

import org.apache.commons.io.IOUtils;
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
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.CommentStructure.CommentLocation;
import org.springframework.roo.classpath.details.comments.JavadocComment;
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
 * @author Sergio Clares
 * @since 2.0
 */
public class ThymeleafMetadata extends AbstractViewMetadata {

  // Method names
  protected static final JavaSymbolName CREATE_METHOD_NAME = new JavaSymbolName("create");
  protected static final JavaSymbolName LIST_METHOD_NAME = new JavaSymbolName("list");
  protected static final JavaSymbolName DELETE_METHOD_NAME = new JavaSymbolName("delete");
  protected static final JavaSymbolName DELETE_BATCH_METHOD_NAME =
      new JavaSymbolName("deleteBatch");
  protected static final JavaSymbolName LIST_URI_METHOD_NAME = new JavaSymbolName("listURI");
  protected static final JavaSymbolName LIST_DATATABLES_METHOD_NAME = new JavaSymbolName(
      "datatables");
  protected static final JavaSymbolName LIST_DATATABLES_DETAILS_METHOD_NAME = new JavaSymbolName(
      "datatables");
  protected static final JavaSymbolName SELECT2_METHOD_NAME = new JavaSymbolName("select2");
  protected static final JavaSymbolName SHOW_URI_METHOD_NAME = new JavaSymbolName("showURI");
  protected static final JavaSymbolName SHOW_METHOD_NAME = new JavaSymbolName("show");
  protected static final JavaSymbolName SHOW_INLINE_METHOD_NAME = new JavaSymbolName("showInline");
  protected static final JavaSymbolName CREATE_FORM_METHOD_NAME = new JavaSymbolName("createForm");
  protected static final JavaSymbolName EDIT_FORM_METHOD_NAME = new JavaSymbolName("editForm");
  protected static final JavaSymbolName UPDATE_METHOD_NAME = new JavaSymbolName("update");
  protected static final JavaSymbolName EXPORT_METHOD_NAME = new JavaSymbolName("export");
  protected static final JavaSymbolName EXPORT_CSV_METHOD_NAME = new JavaSymbolName("exportCsv");
  protected static final JavaSymbolName EXPORT_PDF_METHOD_NAME = new JavaSymbolName("exportPdf");
  protected static final JavaSymbolName EXPORT_XLS_METHOD_NAME = new JavaSymbolName("exportXls");
  protected static final JavaSymbolName ADD_COLUMN_TO_REPORT_BUILDER_METHOD_NAME =
      new JavaSymbolName("addColumnToReportBuilder");
  protected static final JavaSymbolName FIN_ONE_FOR_UPDATE_METHOD_NAME = new JavaSymbolName(
      "findOneForUpdate");

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
  private static final JavaSymbolName LOCALE_PARAM_NAME = new JavaSymbolName("locale");
  private static final AnnotationMetadataBuilder RESPONSE_BODY_ANNOTATION =
      new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
  private static final JavaType JODA_DATETIME_FORMAT_JAVA_TYPE = new JavaType(
      "org.joda.time.format.DateTimeFormat");
  private static final JavaSymbolName MESSAGE_SOURCE = new JavaSymbolName("messageSource");
  private static final JavaSymbolName CONVERSION_SERVICE_FIELD_NAME = new JavaSymbolName(
      "conversionService");
  private static final AnnotationMetadata ANN_METADATA_MODEL_ATTRIBUTE = AnnotationMetadataBuilder
      .getInstance(SpringJavaType.MODEL_ATTRIBUTE);
  private static final AnnotatedJavaType STRING_ARRAY_PARAM = new AnnotatedJavaType(
      JavaType.STRING_ARRAY);
  private static final AnnotatedJavaType DATATABLES_COLUMNS_PARAM = new AnnotatedJavaType(
      SpringletsJavaType.SPRINGLETS_DATATABLES_COLUMNS);
  private static final JavaSymbolName DATATABLES_COLUMNS_PARAM_NAME = new JavaSymbolName(
      "datatablesColumns");
  private static final JavaSymbolName RESPONSE_PARAM_NAME = new JavaSymbolName("response");
  private static final AnnotatedJavaType STRING_PARAM = new AnnotatedJavaType(JavaType.STRING);
  private static final JavaSymbolName FILE_NAME_PARAM_NAME = new JavaSymbolName("fileName");
  private static final JavaSymbolName EXPORTER_PARAM_NAME = new JavaSymbolName("exporter");
  private static final JavaSymbolName HTTP_METHOD_PARAM_NAME = new JavaSymbolName("method");
  private static final JavaSymbolName VERSION_PARAM_NAME = new JavaSymbolName("version");

  private static final AnnotationMetadata ANN_METADATA_VALID = AnnotationMetadataBuilder
      .getInstance(Jsr303JavaType.VALID);

  // Static Types
  private static final JavaType JR_EXCEPTION = new JavaType(
      "net.sf.jasperreports.engine.JRException");
  private static final JavaType COLUMN_BUILDER_EXCEPTION = new JavaType(
      "ar.com.fdvs.dj.domain.builders.ColumnBuilderException");
  private static final JavaType IO_EXCEPTION = new JavaType("java.io.IOException");
  private static final JavaType CLASS_NOT_FOUND_EXCEPTION = new JavaType(
      "java.lang.ClassNotFoundException");
  private static final JavaType FAST_REPORT_BUILDER = new JavaType(
      "ar.com.fdvs.dj.domain.builders.FastReportBuilder");
  private static final JavaType JR_DATA_SOURCE = new JavaType(
      "net.sf.jasperreports.engine.JRDataSource");
  private static final JavaType JR_BEAN_COLLECTION_DATA_SOURCE = new JavaType(
      "net.sf.jasperreports.engine.data.JRBeanCollectionDataSource");
  private static final JavaType JASPER_PRINT = new JavaType(
      "net.sf.jasperreports.engine.JasperPrint");
  private static final JavaType DYNAMIC_JASPER_HELPER = new JavaType(
      "ar.com.fdvs.dj.core.DynamicJasperHelper");
  private static final JavaType CLASSIC_LAYOUT_MANAGER = new JavaType(
      "ar.com.fdvs.dj.core.layout.ClassicLayoutManager");
  private static final JavaType STRING_UTILS_APACHE = new JavaType(
      "org.apache.commons.lang3.StringUtils");

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
  private final String entityLabel;
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
  private final JavaType relatedCollectionLinkFactory;
  private final JavaType relatedItemLinkFactory;

  // Common method
  private final MethodMetadata initBinderMethod;

  // Collection Methods
  private final MethodMetadata createFormMethod;
  private final MethodMetadata createMethod;
  private final MethodMetadata listMethod;
  private final MethodMetadata listDatatablesMethod;
  private final MethodMetadata select2Method;
  private final MethodMetadata deleteBatchMethod;

  // Item Methods
  private final MethodMetadata modelAttributeMethod;
  private final MethodMetadata editFormMethod;
  private final MethodMetadata updateMethod;
  private final MethodMetadata deleteMethod;
  private final MethodMetadata showMethod;
  private final MethodMetadata showInlineMethod;
  private final MethodMetadata populateFormMethod;
  private final MethodMetadata populateFormatsMethod;

  // Details Methods
  private final Map<RelationInfo, MethodMetadata> modelAttributeDetailsMethod;
  private final MethodMetadata listDatatablesDetailsMethod;
  private final MethodMetadata createFormDetailsMethod;
  private final MethodMetadata createDetailsMethod;
  private final MethodMetadata removeFromDetailsMethod;
  private final MethodMetadata removeFromDetailsBatchMethod;

  // Finder Methods
  private final Map<String, MethodMetadata> finderFormMethods;
  private final Map<String, MethodMetadata> finderListMethods;
  private final Map<String, MethodMetadata> finderDatatableMethods;

  // Export Methods
  private final MethodMetadata exportMethod;
  private final MethodMetadata exportCsvMethod;
  private final MethodMetadata exportPdfMethod;
  private final MethodMetadata exportXlsMethod;
  private final MethodMetadata addColumnToReportBuilderMethod;
  private final List<MethodMetadata> exportMethods;

  // TODO
  // private final Map<String, MethodMetadata> finderListMethods;
  // ????

  private final List<MethodMetadata> allMethods;
  private final FieldMetadata messageSourceField;
  private final FieldMetadata methodLinkBuilderFactoryField;
  private final FieldMetadata conversionServiceField;
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
  private final MethodMetadata showDetailInlineMethod;
  private final String ITEM_LINK = "itemLink";
  private final String COLLECTION_LINK = "collectionLink";
  private final String entityPluralUncapitalized;
  private final List<FieldMetadata> entityValidFields;
  private final Map<String, JavaType> jasperReportsMap;

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
   * 
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
      final JavaType relatedCollectionController, final JavaType relatedItemController,
      final List<FieldMetadata> validFields, final Map<String, JavaType> jasperReportsMap,
      final JavaType relatedCollectionLinkFactory, final JavaType relatedItemLinkFactory) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.jasperReportsMap = jasperReportsMap;
    this.entityValidFields = validFields;
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
    this.entityLabel = "label_".concat(this.entityItemName.toLowerCase());
    this.entityPlural = entityPlural;
    this.entityPluralUncapitalized = StringUtils.uncapitalize(entityPlural);
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
    this.relatedCollectionLinkFactory = relatedCollectionLinkFactory;
    this.relatedItemLinkFactory = relatedItemLinkFactory;

    // Add @Controller
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

        // ROO-3868: New entity visualization support needs a
        // ConversionService field
        this.conversionServiceField = getConversionServiceField();
        ensureGovernorHasField(new FieldMetadataBuilder(this.conversionServiceField));

        // Build constructor
        String linkBuilderLine =
            String.format("%s(linkBuilder.of(%s.class));",
                getMutatorMethod(this.methodLinkBuilderFactoryField).getMethodName(),
                getNameOfJavaType(this.itemController));
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        this.listMethod = addAndGet(getListMethod(), allMethods);
        this.listDatatablesMethod = addAndGet(getListDatatablesMethod(), allMethods);

        boolean generateSelect2 = true;
        // XXX To Be Analyzed
        // for (FieldMetadata relationField :
        // entityMetadata.getRelationsAsChild().values()) {
        // if (relationField.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
        // || relationField.getAnnotation(JpaJavaType.MANY_TO_MANY) != null)
        // {
        // generateSelect2 = true;
        // break;
        // }
        // }
        if (generateSelect2) {
          this.select2Method = addAndGet(getSelect2Method(), allMethods);
        } else {
          this.select2Method = null;
        }

        if (this.readOnly) {
          this.initBinderMethod = null;
          this.populateFormMethod = null;
          this.populateFormatsMethod = null;
          this.createMethod = null;
          this.createFormMethod = null;
          this.deleteBatchMethod = null;
        } else {
          this.initBinderMethod = addAndGet(getInitBinderMethod(entity), allMethods);
          this.populateFormatsMethod = addAndGet(getPopulateFormatsMethod(), allMethods);
          this.populateFormMethod = addAndGet(getPopulateFormMethod(), allMethods);
          this.createMethod = addAndGet(getCreateMethod(), allMethods);
          this.createFormMethod = addAndGet(getCreateFormMethod(), allMethods);
          this.deleteBatchMethod = addAndGet(getDeleteBatchMethod(), allMethods);
        }

        this.modelAttributeMethod = null;
        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.showInlineMethod = null;
        this.modelAttributeDetailsMethod = null;
        this.listDatatablesDetailsMethod = null;
        this.finderListMethods = null;
        this.finderDatatableMethods = null;
        this.finderFormMethods = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.removeFromDetailsBatchMethod = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        this.showDetailInlineMethod = null;

        // Jasper export methods
        List<MethodMetadata> exportMethods = new ArrayList<MethodMetadata>();
        this.exportMethod = addAndGet(getExportMethod(), exportMethods);
        this.exportCsvMethod = addAndGet(getCsvExportMethod(), exportMethods);
        this.exportPdfMethod = addAndGet(getPdfExportMethod(), exportMethods);
        this.exportXlsMethod = addAndGet(getXlsMethod(), exportMethods);
        this.addColumnToReportBuilderMethod =
            addAndGet(getAddColumnToReportBuilderMethod(), exportMethods);
        this.exportMethods = exportMethods;

        break;
      }
      case ITEM: {

        // Add MethodLinkBuilderFactory field
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(ITEM_LINK, this.governorTypeDetails.getType());
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        this.conversionServiceField = null;

        // Build constructor
        String linkBuilderLine =
            String.format("%s(linkBuilder.of(%s.class));",
                getMutatorMethod(methodLinkBuilderFactoryField).getMethodName(),
                getNameOfJavaType(this.governorTypeDetails.getType()));
        this.constructor = addAndGetConstructor(getConstructor(linkBuilderLine));

        // Build methods
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        this.showMethod = addAndGet(getShowMethod(), allMethods);
        this.showInlineMethod = addAndGet(getShowInlineMethod(), allMethods);
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

        this.deleteBatchMethod = null;
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
        this.removeFromDetailsBatchMethod = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        this.showDetailInlineMethod = null;

        // Jasper export methods
        this.exportMethod = null;
        this.exportCsvMethod = null;
        this.exportPdfMethod = null;
        this.exportXlsMethod = null;
        this.addColumnToReportBuilderMethod = null;
        this.exportMethods = null;

        break;
      }
      case SEARCH: {

        this.methodLinkBuilderFactoryField = null;
        this.conversionServiceField = null;

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

        this.deleteBatchMethod = null;
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
        this.showInlineMethod = null;
        this.populateFormMethod = null;
        this.populateFormatsMethod = null;
        this.modelAttributeDetailsMethod = null;
        this.listDatatablesDetailsMethod = null;
        this.select2Method = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.removeFromDetailsBatchMethod = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        this.showDetailInlineMethod = null;

        // Jasper export methods
        this.exportMethod = null;
        this.exportCsvMethod = null;
        this.exportPdfMethod = null;
        this.exportXlsMethod = null;
        this.addColumnToReportBuilderMethod = null;
        this.exportMethods = null;

        break;
      }
      case DETAIL: {

        // Add MethodLinkBuilderFactory field
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(COLLECTION_LINK, this.collectionController);
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        // ROO-3868: New entity visualization support needs a
        // ConversionService field
        this.conversionServiceField = getConversionServiceField();
        ensureGovernorHasField(new FieldMetadataBuilder(this.conversionServiceField));

        // Build constructor
        String linkBuilderLine =
            String.format("%s(linkBuilder.of(%s.class));",
                getMutatorMethod(methodLinkBuilderFactoryField).getMethodName(),
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

        // ReadOnly entities doesn't manage relations
        if (controllerMetadata.getLastDetailsInfo().type == JpaRelationType.AGGREGATION
            && !entityMetadata.isReadOnly()) {
          this.removeFromDetailsMethod = addAndGet(getRemoveFromDetailsMethod(), allMethods);
          this.removeFromDetailsBatchMethod =
              addAndGet(getRemoveFromDetailsBatchMethod(), allMethods);
          this.createDetailsMethod = addAndGet(getCreateDetailsMethod(), allMethods);
          this.initBinderMethod = null;
        } else if (!entityMetadata.isReadOnly()) {
          this.initBinderMethod =
              addAndGet(getInitBinderMethod(controllerMetadata.getLastDetailEntity()), allMethods);
          this.createDetailsMethod = addAndGet(getCreateDetailsCompositionMethod(), allMethods);
          this.removeFromDetailsMethod = null;
          this.removeFromDetailsBatchMethod = null;
        } else {
          this.initBinderMethod = null;
          this.createDetailsMethod = null;
          this.removeFromDetailsMethod = null;
          this.removeFromDetailsBatchMethod = null;
        }

        this.listMethod = null;
        this.listDatatablesMethod = null;
        this.createMethod = null;
        this.createFormMethod = null;

        this.deleteBatchMethod = null;
        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.showInlineMethod = null;
        this.finderListMethods = null;
        this.finderDatatableMethods = null;
        this.finderFormMethods = null;
        this.select2Method = null;

        this.editFormDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        this.showDetailMethod = null;
        this.showDetailInlineMethod = null;

        // Jasper export methods
        this.exportMethod = null;
        this.exportCsvMethod = null;
        this.exportPdfMethod = null;
        this.exportXlsMethod = null;
        this.addColumnToReportBuilderMethod = null;
        this.exportMethods = null;

        break;
      }
      case DETAIL_ITEM: {

        // Add MethodLinkBuilderFactory field
        this.methodLinkBuilderFactoryField =
            getMethodLinkBuilderFactoryField(COLLECTION_LINK, this.collectionController);
        ensureGovernorHasField(new FieldMetadataBuilder(this.methodLinkBuilderFactoryField));

        this.conversionServiceField = null;

        // Build constructor
        String linkBuilderLine =
            String.format("%s(linkBuilder.of(%s.class));",
                getMutatorMethod(methodLinkBuilderFactoryField).getMethodName(),
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
        this.showDetailInlineMethod = addAndGet(getShowDetailInlineMethod(), allMethods);
        this.listMethod = null;
        this.listDatatablesMethod = null;
        this.createMethod = null;
        this.createFormMethod = null;

        this.deleteBatchMethod = null;
        this.editFormMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.showInlineMethod = null;
        this.finderDatatableMethods = null;
        this.finderListMethods = null;
        this.finderFormMethods = null;
        this.select2Method = null;
        this.listDatatablesDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.removeFromDetailsBatchMethod = null;
        this.createDetailsMethod = null;
        this.createFormDetailsMethod = null;

        // Jasper export methods
        this.exportMethod = null;
        this.exportCsvMethod = null;
        this.exportPdfMethod = null;
        this.exportXlsMethod = null;
        this.addColumnToReportBuilderMethod = null;
        this.exportMethods = null;

        break;

      }
      default:
        throw new IllegalArgumentException("Unsupported Controller type: " + this.type.name());
    }

    this.allMethods = Collections.unmodifiableList(allMethods);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private FieldMetadata getConversionServiceField() {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
        new ArrayList<AnnotationMetadataBuilder>(), CONVERSION_SERVICE_FIELD_NAME,
        SpringJavaType.CONVERSION_SERVICE).build();
  }

  private FieldMetadata getMessageSourceField() {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
        new ArrayList<AnnotationMetadataBuilder>(), MESSAGE_SOURCE, SpringJavaType.MESSAGE_SOURCE)
        .build();
  }

  private MethodMetadata addAndGet(MethodMetadata method, List<MethodMetadata> allMethods) {
    if (allMethods != null) {
      allMethods.add(method);
    }
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

    // Add produces
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
    bodyBuilder.appendFormalLine(String.format("%s(%s);",
        getMutatorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceFieldName));

    if (this.type == ControllerType.DETAIL || this.type == ControllerType.DETAIL_ITEM) {

      for (FieldMetadata serviceField : controllerMetadata.getDetailsServiceFields().values()) {

        // Getting parentServiceFieldName
        String childServiceFieldName = serviceField.getFieldName().getSymbolName();

        // Adding parameters
        constructor.addParameter(childServiceFieldName, serviceField.getFieldType());

        // Generating body
        bodyBuilder.appendFormalLine(String.format("%s(%s);", getMutatorMethod(serviceField)
            .getMethodName(), childServiceFieldName));
      }
    }

    // ROO-3868: New entity visualization support needs a ConversionService
    // field
    if (this.type == ControllerType.COLLECTION || this.type == ControllerType.DETAIL) {
      String conversionServiceFieldName =
          this.conversionServiceField.getFieldName().getSymbolName();
      constructor.addParameter(conversionServiceFieldName,
          this.conversionServiceField.getFieldType());
      bodyBuilder.appendFormalLine("%s(%s);", getMutatorMethod(this.getConversionServiceField())
          .getMethodName(), conversionServiceFieldName);
    }

    String messageSourceName = this.messageSourceField.getFieldName().getSymbolName();
    constructor.addParameter(messageSourceName, messageSourceField.getFieldType());
    bodyBuilder.appendFormalLine("%s(%s);", getMutatorMethod(this.messageSourceField)
        .getMethodName(), messageSourceName);

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

  /* Jasper Export Methods */

  /**
   * Generates a method to add columns to DynamicJasper report builder.
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getAddColumnToReportBuilderMethod() {
    JavaSymbolName methodName = ADD_COLUMN_TO_REPORT_BUILDER_METHOD_NAME;

    // Including parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(STRING_PARAM);
    parameterTypes.add(new AnnotatedJavaType(FAST_REPORT_BUILDER));
    parameterTypes.add(LOCALE_PARAM);
    parameterTypes.add(STRING_PARAM);

    // Check method existence
    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Including parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final JavaSymbolName columnName = new JavaSymbolName("columnName");
    parameterNames.add(columnName);
    final JavaSymbolName reportBuilder = new JavaSymbolName("builder");
    parameterNames.add(reportBuilder);
    parameterNames.add(LOCALE_PARAM_NAME);
    parameterNames.add(FILE_NAME_PARAM_NAME);

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // try {
    bodyBuilder.appendFormalLine("try {");

    for (int i = 0; i < this.entityValidFields.size(); i++) {
      String fieldName = this.entityValidFields.get(i).getFieldName().getSymbolName();
      if (i == 0) {

        // if (columnName.equals("FIELD")) {
        bodyBuilder.appendFormalLine("if (columnName.equals(\"%s\")) {", fieldName);
      } else {

        // else if (columnName.equals("FIELD")) {
        bodyBuilder.appendFormalLine("else if (columnName.equals(\"%s\")) {", fieldName);
      }

      bodyBuilder.indent();

      if (this.entityValidFields.get(i).getFieldName()
          .equals(this.entityMetadata.getCurrentIndentifierField().getFieldName())) {

        // builder.addColumn("FIELD-TITLE", "FIELD-NAME", FIELD-CLASS,
        // 50);
        if (this.entityValidFields.get(i).getFieldType().isPrimitive()) {

          // Print SimpleTypeName of JavaType when it is a primitive
          bodyBuilder
              .appendFormalLine(
                  "builder.addColumn(%s().getMessage(\"%s_%s\", null, \"%s\", locale), \"%s\", %s.class.getName(), 50);",
                  getAccessorMethod(this.messageSourceField).getMethodName(), this.entityLabel,
                  fieldName.toLowerCase(), getFieldDefaultLabelValue(fieldName), fieldName,
                  this.entityValidFields.get(i).getFieldType().getSimpleTypeName());
          getNameOfJavaType(this.entityValidFields.get(i).getFieldType());
        } else {
          bodyBuilder
              .appendFormalLine(
                  "builder.addColumn(%s().getMessage(\"%s_%s\", null, \"%s\", locale), \"%s\", %s.class.getName(), 50);",
                  getAccessorMethod(this.messageSourceField).getMethodName(), this.entityLabel,
                  fieldName.toLowerCase(), getFieldDefaultLabelValue(fieldName), fieldName,
                  getNameOfJavaType(this.entityValidFields.get(i).getFieldType()));
        }
      } else {

        // builder.addColumn("FIELD-TITLE", "FIELD-NAME", FIELD-CLASS,
        // 100);
        if (this.entityValidFields.get(i).getFieldType().isPrimitive()) {

          bodyBuilder
              .appendFormalLine(
                  "builder.addColumn(%s().getMessage(\"%s_%s\", null, \"%s\", locale), \"%s\", %s.class.getName(), 100);",
                  getAccessorMethod(this.messageSourceField).getMethodName(), this.entityLabel,
                  fieldName.toLowerCase(), getFieldDefaultLabelValue(fieldName), fieldName,
                  this.entityValidFields.get(i).getFieldType().getSimpleTypeName());
          getNameOfJavaType(this.entityValidFields.get(i).getFieldType());
        } else {
          bodyBuilder
              .appendFormalLine(
                  "builder.addColumn(%s().getMessage(\"%s_%s\", null, \"%s\", locale), \"%s\", %s.class.getName(), 100);",
                  getAccessorMethod(this.messageSourceField).getMethodName(), this.entityLabel,
                  fieldName.toLowerCase(), getFieldDefaultLabelValue(fieldName), fieldName,
                  getNameOfJavaType(this.entityValidFields.get(i).getFieldType()));
        }
      }

      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");
    }

    // }
    bodyBuilder.appendFormalLine("}");

    // Build catch blocks
    buildExportCatchBlock(bodyBuilder, COLUMN_BUILDER_EXCEPTION);
    buildExportCatchBlock(bodyBuilder, CLASS_NOT_FOUND_EXCEPTION);

    // Build method
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);

    // Add Javadoc to method
    CommentStructure commentStructure = new CommentStructure();
    String description =
        "This method contains all the entity fields that are able to be displayed in a "
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(
                "report. The developer could add a new column to the report builder providing the ")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat("field name and the builder where the new field will be added as column.");
    List<String> paramInfo = new ArrayList<String>();
    paramInfo.add("columnName the field name to show as column");
    paramInfo.add("builder The builder where the new field will be added as column.");
    commentStructure.addComment(new JavadocComment(description, paramInfo, null, null),
        CommentLocation.BEGINNING);
    methodBuilder.setCommentStructure(commentStructure);

    return methodBuilder.build();
  }

  /**
   * Generates a method to export data using DynamicJasper and arguments
   * received as different export methods will delegate in this one.
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getExportMethod() {

    JavaSymbolName methodName = EXPORT_METHOD_NAME;

    // Including parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    AnnotationMetadataBuilder pageableDefaultAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PAGEABLE_DEFAULT);
    pageableDefaultAnnotation.addIntegerAttribute("size", Integer.MAX_VALUE);
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE, pageableDefaultAnnotation
        .build()));
    parameterTypes.add(STRING_ARRAY_PARAM);
    parameterTypes
        .add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse")));
    parameterTypes.add(new AnnotatedJavaType(this.jasperReportsMap.get("JasperReportsExporter")));
    parameterTypes.add(STRING_PARAM);
    parameterTypes.add(LOCALE_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(GLOBAL_SEARCH_PARAM_NAME);
    parameterNames.add(PAGEABLE_PARAM_NAME);
    parameterNames.add(DATATABLES_COLUMNS_PARAM_NAME);
    parameterNames.add(RESPONSE_PARAM_NAME);
    parameterNames.add(EXPORTER_PARAM_NAME);
    parameterNames.add(FILE_NAME_PARAM_NAME);
    parameterNames.add(LOCALE_PARAM_NAME);

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Get findAll method
    MethodMetadata findAllMethod = this.serviceMetadata.getCurrentFindAllWithGlobalSearchMethod();

    // Getting the default return type
    JavaType defaultReturnType = findAllMethod.getReturnType();

    // Obtain the filtered and ordered elements
    // Page<Owner> owners = ownerService.findAll(search, pageable);
    bodyBuilder.appendFormalLine("// Obtain the filtered and ordered elements");
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s, %s);", getNameOfJavaType(defaultReturnType),
        this.entityPluralUncapitalized,
        getAccessorMethod(this.controllerMetadata.getServiceField()).getMethodName(), findAllMethod
            .getMethodName().getSymbolName(), GLOBAL_SEARCH_PARAM_NAME, PAGEABLE_PARAM_NAME);
    bodyBuilder.newLine();

    // // Prevent generation of reports with empty data
    bodyBuilder.appendFormalLine("// Prevent generation of reports with empty data");
    // if (owners == null || owners.getContent().isEmpty()) {
    bodyBuilder.appendFormalLine("if (%1$s == null || %s.getContent().isEmpty()) {",
        this.entityPluralUncapitalized);
    // return;
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return;");
    // }
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.newLine();

    // // Creates a new ReportBuilder using DynamicJasper library
    bodyBuilder.appendFormalLine("// Creates a new ReportBuilder using DynamicJasper library");
    // FastReportBuilder builder = new FastReportBuilder();
    bodyBuilder.appendFormalLine("%1$s builder = new %1$s();",
        getNameOfJavaType(FAST_REPORT_BUILDER));
    bodyBuilder.newLine();

    // // IMPORTANT: By default, this application uses
    // "export_default.jrxml"
    bodyBuilder
        .appendFormalLine("// IMPORTANT: By default, this application uses \"export_default.jrxml\"");
    // // to generate all reports. If you want to customize this specific
    // report,
    bodyBuilder
        .appendFormalLine("// to generate all reports. If you want to customize this specific report,");
    // create a new ".jrxml" template and customize it. Take in account the
    bodyBuilder
        .appendFormalLine("// create a new \".jrxml\" template and customize it. (Take in account the ");
    // DynamicJasper restrictions:
    bodyBuilder.appendFormalLine("// DynamicJasper restrictions: ");
    // http://dynamicjasper.com/2010/10/06/how-to-use-custom-jrxml-templates/)
    bodyBuilder
        .appendFormalLine("// http://dynamicjasper.com/2010/10/06/how-to-use-custom-jrxml-templates/)");
    // builder.setTemplateFile("templates/reports/export_default.jrxml");
    bodyBuilder
        .appendFormalLine("builder.setTemplateFile(\"templates/reports/export_default.jrxml\");");
    bodyBuilder.newLine();

    // // The generated report will display the same columns as the
    // Datatables component.
    bodyBuilder
        .appendFormalLine("// The generated report will display the same columns as the Datatables component.");
    // // However, this is not mandatory. You could edit this code if you
    // want to ignore
    bodyBuilder
        .appendFormalLine("// However, this is not mandatory. You could edit this code if you want to ignore");
    // // the provided datatablesColumns
    bodyBuilder.appendFormalLine("// the provided datatablesColumns");
    // if (datatablesColumns != null) {
    bodyBuilder.appendFormalLine("if (%s != null) {", DATATABLES_COLUMNS_PARAM_NAME);
    bodyBuilder.indent();
    // for (String column : datatablesColumns) {
    bodyBuilder.appendFormalLine("for (String column : %s) {", DATATABLES_COLUMNS_PARAM_NAME);
    bodyBuilder.indent();
    // // Delegates in addColumnToReportBuilder to include each datatables
    // column
    bodyBuilder.appendFormalLine("// Delegates in %s to include each datatables column",
        ADD_COLUMN_TO_REPORT_BUILDER_METHOD_NAME);
    // // to the report builder
    bodyBuilder.appendFormalLine("// to the report builder");
    // addColumnToReportBuilder(column, builder, locale);
    bodyBuilder.appendFormalLine("addColumnToReportBuilder(column, builder, %s, %s);",
        LOCALE_PARAM_NAME, FILE_NAME_PARAM_NAME);
    bodyBuilder.indentRemove();
    // }
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.indentRemove();
    // }
    bodyBuilder.appendFormalLine("}");
    bodyBuilder.newLine();

    // // This property resizes the columns to use full width page.
    bodyBuilder.appendFormalLine("// This property resizes the columns to use full width page.");
    // // Set false value if you want to use the specific width of each
    // column.
    bodyBuilder
        .appendFormalLine("// Set false value if you want to use the specific width of each column.");
    // builder.setUseFullPageWidth(true);
    bodyBuilder.appendFormalLine("builder.setUseFullPageWidth(true);");
    bodyBuilder.newLine();

    // // Creates a new Jasper Reports Datasource using the obtained
    // elements
    bodyBuilder
        .appendFormalLine("// Creates a new Jasper Reports Datasource using the obtained elements");
    // JRDataSource ds = new
    // JRBeanCollectionDataSource(owners.getContent());
    bodyBuilder.appendFormalLine("%s ds = new %s(%s.getContent());",
        getNameOfJavaType(JR_DATA_SOURCE), getNameOfJavaType(JR_BEAN_COLLECTION_DATA_SOURCE),
        this.entityPluralUncapitalized);
    bodyBuilder.newLine();

    // // Generates the JasperReport
    bodyBuilder.appendFormalLine("// Generates the JasperReport");
    // JasperPrint jp;
    bodyBuilder.appendFormalLine("%s jp;", getNameOfJavaType(JASPER_PRINT));
    // try {
    bodyBuilder.appendFormalLine("try {");
    bodyBuilder.indent();
    // jp = DynamicJasperHelper.generateJasperPrint(builder.build(), new
    // ClassicLayoutManager(), ds);
    bodyBuilder.appendFormalLine("jp = %s.generateJasperPrint(builder.build(), new %s(), ds);",
        getNameOfJavaType(DYNAMIC_JASPER_HELPER), getNameOfJavaType(CLASSIC_LAYOUT_MANAGER));
    bodyBuilder.indentRemove();
    // "}"
    bodyBuilder.appendFormalLine("}");

    // Build catch block
    buildExportCatchBlock(bodyBuilder, JR_EXCEPTION);
    bodyBuilder.newLine();

    // // Converts the JaspertReport element to a ByteArrayOutputStream and
    bodyBuilder
        .appendFormalLine("// Converts the JaspertReport element to a ByteArrayOutputStream and");
    // // write it into the response stream using the provided
    // JasperReportExporter
    bodyBuilder
        .appendFormalLine("// write it into the response stream using the provided JasperReportExporter");
    bodyBuilder.appendFormalLine("try {");
    bodyBuilder.indent();
    // exporter.export(jp, fileName, response);
    bodyBuilder.appendFormalLine("%s.export(jp, %s, %s);", EXPORTER_PARAM_NAME,
        FILE_NAME_PARAM_NAME, RESPONSE_PARAM_NAME);
    bodyBuilder.indentRemove();
    // "}"
    bodyBuilder.appendFormalLine("}");
    buildExportCatchBlock(bodyBuilder, JR_EXCEPTION);
    buildExportCatchBlock(bodyBuilder, IO_EXCEPTION);
    bodyBuilder.reset();

    // Build method
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);

    // Add Javadoc to method
    CommentStructure commentStructure = new CommentStructure();
    String description =
        "Method that obtains the filtered and ordered records using the Datatables information and "
            .concat(IOUtils.LINE_SEPARATOR)
            .concat("export them to a new report file. (It ignores the current pagination).")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(IOUtils.LINE_SEPARATOR)
            .concat("To generate the report file it uses the `DynamicJasper` library")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(
                "(http://dynamicjasper.com). This library allows developers to generate reports dynamically")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat("without use an specific template to each entity.")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(
                "To customize the appearance of ALL generated reports, you could customize the ")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(
                "\"export_default.jrxml\" template located in \"src/main/resources/templates/reports/\". However,")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(
                "if you want to customize the appearance of this specific report, you could create a new")
            .concat(IOUtils.LINE_SEPARATOR)
            .concat(
                "\".jrxml\" file and provide it to the library replacing the `builder.setTemplateFile();`")
            .concat(IOUtils.LINE_SEPARATOR).concat("operation used in this implementation.");

    // Create params info
    List<String> paramsInfo = new ArrayList<String>();
    paramsInfo
        .add("search GlobalSearch that contains the filter provided by the Datatables component.");
    paramsInfo
        .add("pageable Pageable that contains the Sort info provided by the Datatabes component.");
    paramsInfo.add("datatablesColumns Columns displayed in the Datatables component.");
    paramsInfo.add("response The HttpServletResponse.");
    paramsInfo.add("exporter An specific JasperReportsExporter to be used during export process.");
    paramsInfo.add("fileName The final filename to use.");
    paramsInfo.add("locale The current Locale in the view context.");

    // Add JavadocComment to CommentStructure and method
    commentStructure.addComment(new JavadocComment(description, paramsInfo, null, null),
        CommentLocation.BEGINNING);
    methodBuilder.setCommentStructure(commentStructure);

    return methodBuilder.build();
  }

  /**
   * Generates a method to export data to CSV using DynamicJasper.
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCsvExportMethod() {
    if (jasperReportsMap.get("JasperReportsCsvExporter") != null) {
      final String exporterMethodInvocation =
          String.format("new %s()",
              getNameOfJavaType(jasperReportsMap.get("JasperReportsCsvExporter")));
      final String fileName =
          String.format("%s_report.csv", StringUtils.uncapitalize(this.entityPlural));
      final JavaSymbolName methodName = EXPORT_CSV_METHOD_NAME;

      return buildExportTypeMethod(exporterMethodInvocation, fileName, methodName, "exportCsv",
          "/export/csv", "CSV");
    }
    return null;
  }

  /**
   * Generates a method to export data to PDF using DynamicJasper.
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getPdfExportMethod() {
    if (jasperReportsMap.get("JasperReportsPdfExporter") != null) {
      final String exporterMethodInvocation =
          String.format("new %s()",
              getNameOfJavaType(jasperReportsMap.get("JasperReportsPdfExporter")));
      final String fileName =
          String.format("%s_report.pdf", StringUtils.uncapitalize(this.entityPlural));
      final JavaSymbolName methodName = EXPORT_PDF_METHOD_NAME;

      return buildExportTypeMethod(exporterMethodInvocation, fileName, methodName, "exportPdf",
          "/export/pdf", "PDF");
    }
    return null;
  }

  /**
   * Generates a method to export data to XLS using DynamicJasper.
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getXlsMethod() {
    if (jasperReportsMap.get("JasperReportsXlsExporter") != null) {
      final String exporterMethodInvocation =
          String.format("new %s()",
              getNameOfJavaType(jasperReportsMap.get("JasperReportsXlsExporter")));
      final String fileName =
          String.format("%s_report.xls", StringUtils.uncapitalize(this.entityPlural));
      final JavaSymbolName methodName = EXPORT_XLS_METHOD_NAME;

      return buildExportTypeMethod(exporterMethodInvocation, fileName, methodName, "exportXls",
          "/export/xls", "XLS");
    }
    return null;
  }

  /**
   * Builds a `catch` block which throws an exception with a localized message
   * for export method.
   * 
   * @param bodyBuilder
   *            the InvocableMemberBodyBuilder which will be appended to
   *            export method
   */
  private void buildExportCatchBlock(InvocableMemberBodyBuilder bodyBuilder, JavaType exceptionType) {
    // } catch (JRException e) {
    bodyBuilder.appendFormalLine("catch (%s e) {", getNameOfJavaType(exceptionType));
    bodyBuilder.indent();
    // String errorMessage =
    // this.messageSource.getMessage("error_exportingErrorException",
    bodyBuilder.appendFormalLine(
        "String errorMessage = %s().getMessage(\"error_exportingErrorException\", ",
        getAccessorMethod(this.messageSourceField).getMethodName());
    // new Object[] {StringUtils.substringAfterLast(fileName,
    // ".").toUpperCase()},
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "new Object[] {%s.substringAfterLast(fileName, \".\").toUpperCase()}, ",
        getNameOfJavaType(STRING_UTILS_APACHE));
    // String.format("Error while exporting data to %s file", StringUtils
    bodyBuilder.appendFormalLine(
        "String.format(\"Error while exporting data to %s file\", StringUtils.",
        getNameOfJavaType(STRING_UTILS_APACHE));
    bodyBuilder.indent();
    // substringAfterLast(fileName, ".").toUpperCase()), locale);
    bodyBuilder.appendFormalLine("substringAfterLast(fileName, \".\").toUpperCase()), locale);");
    bodyBuilder.indentRemove();
    bodyBuilder.indentRemove();
    // throw new ExportingErrorException(errorMessage);
    bodyBuilder.appendFormalLine("throw new %s(errorMessage);",
        getNameOfJavaType(this.jasperReportsMap.get("ExportingErrorException")));
    bodyBuilder.indentRemove();
    // }
    bodyBuilder.appendFormalLine("}");
  }

  /**
   * Builds export (TYPE) method, which is similar in all export target types.
   * 
   * @param exporterClassInstantiation
   *            the String with the instantiation of the JasperReports support
   *            class.
   * @param fileName
   *            the String with the output file name.
   * @param methodName
   *            the JavaSymbolName with the method name.
   * @return MethodMetadata
   */
  private MethodMetadata buildExportTypeMethod(final String exporterClassInstantiation,
      final String fileName, final JavaSymbolName methodName, final String getMappingAnnotatinName,
      final String getMappingAnnotationValue, final String fileType) {

    // Including parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    AnnotationMetadataBuilder pageableDefaultAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PAGEABLE_DEFAULT);
    pageableDefaultAnnotation.addIntegerAttribute("size", Integer.MAX_VALUE);
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE, pageableDefaultAnnotation
        .build()));
    AnnotationMetadataBuilder requestParamAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value",
        DATATABLES_COLUMNS_PARAM_NAME.getSymbolName());
    parameterTypes
        .add(new AnnotatedJavaType(JavaType.STRING_ARRAY, requestParamAnnotation.build()));
    parameterTypes
        .add(new AnnotatedJavaType(new JavaType("javax.servlet.http.HttpServletResponse")));
    parameterTypes.add(LOCALE_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Including parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(GLOBAL_SEARCH_PARAM_NAME);
    parameterNames.add(PAGEABLE_PARAM_NAME);
    parameterNames.add(DATATABLES_COLUMNS_PARAM_NAME);
    parameterNames.add(RESPONSE_PARAM_NAME);
    parameterNames.add(LOCALE_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    AnnotationMetadataBuilder getMappingBuilder = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingBuilder.addStringAttribute("name", getMappingAnnotatinName);
    getMappingBuilder.addStringAttribute("value", getMappingAnnotationValue);
    annotations.add(getMappingBuilder);
    annotations.add(RESPONSE_BODY_ANNOTATION);

    // Add throws types
    final List<JavaType> throwTypes = new ArrayList<JavaType>();
    throwTypes.add(JR_EXCEPTION);
    throwTypes.add(IO_EXCEPTION);
    throwTypes.add(COLUMN_BUILDER_EXCEPTION);
    throwTypes.add(CLASS_NOT_FOUND_EXCEPTION);

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // export(search, pageable, datatablesColumns, response, new
    // JasperReportsCsvExporter(), "ENTITY-ITEM_report.csv");
    bodyBuilder.appendFormalLine("export(%s, %s, %s, %s, %s, \"%s\", %s);",
        GLOBAL_SEARCH_PARAM_NAME, PAGEABLE_PARAM_NAME, DATATABLES_COLUMNS_PARAM_NAME,
        RESPONSE_PARAM_NAME, exporterClassInstantiation, fileName,
        LOCALE_PARAM_NAME.getSymbolName());

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    // Build method
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    // Add JavaDoc
    CommentStructure commentStructure = new CommentStructure();
    String description =
        "It delegates in the `export` method providing the necessary information".concat(
            IOUtils.LINE_SEPARATOR).concat(String.format("to generate a %s report.", fileType));

    // Add params info to commment block
    List<String> paramsInfo = new ArrayList<String>();
    paramsInfo
        .add("search The GlobalSearch that contains the filter provided by the Datatables component");
    paramsInfo
        .add("pageable The Pageable that contains the Sort info provided by the Datatabes component");
    paramsInfo.add("datatablesColumns The Columns displayed in the Datatables component");
    paramsInfo.add("response The HttpServletResponse");

    // Add JavadocComment to CommentStructure and to method
    commentStructure.addComment(new JavadocComment(description, paramsInfo, null, null),
        CommentLocation.BEGINNING);
    methodBuilder.setCommentStructure(commentStructure);

    return methodBuilder.build();
  }

  /**
   * Returns a String with default label to show when cannot find the right
   * label code.
   * 
   * @return a String with default field label
   */
  private String getFieldDefaultLabelValue(String fieldName) {
    String[] splittedFieldName = StringUtils.splitByCharacterTypeCamelCase(fieldName);
    String label = "";

    for (int i = 0; i < splittedFieldName.length; i++) {
      if (i != 0) {
        label = label.concat(" ");
      }
      label = label.concat(StringUtils.capitalize(splittedFieldName[i]));
    }

    return label;
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
    // TODO Delegates on ControllerOperations to obtain the URL for this
    // finder
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
    // TODO Delegates on ControllerOperations to obtain the URL for this
    // finder
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
    // TODO Delegates on ControllerOperations to obtain the URL for this
    // finder
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

    // Page<Customer> customers = customerService.findAll(formBean,
    // globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s);", getNameOfJavaType(serviceReturnType),
        itemNames, getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(),
        serviceFinderMethod.getMethodName(), StringUtils.join(parameterStrings, ","));

    // long totalProductsCount = products.getTotalElements();
    String totalItemNamesCount = String.format("total%sCount", StringUtils.capitalize(itemNames));
    bodyBuilder.appendFormalLine(String.format("long %s = %s.getTotalElements();",
        totalItemNamesCount, itemNames));

    // if (search != null && StringUtils.hasText(search.getText())) {
    bodyBuilder
        .appendFormalLine(String.format("if (%s != null && %s.isNotBlank(%s.getText())) {",
            GLOBAL_SEARCH_PARAM_NAME, getNameOfJavaType(STRING_UTILS_APACHE),
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

    // totalProductsCount =
    // productService.countByNameAndDescription(formBean);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("%s = %s().%s(%s);", totalItemNamesCount,
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(),
        countMethod.getMethodName(), FORM_BEAN_PARAM_NAME));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // DatatablesData<Product> datatablesData = new
    // DatatablesData<Product>(products, totalProductsCount, draw);
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
    String pathVariableUncapitalized = StringUtils.uncapitalize(pathVariable);

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
    pathVariableAnnotation.addStringAttribute("value", pathVariableUncapitalized);

    parameterTypes.add(new AnnotatedJavaType(idType, pathVariableAnnotation.build()));
    parameterTypes.add(LOCALE_PARAM);
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.HTTP_METHOD));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(idName);
    parameterNames.add(LOCALE_PARAM_NAME);
    parameterNames.add(HTTP_METHOD_PARAM_NAME);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    if (this.entityMetadata.isReadOnly()) {

      // Customer customer = customerService.findOne(id);
      bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(entityType),
          pathVariableUncapitalized, serviceField.getFieldName(), serviceMetadata
              .getCurrentFindOneMethod().getMethodName(), idName);

      // if (entity == null) {
      bodyBuilder.appendFormalLine("if (%s == null) {", pathVariableUncapitalized);
      bodyBuilder.indent();

      // String message = messageSource.getMessage("error_NotFound",
      // entity, null, locale);
      bodyBuilder.appendFormalLine(
          "String message = %s.getMessage(\"error_NotFound\", new Object[] "
              + "{\"%s\", %s}, \"The record couldn't be found\", %s);", MESSAGE_SOURCE,
          this.entity.getSimpleTypeName(), idName, LOCALE_PARAM_NAME.getSymbolName());

      // throw new NotFoundException(message);
      bodyBuilder.appendFormalLine("throw new %s(message);",
          getNameOfJavaType(SPRINGLETS_NOT_FOUND_EXCEPTION));
      bodyBuilder.indentRemove();
      // }
      bodyBuilder.appendFormalLine("}");
    } else {

      // Entity entity = null;
      bodyBuilder.appendFormalLine("%s %s = null;", getNameOfJavaType(entityType),
          pathVariableUncapitalized);

      // if (HttpMethod.PUT.equals(method)) {
      bodyBuilder.appendFormalLine("if (%s.PUT.equals(%s)) {",
          getNameOfJavaType(SpringJavaType.HTTP_METHOD), HTTP_METHOD_PARAM_NAME);
      // pet = petService.findOneForUpdate(id);
      bodyBuilder.indent();
      bodyBuilder.appendFormalLine("%s = %s.%s(%s);", pathVariableUncapitalized, serviceField
          .getFieldName().getSymbolName(), FIN_ONE_FOR_UPDATE_METHOD_NAME.getSymbolName(), idName);
      // } else {
      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("} else {");
      // entity = entityService.findOne(id);
      bodyBuilder.indent();
      bodyBuilder.appendFormalLine("%s = %s.%s(%s);", pathVariableUncapitalized, serviceField
          .getFieldName().getSymbolName(), serviceMetadata.getCurrentFindOneMethod()
          .getMethodName().getSymbolName(), idName);
      // }
      bodyBuilder.indentRemove();
      bodyBuilder.appendFormalLine("}");

      // if (entity == null) {
      bodyBuilder.newLine();
      bodyBuilder.appendFormalLine("if (%s == null) {", pathVariableUncapitalized);
      // String message = messageSource.getMessage("error_NotFound",
      // entity, null, locale);
      bodyBuilder.indent();
      bodyBuilder
          .appendFormalLine(
              "String message = %s.getMessage(\"error_NotFound\", new Object[] {\"%s\", %s}, \"The record couldn't be found\", %s);",
              MESSAGE_SOURCE, this.entity.getSimpleTypeName(), idName,
              LOCALE_PARAM_NAME.getSymbolName());
      // throw new NotFoundException(message);
      bodyBuilder.appendFormalLine("throw new %s(message);",
          getNameOfJavaType(SPRINGLETS_NOT_FOUND_EXCEPTION));
      bodyBuilder.indentRemove();
      // }
      bodyBuilder.appendFormalLine("}");
    }

    // return entity;
    bodyBuilder.appendFormalLine("return %s;", pathVariableUncapitalized);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, parameterTypes,
            parameterNames, bodyBuilder);

    methodBuilder.addAnnotation(ANN_METADATA_MODEL_ATTRIBUTE);

    return methodBuilder.build();
  }

  /*
   * =========================================================================
   * ==========
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

    // model.addAttribute("entity", new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(\"%s\", new %s());",
        StringUtils.uncapitalize(getNameOfJavaType(this.entity)), getNameOfJavaType(this.entity)));

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
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s);", getNameOfJavaType(entity), newValueVar,
        getAccessorMethod(this.controllerMetadata.getServiceField()).getMethodName(),
        serviceMetadata.getCurrentSaveMethod().getMethodName(), entityItemName);

    // UriComponents showURI = itemLink.to("show").with("category",
    // newCategory.getId()).toUri();
    bodyBuilder.appendFormalLine("%s showURI = %s().to(%s.SHOW).with(\"%s\", %s.getId()).toUri();",
        getNameOfJavaType(SpringJavaType.URI_COMPONENTS),
        getAccessorMethod(this.methodLinkBuilderFactoryField).getMethodName(),
        getNameOfJavaType(relatedItemLinkFactory), this.entityItemName, newValueVar);

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

    // Version parameter
    AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", VERSION_PARAM_NAME.getSymbolName());
    parameterTypes.add(new AnnotatedJavaType(this.entityMetadata.getCurrentVersionField()
        .getFieldType(), requestParamAnnotation.build()));

    // Concurrency control parameter
    AnnotationMetadataBuilder concurrencyControlRequestParam =
        new AnnotationMetadataBuilder(REQUEST_PARAM);
    concurrencyControlRequestParam.addStringAttribute("value", "concurrency");
    concurrencyControlRequestParam.addBooleanAttribute("required", false);
    concurrencyControlRequestParam.addStringAttribute("defaultValue", "");
    parameterTypes.add(new AnnotatedJavaType(JavaType.STRING, concurrencyControlRequestParam
        .build()));
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
    parameterNames.add(this.entityMetadata.getCurrentVersionField().getFieldName());
    parameterNames.add(new JavaSymbolName("concurrencyControl"));
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

    // // Check if provided form contain errors
    bodyBuilder.appendFormalLine("// Check if provided form contain errors");
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

    // // Concurrency control
    bodyBuilder.appendFormalLine("// Concurrency control");

    // Pet existingPet = getPetService().findOne(pet.getId());
    String existingVarName = "existing".concat(entity.getSimpleTypeName());
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s.%s());", getNameOfJavaType(entity),
        existingVarName, getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(),
        serviceMetadata.getCurrentFindOneMethod().getMethodName(), entityItemName,
        getAccessorMethod(this.entityMetadata.getCurrentIndentifierField()).getMethodName());

    // if(pet.getVersion() != existingPet.getVersion() && StringUtils.isEmpty(concurrencyControl)){
    bodyBuilder.appendFormalLine("if(%s.%s() != %s.%s() && %s.isEmpty(concurrencyControl)){",
        entityItemName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName(), existingVarName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        getNameOfJavaType(new JavaType("org.apache.commons.lang3.StringUtils")));
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

    // model.addAttribute("concurrency", true);
    bodyBuilder.appendFormalLine("model.addAttribute(\"concurrency\", true);");

    // return new ModelAndView("pets/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    bodyBuilder.indentRemove();

    // } else if(pet.getVersion() != existingPet.getVersion() && "discard".equals(concurrencyControl)){
    bodyBuilder.appendFormalLine(
        "} else if(%s.%s() != %s.%s() && \"discard\".equals(concurrencyControl)){", entityItemName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        existingVarName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName());
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // model.addAttribute("pet", existingPet);
    bodyBuilder
        .appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, existingVarName);

    // model.addAttribute("concurrency", false);
    bodyBuilder.appendFormalLine("model.addAttribute(\"concurrency\", false);");

    // return new ModelAndView("pets/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    bodyBuilder.indentRemove();

    // } else if(pet.getVersion() != existingPet.getVersion() && "apply".equals(concurrencyControl)){
    bodyBuilder.appendFormalLine(
        "} else if(%s.%s() != %s.%s() && \"apply\".equals(concurrencyControl)){", entityItemName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        existingVarName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName());
    bodyBuilder.indent();

    // // Update the version field to be able to override the existing values
    bodyBuilder
        .appendFormalLine("// Update the version field to be able to override the existing values");

    // pet.setVersion(existingPet.getVersion());
    bodyBuilder.appendFormalLine("%s.%s(%s.%s());", entityItemName,
        getMutatorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        existingVarName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName());

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");



    String savedVarName = "saved" + entity.getSimpleTypeName();
    // Customer savedCustomer = customerService.save(customer);;
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s);", getNameOfJavaType(entity), savedVarName,
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceMetadata
            .getCurrentSaveMethod().getMethodName(), entityItemName);

    // UriComponents showURI =
    // itemLink.to(CategoryItemThymeleafLinkFactory.SHOW).with("category",
    // savedCategory.getId()).toUri();
    bodyBuilder.appendFormalLine("%s showURI = %s().to(%s.SHOW).with(\"%s\", %s.getId()).toUri();",
        getNameOfJavaType(SpringJavaType.URI_COMPONENTS),
        getAccessorMethod(this.methodLinkBuilderFactoryField).getMethodName(),
        getNameOfJavaType(relatedItemLinkFactory), this.entityItemName, savedVarName);

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

    // Version parameter
    AnnotationMetadataBuilder requestParamAnnotation = new AnnotationMetadataBuilder(REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", VERSION_PARAM_NAME.getSymbolName());
    parameterTypes.add(new AnnotatedJavaType(this.entityMetadata.getCurrentVersionField()
        .getFieldType(), requestParamAnnotation.build()));

    // Concurrency control parameter
    AnnotationMetadataBuilder concurrencyControlRequestParam =
        new AnnotationMetadataBuilder(REQUEST_PARAM);
    concurrencyControlRequestParam.addStringAttribute("value", "concurrency");
    concurrencyControlRequestParam.addBooleanAttribute("required", false);
    concurrencyControlRequestParam.addStringAttribute("defaultValue", "");
    parameterTypes.add(new AnnotatedJavaType(JavaType.STRING, concurrencyControlRequestParam
        .build()));
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
    parameterNames.add(this.entityMetadata.getCurrentVersionField().getFieldName());
    parameterNames.add(new JavaSymbolName("concurrencyControl"));
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

    // // Concurrency control
    bodyBuilder.appendFormalLine("// Concurrency control");

    // Pet existingPet = getPetService().findOne(pet.getId());
    String existingVarName = "existing".concat(entity.getSimpleTypeName());
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s.%s());", getNameOfJavaType(entity),
        existingVarName, getAccessorMethod(controllerMetadata.getLastDetailServiceField())
            .getMethodName(), serviceMetadata.getCurrentFindOneMethod().getMethodName(),
        entityItemName, getAccessorMethod(this.entityMetadata.getCurrentIndentifierField())
            .getMethodName());

    // if(pet.getVersion() != existingPet.getVersion() && StringUtils.isEmpty(concurrencyControl)){
    bodyBuilder.appendFormalLine("if(%s.%s() != %s.%s() && %s.isEmpty(concurrencyControl)){",
        entityItemName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName(), existingVarName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        getNameOfJavaType(new JavaType("org.apache.commons.lang3.StringUtils")));
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // model.addAttribute("concurrency", true);
    bodyBuilder.appendFormalLine("model.addAttribute(\"concurrency\", true);");

    // return new ModelAndView("pets/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    bodyBuilder.indentRemove();

    // } else if(pet.getVersion() != existingPet.getVersion() && "discard".equals(concurrencyControl)){
    bodyBuilder.appendFormalLine(
        "} else if(%s.%s() != %s.%s() && \"discard\".equals(concurrencyControl)){", entityItemName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        existingVarName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName());
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // model.addAttribute("pet", existingPet);
    bodyBuilder
        .appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, existingVarName);

    // model.addAttribute("concurrency", false);
    bodyBuilder.appendFormalLine("model.addAttribute(\"concurrency\", false);");

    // return new ModelAndView("pets/edit");
    bodyBuilder.appendFormalLine("return new %s(\"%s/edit\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    bodyBuilder.indentRemove();

    // } else if(pet.getVersion() != existingPet.getVersion() && "apply".equals(concurrencyControl)){
    bodyBuilder.appendFormalLine(
        "} else if(%s.%s() != %s.%s() && \"apply\".equals(concurrencyControl)){", entityItemName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        existingVarName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName());
    bodyBuilder.indent();

    // // Update the version field to be able to override the existing values
    bodyBuilder
        .appendFormalLine("// Update the version field to be able to override the existing values");

    // pet.setVersion(existingPet.getVersion());
    bodyBuilder.appendFormalLine("%s.%s(%s.%s());", entityItemName,
        getMutatorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        existingVarName, getAccessorMethod(this.entityMetadata.getCurrentVersionField())
            .getMethodName());

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Customer savedCustomer = customerService.save(customer);
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getLastDetailServiceField()).getMethodName(),
        serviceMetadata.getCurrentSaveMethod().getMethodName(), entityItemName);

    // return new ModelAndView("redirect:" +
    // collectionLink.to("list").toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + %s().to(%s.LIST).toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW),
        getAccessorMethod(this.methodLinkBuilderFactoryField).getMethodName(),
        getNameOfJavaType(relatedCollectionLinkFactory));

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
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceMetadata
            .getCurrentDeleteMethod().getMethodName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "deleteBatch" method using Thymeleaf view
   * response type. This method will be only created if it not exists in
   * collection JSON controller.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteBatchMethod() {

    // Define methodName
    final JavaSymbolName methodName = DELETE_BATCH_METHOD_NAME;

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", entityIdentifierPlural);
    parameterTypes.add(new AnnotatedJavaType(JavaType.collectionOf(entityMetadata
        .getCurrentIndentifierField().getFieldType()), pathVariable.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityIdentifierPlural));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder deleteMappingAnnotation =
        new AnnotationMetadataBuilder(DELETE_MAPPING);
    deleteMappingAnnotation.addStringAttribute("value", "/batch/{" + entityIdentifierPlural + "}");
    deleteMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(deleteMappingAnnotation);

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // serviceField.SERVICE_DELETE_METHOD(ids);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(this.controllerMetadata.getServiceField()).getMethodName(),
        serviceMetadata.getCurrentDeleteBatchMethod().getMethodName(), entityIdentifierPlural);

    // return ResponseEntity.ok().build();
    bodyBuilder.newLine();
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
     * final List<AnnotatedJavaType> originalParameterTypes =
     * finderMethod.getParameterTypes();
     * 
     * // Get finder parameter names final List<JavaSymbolName>
     * originalParameterNames = finderMethod.getParameterNames();
     * List<String> stringParameterNames = new ArrayList<String>(); for
     * (JavaSymbolName parameterName : originalParameterNames) {
     * stringParameterNames.add(parameterName.getSymbolName()); }
     * 
     * // Define methodName final JavaSymbolName methodName =
     * finderMethod.getMethodName();
     * 
     * // Define path String path = ""; if
     * (StringUtils.startsWith(methodName.getSymbolName(), "count")) { path
     * = StringUtils.removeStart(methodName.getSymbolName(), "count"); }
     * else if (StringUtils.startsWith(methodName.getSymbolName(), "find"))
     * { path = StringUtils.removeStart(methodName.getSymbolName(), "find");
     * } else if (StringUtils.startsWith(methodName.getSymbolName(),
     * "query")) { path =
     * StringUtils.removeStart(methodName.getSymbolName(), "query"); } else
     * if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
     * path = StringUtils.removeStart(methodName.getSymbolName(), "read"); }
     * else { path = methodName.getSymbolName(); } path =
     * StringUtils.uncapitalize(path);
     * 
     * // Check if exists other method with the same @RequesMapping to
     * generate MethodMetadata existingMVCMethod =
     * getControllerMVCService().getMVCMethodByRequestMapping(
     * controller.getType(), SpringEnumDetails.REQUEST_METHOD_GET, "/" +
     * path, stringParameterNames, null, "application/vnd.datatables+json",
     * ""); if (existingMVCMethod != null &&
     * !existingMVCMethod.getDeclaredByMetadataId().equals(this.
     * metadataIdentificationString)) { return existingMVCMethod; }
     * 
     * // Get parameters List<AnnotatedJavaType> parameterTypes = new
     * ArrayList<AnnotatedJavaType>(); List<JavaSymbolName> parameterNames =
     * new ArrayList<JavaSymbolName>(); StringBuffer finderParamsString =
     * new StringBuffer(); for (int i = 0; i <
     * originalParameterTypes.size(); i++) {
     * 
     * // Add @ModelAttribute for search param if (i == 0) {
     * AnnotationMetadataBuilder requestParamAnnotation = new
     * AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
     * requestParamAnnotation.addStringAttribute("value",
     * originalParameterNames.get(i).getSymbolName());
     * parameterTypes.add(new
     * AnnotatedJavaType(originalParameterTypes.get(i).getJavaType(),
     * requestParamAnnotation.build()));
     * parameterNames.add(originalParameterNames.get(i));
     * finderParamsString.append(originalParameterNames.get(i).getSymbolName
     * ()); } else if (originalParameterTypes.get(i).getJavaType()
     * .equals(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH)) {
     * parameterTypes.add(originalParameterTypes.get(i));
     * addTypeToImport(originalParameterTypes.get(i).getJavaType());
     * parameterNames.add(originalParameterNames.get(i));
     * 
     * // Build finder parameters String
     * finderParamsString.append(", ".concat(originalParameterNames.get(i).
     * getSymbolName())); } }
     * 
     * // Add DatatablesPageable param parameterTypes
     * .add(AnnotatedJavaType.convertFromJavaType(addTypeToImport(this.
     * datatablesPageable))); parameterNames.add(new
     * JavaSymbolName("pageable")); finderParamsString.append(", pageable");
     * 
     * // Add additional 'draw' param AnnotationMetadataBuilder
     * requestParamAnnotation = new
     * AnnotationMetadataBuilder(addTypeToImport(SpringJavaType.
     * REQUEST_PARAM)); requestParamAnnotation.addStringAttribute("value",
     * "draw"); parameterTypes.add(new
     * AnnotatedJavaType(JavaType.INT_OBJECT,
     * requestParamAnnotation.build())); parameterNames.add(new
     * JavaSymbolName("draw"));
     * 
     * // Adding annotations final List<AnnotationMetadataBuilder>
     * annotations = new ArrayList<AnnotationMetadataBuilder>();
     * 
     * // Adding @RequestMapping annotation annotations.add(
     * getControllerMVCService().getRequestMappingAnnotation(
     * SpringEnumDetails.REQUEST_METHOD_GET, "/" + path,
     * stringParameterNames, null, "application/vnd.datatables+json", ""));
     * 
     * // Adding @ResponseBody annotation AnnotationMetadataBuilder
     * responseBodyAnnotation = new
     * AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
     * annotations.add(responseBodyAnnotation);
     * 
     * // Generate body InvocableMemberBodyBuilder bodyBuilder = new
     * InvocableMemberBodyBuilder();
     * 
     * // Generating returnType JavaType returnType =
     * finderMethod.getReturnType(); List<JavaType> returnParameterTypes =
     * returnType.getParameters(); StringBuffer returnTypeParamsString = new
     * StringBuffer(); for (int i = 0; i < returnParameterTypes.size(); i++)
     * { addTypeToImport(returnParameterTypes.get(i)); if (i > 0) {
     * returnTypeParamsString.append(","); }
     * returnTypeParamsString.append(returnParameterTypes.get(i).
     * getSimpleTypeName());
     * 
     * // Add module dependency
     * getTypeLocationService().addModuleDependency(this.controller.getType(
     * ).getModule(), returnParameterTypes.get(i)); }
     * 
     * // ReturnType<ReturnTypeParams> entity = //
     * ENTITY_SERVICE_FIELD.FINDER_NAME(SEARCH_PARAMS); String
     * returnParameterName =
     * StringUtils.uncapitalize(returnParameterTypes.get(0).
     * getSimpleTypeName()); bodyBuilder.newLine(); if
     * (StringUtils.isEmpty(returnTypeParamsString)) {
     * bodyBuilder.appendFormalLine(String.format("%s %s = %s.%s(%s);",
     * addTypeToImport(returnType).getSimpleTypeName(), returnParameterName,
     * getServiceField().getFieldName(), methodName, finderParamsString)); }
     * else {
     * bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(%s);",
     * addTypeToImport(returnType).getSimpleTypeName(),
     * returnTypeParamsString, returnParameterName,
     * getServiceField().getFieldName(), methodName, finderParamsString)); }
     * 
     * // long allAvailableEntity/Projection = //
     * ENTITY_SERVICE_FIELD.COUNT_METHOD(formBean); bodyBuilder.newLine();
     * bodyBuilder.appendFormalLine(String.
     * format("long allAvailable%s = %s.%s(%s);",
     * StringUtils.capitalize(Noun.pluralOf(returnParameterName,
     * Locale.ENGLISH)), getServiceField().getFieldName(),
     * "count".concat(StringUtils.capitalize(path)),
     * parameterNames.get(0))); bodyBuilder.newLine();
     * 
     * // return new DatatablesData<Entity/Projection>(entity/projection, //
     * allAvailableEntity/Projection, draw);
     * bodyBuilder.appendFormalLine(String.
     * format("return new %s<%s>(%s, allAvailable%s, draw);",
     * addTypeToImport(this.datatablesDataType).getSimpleTypeName(),
     * returnTypeParamsString, returnParameterName,
     * StringUtils.capitalize(Noun.pluralOf(returnParameterName,
     * Locale.ENGLISH))));
     * 
     * MethodMetadataBuilder methodBuilder = new
     * MethodMetadataBuilder(this.metadataIdentificationString,
     * Modifier.PUBLIC, methodName, addTypeToImport(new
     * JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0,
     * DataType.TYPE, null, returnParameterTypes)), parameterTypes,
     * parameterNames, bodyBuilder);
     * methodBuilder.setAnnotations(annotations);
     * 
     * return methodBuilder.build();
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
     * 
     * // Get finder parameter names final List<JavaSymbolName>
     * originalParameterNames = finderMethod.getParameterNames();
     * List<String> stringParameterNames = new ArrayList<String>(); for
     * (JavaSymbolName parameterName : originalParameterNames) {
     * stringParameterNames.add(parameterName.getSymbolName()); }
     * 
     * // Define methodName final JavaSymbolName methodName = new
     * JavaSymbolName(finderMethod.getMethodName().getSymbolName().concat(
     * "Redirect"));
     * 
     * // Define path String path = ""; if
     * (StringUtils.startsWith(methodName.getSymbolName(), "count")) { path
     * = StringUtils.removeStart(methodName.getSymbolName(), "count"); }
     * else if (StringUtils.startsWith(methodName.getSymbolName(), "find"))
     * { path = StringUtils.removeStart(methodName.getSymbolName(), "find");
     * } else if (StringUtils.startsWith(methodName.getSymbolName(),
     * "query")) { path =
     * StringUtils.removeStart(methodName.getSymbolName(), "query"); } else
     * if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
     * path = StringUtils.removeStart(methodName.getSymbolName(), "read"); }
     * else { path = methodName.getSymbolName(); } path =
     * StringUtils.uncapitalize(StringUtils.removeEnd(path, "Redirect"));
     * 
     * // Check if exists other method with the same @RequesMapping to
     * generate MethodMetadata existingMVCMethod =
     * getControllerMVCService().getMVCMethodByRequestMapping(
     * controller.getType(), SpringEnumDetails.REQUEST_METHOD_POST, "/" +
     * path, stringParameterNames, null,
     * SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), ""); if
     * (existingMVCMethod != null &&
     * !existingMVCMethod.getDeclaredByMetadataId().equals(this.
     * metadataIdentificationString)) { return existingMVCMethod; }
     * 
     * // Get parameters List<AnnotatedJavaType> parameterTypes = new
     * ArrayList<AnnotatedJavaType>(); List<JavaSymbolName> parameterNames =
     * new ArrayList<JavaSymbolName>();
     * 
     * // Check if finder parameter is a DTO JavaType formBean =
     * finderMethod.getParameterTypes().get(0).getJavaType(); if
     * (getTypeLocationService().getTypeDetails(formBean) != null &&
     * getTypeLocationService()
     * .getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) == null)
     * {
     * 
     * // Finder parameter are entity fields formBean = this.entity; }
     * 
     * // Add form bean parameter AnnotationMetadataBuilder
     * modelAttributeAnnotation = new
     * AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
     * modelAttributeAnnotation.addStringAttribute("value", "formBean");
     * parameterTypes.add(new AnnotatedJavaType(formBean,
     * modelAttributeAnnotation.build())); parameterNames.add(new
     * JavaSymbolName("formBean"));
     * 
     * // Add redirect parameter parameterTypes.add(
     * AnnotatedJavaType.convertFromJavaType(addTypeToImport(SpringJavaType.
     * REDIRECT_ATTRIBUTES))); parameterNames.add(new
     * JavaSymbolName("redirect"));
     * 
     * // Adding annotations final List<AnnotationMetadataBuilder>
     * annotations = new ArrayList<AnnotationMetadataBuilder>();
     * 
     * // Adding @RequestMapping annotation
     * annotations.add(getControllerMVCService().
     * getRequestMappingAnnotation( SpringEnumDetails.REQUEST_METHOD_POST,
     * "/" + path, stringParameterNames, null,
     * SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));
     * 
     * // Generate body InvocableMemberBodyBuilder bodyBuilder = new
     * InvocableMemberBodyBuilder(); bodyBuilder.newLine();
     * 
     * // redirect.addFlashAttribute(entity/dtoSearch);
     * bodyBuilder.appendFormalLine(
     * String.format("redirect.addFlashAttribute(\"formBean\", %s);",
     * parameterNames.get(0))); bodyBuilder.newLine();
     * 
     * // return "redirect:PATH_PREFIX/ENTITY_PLURAL/FINDER_NAME";
     * bodyBuilder.appendFormalLine(String.
     * format("return \"redirect:%s/%s/search/%s\";", this.pathPrefix,
     * this.entityPlural, path));
     * 
     * MethodMetadataBuilder methodBuilder = new
     * MethodMetadataBuilder(this.metadataIdentificationString,
     * Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes,
     * parameterNames, bodyBuilder);
     * methodBuilder.setAnnotations(annotations);
     * 
     * return methodBuilder.build();
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
     * 
     * // Get finder parameter names List<String> stringParameterNames = new
     * ArrayList<String>(); stringParameterNames.add("model");
     * 
     * // Define methodName final JavaSymbolName methodName = new
     * JavaSymbolName(finderMethod.getMethodName().getSymbolName().concat(
     * "Form"));
     * 
     * // Define path String path = ""; if
     * (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(),
     * "count")) { path =
     * StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(),
     * "count"); } else if
     * (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(),
     * "find")) { path =
     * StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(),
     * "find"); } else if
     * (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(),
     * "query")) { path =
     * StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(),
     * "query"); } else if
     * (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(),
     * "read")) { path =
     * StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(),
     * "read"); } else { path = methodName.getSymbolName(); } path =
     * StringUtils.uncapitalize(path).concat("/search-form");
     * 
     * // Check if exists other method with the same @RequesMapping to
     * generate MethodMetadata existingMVCMethod =
     * getControllerMVCService().getMVCMethodByRequestMapping(
     * controller.getType(), SpringEnumDetails.REQUEST_METHOD_GET, "/" +
     * path, stringParameterNames, null,
     * SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), ""); if
     * (existingMVCMethod != null &&
     * !existingMVCMethod.getDeclaredByMetadataId().equals(this.
     * metadataIdentificationString)) { return existingMVCMethod; }
     * 
     * // Get parameters List<AnnotatedJavaType> parameterTypes = new
     * ArrayList<AnnotatedJavaType>(); List<JavaSymbolName> parameterNames =
     * new ArrayList<JavaSymbolName>();
     * 
     * // Add model parameter parameterTypes
     * .add(AnnotatedJavaType.convertFromJavaType(addTypeToImport(
     * SpringJavaType.MODEL))); parameterNames.add(new
     * JavaSymbolName("model"));
     * 
     * // Adding annotations final List<AnnotationMetadataBuilder>
     * annotations = new ArrayList<AnnotationMetadataBuilder>();
     * 
     * // Adding @RequestMapping annotation
     * annotations.add(getControllerMVCService().
     * getRequestMappingAnnotation( SpringEnumDetails.REQUEST_METHOD_GET,
     * "/" + path, stringParameterNames, null,
     * SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));
     * 
     * // Check if finder parameter is a DTO JavaType formBean =
     * finderMethod.getParameterTypes().get(0).getJavaType(); if
     * (getTypeLocationService().getTypeDetails(formBean) != null &&
     * getTypeLocationService()
     * .getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) == null)
     * {
     * 
     * // Finder parameter are entity fields formBean = this.entity; }
     * 
     * // Generate body InvocableMemberBodyBuilder bodyBuilder = new
     * InvocableMemberBodyBuilder(); bodyBuilder.newLine();
     * 
     * // Entity/DTO search = new Entity/DTO();
     * bodyBuilder.appendFormalLine(String.format("%1$s %2$s = new %1$s();",
     * addTypeToImport(formBean).getSimpleTypeName(), "formBean"));
     * bodyBuilder.newLine();
     * 
     * // model.addAttribute("search", search);
     * bodyBuilder.appendFormalLine(String.
     * format("model.addAttribute(\"%1$s\", %1$s);", "formBean"));
     * bodyBuilder.newLine();
     * 
     * // populateForm(model);
     * bodyBuilder.appendFormalLine("populateForm(model);");
     * bodyBuilder.newLine();
     * 
     * // return "PATH_PREFIX/ENTITY_PLURAL/FINDER_NAMEForm"; String
     * pathPrefix = ""; if (StringUtils.isBlank(this.pathPrefix)) {
     * pathPrefix = this.pathPrefix; } else { pathPrefix =
     * this.pathPrefix.concat("/"); }
     * bodyBuilder.appendFormalLine(String.format("return \"%s%s/%s\";",
     * StringUtils.removeStart(pathPrefix, "/"), this.entityPlural,
     * methodName.getSymbolName()));
     * 
     * MethodMetadataBuilder methodBuilder = new
     * MethodMetadataBuilder(this.metadataIdentificationString,
     * Modifier.PUBLIC, methodName, JavaType.STRING, parameterTypes,
     * parameterNames, bodyBuilder);
     * methodBuilder.setAnnotations(annotations);
     * 
     * return methodBuilder.build();
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
   * Build method body which populate in model all required formats for a form
   * based on dateTimeField of a type.
   *
   * Also populate current locale in model.
   *
   * @param bodyBuilder
   * @param dateTimeFields
   *            dateTime fields (optional could be empty or null)
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
    parameterTypes.add(DATATABLES_COLUMNS_PARAM);
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
    parameterNames.add(DATATABLES_COLUMNS_PARAM_NAME);
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

    // Getting the findAllMethod
    MethodMetadata findAllMethod = this.serviceMetadata.getCurrentFindAllWithGlobalSearchMethod();

    // Getting the findAll return type
    JavaType defaultReturnType = findAllMethod.getReturnType().getParameters().get(0);

    // Page<Customer> customers = customerService.findAll(search, pageable);
    bodyBuilder.appendFormalLine("%s<%s> %s = %s().%s(search, pageable);",
        getNameOfJavaType(SpringJavaType.PAGE), getNameOfJavaType(defaultReturnType), itemNames,
        getAccessorMethod(this.controllerMetadata.getServiceField()).getMethodName(),
        findAllMethod.getMethodName());

    final String totalVarName = "total" + StringUtils.capitalize(this.entityPlural) + "Count";
    // long totalCustomersCount = customers.getTotalElements();
    bodyBuilder.appendFormalLine("long %s = %s.getTotalElements();", totalVarName, itemNames);

    // if (search != null && StringUtils.hasText(search.getText())) {
    // totalCustomersCount = customerService.count();
    // }
    bodyBuilder.appendFormalLine("if (search != null && %s.isNotBlank(search.getText())) {",
        getNameOfJavaType(STRING_UTILS_APACHE));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("%s = %s().%s();", totalVarName,
        getAccessorMethod(this.controllerMetadata.getServiceField()).getMethodName(),
        serviceMetadata.getCurrentCountMethod().getMethodName());
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // ConvertedDatatablesData<Owner> datatablesData = new
    // ConvertedDatatablesData<Owner>(owners,
    // totalOwnersCount, draw, conversionService, columns);
    bodyBuilder.appendFormalLine(
        "%1$s<%2$s> datatablesData = new %1$s<%2$s>(%3$s, %4$s, draw, %5$s(), %6$s);",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_CONVERTED_DATATABLES_DATA),
        getNameOfJavaType(defaultReturnType), itemNames, totalVarName,
        getAccessorMethod(this.conversionServiceField).getMethodName(),
        DATATABLES_COLUMNS_PARAM_NAME);

    // return ResponseEntity.ok(datatablesData);
    bodyBuilder.appendFormalLine("return %s.ok(datatablesData);",
        getNameOfJavaType(RESPONSE_ENTITY));

    // Generating returnType
    JavaType returnType =
        JavaType.wrapperOf(RESPONSE_ENTITY, JavaType.wrapperOf(
            SpringletsJavaType.SPRINGLETS_CONVERTED_DATATABLES_DATA, defaultReturnType));

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

    final String itemsName = StringUtils.uncapitalize(this.entityPlural);

    // Getting the findAllMethod
    MethodMetadata findAllMethod = this.serviceMetadata.getCurrentFindAllWithGlobalSearchMethod();

    // Getting the findAll return type
    JavaType defaultReturnType = findAllMethod.getReturnType().getParameters().get(0);

    // Page<Customer> customers = customerService.findAll(search, pageable);
    bodyBuilder.appendFormalLine("%s<%s> %s = %s().%s(search, pageable);",
        getNameOfJavaType(SpringJavaType.PAGE), getNameOfJavaType(defaultReturnType), itemsName,
        getAccessorMethod(this.controllerMetadata.getServiceField()).getMethodName(),
        findAllMethod.getMethodName());

    // String idExpression = "#{id}";
    bodyBuilder.appendFormalLine("String idExpression = \"#{%s}\";", this.entityIdentifier);

    // Select2DataSupport<Customer> select2Data = new
    // Select2DataWithConversion<Customer>(customers, idExpression,
    // conversionService);
    bodyBuilder.appendFormalLine("%s<%s> select2Data = new %s<%s>(%s, idExpression, %s());",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_SELECT2_DATA_SUPPORT),
        getNameOfJavaType(defaultReturnType),
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_SELECT2_DATA_WITH_CONVERSION),
        getNameOfJavaType(defaultReturnType), itemsName,
        getAccessorMethod(this.conversionServiceField).getMethodName());

    // return ResponseEntity.ok(select2Data);
    bodyBuilder.appendFormalLine("return %s.ok(select2Data);", getNameOfJavaType(RESPONSE_ENTITY));

    // Generating returnType
    JavaType returnType =
        JavaType.wrapperOf(RESPONSE_ENTITY, JavaType.wrapperOf(
            SpringletsJavaType.SPRINGLETS_SELECT2_DATA_SUPPORT, defaultReturnType));

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

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

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
   * This method provides the "edit" details form method using Thymeleaf view
   * response type
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

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

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

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

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
   * This method provides the "showInline" method using Thymeleaf view response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowInlineMethod() {
    // Define methodName
    final JavaSymbolName methodName = SHOW_INLINE_METHOD_NAME;

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
    getMappingAnnotation.addStringAttribute("value", "/inline");
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

    // return new ModelAndView("customers/showInline :: inline-content");
    bodyBuilder.appendFormalLine("return new %s(\"%s/showInline :: inline-content\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" detail method using Thymeleaf view
   * response type
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

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

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
   * This method provides the "showInline" detail method using Thymeleaf view
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowDetailInlineMethod() {
    // Define methodName
    final JavaSymbolName methodName = SHOW_INLINE_METHOD_NAME;

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
    getMappingAnnotation.addStringAttribute("value", "/inline");
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // model.addAttribute("entity", entityParam)
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", entityItemName, entityItemName);

    // return new ModelAndView("customerorders/details/showInline :: inline-content");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/showInline :: inline-content\");",
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
    parameterTypes.add(DATATABLES_COLUMNS_PARAM);
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
    parameterNames.add(DATATABLES_COLUMNS_PARAM_NAME);
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
        JavaType.wrapperOf(SpringletsJavaType.SPRINGLETS_CONVERTED_DATATABLES_DATA,
            serviceReturnType.getParameters().get(0));
    final JavaType returnType = JavaType.wrapperOf(RESPONSE_ENTITY, dataReturnType);

    // TODO
    // Add module dependency
    // getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
    // returnParameterTypes.get(i));

    // Generate body
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemsName = StringUtils.uncapitalize(detailsInfo.fieldName);

    // Page<CustomerOrder> orders =
    // customerOrderService.findByCustomer(customer, globalSearch,
    // pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s, search, pageable);",
        getNameOfJavaType(serviceReturnType), itemsName, getAccessorMethod(detailsServiceField)
            .getMethodName(), findAllMethod.getMethodName(), parentParamName);

    final String totalVarName = "total" + StringUtils.capitalize(itemsName) + "Count";

    // long totalOrdersCount =
    // customerOrderService.countByCustomer(customer);
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s);",
        getNameOfJavaType(countByDetailMethod.getReturnType()), totalVarName,
        getAccessorMethod(detailsServiceField).getMethodName(),
        countByDetailMethod.getMethodName(), parentParamName);

    // ConvertedDatatablesData<CustomerOrder> data = new
    // ConvertedDatatablesData<CustomerOrder>(orders,
    // totalOrdersCount, draw, conversionService, columns);
    bodyBuilder.appendFormalLine("%1$s data =  new %1$s(%2$s, %3$s, draw, %4$s(), %5$s);",
        getNameOfJavaType(dataReturnType), itemsName, totalVarName,
        getAccessorMethod(this.conversionServiceField).getMethodName(),
        DATATABLES_COLUMNS_PARAM_NAME);

    // return ResponseEntity.ok(data);
    bodyBuilder.appendFormalLine("return %s.ok(data);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method for details association
   * relationship using Thymeleaf view response type
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

    // customerOrderService.addToDetails(customerOrder,
    // Collections.singleton(orderDetail));
    bodyBuilder.appendFormalLine("%s().%s(%s,%s.singleton(%s));",
        getAccessorMethod(detailsServiceField).getMethodName(),
        addToRelationMethod.getMethodName(), parentItemName,
        getNameOfJavaType(JavaType.COLLECTIONS), entityItemName);

    // return new ModelAndView("redirect:" +
    // collectionLink.to("list").toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + %s().to(%s.LIST).toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW),
        getAccessorMethod(this.methodLinkBuilderFactoryField).getMethodName(),
        getNameOfJavaType(relatedCollectionLinkFactory));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method for details association
   * relationship using Thymeleaf view response type
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
    requestParamAnnotation.addBooleanAttribute("required", false);
    JavaType identifierType =
        setMethod.getParameterTypes().get(1).getJavaType().getParameters().get(0);
    parameterTypes.add(new AnnotatedJavaType(JavaType.wrapperOf(JavaType.LIST, identifierType),
        requestParamAnnotation.build()));

    // Version parameter
    AnnotationMetadataBuilder versionRequestParamAnnotation =
        new AnnotationMetadataBuilder(REQUEST_PARAM);
    versionRequestParamAnnotation.addStringAttribute("value", "parentVersion");
    parameterTypes.add(new AnnotatedJavaType(this.entityMetadata.getCurrentVersionField()
        .getFieldType(), versionRequestParamAnnotation.build()));

    // Concurrency control parameter
    AnnotationMetadataBuilder concurrencyControlRequestParam =
        new AnnotationMetadataBuilder(REQUEST_PARAM);
    concurrencyControlRequestParam.addStringAttribute("value", "concurrency");
    concurrencyControlRequestParam.addBooleanAttribute("required", false);
    concurrencyControlRequestParam.addStringAttribute("defaultValue", "");
    parameterTypes.add(new AnnotatedJavaType(JavaType.STRING, concurrencyControlRequestParam
        .build()));

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
    parameterNames.add(VERSION_PARAM_NAME);
    parameterNames.add(new JavaSymbolName("concurrencyControl"));
    parameterNames.add(MODEL_PARAM_NAME);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    /*
     * // Remove empty values for (Iterator<Long> iterator =
     * products.iterator(); iterator.hasNext();) { if (iterator.next() ==
     * null) { iterator.remove(); } }
     */
    bodyBuilder.appendFormalLine("// Remove empty values");

    // if(books != null){
    bodyBuilder.appendFormalLine("if (%s != null) {", itemsName);
    bodyBuilder.indent();

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

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // // Concurrency control
    bodyBuilder.appendFormalLine("// Concurrency control");

    JavaSymbolName parentName = setMethod.getParameterNames().get(0);
    // if(version != pet.getVersion() && StringUtils.isEmpty(concurrencyControl)){
    bodyBuilder.appendFormalLine("if(%s != %s.%s() && %s.isEmpty(concurrencyControl)){",
        VERSION_PARAM_NAME, parentName,
        getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName(),
        getNameOfJavaType(new JavaType("org.apache.commons.lang3.StringUtils")));
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // // Obtain the selected books and include them in the author that will be 
    bodyBuilder
        .appendFormalLine("// Obtain the selected books and include them in the author that will be ");
    // // included in the view
    bodyBuilder.appendFormalLine("// included in the view");

    // if(books != null){
    bodyBuilder.appendFormalLine("if (%s != null) {", itemsName);
    bodyBuilder.indent();

    // author.setBooks(new HashSet<Book>(getBookService().findAll(books)));
    bodyBuilder.appendFormalLine("%s.%s(new %s<%s>(%s().findAll(%s)));", parentName, setMethod
        .getMethodName(), getNameOfJavaType(JavaType.HASH_SET),
        getNameOfJavaType(detailsInfo.childType),
        getAccessorMethod(controllerMetadata.getDetailsServiceFields(detailsInfo.childType))
            .getMethodName(), itemsName);

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}else{");
    bodyBuilder.indent();

    // author.setBooks(new HashSet<Book>());
    bodyBuilder.appendFormalLine("%s.%s(new %s<%s>());", parentName, setMethod.getMethodName(),
        getNameOfJavaType(JavaType.HASH_SET), getNameOfJavaType(detailsInfo.childType));

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // // Reset the version to prevent update
    bodyBuilder.appendFormalLine("// Reset the version to prevent update");

    // author.setVersion(version);
    bodyBuilder.appendFormalLine(" %s.setVersion(%s);", parentName, VERSION_PARAM_NAME);

    // // Include the updated author in the model
    bodyBuilder.appendFormalLine("// Include the updated element in the model");

    // model.addAttribute("author", author);
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", parentName, parentName);

    // model.addAttribute("concurrency", true);
    bodyBuilder.appendFormalLine("model.addAttribute(\"concurrency\", true);");

    // return new ModelAndView("owners/pets/create");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/create\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath, itemsName);

    bodyBuilder.indentRemove();

    // }else if(version != pet.getVersion() && "discard".equals(concurrencyControl)){
    bodyBuilder
        .appendFormalLine("}else if(%s != %s.%s() && \"discard\".equals(concurrencyControl)){",
            VERSION_PARAM_NAME, parentName,
            getAccessorMethod(this.entityMetadata.getCurrentVersionField()).getMethodName());
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // // Provide the original author from the Database
    bodyBuilder.appendFormalLine("// Provide the original element from the Database");

    // model.addAttribute("author", author);
    bodyBuilder.appendFormalLine("model.addAttribute(\"%s\", %s);", parentName, parentName);

    // model.addAttribute("concurrency", false);
    bodyBuilder.appendFormalLine("model.addAttribute(\"concurrency\", false);");

    // return new ModelAndView("owners/pets/create");
    bodyBuilder.appendFormalLine("return new %s(\"%s/%s/create\");",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW), viewsPath, itemsName);

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // categoryService.setProducts(category, products);
    bodyBuilder.appendFormalLine("%s().%s(%s,%s);", getAccessorMethod(detailsServiceField)
        .getMethodName(), setMethod.getMethodName(), setMethod.getParameterNames().get(0),
        itemsName);

    // return new ModelAndView("redirect:" +
    // collectionLink.to("list").toUriString());
    bodyBuilder.appendFormalLine("return new %s(\"redirect:\" + %s().to(%s.LIST).toUriString());",
        getNameOfJavaType(SpringJavaType.MODEL_AND_VIEW),
        getAccessorMethod(this.methodLinkBuilderFactoryField).getMethodName(),
        getNameOfJavaType(relatedCollectionLinkFactory));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.MODEL_AND_VIEW, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" form method for details using Thymeleaf
   * view response type
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
    String parentEntityParamName = StringUtils.uncapitalize(parentEntity.getSimpleTypeName());
    parameterNames.add(new JavaSymbolName(parentEntityParamName));
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
    // model.addAttribute("entity", new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(\"%s\", new %s());",
        StringUtils.uncapitalize(getNameOfJavaType(entity)), getNameOfJavaType(entity)));

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
    bodyBuilder.appendFormalLine("%s().%s(%s,%s.singleton(%s.%s()));",
        getAccessorMethod(detailsServiceField).getMethodName(), removeFromMethod.getMethodName(),
        removeFromMethod.getParameterNames().get(0), getNameOfJavaType(JavaType.COLLECTIONS),
        itemsName, detailsInfo.childEntityMetadata.getCurrentIdentifierAccessor().getMethodName());

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

    // customerService.removeFromOrders(customer,
    // Collections.singleton(order));
    bodyBuilder.appendFormalLine("%s().%s(%s,%s.singleton(%s));",
        getAccessorMethod(detailsServiceField).getMethodName(), removeFromMethod.getMethodName(),
        removeFromMethod.getParameterNames().get(0), getNameOfJavaType(JavaType.COLLECTIONS),
        itemsName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getRemoveFromDetailsBatchMethod() {
    RelationInfoExtended detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata removeFromMethod =
        detailsServiceMetadata.getRemoveFromRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final String methodName = removeFromMethod.getMethodName().getSymbolName().concat("Batch");
    JavaSymbolName itemsName = removeFromMethod.getParameterNames().get(1);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(0)
        .getJavaType(), AnnotationMetadataBuilder.getInstance(SpringJavaType.MODEL_ATTRIBUTE)));
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", itemsName.getSymbolName());

    parameterTypes.add(new AnnotatedJavaType(JavaType.collectionOf(removeFromMethod
        .getParameterTypes().get(1).getJavaType().getParameters().get(0)), pathVariableAnnotation
        .build()));

    MethodMetadata existingMethod =
        getGovernorMethod(new JavaSymbolName(methodName),
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.DELETE_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName);
    postMappingAnnotation.addStringAttribute("value", "/batch/{" + itemsName.getSymbolName() + "}");
    annotations.add(postMappingAnnotation);
    this.mvcMethodNames.put(methodName, methodName);

    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(removeFromMethod.getParameterNames());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.removeFromOrders(customer, ordersToRemove);
    bodyBuilder.appendFormalLine("%s().%s(%s, %s);", getAccessorMethod(detailsServiceField)
        .getMethodName(), removeFromMethod.getMethodName(), removeFromMethod.getParameterNames()
        .get(0), itemsName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(methodName),
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /*
   * =========================================================================
   * =====
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
   * 
   * @param controller
   * @param methodName
   * @return
   */
  public static String getMvcUrlNameFor(JavaType controller, JavaSymbolName methodName) {
    return getMvcControllerName(controller) + "#" + getMvcMethodName(methodName);
  }

  /**
   * gets Mvc URL name for a controller method
   * 
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

  public List<MethodMetadata> getExportMethods() {
    return exportMethods;
  }

}
