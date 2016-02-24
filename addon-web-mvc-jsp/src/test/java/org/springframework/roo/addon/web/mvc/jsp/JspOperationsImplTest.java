package org.springframework.roo.addon.web.mvc.jsp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link JspOperationsImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JspOperationsImplTest {

    /**
     * Asserts that the given preferred mapping provided by the user gives rise
     * to the expected folder name and mapping to use in the annotation
     * 
     * @param preferredMapping
     * @param expectedFolder
     * @param expectedMapping
     */
    private void assertFolderAndMapping(final String preferredMapping,
            final String expectedFolder, final String expectedMapping) {
        // Set up
        final JavaType mockController = mock(JavaType.class);
        when(mockController.getSimpleTypeName()).thenReturn("FooController");

        // Invoke
        final ImmutablePair<String, String> pair = JspOperationsImpl
                .getFolderAndMapping(preferredMapping, mockController);

        // Check
        assertEquals(expectedFolder, pair.getKey());
        assertEquals(expectedMapping, pair.getValue());
    }

    @Test
    public void testGetFolderAndMappingForBlankPreferredMapping() {
        assertFolderAndMapping("", "foo", "/foo/**");
    }

    @Test
    public void testGetFolderAndMappingForPreferredMappingWithLeadingSlash() {
        assertFolderAndMapping("/foo", "foo", "/foo/**");
    }

    @Test
    public void testGetFolderAndMappingForPreferredMappingWithMixedCase() {
        assertFolderAndMapping("fooBar", "fooBar", "/fooBar/**");
    }

    @Test
    public void testGetFolderAndMappingForPreferredMappingWithTrailingSlash() {
        assertFolderAndMapping("foo/", "foo", "/foo/**");
    }

    @Test
    public void testGetFolderAndMappingForPreferredMappingWithTrailingWildcard() {
        assertFolderAndMapping("foo/**", "foo", "/foo/**");
    }

    @Test
    public void testGetFolderAndMappingForUnadornedPreferredMapping() {
        assertFolderAndMapping("foo", "foo", "/foo/**");
    }
}
