package org.springframework.roo.addon.gwt;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link GwtProxyProperty}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GwtProxyPropertyTest {

    private static final String GETTER = "getBar";
    private static final String NAME = "foo";

    @Test
    public void testSetIsCollectionOfProxy() {
        // Set up
        final JavaPackage mockTopLevelPackage = mock(JavaPackage.class);
        final ClassOrInterfaceTypeDetails mockCoitd = mock(ClassOrInterfaceTypeDetails.class);
        final JavaType genericType = new JavaType(
                "com.foo.roo2881.client.proxy.Foo1Proxy");
        final JavaType proxyType = new JavaType("java.util.Set", 0,
                DataType.TYPE, null, Arrays.asList(genericType));
        final List<AnnotationMetadata> annotations = Collections.emptyList();
        final GwtProxyProperty proxyProperty = new GwtProxyProperty(
                mockTopLevelPackage, mockCoitd, proxyType, NAME, annotations,
                GETTER);

        // Invoke and check
        assertTrue(proxyProperty.isCollectionOfProxy());
    }
}
