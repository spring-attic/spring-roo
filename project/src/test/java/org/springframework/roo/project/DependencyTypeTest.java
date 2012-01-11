package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test of the {@link DependencyType} enum.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DependencyTypeTest {

    @Test
    public void testValueOfEmptyCode() {
        assertEquals(DependencyType.JAR, DependencyType.valueOfTypeCode(""));
    }

    @Test
    public void testValueOfKnownCodes() {
        for (final DependencyType dependencyType : DependencyType.values()) {
            assertEquals(dependencyType,
                    DependencyType.valueOfTypeCode(dependencyType.name()
                            .toLowerCase()));
        }
    }

    @Test
    public void testValueOfNullCode() {
        assertEquals(DependencyType.JAR, DependencyType.valueOfTypeCode(null));
    }

    @Test
    public void testValueOfUnknownCode() {
        assertEquals(DependencyType.OTHER,
                DependencyType.valueOfTypeCode("guff"));
    }
}
