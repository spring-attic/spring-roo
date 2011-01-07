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
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
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
 * Provides addon generation operations.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class CreatorOperationsImpl implements CreatorOperations {
	
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private PathResolver pathResolver;
	@Reference private UrlInputStreamService httpService;
	
	private char separator = File.separatorChar;
	
	public boolean isCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) == null;
	}
	
	private enum Type {SIMPLE, ADVANCED, I18N, WRAPPER};
	
	public void createAdvancedAddon(JavaPackage topLevelPackage, String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		
		createProject(topLevelPackage, Type.ADVANCED, description, projectName);
		
		installIfNeeded("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		installIfNeeded("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		installIfNeeded("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		installIfNeeded("Metadata.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		installIfNeeded("MetadataProvider.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		installIfNeeded("RooAnnotation.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.ADVANCED, projectName);
		installIfNeeded("assembly.xml", topLevelPackage, Path.ROOT, Type.ADVANCED, projectName);
		installIfNeeded("configuration.xml", topLevelPackage, Path.SRC_MAIN_RESOURCES, Type.ADVANCED, projectName);
	}
	
	public void createSimpleAddon(JavaPackage topLevelPackage, String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		
		createProject(topLevelPackage, Type.SIMPLE, description, projectName);
		
		installIfNeeded("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		installIfNeeded("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		installIfNeeded("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		installIfNeeded("PropertyName.java", topLevelPackage, Path.SRC_MAIN_JAVA, Type.SIMPLE, projectName);
		installIfNeeded("assembly.xml", topLevelPackage, Path.ROOT, Type.SIMPLE, projectName);
		installIfNeeded("info.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES, Type.SIMPLE, projectName);
		installIfNeeded("show.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES, Type.SIMPLE, projectName);
	}
	
	public void createWrapperAddon(JavaPackage topLevelPackage, String groupId, String artifactId, String version, String vendorName, String lincenseUrl, String docUrl, String osgiImports, String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		if (projectName == null || projectName.length() == 0) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), "wrapper/roo-addon-wrapper-template.xml"));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		Element rootElement = pom.getDocumentElement();
		XmlUtils.findRequiredElement("/project/name", rootElement).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/groupId", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/dependencies/dependency/groupId", rootElement).setTextContent(groupId);
		XmlUtils.findRequiredElement("/project/dependencies/dependency/artifactId", rootElement).setTextContent(artifactId);
		XmlUtils.findRequiredElement("/project/dependencies/dependency/version", rootElement).setTextContent(version);
		XmlUtils.findRequiredElement("/project/properties/pkgArtifactId", rootElement).setTextContent(artifactId);
		XmlUtils.findRequiredElement("/project/properties/pkgVersion", rootElement).setTextContent(version);
		XmlUtils.findRequiredElement("/project/properties/pkgVendor", rootElement).setTextContent(vendorName);
		XmlUtils.findRequiredElement("/project/properties/pkgLicense", rootElement).setTextContent(lincenseUrl);
		XmlUtils.findRequiredElement("/project/properties/repo.folder", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName().replace(".", "/"));
		if (docUrl != null && docUrl.length() > 0) {
			XmlUtils.findRequiredElement("/project/properties/pkgDocUrl", rootElement).setTextContent(docUrl);
		}
		if (osgiImports != null && osgiImports.length() > 0) {
			Element config = XmlUtils.findRequiredElement("/project/build/plugins/plugin[artifactId = 'maven-bundle-plugin']/configuration/instructions", rootElement);
			config.appendChild(new XmlElementBuilder("Import-Package", pom).setText(osgiImports).build());
		}
		if (description != null && description.length() > 0) {
			Element descriptionE = XmlUtils.findRequiredElement("/project/description", rootElement);
			descriptionE.setTextContent(description + " " + descriptionE.getTextContent());
		}
		
		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
	}

	public void createI18nAddon(JavaPackage topLevelPackage, String language, Locale locale, File messageBundle, File flagGraphic, String description, String projectName) {
		Assert.notNull(topLevelPackage, "Top Level Package required");
		Assert.notNull(locale, "Locale required");
		Assert.notNull(messageBundle, "Message Bundle required");

		if (language == null || language.length() == 0) {
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
		
		String packagePath = topLevelPackage.getFullyQualifiedPackageName().replace('.', separator);
		
		if (description == null || description.length() == 0) {
			description = languageName + " language support for Spring Roo Web MVC JSP Scaffolding";
		}
		if (!description.contains("#mvc") || !description.contains("#localization") || !description.contains("locale:")) {
			description = description + "; #mvc,#localization,locale:" + locale.getCountry().toLowerCase();
		}
		createProject(topLevelPackage, Type.I18N, description, projectName);

		installIfNeeded("assembly.xml", topLevelPackage, Path.ROOT, Type.I18N, projectName);
		
		try {
			FileCopyUtils.copy(new FileInputStream(messageBundle), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + separator + messageBundle.getName())).getOutputStream());
			if (flagGraphic != null) {
				FileCopyUtils.copy(new FileInputStream(flagGraphic), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, packagePath + separator + flagGraphic.getName())).getOutputStream());
			} else {
				installFlagGraphic(locale, packagePath);
			} 
		} catch (IOException e) {
			throw new IllegalStateException("Could not copy addon resources into project", e);
		}
		
		String destinationFile = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, packagePath + separator + languageName + "Language.java");
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), Type.I18N.name().toLowerCase() +  "/Language.java-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
				input = input.replace("__APP_NAME__", languageName);
