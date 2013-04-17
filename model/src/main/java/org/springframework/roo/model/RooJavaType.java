package org.springframework.roo.model;

/**
 * Constants for Roo-specific {@link JavaType}s. Use them in preference to
 * creating new instances of these types.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class RooJavaType {

    // org.springframework.roo.addon
    public static final JavaType ROO_CONFIGURABLE = new JavaType(
            "org.springframework.roo.addon.configurable.RooConfigurable");
    public static final JavaType ROO_CONVERSION_SERVICE = new JavaType(
            "org.springframework.roo.addon.web.mvc.controller.converter.RooConversionService");
    public static final JavaType ROO_DATA_ON_DEMAND = new JavaType(
            "org.springframework.roo.addon.dod.RooDataOnDemand");
    public static final JavaType ROO_DB_MANAGED = new JavaType(
            "org.springframework.roo.addon.dbre.RooDbManaged");
    public static final JavaType ROO_EDITOR = new JavaType(
            "org.springframework.roo.addon.property.editor.RooEditor");
    public static final JavaType ROO_EQUALS = new JavaType(
            "org.springframework.roo.addon.equals.RooEquals");
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
            "org.springframework.roo.addon.jpa.identifier.RooIdentifier");
    public static final JavaType ROO_INTEGRATION_TEST = new JavaType(
            "org.springframework.roo.addon.test.RooIntegrationTest");
    public static final JavaType ROO_JAVA_BEAN = new JavaType(
            "org.springframework.roo.addon.javabean.RooJavaBean");
    public static final JavaType ROO_JPA_ACTIVE_RECORD = new JavaType(
            "org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord");
    public static final JavaType ROO_JPA_ENTITY = new JavaType(
            "org.springframework.roo.addon.jpa.entity.RooJpaEntity");
    public static final JavaType ROO_JSF_APPLICATION_BEAN = new JavaType(
            "org.springframework.roo.addon.jsf.application.RooJsfApplicationBean");
    public static final JavaType ROO_JSF_CONVERTER = new JavaType(
            "org.springframework.roo.addon.jsf.converter.RooJsfConverter");
    public static final JavaType ROO_JSF_MANAGED_BEAN = new JavaType(
            "org.springframework.roo.addon.jsf.managedbean.RooJsfManagedBean");
    public static final JavaType ROO_JSON = new JavaType(
            "org.springframework.roo.addon.json.RooJson");
    public static final JavaType ROO_MONGO_ENTITY = new JavaType(
            "org.springframework.roo.addon.layers.repository.mongo.RooMongoEntity");
    public static final JavaType ROO_NEO4J_ENTITY = new JavaType(
            "org.springframework.roo.addon.layers.repository.neo4j.RooNeo4jEntity");
    public static final JavaType ROO_OP4J = new JavaType(
            "org.springframework.roo.addon.op4j.RooOp4j");
    public static final JavaType ROO_PERMISSION_EVALUATOR = new JavaType(
            "org.springframework.roo.addon.security.RooPermissionEvaluator");
    public static final JavaType ROO_PLURAL = new JavaType(
            "org.springframework.roo.addon.plural.RooPlural");
    public static final JavaType ROO_REPOSITORY_JPA = new JavaType(
            "org.springframework.roo.addon.layers.repository.jpa.RooJpaRepository");
    public static final JavaType ROO_REPOSITORY_MONGO = new JavaType(
            "org.springframework.roo.addon.layers.repository.mongo.RooMongoRepository");
    public static final JavaType ROO_REPOSITORY_NEO4J = new JavaType(
            "org.springframework.roo.addon.layers.repository.neo4j.RooNeo4jRepository");
    public static final JavaType ROO_SERIALIZABLE = new JavaType(
            "org.springframework.roo.addon.serializable.RooSerializable");
    public static final JavaType ROO_SERVICE = new JavaType(
            "org.springframework.roo.addon.layers.service.RooService");
    public static final JavaType ROO_SOLR_SEARCHABLE = new JavaType(
            "org.springframework.roo.addon.solr.RooSolrSearchable");
    public static final JavaType ROO_SOLR_WEB_SEARCHABLE = new JavaType(
            "org.springframework.roo.addon.solr.RooSolrWebSearchable");
    public static final JavaType ROO_TO_STRING = new JavaType(
            "org.springframework.roo.addon.tostring.RooToString");
    public static final JavaType ROO_UPLOADED_FILE = new JavaType(
            "org.springframework.roo.classpath.operations.jsr303.RooUploadedFile");
    public static final JavaType ROO_WEB_FINDER = new JavaType(
            "org.springframework.roo.addon.web.mvc.controller.finder.RooWebFinder");
    public static final JavaType ROO_WEB_JSON = new JavaType(
            "org.springframework.roo.addon.web.mvc.controller.json.RooWebJson");
    public static final JavaType ROO_WEB_SCAFFOLD = new JavaType(
            "org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold");

    /**
     * Constructor is private to prevent instantiation
     */
    private RooJavaType() {
    }
}