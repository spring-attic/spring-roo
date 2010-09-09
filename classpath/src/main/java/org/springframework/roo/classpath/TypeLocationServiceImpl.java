package org.springframework.roo.classpath;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

/**
 * Implementation of {@link TypeLocationService}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class TypeLocationServiceImpl implements TypeLocationService {
	@Reference private PathResolver pathResolver;
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;

	public void processTypesWithAnnotation(List<JavaType> annotationsToDetect, LocatedTypeCallback callback) {
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);
		for (FileDetails file : entries) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			try {
				javaType = new JavaType(fullPath);
			} catch (RuntimeException e) { // ROO-1022
				continue;
			}
			
			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id != null) {
				// Now I've found it, let's work out the Path it is from
				Path path = PhysicalTypeIdentifier.getPath(id);
				ClassOrInterfaceTypeDetails located = getClassOrInterfaceTypeDetails(javaType, path);
				if (located != null) {
					for (JavaType annotation : annotationsToDetect) {
						if (MemberFindingUtils.getTypeAnnotation(located, annotation) != null) {
							callback.process(located);
						}
					}
				}
			}
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
	
	private ClassOrInterfaceTypeDetails getClassOrInterfaceTypeDetails(JavaType javaType, Path path) {
		String physicalTypeMid = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeMid);
		if (physicalTypeMetadata != null && physicalTypeMetadata.getPhysicalTypeDetails() != null && physicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			return (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getPhysicalTypeDetails();
		}
		return null;
	}
}
