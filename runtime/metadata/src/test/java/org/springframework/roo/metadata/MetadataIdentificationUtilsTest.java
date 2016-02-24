package org.springframework.roo.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.metadata.MetadataIdentificationUtils.INSTANCE_DELIMITER;
import static org.springframework.roo.metadata.MetadataIdentificationUtils.MID_PREFIX;
import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit test of {@link MetadataIdentificationUtils}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MetadataIdentificationUtilsTest {

    private static final String INSTANCE_CLASS = Integer.class.getName(); // normally
    private static final String METADATA_CLASS = MetadataItem.class.getName();
    private static final String CLASS_MID = MID_PREFIX + METADATA_CLASS;
    private static final String INSTANCE_MID = MID_PREFIX + METADATA_CLASS
            + INSTANCE_DELIMITER + INSTANCE_CLASS;

    @Test
    public void testBlankMidIsNotValid() {
        assertFalse(MetadataIdentificationUtils.isValid("\t\n\r"));
    }

    @Test
    public void testClassIdFromBadlyFormedMetadataClassName() {
        assertNull(MetadataIdentificationUtils.create("foo#bar"));
    }

    @Test
    public void testClassIdFromEmptyMetadataClassName() {
        assertNull(MetadataIdentificationUtils.create(""));
    }

    @Test
    public void testClassIdFromNonNullMetadataClass() {
        assertEquals(CLASS_MID,
                MetadataIdentificationUtils.create(MetadataItem.class));
    }

    @Test
    public void testClassIdFromNullMetadataClass() {
        assertNull(MetadataIdentificationUtils.create((Class<?>) null));
    }

    @Test
    public void testClassIdFromNullMetadataClassName() {
        assertNull(MetadataIdentificationUtils.create((String) null));
    }

    @Test
    public void testClassIdFromWellFormedMetadataClassName() {
        // Normally this would be a class that implements MetadataItem
        assertEquals(MID_PREFIX + METADATA_CLASS,
                MetadataIdentificationUtils.create(METADATA_CLASS));
    }

    @Test
    public void testClassMidIsClassMid() {
        assertTrue(MetadataIdentificationUtils.isIdentifyingClass(CLASS_MID));
    }

    @Test
    public void testClassMidIsNotInstanceMid() {
        assertFalse(MetadataIdentificationUtils
                .isIdentifyingInstance(CLASS_MID));
    }

    @Test
    public void testClassMidIsValid() {
        assertTrue(MetadataIdentificationUtils.isValid(CLASS_MID));
    }

    @Test
    public void testEmptyMidIsNotValid() {
        assertFalse(MetadataIdentificationUtils.isValid(""));
    }

    @Test
    public void testGetInstanceKey() {
        assertEquals(INSTANCE_CLASS,
                MetadataIdentificationUtils.getMetadataInstance(INSTANCE_MID));
    }

    @Test
    public void testGetMetadataClassFromClassMid() {
        assertEquals(METADATA_CLASS,
                MetadataIdentificationUtils.getMetadataClass(CLASS_MID));
    }

    @Test
    public void testGetMetadataClassFromEmptyMid() {
        assertNull(MetadataIdentificationUtils.getMetadataClass(""));
    }

    @Test
    public void testGetMetadataClassFromInstanceMid() {
        assertEquals(METADATA_CLASS,
                MetadataIdentificationUtils.getMetadataClass(INSTANCE_MID));
    }

    @Test
    public void testGetMetadataClassFromMidPrefix() {
        assertNull(MetadataIdentificationUtils.getMetadataClass(MID_PREFIX));
    }

    @Test
    public void testGetMetadataClassFromMidPrefixPlusDelimiter() {
        assertNull(MetadataIdentificationUtils.getMetadataClass(MID_PREFIX
                + INSTANCE_DELIMITER));
    }

    @Test
    public void testGetMetadataClassFromNullMid() {
        assertNull(MetadataIdentificationUtils.getMetadataClass(null));
    }

    @Test
    public void testGetMetadataClassIdFromClassMid() {
        assertEquals(MID_PREFIX + METADATA_CLASS,
                MetadataIdentificationUtils.getMetadataClassId(CLASS_MID));
    }

    @Test
    public void testGetMetadataClassIdFromEmptyMid() {
        assertNull(MetadataIdentificationUtils.getMetadataClassId(""));
    }

    @Test
    public void testGetMetadataClassIdFromInstanceMid() {
        assertEquals(MID_PREFIX + METADATA_CLASS,
                MetadataIdentificationUtils.getMetadataClassId(INSTANCE_MID));
    }

    @Test
    public void testGetMetadataClassIdFromMidPrefix() {
        assertNull(MetadataIdentificationUtils.getMetadataClassId(MID_PREFIX));
    }

    @Test
    public void testGetMetadataClassIdFromMidPrefixPlusDelimiter() {
        assertNull(MetadataIdentificationUtils.getMetadataClassId(MID_PREFIX
                + INSTANCE_DELIMITER));
    }

    @Test
    public void testGetMetadataClassIdFromNullMid() {
        assertNull(MetadataIdentificationUtils.getMetadataClassId(null));
    }

    @Test
    public void testGetMetadataInstanceFromClassMid() {
        assertNull(MetadataIdentificationUtils.getMetadataInstance(CLASS_MID));
    }

    @Test
    public void testGetMetadataInstanceFromEmptyMid() {
        assertNull(MetadataIdentificationUtils.getMetadataInstance(""));
    }

    @Test
    public void testGetMetadataInstanceFromInstanceMid() {
        assertEquals(INSTANCE_CLASS,
                MetadataIdentificationUtils.getMetadataInstance(INSTANCE_MID));
    }

    @Test
    public void testGetMetadataInstanceFromMidPrefix() {
        assertNull(MetadataIdentificationUtils.getMetadataInstance(MID_PREFIX));
    }

    @Test
    public void testGetMetadataInstanceFromMidPrefixPlusDelimiter() {
        assertNull(MetadataIdentificationUtils.getMetadataInstance(MID_PREFIX
                + INSTANCE_DELIMITER));
    }

    @Test
    public void testGetMetadataInstanceFromNullMid() {
        assertNull(MetadataIdentificationUtils.getMetadataInstance(null));
    }

    @Test
    public void testInstanceIdFromNullInstanceKey() {
        assertNull(MetadataIdentificationUtils.create(METADATA_CLASS, null));
    }

    @Test
    public void testInstanceIdFromValidInputs() {
        assertEquals(MID_PREFIX + METADATA_CLASS + INSTANCE_DELIMITER
                + INSTANCE_CLASS, MetadataIdentificationUtils.create(
                METADATA_CLASS, INSTANCE_CLASS));
    }

    @Test
    public void testInstanceMidIsInstanceMid() {
        assertTrue(MetadataIdentificationUtils
                .isIdentifyingInstance(INSTANCE_MID));
    }

    @Test
    public void testInstanceMidIsNotClassMid() {
        assertFalse(MetadataIdentificationUtils
                .isIdentifyingClass(INSTANCE_MID));
    }

    @Test
    public void testInstanceMidIsValid() {
        assertTrue(MetadataIdentificationUtils.isValid(CLASS_MID));
    }

    @Test
    public void testMetadataIdentifierCreation() {
        Assert.assertEquals("MID:com.foo.Bar",
                MetadataIdentificationUtils.create("com.foo.Bar"));
        Assert.assertNull(MetadataIdentificationUtils.create((String) null));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#"));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#foo"));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar # "));
        Assert.assertNull(MetadataIdentificationUtils.create(""));
        Assert.assertNull(MetadataIdentificationUtils.create("#"));
        Assert.assertEquals("MID:com.foo.Bar#239",
                MetadataIdentificationUtils.create("com.foo.Bar", "239"));
        Assert.assertEquals("MID:com.foo.Bar#239 #40",
                MetadataIdentificationUtils.create("com.foo.Bar", "239 #40"));
        Assert.assertNull(MetadataIdentificationUtils.create(null, "239"));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#",
                "239"));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#foo",
                "239"));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar # ",
                "239"));
        Assert.assertNull(MetadataIdentificationUtils.create("", "239"));
        Assert.assertNull(MetadataIdentificationUtils.create("#", "239"));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar",
                null));
        Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar", ""));
    }

    @Test
    public void testMetadataIdentifierParsing() {
        Assert.assertFalse(MetadataIdentificationUtils
                .isIdentifyingInstance("MID:com.foo.Bar"));
        Assert.assertFalse(MetadataIdentificationUtils
                .isIdentifyingInstance("MID:com.foo.Bar#"));
        Assert.assertTrue(MetadataIdentificationUtils
                .isIdentifyingInstance("MID:com.foo.Bar#239"));
        Assert.assertTrue(MetadataIdentificationUtils
                .isIdentifyingClass("MID:com.foo.Bar"));
        Assert.assertFalse(MetadataIdentificationUtils
                .isIdentifyingClass("MID:com.foo.Bar#"));
        Assert.assertFalse(MetadataIdentificationUtils
                .isIdentifyingClass("MID:com.foo.Bar#239"));
        Assert.assertNull(MetadataIdentificationUtils.getMetadataClass("MID:"));
        Assert.assertNull(MetadataIdentificationUtils.getMetadataClass("MID:#"));
        Assert.assertEquals("com.foo.Bar",
                MetadataIdentificationUtils.getMetadataClass("MID:com.foo.Bar"));
        Assert.assertEquals("com.foo.Bar", MetadataIdentificationUtils
                .getMetadataClass("MID:com.foo.Bar#"));
        Assert.assertEquals("com.foo.Bar", MetadataIdentificationUtils
                .getMetadataClass("MID:com.foo.Bar#239"));
        Assert.assertEquals("239", MetadataIdentificationUtils
                .getMetadataInstance("MID:com.foo.Bar#239"));
        Assert.assertEquals("239",
                MetadataIdentificationUtils.getMetadataInstance("MID:#239"));
        Assert.assertEquals("239 #40",
                MetadataIdentificationUtils.getMetadataInstance("MID:#239 #40"));
        Assert.assertNull(MetadataIdentificationUtils
                .getMetadataInstance("MID:com.foo.Bar#"));
        Assert.assertNull(MetadataIdentificationUtils
                .getMetadataInstance("MID:com.foo.Bar 239"));
    }

    @Test
    public void testMidPrefixIsNotValid() {
        assertFalse(MetadataIdentificationUtils.isValid(MID_PREFIX));
    }

    @Test
    public void testNullMidIsNotValid() {
        assertFalse(MetadataIdentificationUtils.isValid(null));
    }

    @Test
    public void testUnprefixedMidIsNotValid() {
        assertFalse(MetadataIdentificationUtils.isValid(METADATA_CLASS));
    }
}
