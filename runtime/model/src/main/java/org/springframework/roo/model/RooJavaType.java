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
  public static final JavaType ROO_JPA_DATA_ON_DEMAND = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.dod.RooJpaDataOnDemand");
  public static final JavaType ROO_DB_MANAGED = new JavaType(
      "org.springframework.roo.addon.dbre.annotations.RooDbManaged");
  public static final JavaType ROO_DESERIALIZER = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.config.RooDeserializer");
  public static final JavaType ROO_DOMAIN_MODEL_MODULE = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.config.RooDomainModelModule");
  public static final JavaType ROO_EQUALS = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooEquals");
  @Deprecated
  public static final JavaType ROO_GWT_LOCATOR = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtLocator");
  @Deprecated
  public static final JavaType ROO_GWT_MIRRORED_FROM = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtMirroredFrom");
  @Deprecated
  public static final JavaType ROO_GWT_PROXY = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtProxy");
  @Deprecated
  public static final JavaType ROO_GWT_REQUEST = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtRequest");
  @Deprecated
  public static final JavaType ROO_GWT_UNMANAGED_REQUEST = new JavaType(
      "org.springframework.roo.addon.gwt.RooGwtUnmanagedRequest");
  public static final JavaType ROO_IDENTIFIER = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.identifier.RooIdentifier");
  public static final JavaType ROO_JAVA_BEAN = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooJavaBean");
  @Deprecated
  public static final JavaType ROO_JSF_APPLICATION_BEAN = new JavaType(
      "org.springframework.roo.addon.jsf.application.RooJsfApplicationBean");
  @Deprecated
  public static final JavaType ROO_JSF_CONVERTER = new JavaType(
      "org.springframework.roo.addon.jsf.converter.RooJsfConverter");
  @Deprecated
  public static final JavaType ROO_JSF_MANAGED_BEAN = new JavaType(
      "org.springframework.roo.addon.jsf.managedbean.RooJsfManagedBean");
  public static final JavaType ROO_JSON_MIXIN = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.config.RooJsonMixin");
  @Deprecated
  public static final JavaType ROO_MONGO_ENTITY = new JavaType(
      "org.springframework.roo.addon.layers.repository.mongo.RooMongoEntity");
  @Deprecated
  public static final JavaType ROO_NEO4J_ENTITY = new JavaType(
      "org.springframework.roo.addon.layers.repository.neo4j.RooNeo4jEntity");
  public static final JavaType ROO_OPERATIONS = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.RooOperations");
  @Deprecated
  public static final JavaType ROO_OP4J =
      new JavaType("org.springframework.roo.addon.op4j.RooOp4j");
  public static final JavaType ROO_PLURAL = new JavaType(
      "org.springframework.roo.addon.plural.annotations.RooPlural");
  @Deprecated
  public static final JavaType ROO_REPOSITORY_MONGO = new JavaType(
      "org.springframework.roo.addon.layers.repository.mongo.RooMongoRepository");
  public static final JavaType ROO_REPOSITORY_NEO4J = new JavaType(
      "org.springframework.roo.addon.layers.repository.neo4j.RooNeo4jRepository");
  public static final JavaType ROO_SERIALIZABLE = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooSerializable");
  @Deprecated
  public static final JavaType ROO_SOLR_SEARCHABLE = new JavaType(
      "org.springframework.roo.addon.solr.RooSolrSearchable");
  @Deprecated
  public static final JavaType ROO_SOLR_WEB_SEARCHABLE = new JavaType(
      "org.springframework.roo.addon.solr.RooSolrWebSearchable");
  public static final JavaType ROO_TO_STRING = new JavaType(
      "org.springframework.roo.addon.javabean.annotations.RooToString");
  public static final JavaType ROO_UPLOADED_FILE = new JavaType(
      "org.springframework.roo.classpath.operations.jsr303.RooUploadedFile");
  public static final JavaType ROO_SEARCH = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.finder.RooSearch");
  @Deprecated
  public static final JavaType ROO_WEB_SCAFFOLD = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.scaffold.RooWebScaffold");

  // Spring Roo 2.x annotations
  public static final JavaType ROO_JPA_DATA_ON_DEMAND_CONFIGURATION = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.dod.RooJpaDataOnDemandConfiguration");
  public static final JavaType ROO_JPA_ENTITY_FACTORY = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.entity.factory.RooJpaEntityFactory");
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
  public static final JavaType ROO_JPA_REPOSITORY_CONFIGURATION =
      new JavaType(
          "org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryConfiguration");
  public static final JavaType ROO_FINDER = new JavaType(
      "org.springframework.roo.addon.layers.repository.jpa.annotations.finder.RooFinder");
  public static final JavaType ROO_DTO = new JavaType(
      "org.springframework.roo.addon.dto.annotations.RooDTO");
  public static final JavaType ROO_SERVICE = new JavaType(
      "org.springframework.roo.addon.layers.service.annotations.RooService");
  public static final JavaType ROO_SERVICE_IMPL = new JavaType(
      "org.springframework.roo.addon.layers.service.annotations.RooServiceImpl");
  public static final JavaType ROO_JPA_AUDIT = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.audit.RooJpaAudit");
  public static final JavaType ROO_CONTROLLER = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.RooController");
  public static final JavaType ROO_DETAIL = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.RooDetail");

  public static final JavaType ROO_WEB_MVC_CONFIGURATION = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.config.RooWebMvcConfiguration");
  public static final JavaType ROO_JSON = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.responses.json.RooJSON");
  public static final JavaType ROO_EXCEPTION_HANDLERS = new JavaType(
      "org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandlers");
  public static final JavaType ROO_EXCEPTION_HANDLER = new JavaType(
      "org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandler");
  public static final JavaType ROO_LINK_FACTORY = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooLinkFactory");
  public static final JavaType ROO_THYMELEAF = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf");
  public static final JavaType ROO_THYMELEAF_MAIN_CONTROLLER = new JavaType(
      "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleafMainController");
  public static final JavaType ROO_WEB_MVC_THYMELEAF_UI_CONFIGURATION =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooWebMvcThymeleafUIConfiguration");

  public static final JavaType ROO_ENUM_CONTROLLER_TYPE = new JavaType(
      "org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType");

  public static final JavaType ROO_ENUM_RELATION_TYPE = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType");

  public static final JavaType ROO_WS_CLIENTS = new JavaType(
      "org.springframework.roo.addon.ws.annotations.RooWsClients");

  public static final JavaType ROO_WS_CLIENT = new JavaType(
      "org.springframework.roo.addon.ws.annotations.RooWsClient");

  public static final JavaType ROO_ENUM_SOAP_BINDING_TYPE = new JavaType(
      "org.springframework.roo.addon.ws.annotations.SoapBindingType");

  public static final JavaType ROO_SEI = new JavaType(
      "org.springframework.roo.addon.ws.annotations.RooSei");

  public static final JavaType ROO_SEI_IMPL = new JavaType(
      "org.springframework.roo.addon.ws.annotations.RooSeiImpl");

  public static final JavaType ROO_WS_ENDPOINTS = new JavaType(
      "org.springframework.roo.addon.ws.annotations.RooWsEndpoints");

  public static final JavaType ROO_JAXB_ENTITY = new JavaType(
      "org.springframework.roo.addon.ws.annotations.jaxb.RooJaxbEntity");

  public static final JavaType ROO_SECURITY_FILTER = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooSecurityFilter");
  public static final JavaType ROO_SECURITY_FILTERS = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooSecurityFilters");
  public static final JavaType ROO_SECURITY_AUTHORIZATION = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooSecurityAuthorization");
  public static final JavaType ROO_SECURITY_AUTHORIZATIONS = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooSecurityAuthorizations");

  // Test classes annotations
  public static final JavaType ROO_JPA_UNIT_TEST = new JavaType(
      "org.springframework.roo.addon.jpa.annotations.test.RooJpaUnitTest");
  public static final JavaType ROO_REPOSITORY_JPA_INTEGRATION_TEST =
      new JavaType(
          "org.springframework.roo.addon.layers.repository.jpa.annotations.test.RooRepositoryJpaIntegrationTest");
  public static final JavaType ROO_JSON_CONTROLLER_INTEGRATION_TEST =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.controller.annotations.test.RooJsonControllerIntegrationTest");
  public static final JavaType ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST =
      new JavaType(
          "org.springframework.roo.addon.web.mvc.thymeleaf.annotations.test.RooThymeleafControllerIntegrationTest");

  /**
   * Constructor is private to prevent instantiation
   */
  private RooJavaType() {}
}
