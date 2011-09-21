package org.springframework.roo.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.NaturalOrderComparator;
import org.springframework.roo.support.util.Assert;

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

	private final Map<JavaType, Set<String>> annotationToMidMap = new HashMap<JavaType, Set<String>>();
	private final Map<Object, Set<String>> tagToMidMap = new HashMap<Object, Set<String>>();
	private final Map<String, ClassOrInterfaceTypeDetails> typeMap = new LinkedHashMap<String, ClassOrInterfaceTypeDetails>();
	private final Map<String, String> pathCacheMap = new HashMap<String, String>();
	private Map<JavaType, String> javaTypeIdentifierCache = new HashMap<JavaType, String>();
	private final Map<String, Set<String>> changeMap = new HashMap<String, Set<String>>();
	private Map<String, Set<JavaType>> typeAnnotationMap = new HashMap<String, Set<JavaType>>();
	private Map<String, Set<Object>> typeCustomDataMap = new HashMap<String, Set<Object>>();

	public String getPhysicalTypeCanonicalPath(JavaType javaType, Path path) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(path, "Path required");
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType);
		return getPhysicalTypeCanonicalPath(physicalTypeIdentifier);
	}
	
	public String getPhysicalTypeCanonicalPath(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		if (!pathCacheMap.containsKey(physicalTypeIdentifier)) {
			JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
			Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
			String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
			pathCacheMap.put(physicalTypeIdentifier, projectOperations.getPathResolver().getIdentifier(path, relativePath));
		}
		return pathCacheMap.get(physicalTypeIdentifier);
	}

	public ClassOrInterfaceTypeDetails getClassOrInterface(JavaType requiredClassOrInterface) {
		// The cached is used so update it
		return findClassOrInterface(requiredClassOrInterface);
	}

	public ClassOrInterfaceTypeDetails findClassOrInterface(JavaType requiredClassOrInterface) {
		updateCache();
		String metadataIdentificationString = findIdentifier(requiredClassOrInterface);
		if (metadataIdentificationString == null) {
			return null;
		}
		ClassOrInterfaceTypeDetails cachedType = typeMap.get(metadataIdentificationString);
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

	public void processTypesWithTag(Object tag, LocatedTypeCallback callback) {
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

	public String findIdentifier(JavaType javaType) {
		Assert.notNull(javaType, "Java type to locate is required");
		String result = javaTypeIdentifierCache.get(javaType);
		if (result != null) {
			return result;
		}

		PathResolver pathResolver = getPathResolver();
		for (Path sourcePath : pathResolver.getSourcePaths()) {
			String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
			String fileIdentifier = pathResolver.getIdentifier(sourcePath, relativePath);
			if (fileManager.exists(fileIdentifier)) {
				// Found the file, so use this one
				String mid = PhysicalTypeIdentifier.createIdentifier(javaType, sourcePath);
				javaTypeIdentifierCache.put(javaType, mid);
				return mid;
			}
		}
		return null;
	}

	public String findIdentifier(String fileIdentifier) {
		if (doesPathIndicateJavaType(fileIdentifier)) {
			PathResolver pathResolver = getPathResolver();
			Path sourcePath = null;
			for (Path path : pathResolver.getSourcePaths()) {
				if (new FileDetails(new File(pathResolver.getRoot(path)), null).isParentOf(fileIdentifier)) {
					sourcePath = path;
					break;
				}
			}
			if (sourcePath == null) {
				// The .java file is not under a source path, so ignore it
				return null;
			}
			// Determine the JavaType for this file
			String relativePath = pathResolver.getRelativeSegment(fileIdentifier);
			Assert.hasText(relativePath, "Could not determine compilation unit name for file '" + fileIdentifier + "'");
			Assert.isTrue(relativePath.startsWith(File.separator), "Relative path unexpectedly dropped the '" + File.separator + "' prefix (received '" + relativePath + "' from '" + fileIdentifier + "'");
			relativePath = relativePath.substring(1);
			Assert.isTrue(relativePath.endsWith(".java"), "The relative path unexpectedly dropped the .java extension for file '" + fileIdentifier + "'");
			relativePath = relativePath.substring(0, relativePath.lastIndexOf(".java"));

			JavaType javaType = new JavaType(relativePath.replace(File.separatorChar, '.'));

			// Figure out the PhysicalTypeIdentifier
			return PhysicalTypeIdentifier.createIdentifier(javaType, sourcePath);
		}
		return null;
	}

	public void processTypesWithAnnotation(List<JavaType> annotationsToDetect, LocatedTypeCallback callback) {
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

	public Set<JavaType> findTypesWithAnnotation(List<JavaType> annotationsToDetect) {
		final Set<JavaType> types = new LinkedHashSet<JavaType>();
		processTypesWithAnnotation(annotationsToDetect, new LocatedTypeCallback() {
			public void process(ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located.getName());
				}
			}
		});
		return Collections.unmodifiableSet(types);
	}

	public Set<JavaType> findTypesWithAnnotation(JavaType... annotationsToDetect) {
		return findTypesWithAnnotation(Arrays.asList(annotationsToDetect));
	}

	public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithAnnotation(JavaType... annotationsToDetect) {
		final List<ClassOrInterfaceTypeDetails> types = new ArrayList<ClassOrInterfaceTypeDetails>();
		processTypesWithAnnotation(Arrays.asList(annotationsToDetect), new LocatedTypeCallback() {
			public void process(ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located);
				}
			}
		});
		Collections.sort(types, new NaturalOrderComparator<ClassOrInterfaceTypeDetails>() {
			@Override
			protected String stringify(ClassOrInterfaceTypeDetails object) {
				return object.getName().getSimpleTypeName();
			}
		});

		return Collections.unmodifiableSet(new LinkedHashSet<ClassOrInterfaceTypeDetails>(types));
	}

	public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithTag(Object tag) {
		final Set<ClassOrInterfaceTypeDetails> types = new LinkedHashSet<ClassOrInterfaceTypeDetails>();
		processTypesWithTag(tag, new LocatedTypeCallback() {
			public void process(ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located);
				}
			}
		});
		return Collections.unmodifiableSet(types);
	}

	public ClassOrInterfaceTypeDetails getTypeForIdentifier(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Metadata identification string '" + physicalTypeIdentifier + "' is not valid for this metadata provider");
		updateCache();
		return typeMap.get(physicalTypeIdentifier);
	}

	public List<ClassOrInterfaceTypeDetails> getProjectJavaTypes(Path path) {
		// Before processing the call, any changes to the project should be processed and the cache updated accordingly
		updateCache();

		List<ClassOrInterfaceTypeDetails> projectTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (String physicalTypeIdentifier : typeMap.keySet()) {
			// Returns Path?TypeLocation
			String instance = MetadataIdentificationUtils.getMetadataInstance(physicalTypeIdentifier);
			if (instance.startsWith(path.getName())) {
				projectTypes.add(typeMap.get(physicalTypeIdentifier));
			}
		}

		return projectTypes;
	}

	/**
	 * Obtains the a cached copy of the {@link ClassOrInterfaceTypeDetails} for this physical type identifier,
	 * or null if it cannot be found.
	 *
	 * @param physicalTypeMid to lookup (required)
	 * @return the details (or null if unavailable)
	 */
	private ClassOrInterfaceTypeDetails getCachedClassOrInterfaceTypeDetails(String physicalTypeMid) {
		return typeMap.get(physicalTypeMid);
	}

	/**
	 * Obtains the a fresh copy of the {@link ClassOrInterfaceTypeDetails} for this physical type identifier,
	 * or null if it cannot be found.
	 *
	 * @param physicalTypeMid to lookup (required)
	 * @return the details (or null if unavailable)
	 */
	private ClassOrInterfaceTypeDetails lookupClassOrInterfaceTypeDetails(String physicalTypeMid) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeMid, true);
		if (physicalTypeMetadata != null && physicalTypeMetadata.getMemberHoldingTypeDetails() != null && physicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			return (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		}
		return null;
	}

	private PathResolver getPathResolver() {
		return projectOperations.getPathResolver();
	}

	private boolean doesPathIndicateJavaType(String filePath) {
		return filePath.endsWith(".java") && !filePath.endsWith("package-info.java");
	}

	private void cacheType(String fileCanonicalPath) {
		if (doesPathIndicateJavaType(fileCanonicalPath)) {
			String id = findIdentifier(fileCanonicalPath);
			if (id != null && PhysicalTypeIdentifier.isValid(id)) {
				// Change to Java, so drop the cache
				ClassOrInterfaceTypeDetails cid = lookupClassOrInterfaceTypeDetails(id);
				if (cid == null) {
					if (!fileManager.exists(fileCanonicalPath)) {
						typeMap.remove(id);
						JavaType type = getCachedClassOrInterfaceTypeDetails(id).getName();
						updateChanges(type.getFullyQualifiedTypeName(), true);
					}
					return;
				}
				typeMap.put(id, cid);
				updateAttributeCache(cid);
				updateChanges(cid.getName().getFullyQualifiedTypeName(), false);
			}
		}
	}

	private void updateAttributeCache(MemberHoldingTypeDetails cid) {
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

	private void updateCache() {
		if (typeMap.isEmpty()) {
			initTypeMap();
			return;
		}
		// Retrieve a list of paths that have been discovered or modified since the last invocation by this class
		Set<String> changes = fileMonitorService.getDirtyFiles(TypeLocationServiceImpl.class.getName());
		// Update the type cache
		for (String change : changes) {
			if (doesPathIndicateJavaType(change)) {
				cacheType(change);
			}
		}
	}

	private void updateChanges(String physicalTypeIdentifier, boolean remove) {
		for (String requestingClass : changeMap.keySet()) {
			if (remove) {
				changeMap.get(requestingClass).remove(physicalTypeIdentifier);
			} else {
				changeMap.get(requestingClass).add(physicalTypeIdentifier);
			}
		}
	}

	public boolean hasTypeChanged(String requestingClass, JavaType javaType) {
		updateCache();
		Set<String> changesSinceLastRequest = changeMap.get(requestingClass);
		if (changesSinceLastRequest == null) {
			changesSinceLastRequest = new LinkedHashSet<String>();
			for (ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails : typeMap.values()) {
				changesSinceLastRequest.add(classOrInterfaceTypeDetails.getName().getFullyQualifiedTypeName());
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
		for (Path path : Arrays.asList(Path.SRC_MAIN_JAVA, Path.SRC_TEST_JAVA, Path.ROOT)) {
			PathResolver pathResolver = projectOperations.getPathResolver();
			for (FileDetails file : fileManager.findMatchingAntPath(pathResolver.getRoot(path) + File.separatorChar + "**" + File.separatorChar + "*.java")) {
				cacheType(file.getCanonicalPath());
			}
		}
	}
}
