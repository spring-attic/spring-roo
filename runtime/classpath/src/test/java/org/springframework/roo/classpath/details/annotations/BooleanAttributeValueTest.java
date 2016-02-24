package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Unit test of {@link BooleanAttributeValue}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class BooleanAttributeValueTest {

    @Test
    public void testToStringWhenFalse() {
        assertEquals("foo -> false", new BooleanAttributeValue(
                new JavaSymbolName("foo"), false).toString());
    }

    @Test
    public void testToStringWhenTrue() {
        assertEquals("bar -> true", new BooleanAttributeValue(
                new JavaSymbolName("bar"), true).toString());
    }
}