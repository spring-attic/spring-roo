package org.springframework.roo.addon.gwt;

import java.util.List;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

/**
 * Provides a basic implementation of {@link GwtFileManager} which encapsulates
 * the file management functionality required by {@link GwtScaffoldMetadataProviderImpl}.
 *
 * @author James Tyrrell
 * @since 1.1.1
 */
public interface GwtFileManager {

	void write(String destFile, String newContents);

	String write(ClassOrInterfaceTypeDetails typeDetails, boolean includeWarning);

	void write(List<ClassOrInterfaceTypeDetails> typeDetails, boolean includeWarning);

	String write(ClassOrInterfaceTypeDetails typeDetails, String warning);

}
