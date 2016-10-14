package org.springframework.roo.model;

/**
 * Constants for Roo-specific {@link JavaType}s. Use them in preference to
 * creating new instances of these types.
 *
 * @author Andrew Swan
 * @author Juan Carlos García
 * @since 1.2.0
 */
public final class RooJavaType {

  // org.springframework.roo.addon
  public static final JavaType ROO_CONFIGURABLE = new JavaType(
      "org.springframework.roo.addon.configurable.annotations.RooConfigurable");
  public static final JavaType ROO_DATA_ON_DEMAND = new JavaType(
      "org.springframework.roo.addon.dod.annotations.RooDataOnDemand");
  public static final JavaType ROO_DB_MANAGED = new JavaType(
      "org.springframework.roo.addon.dbre.annotations.RooDbManaged");
  public static final JavaType ROO_EQUALS = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooEquals");
  public static final JavaType ROO_FINDERS = new JavaType(
      "org.springframework.roo.addon.finder.annotations.RooFinders");
  public static final JavaType ROO_FINDER = new JavaType(
      "org.springframework.roo.addon.finder.annotations.RooFinder");
  public static final JavaType ROO_GWT_LOCATOR = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtLocator");
  public static final JavaType ROO_GWT_MIRRORED_FROM = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtMirroredFrom");
  public static final JavaType ROO_GWT_PROXY = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtProxy");
  public static final JavaType ROO_GWT_REQUEST = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtRequest");
  public static final JavaType ROO_GWT_UNMANAGED_REQUEST = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtUnmanagedRequest");
  public static final JavaType ROO_IDENTIFIER = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.identifier.RooIdentifier");
  public static final JavaType ROO_INTEGRATION_TEST = new JavaType(
      "org.springframework.roo.addon.test.annotations.RooIntegrationTest");
  public static final JavaType ROO_JAVA_BEAN = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooJavaBean");
  public static final JavaType ROO_JSF_APPLICATION_BEAN = new JavaType(
      "org.springframework.roo.addon.jsf.application.RooJsfApplicationBean");
  public static final JavaType ROO_JSF_CONVERTER = new JavaType(
      "org.springframework.roo.addon.jsf.converter.RooJsfConverter");
  public static final JavaType ROO_JSF_MANAGED_BEAN = new JavaType(
      "org.springframework.roo.addon.jsf.managedbean.RooJsfManagedBean");
  public static final JavaType ROO_MONGO_ENTITY = new JavaType(
      "org.springframework.roo.addon.layers.repository.mongo.RooMongoEntity");
  public static final JavaType ROO_NEO4J_ENTITY = new JavaType(
      "org.springframework.roo.addon.layers.repository.neo4j.RooNeo4jEntity");
  public static final JavaType ROO_OP4J =
      new JavaType("org.springframework.roo.addon.op4j.RooOp4j");
  public static final JavaType ROO_PLURAL = new JavaType(
      "org.springframework.roo.addon.plural.annotations.RooPlural");
  public static final JavaType ROO_REPOSITORY_MONGO = new JavaType(
      "org.springframework.roo.addon.layers.repository.mongo.RooMongoRepository");
  public static final JavaType ROO_REPOSITORY_NEO4J = new JavaType(
      "org.springframework.roo.addon.layers.repository.neo4j.RooNeo4jRepository");
  public static final JavaType ROO_SERIALIZABLE = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooSerializable");
  public static final JavaType ROO_SOLR_SEARCHABLE = new JavaType(
      "org.springframework.roo.addon.solr.RooSolrSearchable");
  public static final JavaType ROO_SOLR_WEB_SEARCHABLE = new JavaType(
      "org.springframework.roo.addon.solr.RooSolrWebSearchable");
  public static final JavaType ROO_TO_STRING = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooToString");
  public static final JavaType ROO_UPLOADED_FILE = new JavaType(
      "org.springframework.roo.classpath.operations.jsr303.RooUploadedFile");
  public static final JavaType ROO_SEARCH = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.finder.RooSearch");
  public static final JavaType ROO_WEB_JSON = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.json.RooWebJson");
  public static final JavaType ROO_WEB_SCAFFOLD = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.scaffold.RooWebScaffold");

