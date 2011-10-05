package org.springframework.roo.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.metadata.MetadataIdentificationUtils.INSTANCE_DELIMITER;
import static org.springframework.roo.metadata.MetadataIdentificationUtils.MID_PREFIX;

import org.junit.Test;

/**
 * Unit test of {@link MetadataIdentificationUtils}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MetadataIdentificationUtilsTest {
	
	// Constants
	private static final String METADATA_CLASS = MetadataItem.class.getName();	// normally this would be a concrete class
	private static final String INSTANCE_CLASS = Integer.class.getName();	// normally this would be a project type
	private static final String CLASS_MID = MID_PREFIX + METADATA_CLASS;
	private static final String INSTANCE_MID = MID_PREFIX + METADATA_CLASS + INSTANCE_DELIMITER + INSTANCE_CLASS;
	
	@Test
	public void testClassIdFromNullMetadataClassName() {
		assertNull(MetadataIdentificationUtils.create(null));
	}
	
	@Test
	public void testClassIdFromEmptyMetadataClassName() {
		assertNull(MetadataIdentificationUtils.create(""));
	}
	
	@Test
	public void testClassIdFromBadlyFormedMetadataClassName() {
		assertNull(MetadataIdentificationUtils.create("foo#bar"));
	}
	
	@Test
	public void testClassIdFromWellFormedMetadataClassName() {
		// Normally this would be a class that implements MetadataItem
		assertEquals(MID_PREFIX + METADATA_CLASS, MetadataIdentificationUtils.create(METADATA_CLASS));
	}
	
	@Test
	public void testInstanceIdFromValidInputs() {
		assertEquals(MID_PREFIX + METADATA_CLASS + INSTANCE_DELIMITER + INSTANCE_CLASS, MetadataIdentificationUtils.create(METADATA_CLASS, INSTANCE_CLASS));
	}
	
	@Test
	public void testInstanceIdFromNullInstanceKey() {
		assertNull(MetadataIdentificationUtils.create(METADATA_CLASS, null));
	}
	
	@Test
	public void testGetMetadataClassFromNullMid() {
		assertNull(MetadataIdentificationUtils.getMetadataClass(null));
	}
	
	@Test
	public void testGetMetadataClassFromEmptyMid() {
		assertNull(MetadataIdentificationUtils.getMetadataClass(""));
	}
	
	@Test
	public void testGetMetadataClassFromMidPrefix() {
		assertNull(MetadataIdentificationUtils.getMetadataClass(MID_PREFIX));
	}
	
	@Test
	public void testGetMetadataClassFromMidPrefixPlusDelimiter() {
		assertNull(MetadataIdentificationUtils.getMetadataClass(MID_PREFIX + INSTANCE_DELIMITER));
	}
	
	@Test
	public void testGetMetadataClassFromClassMid() {
		assertEquals(METADATA_CLASS, MetadataIdentificationUtils.getMetadataClass(CLASS_MID));
	}
	
	@Test
	public void testGetMetadataClassFromInstanceMid() {
		assertEquals(METADATA_CLASS, MetadataIdentificationUtils.getMetadataClass(INSTANCE_MID));
	}
	
	@Test
	public void testGetInstanceKey() {
		assertEquals(INSTANCE_CLASS, MetadataIdentificationUtils.getMetadataInstance(INSTANCE_MID));
	}
	
	@Test
	public void testClassMidIsClassMid() {
		assertTrue(MetadataIdentificationUtils.isIdentifyingClass(CLASS_MID));
	}
	
	@Test
	public void testClassMidIsNotInstanceMid() {
		assertFalse(MetadataIdentificationUtils.isIdentifyingInstance(CLASS_MID));
	}
	
	@Test
	public void testInstanceMidIsInstanceMid() {
		assertTrue(MetadataIdentificationUtils.isIdentifyingInstance(INSTANCE_MID));
	}
	
	@Test
	public void testInstanceMidIsNotClassMid() {
		assertFalse(MetadataIdentificationUtils.isIdentifyingClass(INSTANCE_MID));
	}
	
	@Test
	public void testNullMidIsNotValid() {
		assertFalse(MetadataIdentificationUtils.isValid(null));
	}
	
	@Test
	public void testEmptyMidIsNotValid() {
		assertFalse(MetadataIdentificationUtils.isValid(""));
	}
	
	@Test
	public void testBlankMidIsNotValid() {
		assertFalse(MetadataIdentificationUtils.isValid("\t\n\r"));
	}
	
	@Test
	public void testMidPrefixIsNotValid() {
		assertFalse(MetadataIdentificationUtils.isValid(MID_PREFIX));
	}
	
	@Test
	public void testUnprefixedMidIsNotValid() {
		assertFalse(MetadataIdentificationUtils.isValid(METADATA_CLASS));
	}
	
	@Test
	public void testClassMidIsValid() {
		assertTrue(MetadataIdentificationUtils.isValid(CLASS_MID));
	}
	
	@Test
	public void testInstanceMidIsValid() {
		assertTrue(MetadataIdentificationUtils.isValid(CLASS_MID));
	}
	
	@Test
	public void testGetMetadataInstanceFromNullMid() {
		assertNull(MetadataIdentificationUtils.getMetadataInstance(null));
	}
	
	@Test
	public void testGetMetadataInstanceFromEmptyMid() {
		assertNull(MetadataIdentificationUtils.getMetadataInstance(""));
	}
	
	@Test
	public void testGetMetadataInstanceFromMidPrefix() {
		assertNull(MetadataIdentificationUtils.getMetadataInstance(MID_PREFIX));
	}
	
	@Test
	public void testGetMetadataInstanceFromMidPrefixPlusDelimiter() {
		assertNull(MetadataIdentificationUtils.getMetadataInstance(MID_PREFIX + INSTANCE_DELIMITER));
	}
	
	@Test
	public void testGetMetadataInstanceFromClassMid() {
		assertNull(MetadataIdentificationUtils.getMetadataInstance(CLASS_MID));
	}
	
	@Test
	public void testGetMetadataInstanceFromInstanceMid() {
		assertEquals(INSTANCE_CLASS, MetadataIdentificationUtils.getMetadataInstance(INSTANCE_MID));
	}
}
