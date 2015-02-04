package org.springframework.roo.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test of {@link JavaSymbolName}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JavaSymbolNameTest {

    @Test
    public void testAssertJavaNameLegal() {
        final String symbolName = "META.INF.web.resources.dojo.1.5.util.shrinksafe.src.org.dojotoolkit.shrinksafe.Compressor";
        final JavaSymbolName symbol = new JavaSymbolName(symbolName);
        assertEquals(symbolName, symbol.getSymbolName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailAssertJavaNameLegal() {
        new JavaSymbolName(
                "META-INF.web-resources.dojo-1.5.util.shrinksafe.src.org.dojotoolkit.shrinksafe.Compressor");
    }
}
