package org.springframework.roo.addon.creator;

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
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
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
	private static final char SEPARATOR = File.separatorChar;
	private static final String ICON_SET_URL = "http://www.famfamfam.com/lab/icons/flags/famfamfam_flag_icons.zip";
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	@Reference private UrlInputStreamService httpService;
	private String iconSetUrl;

	private enum Type {
		SIMPLE, ADVANCED, I18N, WRAPPER
	};
	
	protected void activate(ComponentContext context) {
		iconSetUrl = context.getBundleContext().getProperty("creator.i18n.iconset.url");
		if (!StringUtils.hasText(iconSetUrl)) {
			iconSetUrl = ICON_SET_URL;
		}
	}
	
	public boolean isCommandAvailable() {
		return !projectOperations.isProjectAvailable();
	}
	
	public void createAdvancedAddon(JavaPackage topLevelPackage, String description, String projectName) {
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
	
	public void createSimpleAddon(JavaPackage topLevelPackage, String description, String projectName) {
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
	
	public void createWrapperAddon(JavaPackage topLevelPackage, String groupId, String artifactId, String version, String vendorName, String lincenseUrl, String docUrl, String osgiImports, String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		if (!StringUtils.hasText(projectName)) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), "wrapper/roo-addon-wrapper-template.xml"));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
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
		
		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
	}

	public void createI18nAddon(JavaPackage topLevelPackage, String language, Locale locale, File messageBundle, File flagGraphic, String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		Assert.notNull(locale, "Locale required");
		Assert.notNull(messageBundle, "Message Bundle required");

		if (!StringUtils.hasText(language)) {
			language = "";
			InputStreamReader is = new InputStreamReader(TemplateUtils.getTemplate(getClass(), Type.I18N.name().toLowerCase() +  "/iso3166.txt"));
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
				try {
					br.close();
					is.close();
				} catch (Exception ignored) {}
			}		
		} 
		String[] langWords = language.split("\\s");
		StringBuffer b = new StringBuffer();
		for (String word: langWords) {
			b.append(StringUtils.capitalize(word.toLowerCase()));
		}
		String languageName = b.toString();
		String packagePath = topLevelPackage.getFullyQualifiedPackageName().replace('.', SEPARATOR);
		
		if (!StringUtils.hasText(description)) {
			description = languageName + " language support for Spring Roo Web MVC JSP Scaffolding";
		}
		if (!description.contains("#mvc") || !description.contains("#localization") || !description.contains("locale:")) {
			description = description + "; #mvc,#localization,locale:" + locale.getCountry().toLowerCase();
		}
		createProject(topLevelPackage, Type.I18N, description, projectName);

		install("assembly.xml", topLevelPackage, Path.ROOT, Type.I18N, projectName);
		
		try {
			FileCopyUtils.copy(new FileInputStream(messageBundle), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + SEPARATOR + messageBundle.getName())).getOutputStream());
			if (flagGraphic != null) {
				FileCopyUtils.copy(new FileInputStream(flagGraphic), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + SEPARATOR + flagGraphic.getName())).getOutputStream());
			} else {
				installFlagGraphic(locale, packagePath);
			} 
		} catch (IOException e) {
			throw new IllegalStateException("Could not copy addon resources into project", e);
		}
		
		String destinationFile = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, packagePath + SEPARATOR + languageName + "Language.java");
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), Type.I18N.name().toLowerCase() +  "/Language.java-template");
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

	private void createProject(JavaPackage topLevelPackage, Type type, String description, String projectName) {
		if (!StringUtils.hasText(projectName)) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/roo-addon-" + type.name().toLowerCase() + "-template.xml"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = pom.getDocumentElement();
		
		XmlUtils.findRequiredElement("/project/artifactId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", root).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/properties/repo.folder", root).setTextContent(topLevelPackage.getFullyQualifiedPackageName().replace(".", "/"));
		if (StringUtils.hasText(description)) {
			XmlUtils.findRequiredElement("/project/description", root).setTextContent(description);
		}

		// Create new project
		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);

		writeTextFile("readme.txt", "Welcome to my addon!");
		writeTextFile("legal" + SEPARATOR + "LICENSE.TXT", "Your license goes here");

		fileManager.scan();
	}

	private void install(String targetFilename, JavaPackage topLevelPackage, Path path, Type type, String projectName) {
		if (!StringUtils.hasText(projectName)) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		String topLevelPackageName = topLevelPackage.getFullyQualifiedPackageName();
		String packagePath = topLevelPackageName.replace('.', SEPARATOR);
		String destinationFile = "";
		if (targetFilename.endsWith(".java")) {
			destinationFile = pathResolver.getIdentifier(path, packagePath + SEPARATOR + StringUtils.capitalize(topLevelPackageName.substring(topLevelPackageName.lastIndexOf(".") + 1)) + targetFilename);
		} else {
			destinationFile = pathResolver.getIdentifier(path, packagePath + SEPARATOR + targetFilename);
		}
		
		// Different destination for assembly.xml
		if ("assembly.xml".equals(targetFilename)) {
			destinationFile = pathResolver.getIdentifier(path, "src" + SEPARATOR + "main" + SEPARATOR + "assembly" + SEPARATOR + targetFilename);
		} else if (targetFilename.startsWith("RooAnnotation")) { // Adjust name for Roo Annotation
			destinationFile = pathResolver.getIdentifier(path, packagePath + SEPARATOR + "Roo" + StringUtils.capitalize(topLevelPackageName.substring(topLevelPackageName.lastIndexOf(".") + 1)) + ".java");
		}
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/" + targetFilename + "-template");
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
	
	private void writeTextFile(String fullPathFromRoot, String message) {
		Assert.hasText(fullPathFromRoot, "Text file name to write is required");
		Assert.hasText(message, "Message required");
		String path = pathResolver.getIdentifier(Path.ROOT, fullPathFromRoot);
		MutableFile mutableFile = fileManager.exists(path) ? fileManager.updateFile(path) : fileManager.createFile(path);
		byte[] input = message.getBytes();
		try {
			FileCopyUtils.copy(input, mutableFile.getOutputStream());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
	
	private void installFlagGraphic(Locale locale, String packagePath) {
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
					MutableFile target = fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + "/" + countryCode + ".png"));
					BufferedOutputStream bos = new BufferedOutputStream(target.getOutputStream(), buffer.length);
					while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
						bos.write(buffer, 0, size);
					}
					bos.flush();
					bos.close();
					success = true;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(getErrorMsg(locale.getCountry()), e);
		} finally {
			try {
				if (zis != null) zis.close();
				if (bis != null) bis.close();
			} catch (Exception ignored) {}
		}
		
		if (!success) {
			throw new IllegalStateException(getErrorMsg(locale.toString()));
		}
	}

	private String getErrorMsg(String localeStr) {
		return "Could not acquire flag icon for locale " + localeStr + " please use --flagGraphic to specify the flag manually";
	}
}
