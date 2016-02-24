package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Unit test of {@link LongAttributeValue}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class LongAttributeValueTest {

    @Test
    public void testToString() {
        assertEquals("beast -> 666", new LongAttributeValue(new JavaSymbolName(
                "beast"), 666).toString());
    }
}