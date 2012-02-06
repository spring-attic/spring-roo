package org.springframework.roo.addon.creator;

import static java.io.File.separatorChar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link CreatorOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class CreatorOperationsImpl implements CreatorOperations {

    /**
     * The types of project that can be created
     */
    private enum Type {

        /**
         * A simple addon
         */
        SIMPLE,

        /**
         * An advanced addon
         */
        ADVANCED,

        /**
         * A language bundle
         */
        I18N,

        /**
         * An OSGi wrapper for a non-OSGi library
         */
        WRAPPER
    };

    private static final String ICON_SET_URL = "http://www.famfamfam.com/lab/icons/flags/famfamfam_flag_icons.zip";
    private static final String POM_XML = "pom.xml";

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private UrlInputStreamService httpService;

    private String iconSetUrl;

    protected void activate(final ComponentContext context) {
        iconSetUrl = context.getBundleContext().getProperty(
                "creator.i18n.iconset.url");
        if (StringUtils.isBlank(iconSetUrl)) {
            iconSetUrl = ICON_SET_URL;
        }
    }

    public void createAdvancedAddon(final JavaPackage topLevelPackage,
            final String description, final String projectName) {
        Validate.notNull(topLevelPackage, "Top-level package required");

        createProject(topLevelPackage, Type.ADVANCED, description, projectName);

        install("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName);
        install("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName);
        install("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName);
        install("Metadata.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName);
        install("MetadataProvider.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName);
        install("RooAnnotation.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName);
        install("assembly.xml", topLevelPackage, Path.ROOT, Type.ADVANCED,
                projectName);
        install("configuration.xml", topLevelPackage, Path.SRC_MAIN_RESOURCES,
                Type.ADVANCED, projectName);
    }

    public void createI18nAddon(final JavaPackage topLevelPackage,
            String language, final Locale locale, final File messageBundle,
            final File flagGraphic, String description, final String projectName) {
        Validate.notNull(topLevelPackage, "Top Level Package required");
        Validate.notNull(locale, "Locale required");
        Validate.notNull(messageBundle, "Message Bundle required");

        if (StringUtils.isBlank(language)) {
            language = "";
            final InputStream inputStream = FileUtils
                    .getInputStream(getClass(), Type.I18N.name().toLowerCase()
                            + "/iso3166.txt");
            try {
                for (String line : IOUtils.readLines(inputStream)) {
                    final String[] split = line.split(";");
                    if (split[1].startsWith(locale.getCountry().toUpperCase())) {
                        if (split[0].contains(",")) {
                            split[0] = split[0].substring(0,
                                    split[0].indexOf(",") - 1);
                        }
                        final String[] langWords = split[0].split("\\s");
                        final StringBuilder b = new StringBuilder();
                        for (final String word : langWords) {
                            b.append(StringUtils.capitalize(word.toLowerCase()))
                                    .append(" ");
                        }
                        language = b.toString().substring(0, b.length() - 1);
                    }
                }
            }
            catch (final IOException e) {
                throw new IllegalStateException(
                        "Could not parse ISO 3166 language list, please use --language option in command");
            }
            finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        final String[] langWords = language.split("\\s");
        final StringBuilder builder = new StringBuilder();
        for (final String word : langWords) {
            builder.append(StringUtils.capitalize(word.toLowerCase()));
        }
        final String languageName = builder.toString();
        final String packagePath = topLevelPackage
                .getFullyQualifiedPackageName().replace('.', separatorChar);

        if (StringUtils.isBlank(description)) {
            description = languageName
                    + " language support for Spring Roo Web MVC JSP Scaffolding";
        }
        if (!description.contains("#mvc")
                || !description.contains("#localization")
                || !description.contains("locale:")) {
            description = description + "; #mvc,#localization,locale:"
                    + locale.getCountry().toLowerCase();
        }
        createProject(topLevelPackage, Type.I18N, description, projectName);

        install("assembly.xml", topLevelPackage, Path.ROOT, Type.I18N,
                projectName);

        OutputStream outputStream = null;
        try {
            outputStream = fileManager.createFile(
                    pathResolver.getFocusedIdentifier(
                            Path.SRC_MAIN_RESOURCES,
                            packagePath + separatorChar
                                    + messageBundle.getName()))
                    .getOutputStream();
            org.apache.commons.io.FileUtils.copyFile(messageBundle,
                    outputStream);
            if (flagGraphic != null) {
                outputStream = fileManager
                        .createFile(
                                pathResolver.getFocusedIdentifier(
                                        Path.SRC_MAIN_RESOURCES,
                                        packagePath + separatorChar
                                                + flagGraphic.getName()))
                        .getOutputStream();
                org.apache.commons.io.FileUtils.copyFile(flagGraphic,
                        outputStream);
            }
            else {
                installFlagGraphic(locale, packagePath);
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Could not copy addon resources into project", e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }

        final String destinationFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_JAVA, packagePath + separatorChar + languageName
                        + "Language.java");

        if (!fileManager.exists(destinationFile)) {
            final InputStream templateInputStream = FileUtils.getInputStream(
                    getClass(), Type.I18N.name().toLowerCase()
                            + "/Language.java-template");
            try {
                // Read template and insert the user's package
                String input = IOUtils.toString(templateInputStream);
                input = input.replace("__TOP_LEVEL_PACKAGE__",
                        topLevelPackage.getFullyQualifiedPackageName());
                input = input.replace("__APP_NAME__", languageName);
                input = input.replace("__LOCALE__", locale.getLanguage());
                input = input.replace("__LANGUAGE__",
                        StringUtils.capitalize(language));
                if (flagGraphic != null) {
                    input = input.replace("__FLAG_FILE__",
                            flagGraphic.getName());
                }
                else {
                    input = input.replace("__FLAG_FILE__", locale.getCountry()
                            .toLowerCase() + ".png");
                }
                input = input.replace("__MESSAGE_BUNDLE__",
                        messageBundle.getName());

                // Output the file for the user
                final MutableFile mutableFile = fileManager
                        .createFile(destinationFile);
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(input, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException("Unable to create '"
                        + languageName + "Language.java'", ioe);
            }
            finally {
                IOUtils.closeQuietly(templateInputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    /**
     * Creates the root files for a new project, namely the:
     * <ul>
     * <li>Maven POM</li>
     * <li>readme.txt</li>
     * <li>
     * 
     * @param topLevelPackage the top-level package of the project being created
     *            (required)
     * @param type the type of project being created (required)
     * @param description the description to put into the POM (can be blank)
     * @param projectName if blank, a sanitised version of the given top-level
     *            package is used for the project name
     */
    private void createProject(final JavaPackage topLevelPackage,
            final Type type, final String description, String projectName) {
        if (StringUtils.isBlank(projectName)) {
            projectName = topLevelPackage.getFullyQualifiedPackageName()
                    .replace(".", "-");
        }

        // Load the POM template
        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), type.name().toLowerCase() + "/roo-addon-"
                        + type.name().toLowerCase() + "-template.xml");
        final Document pom = XmlUtils.readXml(templateInputStream);
        final Element root = pom.getDocumentElement();

        // Populate it from the given inputs
        XmlUtils.findRequiredElement("/project/artifactId", root)
                .setTextContent(topLevelPackage.getFullyQualifiedPackageName());
        XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(
                topLevelPackage.getFullyQualifiedPackageName());
        XmlUtils.findRequiredElement("/project/name", root).setTextContent(
                projectName);
        XmlUtils.findRequiredElement("/project/properties/repo.folder", root)
                .setTextContent(
                        topLevelPackage.getFullyQualifiedPackageName().replace(
                                ".", "/"));
        if (StringUtils.isNotBlank(description)) {
            XmlUtils.findRequiredElement("/project/description", root)
                    .setTextContent(description);
        }

        // Write the new POM to disk
        writePomFile(pom);

        // Write the other root files
        writeTextFile("readme.txt", "Welcome to my addon!");
        writeTextFile("legal" + separatorChar + "LICENSE.TXT",
                "Your license goes here");

        fileManager.scan();
    }

    public void createSimpleAddon(final JavaPackage topLevelPackage,
            final String description, final String projectName) {
        Validate.notNull(topLevelPackage, "Top Level Package required");

        createProject(topLevelPackage, Type.SIMPLE, description, projectName);

        install("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName);
        install("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName);
        install("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName);
        install("PropertyName.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName);
        install("assembly.xml", topLevelPackage, Path.ROOT, Type.SIMPLE,
                projectName);
        install("info.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES,
                Type.SIMPLE, projectName);
        install("show.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES,
                Type.SIMPLE, projectName);
    }

    public void createWrapperAddon(final JavaPackage topLevelPackage,
            final String groupId, final String artifactId,
            final String version, final String vendorName,
            final String lincenseUrl, final String docUrl,
            final String osgiImports, final String description,
            String projectName) {
        Validate.notNull(topLevelPackage, "Top Level Package required");
        if (StringUtils.isBlank(projectName)) {
            projectName = topLevelPackage.getFullyQualifiedPackageName()
                    .replace(".", "-");
        }
        final String wrapperGroupId = topLevelPackage
                .getFullyQualifiedPackageName();

        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), "wrapper/roo-addon-wrapper-template.xml");
        final Document pom = XmlUtils.readXml(templateInputStream);
        final Element root = pom.getDocumentElement();

        XmlUtils.findRequiredElement("/project/name", root).setTextContent(
                projectName);
        XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(
                wrapperGroupId);
        XmlUtils.findRequiredElement("/project/artifactId", root)
                .setTextContent(wrapperGroupId + "." + artifactId);
        XmlUtils.findRequiredElement("/project/version", root).setTextContent(
                version + ".0001");
        XmlUtils.findRequiredElement(
                "/project/dependencies/dependency/groupId", root)
                .setTextContent(groupId);
        XmlUtils.findRequiredElement(
                "/project/dependencies/dependency/artifactId", root)
                .setTextContent(artifactId);
        XmlUtils.findRequiredElement(
                "/project/dependencies/dependency/version", root)
                .setTextContent(version);
        XmlUtils.findRequiredElement("/project/properties/pkgArtifactId", root)
                .setTextContent(artifactId);
        XmlUtils.findRequiredElement("/project/properties/pkgVersion", root)
                .setTextContent(version);
        XmlUtils.findRequiredElement("/project/properties/pkgVendor", root)
                .setTextContent(vendorName);
        XmlUtils.findRequiredElement("/project/properties/pkgLicense", root)
                .setTextContent(lincenseUrl);
        XmlUtils.findRequiredElement("/project/properties/repo.folder", root)
                .setTextContent(
                        topLevelPackage.getFullyQualifiedPackageName().replace(
                                ".", "/"));
        if (docUrl != null && docUrl.length() > 0) {
            XmlUtils.findRequiredElement("/project/properties/pkgDocUrl", root)
                    .setTextContent(docUrl);
        }
        if (osgiImports != null && osgiImports.length() > 0) {
            final Element config = XmlUtils
                    .findRequiredElement(
                            "/project/build/plugins/plugin[artifactId = 'maven-bundle-plugin']/configuration/instructions",
                            root);
            config.appendChild(new XmlElementBuilder("Import-Package", pom)
                    .setText(osgiImports).build());
        }
        if (description != null && description.length() > 0) {
            final Element descriptionE = XmlUtils.findRequiredElement(
                    "/project/description", root);
            descriptionE.setTextContent(description + " "
                    + descriptionE.getTextContent());
        }

        writePomFile(pom);
    }

    private String getErrorMsg(final String localeStr) {
        return "Could not acquire flag icon for locale " + localeStr
                + " please use --flagGraphic to specify the flag manually";
    }

    private void install(final String targetFilename,
            final JavaPackage topLevelPackage, final Path path,
            final Type type, String projectName) {
        if (StringUtils.isBlank(projectName)) {
            projectName = topLevelPackage.getFullyQualifiedPackageName()
                    .replace(".", "-");
        }
        final String topLevelPackageName = topLevelPackage
                .getFullyQualifiedPackageName();
        final String packagePath = topLevelPackageName.replace('.',
                separatorChar);
        String destinationFile = "";

        if (targetFilename.endsWith(".java")) {
            destinationFile = pathResolver.getFocusedIdentifier(
                    path,
                    packagePath
                            + separatorChar
                            + StringUtils.capitalize(topLevelPackageName
                                    .substring(topLevelPackageName
                                            .lastIndexOf(".") + 1))
                            + targetFilename);
        }
        else {
            destinationFile = pathResolver.getFocusedIdentifier(path,
                    packagePath + separatorChar + targetFilename);
        }

        // Different destination for assembly.xml
        if ("assembly.xml".equals(targetFilename)) {
            destinationFile = pathResolver.getFocusedIdentifier(path, "src"
                    + separatorChar + "main" + separatorChar + "assembly"
                    + separatorChar + targetFilename);
        }
        // Adjust name for Roo Annotation
        else if (targetFilename.startsWith("RooAnnotation")) {
            destinationFile = pathResolver.getFocusedIdentifier(
                    path,
                    packagePath
                            + separatorChar
                            + "Roo"
                            + StringUtils.capitalize(topLevelPackageName
                                    .substring(topLevelPackageName
                                            .lastIndexOf(".") + 1)) + ".java");
        }

        if (!fileManager.exists(destinationFile)) {
            final InputStream templateInputStream = FileUtils.getInputStream(
                    getClass(), type.name().toLowerCase() + "/"
                            + targetFilename + "-template");
            OutputStream outputStream = null;
            try {
                // Read template and insert the user's package
                String input = IOUtils.toString(templateInputStream);
                input = input.replace("__TOP_LEVEL_PACKAGE__",
                        topLevelPackage.getFullyQualifiedPackageName());
                input = input
                        .replace("__APP_NAME__", StringUtils
                                .capitalize(topLevelPackageName
                                        .substring(topLevelPackageName
                                                .lastIndexOf(".") + 1)));
                input = input.replace(
                        "__APP_NAME_LWR_CASE__",
                        topLevelPackageName.substring(
                                topLevelPackageName.lastIndexOf(".") + 1)
                                .toLowerCase());
                input = input.replace("__PROJECT_NAME__",
                        projectName.toLowerCase());

                // Output the file for the user
                final MutableFile mutableFile = fileManager
                        .createFile(destinationFile);
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(input, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException("Unable to create '"
                        + targetFilename + "'", ioe);
            }
            finally {
                IOUtils.closeQuietly(templateInputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    private void installFlagGraphic(final Locale locale,
            final String packagePath) {
        boolean success = false;
        final String countryCode = locale.getCountry().toLowerCase();

        // Retrieve the icon file:
        BufferedInputStream bis = null;
        ZipInputStream zis = null;
        try {
            bis = new BufferedInputStream(httpService.openConnection(new URL(
                    iconSetUrl)));
            zis = new ZipInputStream(bis);
            ZipEntry entry;
            final String expectedEntryName = "png/" + countryCode + ".png";
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(expectedEntryName)) {
                    final MutableFile target = fileManager
                            .createFile(pathResolver.getFocusedIdentifier(
                                    Path.SRC_MAIN_RESOURCES, packagePath + "/"
                                            + countryCode + ".png"));
                    OutputStream outputStream = null;
                    try {
                        outputStream = target.getOutputStream();
                        IOUtils.copy(zis, outputStream);
                        success = true;
                    }
                    finally {
                        IOUtils.closeQuietly(outputStream);
                    }
                }
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(getErrorMsg(locale.getCountry()), e);
        }
        finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(zis);
        }

        if (!success) {
            throw new IllegalStateException(getErrorMsg(locale.toString()));
        }
    }

    public boolean isAddonCreatePossible() {
        return !projectOperations.isFocusedProjectAvailable();
    }

    /**
     * Writes the given Maven POM to disk
     * 
     * @param pom the POM to write (required)
     */
    private void writePomFile(final Document pom) {
        final LogicalPath rootPath = LogicalPath.getInstance(Path.ROOT, "");
        final MutableFile pomFile = fileManager.createFile(pathResolver
                .getIdentifier(rootPath, POM_XML));
        XmlUtils.writeXml(pomFile.getOutputStream(), pom);
    }

    private void writeTextFile(final String fullPathFromRoot,
            final String message) {
        Validate.notBlank(fullPathFromRoot,
                "Text file name to write is required");
        Validate.notBlank(message, "Message required");
        final String path = pathResolver.getFocusedIdentifier(Path.ROOT,
                fullPathFromRoot);
        final MutableFile mutableFile = fileManager.exists(path) ? fileManager
                .updateFile(path) : fileManager.createFile(path);
        OutputStream outputStream = null;
        try {
            outputStream = mutableFile.getOutputStream();
            IOUtils.write(message, outputStream);
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
