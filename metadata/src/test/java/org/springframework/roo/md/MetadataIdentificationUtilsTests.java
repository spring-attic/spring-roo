package org.springframework.roo.md;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.roo.metadata.MetadataIdentificationUtils;

public class MetadataIdentificationUtilsTests {

	@Test
	public void testMetadataIdentifierParsing() {
		Assert.assertFalse(MetadataIdentificationUtils.isIdentifyingInstance("MID:com.foo.Bar"));
		Assert.assertTrue(MetadataIdentificationUtils.isIdentifyingInstance("MID:com.foo.Bar#"));
		Assert.assertTrue(MetadataIdentificationUtils.isIdentifyingInstance("MID:com.foo.Bar#239"));
		Assert.assertTrue(MetadataIdentificationUtils.isIdentifyingClass("MID:com.foo.Bar"));
		Assert.assertFalse(MetadataIdentificationUtils.isIdentifyingClass("MID:com.foo.Bar#"));
		Assert.assertFalse(MetadataIdentificationUtils.isIdentifyingClass("MID:com.foo.Bar#239"));
		Assert.assertNull(MetadataIdentificationUtils.getMetadataClass("MID:"));
		Assert.assertNull(MetadataIdentificationUtils.getMetadataClass("MID:#"));
		Assert.assertEquals("com.foo.Bar", MetadataIdentificationUtils.getMetadataClass("MID:com.foo.Bar"));
		Assert.assertEquals("com.foo.Bar", MetadataIdentificationUtils.getMetadataClass("MID:com.foo.Bar#"));
		Assert.assertEquals("com.foo.Bar", MetadataIdentificationUtils.getMetadataClass("MID:com.foo.Bar#239"));
		Assert.assertEquals("239", MetadataIdentificationUtils.getMetadataInstance("MID:com.foo.Bar#239"));
		Assert.assertEquals("239", MetadataIdentificationUtils.getMetadataInstance("MID:#239"));
		Assert.assertEquals("239 #40", MetadataIdentificationUtils.getMetadataInstance("MID:#239 #40"));
		Assert.assertNull(MetadataIdentificationUtils.getMetadataInstance("MID:com.foo.Bar#"));
		Assert.assertNull(MetadataIdentificationUtils.getMetadataInstance("MID:com.foo.Bar 239"));
	}
	
	@Test
	public void testMetadataIdentifierCreation() {
		Assert.assertEquals("MID:com.foo.Bar", MetadataIdentificationUtils.create("com.foo.Bar"));
		Assert.assertNull(MetadataIdentificationUtils.create(null));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#"));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#foo"));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar # "));
		Assert.assertNull(MetadataIdentificationUtils.create(""));
		Assert.assertNull(MetadataIdentificationUtils.create("#"));
		Assert.assertEquals("MID:com.foo.Bar#239", MetadataIdentificationUtils.create("com.foo.Bar", "239"));
		Assert.assertEquals("MID:com.foo.Bar#239 #40", MetadataIdentificationUtils.create("com.foo.Bar", "239 #40"));
		Assert.assertNull(MetadataIdentificationUtils.create(null, "239"));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#", "239"));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar#foo", "239"));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar # ", "239"));
		Assert.assertNull(MetadataIdentificationUtils.create("", "239"));
		Assert.assertNull(MetadataIdentificationUtils.create("#", "239"));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar", null));
		Assert.assertNull(MetadataIdentificationUtils.create("com.foo.Bar", ""));
	}
}
