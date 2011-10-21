package org.springframework.roo.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.project.maven.PomBuilder;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Service
public class PomManagementServiceImpl implements PomManagementService {

	@Reference private NotifiableFileMonitorService fileMonitorService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	@Reference private Shell shell;

	private static final String DEFAULT_POM_NAME = "pom.xml";
	private static final String DEFAULT_RELATIVE_PATH = "../pom.xml";

	private String focusedModulePath;
	private Map<String, Pom> pomMap = new HashMap<String, Pom>();
	private String rootPath;
	private final Set<String> toBeParsed = new HashSet<String>();

	public Pom getModuleForFileIdentifier(final String fileIdentifier) {
		updatePomCache();
		String startingPoint = FileUtils.getFirstDirectory(fileIdentifier);
		String pomPath = FileUtils.normalise(startingPoint) + "pom.xml";
		File pom = new File(pomPath);
		while (!pom.exists()) {
			if (startingPoint.equals(File.separator)) {
				break;
			}
			startingPoint = FileUtils.backOneDirectory(startingPoint);
			pomPath = FileUtils.normalise(startingPoint) + "pom.xml";
			pom = new File(pomPath);
		}
		return getPomFromPath(pomPath);
	}

	public Pom getPomFromPath(final String pomPath) {
		updatePomCache();
		return pomMap.get(pomPath);
	}

	public Pom getPomFromModuleName(final String moduleName) {
		updatePomCache();
		for (final Map.Entry<String, Pom> entry : pomMap.entrySet()) {
			if (moduleName.equals(entry.getValue().getModuleName())) {
				return pomMap.get(entry.getKey());
			}
		}
		return null;
	}

	private String getModuleName(final String pomRoot) {
		final String moduleName = FileUtils.normalise(pomRoot).replaceAll(FileUtils.normalise(rootPath), "");
		return FileUtils.removeTrailingSeparator(moduleName);
	}

	public Set<String> getModuleNames() {
		final Set<String> moduleNames = new HashSet<String>();
		for (final Pom module : pomMap.values()) {
		 	moduleNames.add(module.getModuleName());
		}
		return moduleNames;
	}

	public Map<String, Pom> getPomMap() {
		updatePomCache();
		return pomMap;
	}

	public Pom getRootPom() {
		updatePomCache();
		return pomMap.get(rootPath + File.separator + DEFAULT_POM_NAME);
	}

	public Pom getFocusedModule() {
		updatePomCache();
		if (focusedModulePath == null && getRootPom() != null) {
			focusedModulePath = getRootPom().getPath();
		}
		return getPomFromPath(focusedModulePath);
	}

	public String getFocusedModuleName() {
		if (getFocusedModule() == null) {
			return "";
		}
		return getFocusedModule().getModuleName();
	}

	public void setFocusedModule(final Pom focusedModule) {
		setFocusedModule(focusedModule.getPath());
	}

	public void setFocusedModule(final String focusedModulePath) {
		Assert.hasText(focusedModulePath, "Module path required");
		if (focusedModulePath.equals(this.focusedModulePath)) {
			return;
		}
		this.focusedModulePath = focusedModulePath;
		shell.setPromptPath(pomMap.get(focusedModulePath).getModuleName());
	}

	protected void activate(final ComponentContext context) {
		final String workingDir = OSGiUtils.getRooWorkingDirectory(context);
		final File root = MonitoringRequest.getInitialMonitoringRequest(workingDir).getFile();
		rootPath = FileDetails.getCanonicalPath(root);
	}

