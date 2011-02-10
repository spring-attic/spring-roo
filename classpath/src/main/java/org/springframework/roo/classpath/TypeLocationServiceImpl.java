package org.springframework.roo.classpath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
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
 * @since 1.1
 */
@Component(immediate = true) 
@Service 
public class TypeLocationServiceImpl implements TypeLocationService, MetadataNotificationListener {
	@Reference private FileManager fileManager;
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ProjectOperations projectOperations;
	private Map<List<JavaType>, List<String>> cache = new HashMap<List<JavaType>, List<String>>();

	protected void activate(ComponentContext context) {
		dependencyRegistry.addNotificationListener(this);
	}

	protected void deactivate(ComponentContext context) {
		dependencyRegistry.removeNotificationListener(this);
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (upstreamDependency.startsWith("MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#")) {
			// Change to Java, so drop the cache
			cache.clear();
			/*
			PTM ptm = mdservice.get(upstream)
			
			List<Annotations> incomingAnnotations = ptm.getTypeAnnotatios()
			// adding incoming type to any searches it would match
			for (List<JavaType> annotationsSearchedFor : cache.keySet()) {
			     if (annotationsSearchedFor.contains(oneOfTheIncomingAnnotations) {
			          // edit that cache element so this ptm.getName() is part of the list
			     }
			}
			
			// removing the incoming type from any sarches it would no longer match on
			for (List<JavaType> annotationsSearchedFor : cache.keySet()) {
			     if (cache.get(annotationssarchedFor).contains(ptm.getName()) {
			          // found one we used to \match on
			          // remove me from that element if i no longer have that annotaiton
			     }
			}
			incomingType
			
			*/
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

	public void processTypesWithAnnotation(List<JavaType> annotationsToDetect, LocatedTypeCallback callback) {
		List<String> locatedPhysicalTypeMids = cache.get(annotationsToDetect);

		if (locatedPhysicalTypeMids == null) {
			locatedPhysicalTypeMids = new ArrayList<String>();
			PathResolver pathResolver = projectOperations.getPathResolver();
			FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
			String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";

			SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);
			for (FileDetails file : entries) {
				String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
				fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
				JavaType javaType;
				try {
					javaType = new JavaType(fullPath);
				} catch (RuntimeException e) { // ROO-1022
					continue;
				}

				String id = physicalTypeMetadataProvider.findIdentifier(javaType);
				if (id != null) {
					// Now I've found it, let's work out the Path it is from
					Path path = PhysicalTypeIdentifier.getPath(id);
					String physicalTypeMid = PhysicalTypeIdentifier.createIdentifier(javaType, path);
					ClassOrInterfaceTypeDetails located = getClassOrInterfaceTypeDetails(physicalTypeMid);
					if (located != null) {
						annotation: for (JavaType annotation : annotationsToDetect) {
							if (MemberFindingUtils.getTypeAnnotation(located, annotation) != null) {
								locatedPhysicalTypeMids.add(physicalTypeMid);
								break annotation;
							}
						}
					}
				}
			}

			// Store in cache if anything was found
			if (locatedPhysicalTypeMids.size() > 0) {
				cache.put(annotationsToDetect, locatedPhysicalTypeMids);
			}
		}

		for (String locatedPhysicalTypeMid : locatedPhysicalTypeMids) {
			ClassOrInterfaceTypeDetails located = getClassOrInterfaceTypeDetails(locatedPhysicalTypeMid);
			callback.process(located);
		}
	}

	public Set<JavaType> findTypesWithAnnotation(List<JavaType> annotationsToDetect) {
		final Set<JavaType> types = new HashSet<JavaType>();
		processTypesWithAnnotation(annotationsToDetect, new LocatedTypeCallback() {
			public void process(ClassOrInterfaceTypeDetails located) {
				types.add(located.getName());
			}
		});
		return types;
	}

	public Set<JavaType> findTypesWithAnnotation(JavaType... annotationsToDetect) {
		return findTypesWithAnnotation(Arrays.asList(annotationsToDetect));
	}

	public Set<ClassOrInterfaceTypeDetails> findClassesOrInterfaceDetailsWithAnnotation(JavaType... annotationsToDetect) {
		final Set<ClassOrInterfaceTypeDetails> types = new HashSet<ClassOrInterfaceTypeDetails>();
		processTypesWithAnnotation(Arrays.asList(annotationsToDetect), new LocatedTypeCallback() {
			public void process(ClassOrInterfaceTypeDetails located) {
				types.add(located);
			}
		});
		return types;
	}

	private ClassOrInterfaceTypeDetails getClassOrInterfaceTypeDetails(String physicalTypeMid) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeMid);
		if (physicalTypeMetadata != null && physicalTypeMetadata.getMemberHoldingTypeDetails() != null && physicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			return (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		}
		return null;
	}
}
