package org.springframework.roo.shell.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.shell.Completion;

/**
 * Unit test of {@link EnumConverter}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class EnumConverterTest {

    /**
     * A simple test enum (enums can't be mocked).
     * 
     * @author Andrew Swan
     * @since 1.2.0
     */
    private enum Flavour {
        BANANA, CHERRY, RASPBERRY;
    }

    // Fixture
    private EnumConverter enumConverter;

    @Before
    public void setUp() {
        enumConverter = new EnumConverter();
    }

    @Test
    public void testConvertFromText() {
        // Invoke
        final Enum<?> result = enumConverter.convertFromText(
                Flavour.BANANA.name(), Flavour.class, "anything");

        // Check
        assertEquals(Flavour.BANANA, result);
    }

    @Test
    public void testGetAllPossibleValuesForPartialName() {
        // Set up
        final List<Completion> completions = new ArrayList<Completion>();

        // Invoke
        final boolean result = enumConverter.getAllPossibleValues(completions,
                Flavour.class, "b", "anything", null);

        // Check
        assertTrue(result);
        assertEquals(1, completions.size());
        assertEquals(Flavour.BANANA.name(), completions.get(0).getValue());
    }

    @Test
    public void testSupports() {
        assertTrue(enumConverter.supports(Flavour.class, "anything"));
    }
}
