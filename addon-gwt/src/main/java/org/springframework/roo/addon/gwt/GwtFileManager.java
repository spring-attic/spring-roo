package org.springframework.roo.addon.gwt;

import java.util.List;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Provides a basic implementation of {@link GwtFileManager} which encapsulates
 * the file management functionality required by
 * {@link org.springframework.roo.addon.gwt.scaffold.GwtScaffoldMetadataProviderImpl}
 * .
 * 
 * @author James Tyrrell
 * @since 1.1.1
 */
public interface GwtFileManager {

    String write(ClassOrInterfaceTypeDetails typeDetails, boolean includeWarning);

    /**
     * Writes the given Java type to disk in the user project
     * 
     * @param typeDetails the type to write (required)
     * @param warning any warning to appear at the top of the source file
     *            (cannot be <code>null</code>; include a trailing newline if
     *            not empty)
     * @return the contents of the type (minus the warning)
     */
    String write(ClassOrInterfaceTypeDetails typeDetails, String warning);

    void write(List<ClassOrInterfaceTypeDetails> typeDetails,
            boolean includeWarning);

    void write(String destFile, String newContents);
}
