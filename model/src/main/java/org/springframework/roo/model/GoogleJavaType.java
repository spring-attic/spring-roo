package org.springframework.roo.model;

/**
 * Constants for Google-specific {@link JavaType}s. N.B. GWT-specific types are
 * in GwtUtils. Use them in preference to creating new instances of these types.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class GoogleJavaType {

    // com.google.appengine
    public static final JavaType GAE_DATASTORE_KEY = new JavaType(
            "com.google.appengine.api.datastore.Key");
    public static final JavaType GAE_DATASTORE_KEY_FACTORY = new JavaType(
            "com.google.appengine.api.datastore.KeyFactory");
    public static final JavaType GAE_LOCAL_SERVICE_TEST_HELPER = new JavaType(
            "com.google.appengine.tools.development.testing.LocalServiceTestHelper");
    // org.datanucleus
    public static final JavaType DATANUCLEUS_JPA_EXTENSION = new JavaType(
            "org.datanucleus.api.jpa.annotations.Extension");

    /**
     * Constructor is private to prevent instantiation
     */
    private GoogleJavaType() {
    }
}
