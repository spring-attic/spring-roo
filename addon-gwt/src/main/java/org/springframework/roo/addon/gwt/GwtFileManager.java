package org.springframework.roo.addon.gwt;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;

import java.util.List;

/**
 * Provides a basic implementation of {@link GwtFileManager} which encapsulates
 * the file management functionality required by {@link GwtMetadataProviderImpl}
 *
 * @author James Tyrrell
 * @since 1.1.1
 */
public interface GwtFileManager {

	void write(String destFile, String newContents);

	void write(ClassOrInterfaceTypeDetails typeDetails);

	void write(ClassOrInterfaceTypeDetails typeDetails, boolean includeWarning);

	void write(List<ClassOrInterfaceTypeDetails> typeDetails, boolean includeWarning);
}