  // Spring Roo 2.x annotations
  public static final JavaType ROO_JPA_ENTITY = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.entity.RooJpaEntity");
  public static final JavaType ROO_JPA_RELATION = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.entity.RooJpaRelation");
  public static final JavaType ROO_ENTITY_PROJECTION = new JavaType(
      "org.springframework.roo.addon.dto.annotations.RooEntityProjection");
  public static final JavaType ROO_READ_ONLY_REPOSITORY = new JavaType(
      "org.springframework.roo.addon.layers.repository.jpa.annotations.RooReadOnlyRepository");
  public static final JavaType ROO_REPOSITORY_JPA = new JavaType(
      "org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository");
  public static final JavaType ROO_REPOSITORY_JPA_CUSTOM = new JavaType(
      "org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustom");
  public static final JavaType ROO_REPOSITORY_JPA_CUSTOM_IMPL = new JavaType(
      "org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl");
  public static final JavaType ROO_DTO = new JavaType(
      "org.springframework.roo.addon.dto.annotations.RooDTO");
  public static final JavaType ROO_SERVICE = new JavaType(
      "org.springframework.roo.addon.layers.service.annotations.RooService");
  public static final JavaType ROO_SERVICE_IMPL = new JavaType(
      "org.springframework.roo.addon.layers.service.annotations.RooServiceImpl");
  public static final JavaType ROO_JPA_AUDIT = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.audit.RooJpaAudit");
  public static final JavaType ROO_UNIT_TEST = new JavaType(
      "org.springframework.roo.addon.test.annotations.RooUnitTest");
  public static final JavaType ROO_CONTROLLER = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.RooController");
  public static final JavaType ROO_DETAIL = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.RooDetail");

  public static final JavaType ROO_WEB_MVC_CONFIGURATION = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.config.RooWebMvcConfiguration");
  public static final JavaType ROO_WEB_MVC_JSON_CONFIGURATION =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.config.RooWebMvcJSONConfiguration");
  public static final JavaType ROO_JSON = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.responses.json.RooJSON");
  public static final JavaType ROO_THYMELEAF = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf");
  public static final JavaType ROO_THYMELEAF_MAIN_CONTROLLER = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafMainController");
  public static final JavaType ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooWebMvcThymeleafUIConfiguration");
  public static final JavaType ROO_THYMELEAF_DATATABLES_DATA = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafDatatablesData");
  public static final JavaType ROO_THYMELEAF_DATATABLES_PAGEABLE_HANDLER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafDatatablesPageableHandler");
  public static final JavaType ROO_THYMELEAF_DATATABLES_PAGEABLE = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafDatatablesPageable");
  public static final JavaType ROO_THYMELEAF_DATATABLES_SORT_HANDLER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafDatatablesSortHandler");
  public static final JavaType ROO_THYMELEAF_DATATABLES_SORT = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafDatatablesSort");
  public static final JavaType ROO_GLOBAL_SEARCH_HANDLER = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.RooGlobalSearchHandler");

  //Roo JSON converter annotations
  public static final JavaType ROO_JSON_BINDING_ERROR_EXCEPTION =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONBindingErrorException");
  public static final JavaType ROO_JSON_BINDING_RESULT_SERIALIZER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONBindingResultSerializer");
  public static final JavaType ROO_JSON_CONVERSION_SERVICE_BEAN_SERIALIZER_MODIFIER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONConversionServiceBeanSerializerModifier");
  public static final JavaType ROO_JSON_CONVERSION_SERVICE_PROPERTY_SERIALIZER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONConversionServicePropertySerializer");
  public static final JavaType ROO_JSON_DATA_BINDER_BEAN_DESERIALIZER_MODIFIER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONDataBinderBeanDeserializerModifier");
  public static final JavaType ROO_JSON_DATA_BINDER_DESERIALIZER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONDataBinderDeserializer");
  public static final JavaType ROO_JSON_FIELD_ERROR_SERIALIZER =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONFieldErrorSerializer");
  public static final JavaType ROO_JSON_JSONP_ADVICE =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json.RooJSONJsonpAdvice");

  public static final JavaType ROO_ENUM_CONTROLLER_TYPE = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType");

  public static final JavaType ROO_ENUM_RELATION_TYPE = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType");

  /**
   * Constructor is private to prevent instantiation
   */
  private RooJavaType() {}
}
