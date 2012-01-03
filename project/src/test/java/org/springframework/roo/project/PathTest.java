package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

/**
 * Unit test of the {@link Path} enum.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PathTest {

    @Test
    public void testDefaultLocationsAreUnique() {
        // Set up
        final Collection<String> distinctLocations = new HashSet<String>();

        // Invoke
        for (final Path path : Path.values()) {
            distinctLocations.add(path.getDefaultLocation());
        }

        // Check
        assertEquals(Path.values().length, distinctLocations.size());
    }
}
