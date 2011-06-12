package org.springframework.roo.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link TypeLocationService}.
 * 
 * <p>
 * For performance reasons automatically caches the queries, invalidating the cache 
 * when any {@link PhysicalTypeMetadata} notification is received.
 * 
 * @author Alan Stewart
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true) 
@Service 
public class TypeLocationServiceImpl implements TypeLocationService, MetadataNotificationListener {
	@Reference private FileManager fileManager;
	@Reference private FileMonitorService fileMonitorService;
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ProjectOperations projectOperations;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	private Map<JavaType, Set<String>> annotationToMidMap = new HashMap<JavaType, Set<String>>();
	private Map<Object, Set<String>> tagToMidMap = new HashMap<Object, Set<String>>();

	protected void activate(ComponentContext context) {
		dependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(ComponentContext context) {
		dependencyRegistry.removeNotificationListener(this);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (upstreamDependency != null && PhysicalTypeIdentifier.isValid(upstreamDependency)) {
			// Change to Java, so drop the cache
			ClassOrInterfaceTypeDetails cid = getClassOrInterfaceTypeDetails(upstreamDependency);
			if (cid == null) return;
			
			// Iterate over the entire annotation cache
			for (JavaType annotationDetected : annotationToMidMap.keySet()) {
				if (MemberFindingUtils.getTypeAnnotation(cid, annotationDetected) != null) {
					// This type has the annotation, so guarantee insertion
					annotationToMidMap.get(annotationDetected).add(cid.getDeclaredByMetadataId());
				} else {
					// This type does not have the annotation, so guarantee removal
					annotationToMidMap.get(annotationDetected).remove(cid.getDeclaredByMetadataId());
				}
			}
			
			// Iterate over the entire tag cache
			MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(TypeLocationServiceImpl.class.getName(), cid);
			for (Object tagDetected : tagToMidMap.keySet()) {
				if (!MemberFindingUtils.getMemberHoldingTypeDetailsWithTag(memberDetails, tagDetected).isEmpty()) {
					// This type has the tag, so guarantee insertion
					tagToMidMap.get(tagDetected).add(cid.getDeclaredByMetadataId());
				} else {
					// This type does not have the tag, so guarantee removal
					tagToMidMap.get(tagDetected).remove(cid.getDeclaredByMetadataId());
				}
			}
		}
	}

	public String getPhysicalLocationCanonicalPath(JavaType javaType, Path path) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(path, "Path required");
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return projectOperations.getPathResolver().getIdentifier(path, relativePath);
	}
	
	public String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
		Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return projectOperations.getPathResolver().getIdentifier(path, relativePath);
	}

	public ClassOrInterfaceTypeDetails getClassOrInterface(JavaType requiredClassOrInterface) {
		String metadataIdentificationString = physicalTypeMetadataProvider.findIdentifier(requiredClassOrInterface);
		Assert.notNull(metadataIdentificationString, "Unable to locate requested type'" + requiredClassOrInterface.getFullyQualifiedTypeName() + "'");
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(metadataIdentificationString);
		PhysicalTypeDetails physicalTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.notNull(physicalTypeDetails, "Type '" + requiredClassOrInterface.getFullyQualifiedTypeName() + "' exists on disk but cannot be parsed");
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, physicalTypeDetails, "Type '" + requiredClassOrInterface.getFullyQualifiedTypeName() + "' is not an interface or class");
		return (ClassOrInterfaceTypeDetails) physicalTypeDetails;
	}

	public ClassOrInterfaceTypeDetails findClassOrInterface(JavaType requiredClassOrInterface) {
		String metadataIdentificationString = physicalTypeMetadataProvider.findIdentifier(requiredClassOrInterface);
		if (metadataIdentificationString == null) {
			return null;
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
		boolean cacheAllowed = !fileMonitorService.isDirty();
		if (cacheAllowed && tagToMidMap.containsKey(tag)) {
			for (String locatedMid : tagToMidMap.get(tag)) {
				ClassOrInterfaceTypeDetails located = getClassOrInterfaceTypeDetails(locatedMid);
				callback.process(located);
			}
		} else {
			Set<String> locatedMids = new HashSet<String>();
			for (ClassOrInterfaceTypeDetails cid : getProjectJavaTypes(Path.SRC_MAIN_JAVA)) {
				MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(TypeLocationServiceImpl.class.getName(), cid);
				if (!MemberFindingUtils.getMemberHoldingTypeDetailsWithTag(memberDetails, tag).isEmpty()) {
					locatedMids.add(cid.getDeclaredByMetadataId());
					callback.process(cid);
				}
			}

			if (cacheAllowed) {
				tagToMidMap.put(tag, locatedMids);
			}
		}
	}

	public void processTypesWithAnnotation(List<JavaType> annotationsToDetect, LocatedTypeCallback callback) {
		boolean cacheAllowed = !fileMonitorService.isDirty();
		for (JavaType annotationType : annotationsToDetect) {
			if (cacheAllowed && annotationToMidMap.containsKey(annotationType)) {
				for (String locatedMid : annotationToMidMap.get(annotationType)) {
					ClassOrInterfaceTypeDetails located = getClassOrInterfaceTypeDetails(locatedMid);
					callback.process(located);
				}
			} else {
				Set<String> locatedMids = new HashSet<String>();
				for (ClassOrInterfaceTypeDetails cid : getProjectJavaTypes(Path.SRC_MAIN_JAVA)) {
					if (MemberFindingUtils.getTypeAnnotation(cid, annotationType) != null) {
						locatedMids.add(cid.getDeclaredByMetadataId());
						callback.process(cid);
					}
				}

				if (cacheAllowed) {
					annotationToMidMap.put(annotationType, locatedMids);
				}
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

	/**
	 * Obtains the class or interface details for this physical type identifier, or null if it cannot be found.
	 * @param physicalTypeMid to lookup (required)
	 * @return the details (or null if unavailable)
	 */
	private ClassOrInterfaceTypeDetails getClassOrInterfaceTypeDetails(String physicalTypeMid) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeMid);
		if (physicalTypeMetadata != null && physicalTypeMetadata.getMemberHoldingTypeDetails() != null && physicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			return (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		}
		return null;
	}
	
	public List<ClassOrInterfaceTypeDetails> getProjectJavaTypes(Path path) {
		PathResolver pathResolver = projectOperations.getPathResolver();
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(path)), null);
		List<ClassOrInterfaceTypeDetails> projectTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
		
		for (FileDetails file : fileManager.findMatchingAntPath(pathResolver.getRoot(path) + File.separatorChar + "**" + File.separatorChar + "*.java")) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
			JavaType javaType;
			try {
				javaType = new JavaType(fullPath);
			} catch (RuntimeException e) { // ROO-1022
				continue;
			}
			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id == null) {
				continue;
			}
			// Now I've found it, let's work out the Path it is from
			Path locatedPath = PhysicalTypeIdentifier.getPath(id);
			String physicalTypeMid = PhysicalTypeIdentifier.createIdentifier(javaType, locatedPath);
			ClassOrInterfaceTypeDetails located = getClassOrInterfaceTypeDetails(physicalTypeMid);
			if (located != null) {
				projectTypes.add(located);
			}
		}
		return projectTypes;
	}
}
