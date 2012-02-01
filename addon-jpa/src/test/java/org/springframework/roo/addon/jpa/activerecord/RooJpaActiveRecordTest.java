package org.springframework.roo.addon.jpa.activerecord;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Test;
import org.springframework.roo.addon.jpa.entity.RooJpaEntity;

/**
 * Unit test of the {@link RooJpaActiveRecord} annotation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RooJpaActiveRecordTest {

    /**
     * Asserts that a method with the same name, return type, and default value
     * as the given target method exists within the given candidate methods.
     * This is less strict than calling {@link Method#equals(Object)}.
     * 
     * @param targetMethod
     * @param candidateMethods
     */
    private void assertMethodExists(final Method targetMethod,
            final Iterable<? extends Method> candidateMethods) {
        for (final Method candidateMethod : candidateMethods) {
            if (candidateMethod.getReturnType().equals(
                    targetMethod.getReturnType())
                    && candidateMethod.getName().equals(targetMethod.getName())
                    && ObjectUtils.equals(candidateMethod.getDefaultValue(),
                            targetMethod.getDefaultValue())) {
                return; // Found a match
            }
        }
        fail("No " + RooJpaActiveRecord.class.getSimpleName()
                + " method has the signature \""
                + targetMethod.getReturnType().getSimpleName() + " "
                + targetMethod.getName() + "() default "
                + targetMethod.getDefaultValue() + "\"");
    }

    /*
     * Since annotations can't share code with each other (e.g. by inheritance),
     * we have manually redeclared all of the RooJpaEntity methods in the
     * RooEntity annotation. This test ensures that we haven't missed any out.
     */
    @Test
    public void testAllRooJpaEntityMethodsExistInRooEntity() {
        final List<Method> rooEntityMethods = Arrays
                .asList(RooJpaActiveRecord.class.getMethods());
        for (final Method jpaEntityMethod : RooJpaEntity.class.getMethods()) {
            assertMethodExists(jpaEntityMethod, rooEntityMethods);
        }
    }
}
