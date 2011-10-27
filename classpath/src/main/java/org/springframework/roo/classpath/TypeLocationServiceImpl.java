package org.springframework.roo.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathInformation;
import org.springframework.roo.project.PomManagementService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.NaturalOrderComparator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

/**
 * Implementation of {@link TypeLocationService}.
 *
 * <p>
 * For performance reasons automatically caches the queries. The cache is invalidated on
 * changes to the file system.
 *
 * @author Alan Stewart
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author James Tyrrell
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class TypeLocationServiceImpl implements TypeLocationService {

	// Fields
	@Reference private FileManager fileManager;
	@Reference private FileMonitorService fileMonitorService;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeResolutionService typeResolutionService;
	@Reference private PomManagementService pomManagementService;
	@Reference private TypeCache typeCache;

	private final Map<JavaType, Set<String>> annotationToMidMap = new HashMap<JavaType, Set<String>>();
	private final Map<Object, Set<String>> tagToMidMap = new HashMap<Object, Set<String>>();
	private final Map<String, Set<String>> changeMap = new HashMap<String, Set<String>>();
	private final Map<String, Set<JavaType>> typeAnnotationMap = new HashMap<String, Set<JavaType>>();
	private final Map<String, Set<Object>> typeCustomDataMap = new HashMap<String, Set<Object>>();

	public ClassOrInterfaceTypeDetails getTypeDetails(final JavaType type) {
		Assert.notNull(type, "Java type required");
		updateCache();
		String metadataIdentificationString = getPhysicalTypeIdentifier(type);
		if (metadataIdentificationString == null) {
			return null;
		}
		ClassOrInterfaceTypeDetails cachedType = typeCache.getTypeDetails(metadataIdentificationString);
		if (cachedType != null) {
			return cachedType;
		}
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(metadataIdentificationString);
		if (physicalTypeMetadata == null) {
			return null;
		}
		PhysicalTypeDetails physicalTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (physicalTypeDetails == null || !(physicalTypeDetails instanceof ClassOrInterfaceTypeDetails)) {
			return null;
		}
		return (ClassOrInterfaceTypeDetails) physicalTypeDetails;
	}

	public void processTypesWithTag(final Object tag, final LocatedTypeCallback callback) {
		Assert.notNull(tag, "Tag required");
		Assert.notNull(callback, "Callback required");
		// If the cache doesn't yet contain the tag it should be added
		if (!tagToMidMap.containsKey(tag)) {
			tagToMidMap.put(tag, new HashSet<String>());
		}

		// Before processing the call any changes to the project should be processed and the cache updated accordingly
		updateCache();

		for (String locatedMid : tagToMidMap.get(tag)) {
			ClassOrInterfaceTypeDetails located = getCachedClassOrInterfaceTypeDetails(locatedMid);
			callback.process(located);
		}
	}

	private String getProposedJavaType(final String fileCanonicalPath) {
		Assert.hasText(fileCanonicalPath, "File canonical path required");
		// Determine the JavaType for this file
		String relativePath = "";
		for (PathInformation pathInformation : pomManagementService.getModuleForFileIdentifier(fileCanonicalPath).getPathInformation()) {
			if (fileCanonicalPath.startsWith(FileUtils.ensureTrailingSeparator(FileUtils.getCanonicalPath(pathInformation.getLocation())))) {
				relativePath = File.separator + fileCanonicalPath.replaceFirst(FileUtils.ensureTrailingSeparator(FileUtils.getCanonicalPath(pathInformation.getLocation())), "");
				break;
			}
		}
		Assert.hasText(relativePath, "Could not determine compilation unit name for file '" + fileCanonicalPath + "'");
		Assert.isTrue(relativePath.startsWith(File.separator), "Relative path unexpectedly dropped the '" + File.separator + "' prefix (received '" + relativePath + "' from '" + fileCanonicalPath + "'");
		relativePath = relativePath.substring(1);
		Assert.isTrue(relativePath.endsWith(".java"), "The relative path unexpectedly dropped the .java extension for file '" + fileCanonicalPath + "'");
		relativePath = relativePath.substring(0, relativePath.lastIndexOf(".java"));

		return relativePath.replace(File.separatorChar, '.');
	}

	public String getPhysicalTypeIdentifier(final String fileCanonicalPath) {
		Assert.hasText(fileCanonicalPath, "File canonical path required");
		if (doesPathIndicateJavaType(fileCanonicalPath)) {
			String physicalTypeIdentifier = typeCache.getTypeIdFromTypeFilePath(fileCanonicalPath);
			if (physicalTypeIdentifier != null) {
				return physicalTypeIdentifier;
			}
			String typeDirectory = FileUtils.getFirstDirectory(fileCanonicalPath);
			String simpleTypeName = fileCanonicalPath.replaceFirst(typeDirectory + File.separator, "").replaceAll("\\.java", "");
			JavaPackage javaPackage = typeResolutionService.getPackage(fileCanonicalPath);
			if (javaPackage == null) {
				return null;
			}
			JavaType javaType = new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + simpleTypeName);
			Pom module = pomManagementService.getModuleForFileIdentifier(fileCanonicalPath);
			Assert.notNull(module, "The module for the file '" + fileCanonicalPath + "' could not be located");
			typeCache.cacheTypeAgainstModule(module, javaType);

			String relativeTypePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
			String reducedPath = fileCanonicalPath.replaceAll(relativeTypePath, "");
			reducedPath = FileUtils.removeTrailingSeparator(reducedPath);

			for (PathInformation pathInformation : module.getPathInformation()) {
				if (pathInformation.getLocationPath().startsWith(reducedPath)) {
					ContextualPath path = pathInformation.getContextualPath();
					physicalTypeIdentifier = MetadataIdentificationUtils.create(PhysicalTypeIdentifier.class.getName(), path.getName() + "?" + javaType.getFullyQualifiedTypeName());
					break;
				}
			}
			typeCache.cacheFilePathAgainstTypeIdentifier(fileCanonicalPath, physicalTypeIdentifier);

			return physicalTypeIdentifier;
		}
		return null;
	}

	public void processTypesWithAnnotation(final List<JavaType> annotationsToDetect, final LocatedTypeCallback callback) {
		Assert.notNull(annotationsToDetect, "Annotations to detect required");
		Assert.notNull(callback, "Callback required");
		// If the cache doesn't yet contain the annotation to be found it should be added
		for (JavaType annotationType : annotationsToDetect) {
			if (!annotationToMidMap.containsKey(annotationType)) {
				annotationToMidMap.put(annotationType, new HashSet<String>());
			}
		}

		// Before processing the call any changes to the project should be processed and the cache updated accordingly
		updateCache();

		for (JavaType annotationType : annotationsToDetect) {
			for (String locatedMid : annotationToMidMap.get(annotationType)) {
				ClassOrInterfaceTypeDetails located = getCachedClassOrInterfaceTypeDetails(locatedMid);
				callback.process(located);
			}
		}
	}

	public Set<JavaType> findTypesWithAnnotation(final List<JavaType> annotationsToDetect) {
		Assert.notNull(annotationsToDetect, "Annotations to detect required");
		final Set<JavaType> types = new LinkedHashSet<JavaType>();
		processTypesWithAnnotation(annotationsToDetect, new LocatedTypeCallback() {
			public void process(final ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located.getName());
				}
			}
		});
		return Collections.unmodifiableSet(types);
	}

	public Set<JavaType> findTypesWithAnnotation(final JavaType... annotationsToDetect) {
		return findTypesWithAnnotation(Arrays.asList(annotationsToDetect));
	}

	public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithAnnotation(final JavaType... annotationsToDetect) {
		final List<ClassOrInterfaceTypeDetails> types = new ArrayList<ClassOrInterfaceTypeDetails>();
		processTypesWithAnnotation(Arrays.asList(annotationsToDetect), new LocatedTypeCallback() {
			public void process(final ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located);
				}
			}
		});
		Collections.sort(types, new NaturalOrderComparator<ClassOrInterfaceTypeDetails>() {
			@Override
			protected String stringify(final ClassOrInterfaceTypeDetails object) {
				return object.getName().getSimpleTypeName();
			}
		});

		return Collections.unmodifiableSet(new LinkedHashSet<ClassOrInterfaceTypeDetails>(types));
	}

	public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithTag(final Object tag) {
		Assert.notNull(tag, "Tag required");
		final Set<ClassOrInterfaceTypeDetails> types = new LinkedHashSet<ClassOrInterfaceTypeDetails>();
		processTypesWithTag(tag, new LocatedTypeCallback() {
			public void process(final ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located);
				}
			}
		});
		return Collections.unmodifiableSet(types);
	}

	public ClassOrInterfaceTypeDetails getTypeDetails(final String physicalTypeIdentifier) {
		Assert.hasText(physicalTypeIdentifier, "Physical type identifier required");
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Metadata identification string '" + physicalTypeIdentifier + "' is not valid for this metadata provider");
		updateCache();
		return typeCache.getTypeDetails(physicalTypeIdentifier);
	}

	/**
	 * Obtains the a cached copy of the {@link ClassOrInterfaceTypeDetails} for this physical type identifier,
	 * or null if it cannot be found.
	 *
	 * @param physicalTypeIdentifier to lookup (required)
	 * @return the details (or null if unavailable)
	 */
	private ClassOrInterfaceTypeDetails getCachedClassOrInterfaceTypeDetails(final String physicalTypeIdentifier) {
		Assert.hasText(physicalTypeIdentifier, "Physical type identifier required");
		return typeCache.getTypeDetails(physicalTypeIdentifier);
	}

	/**
	 * Obtains the a fresh copy of the {@link ClassOrInterfaceTypeDetails} for this physical type identifier,
	 * or null if it cannot be found.
	 *
	 * @param physicalTypeIdentifier to lookup (required)
	 * @return the details (or null if unavailable)
	 */
	private ClassOrInterfaceTypeDetails lookupClassOrInterfaceTypeDetails(final String physicalTypeIdentifier) {
		Assert.hasText(physicalTypeIdentifier, "Physical type identifier required");
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier, true);
		if (physicalTypeMetadata != null && physicalTypeMetadata.getMemberHoldingTypeDetails() != null && physicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			return (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		}
		return null;
	}

	private boolean doesPathIndicateJavaType(final String fileCanonicalPath) {
		Assert.hasText(fileCanonicalPath, "File canonical path required");
		return fileCanonicalPath.endsWith(".java") && !fileCanonicalPath.endsWith("package-info.java") && JavaSymbolName.isLegalJavaName(getProposedJavaType(fileCanonicalPath));
	}

	private void cacheType(final String fileCanonicalPath) {
		Assert.hasText(fileCanonicalPath, "File canonical path required");
		if (doesPathIndicateJavaType(fileCanonicalPath)) {
			String id = getPhysicalTypeIdentifier(fileCanonicalPath);
			if (id != null && PhysicalTypeIdentifier.isValid(id)) {
				// Change to Java, so drop the cache
				ClassOrInterfaceTypeDetails cid = lookupClassOrInterfaceTypeDetails(id);
				if (cid == null) {
					if (!fileManager.exists(fileCanonicalPath)) {

						typeCache.removeType(id);
						JavaType type = getCachedClassOrInterfaceTypeDetails(id).getName();
						updateChanges(type.getFullyQualifiedTypeName(), true);
					}
					return;
				}
				typeCache.cacheType(fileCanonicalPath, cid);
				updateAttributeCache(cid);
				updateChanges(cid.getName().getFullyQualifiedTypeName(), false);
			}
		}
	}

	private void updateAttributeCache(final MemberHoldingTypeDetails cid) {
		Assert.notNull(cid, "Member holding type details required");
		if (!typeAnnotationMap.containsKey(cid.getDeclaredByMetadataId())) {
			typeAnnotationMap.put(cid.getDeclaredByMetadataId(), new HashSet<JavaType>());
		}
		if (!typeCustomDataMap.containsKey(cid.getDeclaredByMetadataId())) {
			typeCustomDataMap.put(cid.getDeclaredByMetadataId(), new HashSet<Object>());
		}
		Set<JavaType> previousAnnotations = typeAnnotationMap.get(cid.getDeclaredByMetadataId());
		for (JavaType previousAnnotation : previousAnnotations) {
			Set<String> midSet = annotationToMidMap.get(previousAnnotation);
			if (midSet != null) {
				midSet.remove(cid.getDeclaredByMetadataId());
			}
		}
		previousAnnotations.clear();
		for (AnnotationMetadata annotationMetadata : cid.getAnnotations()) {
			if (!annotationToMidMap.containsKey(annotationMetadata.getAnnotationType())) {
				annotationToMidMap.put(annotationMetadata.getAnnotationType(), new HashSet<String>());
			}
			previousAnnotations.add(annotationMetadata.getAnnotationType());
			annotationToMidMap.get(annotationMetadata.getAnnotationType()).add(cid.getDeclaredByMetadataId());
		}
		Set<Object> previousCustomDataSet = typeCustomDataMap.get(cid.getDeclaredByMetadataId());
		for (Object previousCustomData : previousCustomDataSet) {
			Set<String> midSet = tagToMidMap.get(previousCustomData);
			if (midSet != null) {
				midSet.remove(cid.getDeclaredByMetadataId());
			}
		}
		previousCustomDataSet.clear();
		for (Object customData : cid.getCustomData().keySet()) {
			if (!tagToMidMap.containsKey(customData)) {
				tagToMidMap.put(customData, new HashSet<String>());
			}
			previousCustomDataSet.add(customData);
			tagToMidMap.get(customData).add(cid.getDeclaredByMetadataId());
		}
	}

	private final Set<String> discoveredTypes = new HashSet<String>();
	private final Set<String> dirtyFiles = new HashSet<String>();

	private Set<String> discoverTypes() {
		// Retrieve a list of paths that have been discovered or modified since the last invocation by this class
		Set<String> changes = fileMonitorService.getDirtyFiles(TypeLocationServiceImpl.class.getName());
		for (String change : changes) {
			if (doesPathIndicateJavaType(change)) {
				discoveredTypes.add(change);
				dirtyFiles.add(change);
			}
		}
		return discoveredTypes;
	}

	private void updateCache() {
		if (typeCache.getAllTypeIdentifiers().isEmpty()) {
			initTypeMap();
		}
		discoverTypes();
		// Update the type cache
		for (String change : dirtyFiles) {
			cacheType(change);
		}
		dirtyFiles.clear();
	}

	private void updateChanges(final String typeName, final boolean remove) {
		Assert.notNull(typeName, "Type name required");
		for (String requestingClass : changeMap.keySet()) {
			if (remove) {
				changeMap.get(requestingClass).remove(typeName);
			} else {
				changeMap.get(requestingClass).add(typeName);
			}
		}
	}

	public String getPhysicalTypeIdentifier(final JavaType type) {
		Assert.notNull(type, "Java type required");

		String typeRelativePath = type.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		String typePath = null;
		for (String typeFile : discoverTypes()) {
			if (typeFile.endsWith(typeRelativePath)) {
				typePath = typeFile;
				break;
			}
		}
		if (typePath == null) {
			return null;
		}
		String reducedPath = FileUtils.ensureTrailingSeparator(typePath.replaceAll(typeRelativePath, ""));
		String mid = null;
		for (Pom pom : pomManagementService.getPomMap().values()) {
			for (Path path : Arrays.asList(Path.SRC_MAIN_JAVA, Path.SRC_TEST_JAVA)) {
				PathInformation pathInformation = pom.getPathInformation(path);
				String pathLocation = FileUtils.ensureTrailingSeparator(pathInformation.getLocationPath());
				if (pathLocation.startsWith(reducedPath)) {
					mid = PhysicalTypeIdentifier.createIdentifier(type, pathInformation.getContextualPath());
					projectOperations.addModuleDependency(pathInformation.getContextualPath().getModule());
					break;
				}
			}
		}
		return mid;
	}

	public String getPhysicalTypeIdentifier(final JavaType type, final ContextualPath path) {
		Assert.notNull(type, "Java type required");
		Assert.notNull(type, "Contextual path required");

		String typeRelativePath = type.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		String typeFilePath = null;
		for (String typeFile : discoverTypes()) {
			if (typeFile.endsWith(typeRelativePath)) {
				typeFilePath = typeFile;
				break;
			}
		}
		if (typeFilePath == null) {
			return null;
			//throw new IllegalStateException("The source for '" + type.getFullyQualifiedTypeName() + "' could not be resolved");
		}
		String reducedPath = FileUtils.ensureTrailingSeparator(typeFilePath.replaceAll(typeRelativePath, ""));
		String mid = null;
		for (Pom pom : pomManagementService.getPomMap().values()) {
			PathInformation pathInformation = pom.getPathInformation(path.getPath());
			String pathLocation = FileUtils.ensureTrailingSeparator(pathInformation.getLocationPath());
			if (pathLocation.startsWith(reducedPath)) {
				mid = PhysicalTypeIdentifier.createIdentifier(type, pathInformation.getContextualPath());
				break;
			}
		}

		projectOperations.addModuleDependency(mid);
		return mid;
	}

	public Set<String> getTypesForModule(final String modulePath) {
		Assert.notNull(modulePath, "Module path required");

		return typeCache.getTypeNamesForModuleFilePath(modulePath);
	}

	public boolean hasTypeChanged(final String requestingClass, final JavaType javaType) {
		Assert.notNull(requestingClass, "Requesting class required");
		Assert.notNull(javaType, "Java type required");

		updateCache();
		Set<String> changesSinceLastRequest = changeMap.get(requestingClass);
		if (changesSinceLastRequest == null) {
			changesSinceLastRequest = new LinkedHashSet<String>();
			for (String typeIdentifier : typeCache.getAllTypeIdentifiers()) {
				changesSinceLastRequest.add(typeCache.getTypeDetails(typeIdentifier).getName().getFullyQualifiedTypeName());
			}
			changeMap.put(requestingClass, changesSinceLastRequest);
		}
		for (String changedId : changesSinceLastRequest) {
			if (changedId.equals(javaType.getFullyQualifiedTypeName())) {
				changesSinceLastRequest.remove(changedId);
				return true;
			}
		}
		return false;
	}

	private void initTypeMap() {
		for (Pom pom : pomManagementService.getPomMap().values()) {
			for (Path path : Arrays.asList(Path.SRC_MAIN_JAVA, Path.SRC_TEST_JAVA)) {
				String pathToResolve = FileUtils.ensureTrailingSeparator(pom.getPathInformation(path).getLocationPath()) + "**" + File.separatorChar + "*.java";
				for (FileDetails file : fileManager.findMatchingAntPath(pathToResolve)) {
					cacheType(file.getCanonicalPath());
				}
			}
		}
	}

	public List<String> getPotentialTopLevelPackagesForModule(final Pom module) {
		Assert.notNull(module, "Module required");

		Map<String, Set<String>> packageMap =  new HashMap<String, Set<String>>();
		Set<String> moduleTypes = getTypesForModule(module.getPath());
		List<String> topLevelPackages = new ArrayList<String>();
		if (moduleTypes.isEmpty()) {
			topLevelPackages.add(module.getGroupId());
			return topLevelPackages;
		}
		for (String typeName : moduleTypes) {
			StringBuilder sb = new StringBuilder();
			String type = typeName.substring(0, typeName.lastIndexOf('.'));
			String[] typeSegments = type.split("\\.");
			Set<String> discoveredPackages = new HashSet<String>();
			for (int i = 0; i < typeSegments.length; i++) {
				String typeSegment = typeSegments[i];
				if (i > 0) {
					sb.append(".");
				}
				sb.append(typeSegment);
				discoveredPackages.add(sb.toString());
			}

			for (String discoveredPackage : discoveredPackages) {
				if (!packageMap.containsKey(discoveredPackage)) {
					packageMap.put(discoveredPackage, new HashSet<String>());
				}
				packageMap.get(discoveredPackage).add(typeName);
			}
		}

		int longestPackage = 0;
		for (Map.Entry<String, Set<String>> entry : packageMap.entrySet()) {
			if (entry.getValue().size() == moduleTypes.size()) {
				topLevelPackages.add(entry.getKey());
				if (entry.getKey().length() > longestPackage) {
					longestPackage = entry.getKey().length();
				}
			}
		}
		return topLevelPackages;
	}

	public String getTopLevelPackageForModule(final Pom module) {
		Assert.notNull(module, "Module required");

		Map<String, Set<String>> packageMap =  new HashMap<String, Set<String>>();
		Set<String> moduleTypes = getTypesForModule(module.getPath());
		if (moduleTypes.isEmpty()) {
			return module.getGroupId();
		}
		Set<String> uniqueTypePackages = new HashSet<String>();
		for (String typeName : moduleTypes) {
			StringBuilder sb = new StringBuilder();
			String typePackage = typeName.substring(0, typeName.lastIndexOf('.'));
			uniqueTypePackages.add(typePackage);
			String[] typeSegments = typePackage.split("\\.");
			Set<String> discoveredPackages = new HashSet<String>();
			for (int i = 0; i < typeSegments.length; i++) {
				String typeSegment = typeSegments[i];
				if (i > 0) {
					sb.append(".");
				}
				sb.append(typeSegment);
				discoveredPackages.add(sb.toString());
			}

			for (String discoveredPackage : discoveredPackages) {
				if (!packageMap.containsKey(discoveredPackage)) {
					packageMap.put(discoveredPackage, new HashSet<String>());
				}
				packageMap.get(discoveredPackage).add(typeName);
			}
		}

		if (uniqueTypePackages.size() == 1) {
			return module.getGroupId();
		}

		List<String> packageList = new ArrayList<String>(packageMap.keySet());
		Collections.sort(packageList, new Comparator<String>() {
			public int compare(final String s1, final String s2) {
				return Integer.valueOf(s1.length()).compareTo(s2.length());
			}
		});
		Map<String, Set<String>> sortedPackageMap = new LinkedHashMap<String, Set<String>>();
		for (String discoveredPackage : packageList) {
			sortedPackageMap.put(discoveredPackage, packageMap.get(discoveredPackage));
		}

		int longestPackage = 0;
		String topLevelPackage = module.getGroupId();
		for (Map.Entry<String, Set<String>> entry : sortedPackageMap.entrySet()) {
			if (entry.getValue().size() == moduleTypes.size()) {
				if (entry.getKey().length() > longestPackage) {
					longestPackage = entry.getKey().length();
					topLevelPackage = entry.getKey();
				}
			}
		}
		return topLevelPackage;
	}

	public ContextualPath getTypePath(final JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return PhysicalTypeIdentifier.getPath(getPhysicalTypeIdentifier(javaType));
	}

	public String getPhysicalTypeCanonicalPath(final JavaType javaType, final ContextualPath path) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(path, "Path required");
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return getPhysicalTypeCanonicalPath(physicalTypeIdentifier);
	}

	public String getPhysicalTypeCanonicalPath(final String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		ContextualPath path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);

		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";

		for (String existingTypePath : discoverTypes()) {
			if (existingTypePath.endsWith(relativePath)) {
				typeCache.cacheTypeAgainstModule(pomManagementService.getPomFromModuleName(path.getModule()), javaType);
				typeCache.cacheFilePathAgainstTypeIdentifier(existingTypePath, physicalTypeIdentifier);
				return existingTypePath;
			}
		}

		Pom pom = pomManagementService.getPomFromModuleName(path.getModule());
		if (pom != null) {
			String filePath = pom.getPathLocation(path.getPath()) + relativePath;
			typeCache.cacheTypeAgainstModule(pom, javaType);
			typeCache.cacheFilePathAgainstTypeIdentifier(filePath, physicalTypeIdentifier);
			return filePath;
		}
		return null;
	}

}
