package org.springframework.roo.addon.dbre;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * JUnit tests for {@link DbreTypeUtils}.
 * 
 * @author Alan Stewart
 * @since 1.2.0.
 */
public class DbreTypeUtilsTest {

	@Test
	public void testSuggestPackageName() {
		String tableName = "table2";
		assertEquals("table2", DbreTypeUtils.suggestPackageName(tableName));

		tableName = "1Roo-2424";
		assertEquals("p1roo2424", DbreTypeUtils.suggestPackageName(tableName));

		tableName = "/-roo_2425_p";
		assertEquals("roo_2425_p", DbreTypeUtils.suggestPackageName(tableName));
	}

	@Test
    public void testSuggestedEntityName(){
    	 String tableName = "t_user";
    	 Set<String> prefixes = new HashSet<String>();
    	 prefixes.add("t_");
    	 JavaType t = new JavaType("test.User");
         assertEquals(t.getFullyQualifiedTypeName(), 
        		 DbreTypeUtils.suggestTypeNameForNewTable(tableName, new JavaPackage("test"), prefixes).getFullyQualifiedTypeName());

    }
}
