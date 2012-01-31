package org.springframework.roo.addon.creator;

import java.io.File;
import java.util.Locale;

import org.springframework.roo.model.JavaPackage;

/**
 * Provides add-on creation operations.
 * 
 * @author Stefan Schmidt
 */
public interface CreatorOperations {

    void createAdvancedAddon(JavaPackage topLevelPackage, String description,
            String projectName);

    void createI18nAddon(JavaPackage topLevelPackage, String language,
            Locale locale, File messageBundle, File flagGraphic,
            String description, String projectName);

    void createSimpleAddon(JavaPackage topLevelPackage, String description,
            String projectName);

    void createWrapperAddon(JavaPackage topLevelPackage, String groupId,
            String artifactId, String version, String vendorName,
            String lincenseUrl, String docUrl, String osgiImports,
            String description, String projectName);

    boolean isAddonCreatePossible();
}