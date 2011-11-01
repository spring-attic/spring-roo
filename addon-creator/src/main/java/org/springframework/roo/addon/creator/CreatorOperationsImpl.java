package org.springframework.roo.addon.creator;

import static java.io.File.separatorChar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.StringUtils;
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

	// Constants
	private static final String ICON_SET_URL = "http://www.famfamfam.com/lab/icons/flags/famfamfam_flag_icons.zip";
	private static final String POM_XML = "pom.xml";

	// Fields
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	@Reference private UrlInputStreamService httpService;

	private String iconSetUrl;

	protected void activate(final ComponentContext context) {
		iconSetUrl = context.getBundleContext().getProperty("creator.i18n.iconset.url");
		if (!StringUtils.hasText(iconSetUrl)) {
			iconSetUrl = ICON_SET_URL;
		}
	}

	public boolean isCommandAvailable() {
		return !projectOperations.isFocusedProjectAvailable();
	}

	public void createAdvancedAddon(final JavaPackage topLevelPackage, final String description, final String projectName) {
		Assert.notNull(topLevelPackage, "Top-level package required");

		createProject(topLevelPackage, Type.ADVANCED, description, projectName);

		install("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		install("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		install("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		install("Metadata.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		install("MetadataProvider.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		install("RooAnnotation.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		install("assembly.xml", topLevelPackage, Path.ROOT, Type.ADVANCED, projectName);
		install("configuration.xml", topLevelPackage, Path.SRC_MAIN_RESOURCES, Type.ADVANCED, projectName);
	}

	public void createSimpleAddon(final JavaPackage topLevelPackage, final String description, final String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");

		createProject(topLevelPackage, Type.SIMPLE, description, projectName);

		install("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		install("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		install("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		install("PropertyName.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		install("assembly.xml", topLevelPackage, Path.ROOT, Type.SIMPLE, projectName);
		install("info.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES, Type.SIMPLE, projectName);
		install("show.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES, Type.SIMPLE, projectName);
	}

	public void createWrapperAddon(final JavaPackage topLevelPackage, final String groupId, final String artifactId, final String version, final String vendorName, final String lincenseUrl, final String docUrl, final String osgiImports, final String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		if (!StringUtils.hasText(projectName)) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		Document pom = XmlUtils.readXml(FileUtils.getInputStream(getClass(), "wrapper/roo-addon-wrapper-template.xml"));
		Element root = pom.getDocumentElement();

		XmlUtils.findRequiredElement("/project/name", root).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/dependencies/dependency/groupId", root).setTextContent(groupId);
		XmlUtils.findRequiredElement("/project/dependencies/dependency/artifactId", root).setTextContent(artifactId);
		XmlUtils.findRequiredElement("/project/dependencies/dependency/version", root).setTextContent(version);
		XmlUtils.findRequiredElement("/project/properties/pkgArtifactId", root).setTextContent(artifactId);
		XmlUtils.findRequiredElement("/project/properties/pkgVersion", root).setTextContent(version);
		XmlUtils.findRequiredElement("/project/properties/pkgVendor", root).setTextContent(vendorName);
		XmlUtils.findRequiredElement("/project/properties/pkgLicense", root).setTextContent(lincenseUrl);
		XmlUtils.findRequiredElement("/project/properties/repo.folder", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName().replace(".", "/"));
		if (docUrl != null && docUrl.length() > 0) {
			XmlUtils.findRequiredElement("/project/properties/pkgDocUrl", root).setTextContent(docUrl);
		}
		if (osgiImports != null && osgiImports.length() > 0) {
			Element config = XmlUtils.findRequiredElement("/project/build/plugins/plugin[artifactId = 'maven-bundle-plugin']/configuration/instructions", root);
			config.appendChild(new XmlElementBuilder("Import-Package", pom).setText(osgiImports).build());
		}
		if (description != null && description.length() > 0) {
			Element descriptionE = XmlUtils.findRequiredElement("/project/description", root);
			descriptionE.setTextContent(description + " " + descriptionE.getTextContent());
		}

		writePomFile(pom);
	}

	public void createI18nAddon(final JavaPackage topLevelPackage, String language, final Locale locale, final File messageBundle, final File flagGraphic, String description, final String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		Assert.notNull(locale, "Locale required");
		Assert.notNull(messageBundle, "Message Bundle required");

		if (!StringUtils.hasText(language)) {
			language = "";
			InputStreamReader is = new InputStreamReader(FileUtils.getInputStream(getClass(), Type.I18N.name().toLowerCase() +  "/iso3166.txt"));
			BufferedReader br = new BufferedReader(is);
			String line;
			try {
				while((line = br.readLine()) != null) {
					String[] split = line.split(";");
					if (split[1].startsWith(locale.getCountry().toUpperCase())) {
						if (split[0].contains(",")) {
							split[0] = split[0].substring(0, split[0].indexOf(",") - 1);
						}
						String[] langWords = split[0].split("\\s");
						StringBuffer b = new StringBuffer();
						for (String word: langWords) {
							b.append(StringUtils.capitalize(word.toLowerCase())).append(" ");
						}
						language = b.toString().substring(0, b.length() -1);
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException("Could not parse ISO 3166 language list, please use --language option in command");
			} finally {
				IOUtils.closeQuietly(br, is);
			}
		}

		String[] langWords = language.split("\\s");
		StringBuffer b = new StringBuffer();
		for (String word: langWords) {
			b.append(StringUtils.capitalize(word.toLowerCase()));
		}
		String languageName = b.toString();
		String packagePath = topLevelPackage.getFullyQualifiedPackageName().replace('.', separatorChar);

		if (!StringUtils.hasText(description)) {
			description = languageName + " language support for Spring Roo Web MVC JSP Scaffolding";
		}
		if (!description.contains("#mvc") || !description.contains("#localization") || !description.contains("locale:")) {
			description = description + "; #mvc,#localization,locale:" + locale.getCountry().toLowerCase();
		}
		createProject(topLevelPackage, Type.I18N, description, projectName);

		install("assembly.xml", topLevelPackage, Path.ROOT, Type.I18N, projectName);

		try {
			FileCopyUtils.copy(new FileInputStream(messageBundle), fileManager.createFile(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + separatorChar + messageBundle.getName())).getOutputStream());
			if (flagGraphic != null) {
				FileCopyUtils.copy(new FileInputStream(flagGraphic), fileManager.createFile(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + separatorChar + flagGraphic.getName())).getOutputStream());
			} else {
				installFlagGraphic(locale, packagePath);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not copy addon resources into project", e);
		}
		
		String destinationFile = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA, packagePath + separatorChar + languageName + "Language.java");

		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = FileUtils.getInputStream(getClass(), Type.I18N.name().toLowerCase() +  "/Language.java-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
				input = input.replace("__APP_NAME__", languageName);
				input = input.replace("__LOCALE__", locale.getLanguage());
				input = input.replace("__LANGUAGE__", StringUtils.capitalize(language));
				if (flagGraphic != null) {
					input = input.replace("__FLAG_FILE__", flagGraphic.getName());
				} else {
					input = input.replace("__FLAG_FILE__", locale.getCountry().toLowerCase() + ".png");
				}
				input = input.replace("__MESSAGE_BUNDLE__", messageBundle.getName());

				// Output the file for the user
				MutableFile mutableFile = fileManager.createFile(destinationFile);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to create '" + languageName + "Language.java'", ioe);
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
	 * @param topLevelPackage the top-level package of the project being created (required)
	 * @param type the type of project being created (required)
	 * @param description the description to put into the POM (can be blank)
	 * @param projectName if blank, a sanitised version of the given top-level
	 * package is used for the project name
	 */
	private void createProject(final JavaPackage topLevelPackage, final Type type, final String description, String projectName) {
		if (!StringUtils.hasText(projectName)) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}

		// Load the POM template
		final String pomTemplate = type.name().toLowerCase() + "/roo-addon-" + type.name().toLowerCase() + "-template.xml";
		final Document pom = XmlUtils.readXml(FileUtils.getInputStream(getClass(), pomTemplate));
		final Element root = pom.getDocumentElement();

		// Populate it from the given inputs
		XmlUtils.findRequiredElement("/project/artifactId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", root).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/properties/repo.folder", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName().replace(".", "/"));
		if (StringUtils.hasText(description)) {
			XmlUtils.findRequiredElement("/project/description", root).setTextContent(description);
		}

		// Write the new POM to disk
		writePomFile(pom);

		// Write the other root files
		writeTextFile("readme.txt", "Welcome to my addon!");
		writeTextFile("legal" + separatorChar + "LICENSE.TXT", "Your license goes here");

		fileManager.scan();
	}

	/**
	 * Writes the given Maven POM to disk
	 *
	 * @param pom the POM to write (required)
	 */
	private void writePomFile(final Document pom) {
		final MutableFile pomFile = fileManager.createFile(pathResolver.getFocusedIdentifier(Path.ROOT, POM_XML));
		XmlUtils.writeXml(pomFile.getOutputStream(), pom);
	}

	private void install(final String targetFilename, final JavaPackage topLevelPackage, final Path path, final Type type, String projectName) {
		if (!StringUtils.hasText(projectName)) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		String topLevelPackageName = topLevelPackage.getFullyQualifiedPackageName();
		String packagePath = topLevelPackageName.replace('.', separatorChar);
		String destinationFile = "";

		if (targetFilename.endsWith(".java")) {
			destinationFile = pathResolver.getFocusedIdentifier(path, packagePath + separatorChar + StringUtils.capitalize(topLevelPackageName.substring(topLevelPackageName.lastIndexOf(".") + 1)) + targetFilename);
		} else {
			destinationFile = pathResolver.getFocusedIdentifier(path, packagePath + separatorChar + targetFilename);
		}

		// Different destination for assembly.xml
		if ("assembly.xml".equals(targetFilename)) {
			destinationFile = pathResolver.getFocusedIdentifier(path, "src" + separatorChar + "main" + separatorChar + "assembly" + separatorChar + targetFilename);
		} else if (targetFilename.startsWith("RooAnnotation")) { // Adjust name for Roo Annotation
			destinationFile = pathResolver.getFocusedIdentifier(path, packagePath + separatorChar + "Roo" + StringUtils.capitalize(topLevelPackageName.substring(topLevelPackageName.lastIndexOf(".") + 1)) + ".java");
		}

		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = FileUtils.getInputStream(getClass(), type.name().toLowerCase() + "/" + targetFilename + "-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
				input = input.replace("__APP_NAME__", StringUtils.capitalize(topLevelPackageName.substring(topLevelPackageName.lastIndexOf(".") + 1)));
				input = input.replace("__APP_NAME_LWR_CASE__", topLevelPackageName.substring(topLevelPackageName.lastIndexOf(".") + 1).toLowerCase());
				input = input.replace("__PROJECT_NAME__", projectName.toLowerCase());

				// Output the file for the user
				MutableFile mutableFile = fileManager.createFile(destinationFile);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
			}
		}
	}

	private void writeTextFile(final String fullPathFromRoot, final String message) {
		Assert.hasText(fullPathFromRoot, "Text file name to write is required");
		Assert.hasText(message, "Message required");
		String path = pathResolver.getFocusedIdentifier(Path.ROOT, fullPathFromRoot);
		MutableFile mutableFile = fileManager.exists(path) ? fileManager.updateFile(path) : fileManager.createFile(path);
		byte[] input = message.getBytes();
		try {
			FileCopyUtils.copy(input, mutableFile.getOutputStream());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	private void installFlagGraphic(final Locale locale, final String packagePath) {
		boolean success = false;
		String countryCode = locale.getCountry().toLowerCase();

		// Retrieve the icon file:
		BufferedInputStream bis = null;
		ZipInputStream zis = null;
		try {
			bis = new BufferedInputStream(httpService.openConnection(new URL(iconSetUrl)));
			zis = new ZipInputStream(bis);
			ZipEntry entry;
			String expectedEntryName = "png/" + countryCode + ".png";
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals(expectedEntryName)) {
					int size;
					byte[] buffer = new byte[2048];
					MutableFile target = fileManager.createFile(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/" + countryCode + ".png"));
					BufferedOutputStream bos = null;
					try {
						bos = new BufferedOutputStream(target.getOutputStream(), buffer.length);
						while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
							bos.write(buffer, 0, size);
						}
						success = true;
					} finally {
						IOUtils.closeQuietly(bos);
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(getErrorMsg(locale.getCountry()), e);
		} finally {
			IOUtils.closeQuietly(zis, bis);
		}

		if (!success) {
			throw new IllegalStateException(getErrorMsg(locale.toString()));
		}
	}

	private String getErrorMsg(final String localeStr) {
		return "Could not acquire flag icon for locale " + localeStr + " please use --flagGraphic to specify the flag manually";
	}
}
