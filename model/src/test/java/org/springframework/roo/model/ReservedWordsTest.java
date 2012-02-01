package org.springframework.roo.model;

import org.junit.Test;

/**
 * Unit test of {@link ReservedWords}
 * 
 * @author Alan Stewart
 * @since 1.2.1
 */
public class ReservedWordsTest {

    @Test
    public void testVerifyReservedJavaKeywordsNotPresent() {
        ReservedWords.verifyReservedJavaKeywordsNotPresent("test");
    }

    @Test
    public void testVerifyReservedJavaKeywordsNotPresent2() {
        ReservedWords.verifyReservedJavaKeywordsNotPresent(new JavaSymbolName(
                "test"));
    }

    @Test
    public void testVerifyReservedJavaKeywordsNotPresent3() {
        ReservedWords.verifyReservedJavaKeywordsNotPresent(new JavaType(
                "com.foo.bar.Foo"));
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyReservedJavaKeywordsPresent() {
        ReservedWords.verifyReservedJavaKeywordsNotPresent(new JavaSymbolName(
                "if"));
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyReservedJavaKeywordsPresent2() {
        ReservedWords.verifyReservedJavaKeywordsNotPresent(new JavaType(
                "com.return.bar"));
    }

    @Test
    public void testVerifyReservedSqlKeywordsNotPresent() {
        ReservedWords.verifyReservedSqlKeywordsNotPresent("ROW_VER_NO");
    }

    @Test
    public void testVerifyReservedSqlKeywordsNotPresent2() {
        ReservedWords.verifyReservedSqlKeywordsNotPresent(new JavaSymbolName(
                "ROW_VER_NO"));
    }

    @Test
    public void testVerifyReservedSqlKeywordsNotPresent3() {
        ReservedWords.verifyReservedSqlKeywordsNotPresent(new JavaType(
                "com.bar.Foo"));
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyReservedSqlKeywordsPresent() {
        ReservedWords.verifyReservedSqlKeywordsNotPresent(new JavaSymbolName(
                "alter"));
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyReservedSqlKeywordsPresent2() {
        ReservedWords.verifyReservedSqlKeywordsNotPresent(new JavaType(
                "com.bar.Outer"));
    }
}
