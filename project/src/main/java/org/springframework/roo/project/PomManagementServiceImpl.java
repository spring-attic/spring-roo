package org.springframework.roo.project;

import static org.springframework.roo.support.util.FileUtils.CURRENT_DIRECTORY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.project.maven.PomFactory;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Service
public class PomManagementServiceImpl implements PomManagementService {

    private static class PomComparator implements Comparator<String> {

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
            }
            else if (p2.startsWith(p1)) {
                return 1;
            }
            return 0;
        }
    }

    private static final String DEFAULT_POM_NAME = "pom.xml";

    private static final String DEFAULT_RELATIVE_PATH = ".." + File.separator
            + DEFAULT_POM_NAME;
    @Reference FileManager fileManager;
    @Reference FileMonitorService fileMonitorService;
    private String focusedModulePath;
    @Reference MetadataDependencyRegistry metadataDependencyRegistry;
    @Reference MetadataService metadataService;

    @Reference PomFactory pomFactory;
    private final Map<String, Pom> pomMap = new LinkedHashMap<String, Pom>();
    private String projectRootDirectory;
    @Reference Shell shell;

    // ------------------------ OSGi lifecycle callbacks -----------------------

    private final Set<String> toBeParsed = new HashSet<String>();

    // --------------------- PomManagementService methods ----------------------

    protected void activate(final ComponentContext context) {
        final File projectDirectory = new File(StringUtils.defaultIfEmpty(
                OSGiUtils.getRooWorkingDirectory(context), CURRENT_DIRECTORY));
        projectRootDirectory = FileUtils.getCanonicalPath(projectDirectory);
    }

    /**
     * For test cases to set up the state of this service
     * 
     * @param pom the POM to add (required)
     */
    void addPom(final Pom pom) {
        pomMap.put(pom.getPath(), pom);
    }

    private void findUnparsedPoms() {
        for (final String change : fileMonitorService.getDirtyFiles(getClass()
                .getName())) {
            if (change.endsWith(DEFAULT_POM_NAME)) {
                toBeParsed.add(change);
            }
        }
    }

    public Pom getFocusedModule() {
        updatePomCache();
        if ((focusedModulePath == null) && (getRootPom() != null)) {
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

    public Pom getModuleForFileIdentifier(final String fileIdentifier) {
        updatePomCache();
        String startingPoint = FileUtils.getFirstDirectory(fileIdentifier);
        String pomPath = FileUtils.ensureTrailingSeparator(startingPoint)
                + DEFAULT_POM_NAME;
        File pom = new File(pomPath);
        while (!pom.exists()) {
            if (startingPoint.equals(File.separator)) {
                break;
            }
            startingPoint = FileUtils.backOneDirectory(startingPoint);
            pomPath = FileUtils.ensureTrailingSeparator(startingPoint)
                    + DEFAULT_POM_NAME;
            pom = new File(pomPath);
        }
        return getPomFromPath(pomPath);
    }

    private String getModuleName(final String pomDirectory) {
        final String normalisedRootPath = FileUtils
                .ensureTrailingSeparator(projectRootDirectory);
        final String normalisedPomDirectory = FileUtils
                .ensureTrailingSeparator(pomDirectory);
        final String moduleName = StringUtils.removePrefix(
                normalisedPomDirectory, normalisedRootPath);
        return FileUtils.removeTrailingSeparator(moduleName);
    }

    public Collection<String> getModuleNames() {
        final Set<String> moduleNames = new HashSet<String>();
        for (final Pom module : pomMap.values()) {
            moduleNames.add(module.getModuleName());
        }
        return moduleNames;
    }

    public Pom getPomFromModuleName(final String moduleName) {
        for (final Pom pom : getPoms()) {
            if (pom.getModuleName().equals(moduleName)) {
                return pom;
            }
        }
        return null;
    }

    public Pom getPomFromPath(final String pomPath) {
        updatePomCache();
        return pomMap.get(pomPath);
    }

    public Collection<Pom> getPoms() {
        updatePomCache();
        return new ArrayList<Pom>(pomMap.values());
    }

    public Pom getRootPom() {
        updatePomCache();
        return pomMap.get(projectRootDirectory + File.separator
                + DEFAULT_POM_NAME);
    }

    private Set<Pom> parseUnparsedPoms() {
        final Map<String, String> pomModuleMap = new HashMap<String, String>();
        final Set<Pom> newPoms = new HashSet<Pom>();
        for (final Iterator<String> iter = toBeParsed.iterator(); iter
                .hasNext();) {
            final String pathToChangedPom = iter.next();
            if (new File(pathToChangedPom).exists()) {
                final String pomContents = FileUtils.read(new File(
                        pathToChangedPom));
                if (StringUtils.hasText(pomContents)) {
                    final Element rootElement = XmlUtils
                            .stringToElement(pomContents);
                    resolvePoms(rootElement, pathToChangedPom, pomModuleMap);
                    final String moduleName = getModuleName(FileUtils
                            .getFirstDirectory(pathToChangedPom));
                    final Pom pom = pomFactory.getInstance(rootElement,
                            pathToChangedPom, moduleName);
                    Assert.notNull(pom, "POM is null for module = '"
                            + moduleName + "' and path = '" + pathToChangedPom
                            + "'");
                    pomMap.put(pathToChangedPom, pom);
                    newPoms.add(pom);
                    iter.remove();
                }
            }
        }
        return newPoms;
    }

    private void resolveChildModulePoms(final Element pomRoot,
            final String pomPath, final Map<String, String> pomSet) {
        for (final Element module : XmlUtils.findElements(
                "/project/modules/module", pomRoot)) {
            final String moduleName = module.getTextContent();
            if (StringUtils.hasText(moduleName)) {
                final String modulePath = resolveRelativePath(pomPath,
                        moduleName);
                final boolean alreadyDiscovered = pomSet
                        .containsKey(modulePath);
                pomSet.put(modulePath, moduleName);
                if (!alreadyDiscovered) {
                    final Document pomDocument = XmlUtils.readXml(fileManager
                            .getInputStream(modulePath));
                    final Element root = pomDocument.getDocumentElement();
                    resolvePoms(root, modulePath, pomSet);
                }
            }
        }
    }

    private void resolveParentPom(final String pomPath,
            final Map<String, String> pomSet, final Element parentElement) {
        final String relativePath = XmlUtils.getTextContent("/relativePath",
                parentElement, DEFAULT_RELATIVE_PATH);
        final String parentPomPath = resolveRelativePath(pomPath, relativePath);
        final boolean alreadyDiscovered = pomSet.containsKey(parentPomPath);
        if (!alreadyDiscovered) {
            pomSet.put(parentPomPath, pomSet.get(parentPomPath));
            if (new File(parentPomPath).isFile()) {
                final Document pomDocument = XmlUtils.readXml(fileManager
                        .getInputStream(parentPomPath));
                final Element root = pomDocument.getDocumentElement();
                resolvePoms(root, parentPomPath, pomSet);
            }
        }
    }

    private void resolvePoms(final Element pomRoot, final String pomPath,
            final Map<String, String> pomSet) {
        pomSet.put(pomPath, pomSet.get(pomPath)); // ensures this key exists

        final Element parentElement = XmlUtils.findFirstElement(
                "/project/parent", pomRoot);
        if (parentElement != null) {
            resolveParentPom(pomPath, pomSet, parentElement);
        }

        resolveChildModulePoms(pomRoot, pomPath, pomSet);
    }

    private String resolveRelativePath(String relativeTo,
            final String relativePath) {
        if (relativeTo.endsWith(File.separator)) {
            relativeTo = relativeTo.substring(0, relativeTo.length() - 1);
        }
        while (new File(relativeTo).isFile()) {
            relativeTo = relativeTo.substring(0,
                    relativeTo.lastIndexOf(File.separator));
        }
        final String[] relativePathSegments = relativePath.split(FileUtils
                .getFileSeparatorAsRegex());

        int backCount = 0;
        for (final String relativePathSegment : relativePathSegments) {
            if (relativePathSegment.equals("..")) {
                backCount++;
            }
            else {
                break;
            }
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = backCount; i < relativePathSegments.length; i++) {
            sb.append(relativePathSegments[i]);
            sb.append(File.separatorChar);
        }

        while (backCount > 0) {
            relativeTo = relativeTo.substring(0,
                    relativeTo.lastIndexOf(File.separatorChar));
            backCount--;
        }
        String path = relativeTo + File.separator + sb.toString();
        if (new File(path).isDirectory()) {
            path = path + DEFAULT_POM_NAME;
        }
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public void setFocusedModule(final Pom focusedModule) {
        Assert.notNull(focusedModule, "Module required");
        if (focusedModule.getPath().equals(focusedModulePath)) {
            return;
        }
        focusedModulePath = focusedModule.getPath();
        shell.setPromptPath(focusedModule.getModuleName());
    }

    private void sortPomMap() {
        final List<String> sortedPomPaths = new ArrayList<String>(
                pomMap.keySet());
        Collections.sort(sortedPomPaths, new PomComparator(pomMap));
        final Map<String, Pom> sortedPomMap = new LinkedHashMap<String, Pom>();
        for (final String pomPath : sortedPomPaths) {
            sortedPomMap.put(pomPath, pomMap.get(pomPath));
        }
        pomMap.clear();
        pomMap.putAll(sortedPomMap);
    }

    private void updatePomCache() {
        findUnparsedPoms();
        final Collection<Pom> newPoms = parseUnparsedPoms();
        if (!newPoms.isEmpty()) {
            sortPomMap();
        }
        updateProjectMetadataForModules(newPoms);
    }

    private void updateProjectMetadataForModules(final Iterable<Pom> newPoms) {
        for (final Pom pom : newPoms) {
            final String projectMetadataId = ProjectMetadata
                    .getProjectIdentifier(pom.getModuleName());
            metadataService.evictAndGet(projectMetadataId);
            metadataDependencyRegistry.notifyDownstream(projectMetadataId);
        }
    }
}
