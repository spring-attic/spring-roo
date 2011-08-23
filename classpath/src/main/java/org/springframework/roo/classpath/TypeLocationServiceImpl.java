package org.springframework.roo.classpath;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

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
import java.util.logging.Logger;

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
public class TypeLocationServiceImpl implements TypeLocationService, FileEventListener {

	private static Logger logger = HandlerUtils.getLogger(TypeLocationServiceImpl.class);
	
	// Fields
	@Reference private FileManager fileManager;
	@Reference private FileMonitorService fileMonitorService;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;

	private final Map<JavaType, Set<String>> annotationToMidMap = new HashMap<JavaType, Set<String>>();
	private final Map<Object, Set<String>> tagToMidMap = new HashMap<Object, Set<String>>();
	private final LinkedHashMap<String, ClassOrInterfaceTypeDetails> typeMap = new LinkedHashMap<String, ClassOrInterfaceTypeDetails>();
	private final HashMap<String, String> pathCacheMap = new HashMap<String, String>();
	private Map<JavaType, String> javaTypeIdentifierCache = new HashMap<JavaType, String>();

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
		updateCache();
		String metadataIdentificationString = findIdentifier(requiredClassOrInterface);
		Assert.notNull(metadataIdentificationString, "Unable to locate requested type'" + requiredClassOrInterface.getFullyQualifiedTypeName() + "'");
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeMap.get(metadataIdentificationString);
		Assert.notNull(classOrInterfaceTypeDetails, "Type '" + requiredClassOrInterface.getFullyQualifiedTypeName() + "' exists on disk but cannot be parsed");
		return classOrInterfaceTypeDetails;
	}

	public ClassOrInterfaceTypeDetails findClassOrInterface(JavaType requiredClassOrInterface) {
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
		final Set<ClassOrInterfaceTypeDetails> types = new LinkedHashSet<ClassOrInterfaceTypeDetails>();
		processTypesWithAnnotation(Arrays.asList(annotationsToDetect), new LocatedTypeCallback() {
			public void process(ClassOrInterfaceTypeDetails located) {
				if (located != null) {
					types.add(located);
				}
			}
		});
		return Collections.unmodifiableSet(types);
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

		// Before processing the call any changes to the project should be processed and the cache updated accordingly
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
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata unavailable");
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
						updateChanges(id, true);
					}
					return;
				}
				if (cid.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
					return;
				}
				updateChanges(id, false);
				typeMap.put(id, cid);
				updateAttributeCache(cid);
			}
		}
	}

	private void updateAttributeCache(MemberHoldingTypeDetails cid) {
		for (AnnotationMetadata annotationMetadata : cid.getAnnotations()) {
			if (!annotationToMidMap.containsKey(annotationMetadata.getAnnotationType())) {
				annotationToMidMap.put(annotationMetadata.getAnnotationType(), new HashSet<String>());
			}
			annotationToMidMap.get(annotationMetadata.getAnnotationType()).add(cid.getDeclaredByMetadataId());
		}
		for (Object customData : cid.getCustomData().keySet()) {
			if (!tagToMidMap.containsKey(customData)) {
				tagToMidMap.put(customData, new HashSet<String>());
			}
			tagToMidMap.get(customData).add(cid.getDeclaredByMetadataId());
		}
	}

	private void updateCache() {
		// Retrieve a list of paths that have been discovered or modified since the last invocation by this class
		HashSet<String> changes = fileMonitorService.getWhatsDirty(TypeLocationServiceImpl.class.getName());

		// Update the type cache
		for (String change : changes) {
			if (doesPathIndicateJavaType(change)) {
				//logger.severe("Update cache: " + change);
				cacheType(change);
			}
		}
	}

	private final HashMap<String, LinkedHashSet<String>> changeMap = new HashMap<String, LinkedHashSet<String>>();

	public LinkedHashSet<String> getWhatsDirty(String requestingClass) {
		updateCache();
		LinkedHashSet<String> changesSinceLastRequest = changeMap.get(requestingClass);
		if (changesSinceLastRequest == null) {
			changesSinceLastRequest = new LinkedHashSet<String>(typeMap.keySet());
			changeMap.put(requestingClass, new LinkedHashSet<String>());
		} else {
			LinkedHashSet<String> copyOfChangesSinceLastRequest = new LinkedHashSet<String>(changesSinceLastRequest);
			changesSinceLastRequest.removeAll(copyOfChangesSinceLastRequest);
			changesSinceLastRequest = copyOfChangesSinceLastRequest;
		}
		LinkedHashSet<String> changedTypes = new LinkedHashSet<String>();
		for (String changedId : changesSinceLastRequest) {
			changedTypes.add(changedId);
		}
		return changedTypes;

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

	public void onFileEvent(FileEvent fileEvent) {

		/*String change = fileEvent.getFileDetails().getCanonicalPath();

		if (doesPathIndicateJavaType(change)) {
			String id = findIdentifier(change);
			if (id != null && PhysicalTypeIdentifier.isValid(id)) {
				logger.warning("Update cache: " + change);
				cacheType(change);
			}
		}*/
	}
}
