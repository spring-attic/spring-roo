package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Unit test of {@link IntegerAttributeValue}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class IntegerAttributeValueTest {

    @Test
    public void testToString() {
        assertEquals("answer -> 42", new IntegerAttributeValue(
                new JavaSymbolName("answer"), 42).toString());
    }
}