//				input = input.replace("__LOCALE__", locale.toString());
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
		if (projectName == null || projectName.length() == 0) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/roo-addon-" + type.name().toLowerCase() + "-template.xml"));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = pom.getDocumentElement();
		XmlUtils.findRequiredElement("/project/artifactId", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/groupId", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName());
		XmlUtils.findRequiredElement("/project/name", rootElement).setTextContent(projectName);
		XmlUtils.findRequiredElement("/project/properties/repo.folder", rootElement).setTextContent(topLevelPackage.getFullyQualifiedPackageName().replace(".", "/"));
		if (description != null && description.length() != 0) {
			XmlUtils.findRequiredElement("/project/description", rootElement).setTextContent(description);
		}

		MutableFile pomMutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.ROOT, "pom.xml"));
		XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");

		writeTextFile("readme.txt", "welcome to my addon!", projectMetadata);
		writeTextFile("legal" + separator + "LICENSE.TXT", "Your license goes here", projectMetadata);

		fileManager.scan();
	}
	
	private void installIfNeeded(String targetFilename, JavaPackage topLevelPackage, Path path, Type type, String projectName) {
		if (projectName == null || projectName.length() == 0) {
			projectName = topLevelPackage.getFullyQualifiedPackageName().replace(".", "-");
		}
		String tlp = topLevelPackage.getFullyQualifiedPackageName();
		String packagePath = tlp.replace('.', separator);
		String destinationFile = "";
		if (targetFilename.endsWith(".java")) {
			destinationFile = pathResolver.getIdentifier(path, packagePath + separator + StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)) + targetFilename);
		} else {
			destinationFile = pathResolver.getIdentifier(path, packagePath + separator + targetFilename);
		}
		
		// Different destination for assembly.xml
		if ("assembly.xml".equals(targetFilename)) {
			destinationFile = pathResolver.getIdentifier(path, "src" + separator + "main" + separator + "assembly" + separator + targetFilename);
		} else if (targetFilename.startsWith("RooAnnotation")) { // Adjust name for Roo Annotation
			destinationFile = pathResolver.getIdentifier(path, packagePath + separator + "Roo" + StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)) + ".java");
		}
		
		if (!fileManager.exists(destinationFile)) {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), type.name().toLowerCase() + "/" + targetFilename + "-template");
			try {
				// Read template and insert the user's package
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("__TOP_LEVEL_PACKAGE__", topLevelPackage.getFullyQualifiedPackageName());
				input = input.replace("__APP_NAME__", StringUtils.capitalize(tlp.substring(tlp.lastIndexOf(".") + 1)));
				input = input.replace("__APP_NAME_LWR_CASE__", tlp.substring(tlp.lastIndexOf(".") + 1).toLowerCase());
				input = input.replace("__PROJECT_NAME__", projectName.toLowerCase());
				
				// Output the file for the user
				MutableFile mutableFile = fileManager.createFile(destinationFile);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Unable to create '" + targetFilename + "'", ioe);
			}
		}
	}
	
	private void writeTextFile(String fullPathFromRoot, String message, ProjectMetadata projectMetadata) {
		Assert.hasText(fullPathFromRoot, "Text file name to write is required");
		Assert.hasText(message, "Message required");
		Assert.notNull(projectMetadata, "Project metadata required");
		String path = projectMetadata.getPathResolver().getIdentifier(Path.ROOT, fullPathFromRoot);
		File file = new File(path);
		MutableFile mutableFile;
		if (file.exists()) {
			mutableFile = fileManager.updateFile(path);
		} else {
			mutableFile = fileManager.createFile(path);
		}
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

		//retrieve the icon file:
		BufferedInputStream bis = null;
		ZipInputStream zis = null;
		try {
			bis = new BufferedInputStream(httpService.openConnection(new URL("http://www.famfamfam.com/lab/icons/flags/famfamfam_flag_icons.zip")));
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
			throw new IllegalStateException("Could not acquire flag icon for locale " + locale.getCountry() + " please use --flagGraphic to specify the flag manually", e);
		} finally {
			try {
				zis.close();
				bis.close();
			} catch (Exception ignore) {}
		}
		if (!success) {
			throw new IllegalStateException("Could not acquire flag icon for locale " + locale + " please use --flagGraphic to specify the flag manually");
		}
	}
}
