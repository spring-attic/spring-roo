package org.springframework.roo.addon.gwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

/**
 * Unit test of the {@link GwtPath} enum.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GwtPathTest {

    @Test
    public void testPackageNameForWebPath() {
        assertEquals("", GwtPath.WEB.packageName(null));
    }

    @Test
    public void testSegmentNamesAreNonNull() {
        for (final GwtPath gwtPath : GwtPath.values()) {
            assertNotNull("Null segment name for " + gwtPath,
                    gwtPath.getSegmentName());
        }
    }

    @Test
    public void testSegmentNamesAreUnique() {
        final Collection<String> segmentNames = new HashSet<String>();
        for (final GwtPath gwtPath : GwtPath.values()) {
            final String segmentName = gwtPath.getSegmentName();
            assertTrue("Duplicate segment name '" + segmentName + "'",
                    segmentNames.add(segmentName));
        }
    }

    @Test
    public void testSegmentPackageForWebPath() {
        assertEquals("", GwtPath.WEB.segmentPackage());
    }
}
