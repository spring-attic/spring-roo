package org.springframework.roo.classpath.layers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link MemberTypeAdditions}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MemberTypeAdditionsTest {

    /**
     * Asserts that
     * {@link MemberTypeAdditions#buildMethodCall(String, String, java.util.Iterator)}
     * builds the expected method call from the given parameters
     * 
     * @param expectedMethodCall
     * @param target
     * @param method
     * @param parameterNames
     */
    private void assertMethodCall(final String expectedMethodCall,
            final String target, final String method,
            final MethodParameter... parameters) {
        assertEquals(
                expectedMethodCall,
                MemberTypeAdditions.buildMethodCall(target, method,
                        Arrays.asList(parameters)));
    }

    @Test
    public void testGetInvokedFieldWhenBuilderIsNull() {
        // Set up
        final MemberTypeAdditions memberTypeAdditions = new MemberTypeAdditions(
                null, "foo", "foo()", false, null);

        // Invoke and check
        assertNull(memberTypeAdditions.getInvokedField());
    }

    @Test
    public void testGetMethodCallWithBlankTargetAndNoParameters() {
        assertMethodCall("foo()", null, "foo");
    }

    @Test
    public void testGetMethodCallWithBlankTargetAndTwoParameters() {
        final MethodParameter firstNameParameter = new MethodParameter(
                JavaType.STRING, "firstName");
        final MethodParameter lastNameParameter = new MethodParameter(
                JavaType.STRING, "lastName");
        assertMethodCall("matchmakingService.marry(firstName, lastName)",
                "matchmakingService", "marry", firstNameParameter,
                lastNameParameter);
    }

    @Test
    public void testGetMethodCallWithTargetAndNoParameters() {
        assertMethodCall("Foo.bar()", "Foo", "bar");
    }
}