	private void updatePomCache() {
		final Set<String> changes = fileMonitorService.getDirtyFiles(getClass().getName());
		for (final String change : changes) {
			if (change.endsWith("pom.xml")) {
				toBeParsed.add(change);
			}
		}
		final Map<String, String> pomModuleMap = new HashMap<String, String>();
		final Set<String> toRemove = new HashSet<String>();
		final Set<Pom> newPoms = new HashSet<Pom>();
		for (final String change : toBeParsed) {
			try {
				if (new File(change).exists()) {
					final String fileContents = FileCopyUtils.copyToString(new File(change));
					if (StringUtils.hasText(fileContents)) {
						final Document pomDocument = XmlUtils.readXml(fileManager.getInputStream(change));
						resolvePoms(pomDocument.getDocumentElement(), change, pomModuleMap);
						final String moduleName = getModuleName(FileUtils.getFirstDirectory(change));
						final PomBuilder pomBuilder = new PomBuilder(pomDocument.getDocumentElement(), change, moduleName);
						final Pom pom = pomBuilder.build();
						pomMap.put(change, pom);
						newPoms.add(pom);
						toRemove.add(change);
					}
				}
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}
		toBeParsed.removeAll(toRemove);
		if (!newPoms.isEmpty()) {
			sortPomMap();
			for (final Pom pom : newPoms) {
				metadataService.get(ProjectMetadata.getProjectIdentifier(pom.getModuleName()), true);
				metadataDependencyRegistry.notifyDownstream(ProjectMetadata.getProjectIdentifier(pom.getModuleName()));
			}
		}
	}

	private void resolvePoms(final Element pomRoot, final String pomPath, final Map<String, String> pomSet) {
		pomSet.put(pomPath, pomSet.get(pomPath));

		final Element parentElement = XmlUtils.findFirstElement("/project/parent", pomRoot);

		if (parentElement != null) {
			final String relativePath = XmlUtils.getTextContent("/relativePath", parentElement, DEFAULT_RELATIVE_PATH);
			final String parentPomPath = resolveRelativePath(pomPath, relativePath);
			final boolean alreadyDiscovered = pomSet.containsKey(parentPomPath);
			pomSet.put(parentPomPath, pomSet.get(parentPomPath));
			if (!alreadyDiscovered) {
				final Document pomDocument = XmlUtils.readXml(fileManager.getInputStream(parentPomPath));
				final Element root = pomDocument.getDocumentElement();
				resolvePoms(root, parentPomPath, pomSet);
			}
		}

		for (final Element module : XmlUtils.findElements("/project/modules/module", pomRoot)) {
			final String moduleName = module.getTextContent();
			if (StringUtils.hasText(moduleName)) {
				final String modulePath = resolveRelativePath(pomPath, moduleName);
				final boolean alreadyDiscovered = pomSet.containsKey(modulePath);
				pomSet.put(modulePath, moduleName);
				if (!alreadyDiscovered) {
					final Document pomDocument = XmlUtils.readXml(fileManager.getInputStream(modulePath));
					final Element root = pomDocument.getDocumentElement();
					resolvePoms(root, modulePath, pomSet);
				}
			}
		}
	}

	private String resolveRelativePath(String relativeTo, final String relativePath) {
		if (relativeTo.endsWith(File.separator)) {
			relativeTo = relativeTo.substring(0, relativeTo.length() - 1);
		}
		while (new File(relativeTo).isFile()) {
			relativeTo = relativeTo.substring(0, relativeTo.lastIndexOf(File.separator));
		}
		final String[] relativePathSegments = relativePath.split(File.separator);

		int backCount = 0;
		for (final String relativePathSegment : relativePathSegments) {
			if (relativePathSegment.equals("..")) {
				backCount++;
			} else {
				break;
			}
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = backCount; i < relativePathSegments.length; i++) {
			sb.append(relativePathSegments[i]);
			sb.append("/");
		}

		while (backCount > 0) {
			relativeTo = relativeTo.substring(0, relativeTo.lastIndexOf(File.separatorChar));
			backCount--;
		}
		String path = relativeTo + File.separator + sb.toString();
		if (new File(path).isDirectory()) {
			path = path + "pom.xml";
		}
		if (path.endsWith(File.separator)) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private static class PomComparator implements Comparator<String> {

		// Fields
		private final Map<String, Pom> pomMap;

		/**
		 * Constructor
		 *
		 * @param pomMap
		 */
		private PomComparator(final Map<String, Pom> pomMap) {
			this.pomMap = pomMap;
		}

		public int compare(final String s1, final String s2) {
			final String p1 = pomMap.get(s1).getRoot() + File.separator;
			final String p2 = pomMap.get(s2).getRoot() + File.separator;
			if (p1.startsWith(p2)) {
				return -1;
			} else if (p2.startsWith(p1)) {
				return 1;
			}
			return 0;
		}
	}

	private void sortPomMap() {
		final List<String> sortedPomPaths = new ArrayList<String>(pomMap.keySet());
		Collections.sort(sortedPomPaths, new PomComparator(pomMap));
		final Map<String, Pom> sortedPomMap = new LinkedHashMap<String, Pom>();
		for (final String pomPath : sortedPomPaths) {
			sortedPomMap.put(pomPath, pomMap.get(pomPath));
		}
		pomMap = sortedPomMap;
	}
}
