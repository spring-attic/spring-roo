package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Unit test of {@link CharAttributeValue}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CharAttributeValueTest {

    @Test
    public void testToString() {
        assertEquals("baz -> q", new CharAttributeValue(new JavaSymbolName(
                "baz"), 'q').toString());
    }
}
