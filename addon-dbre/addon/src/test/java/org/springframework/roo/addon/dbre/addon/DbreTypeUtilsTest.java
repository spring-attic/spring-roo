package org.springframework.roo.addon.dbre.addon;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.addon.dbre.addon.DbreTypeUtils;

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
}
