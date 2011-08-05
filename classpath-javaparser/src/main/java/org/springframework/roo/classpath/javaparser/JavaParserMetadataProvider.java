package org.springframework.roo.classpath.javaparser;

import japa.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.MutablePhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ClasspathProvidingProjectMetadata;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Monitors for *.java files and produces a {@link PhysicalTypeMetadata} for each,
 * also providing type creation and deleting methods.
 * 
 * <p>
 * This implementation does not support {@link ClasspathProvidingProjectMetadata}. Whilst the
 * project metadata may implement this interface, the {@link #findIdentifier(JavaType)} will ignore
 * such paths in the current release.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true) 
@Service
public class JavaParserMetadataProvider implements MutablePhysicalTypeMetadataProvider, FileEventListener {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private ProjectOperations projectOperations;
	private Map<JavaType, String> cache = new HashMap<JavaType, String>();

	public String getProvidesType() {
		return PhysicalTypeIdentifier.getMetadataIdentiferType();
	}
	
	private PathResolver getPathResolver() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata unavailable");
		return projectOperations.getPathResolver();
	}
	
	public String findIdentifier(JavaType javaType) {
		Assert.notNull(javaType, "Java type to locate is required");
		String result = cache.get(javaType);
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
				cache.put(javaType, mid);
				return mid;
			}
		}
		return null;
	}

	public void createPhysicalType(PhysicalTypeMetadata toCreate) {
		Assert.notNull(toCreate, "Metadata to create is required");
		PhysicalTypeDetails physicalTypeDetails = toCreate.getMemberHoldingTypeDetails();
		Assert.notNull(physicalTypeDetails, "Unable to parse '" + toCreate + "'");
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, physicalTypeDetails, "This implementation can only create class or interface types");
		ClassOrInterfaceTypeDetails cit = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
		String fileIdentifier = toCreate.getPhysicalLocationCanonicalPath();
		JavaParserMutableClassOrInterfaceTypeDetails.createOrUpdateTypeOnDisk(fileManager, cit, fileIdentifier);
	}

	public void onFileEvent(FileEvent fileEvent) {
		String fileIdentifier = fileEvent.getFileDetails().getCanonicalPath();
		
		if (fileIdentifier.endsWith(".java") && fileEvent.getOperation() != FileOperation.MONITORING_FINISH && !fileIdentifier.endsWith("package-info.java")) {
			// File is of interest
			// Start by evicting the cache
			cache.clear();
			
			// Figure out the JavaType this should be
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
				return;
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
			String id = PhysicalTypeIdentifier.createIdentifier(javaType, sourcePath);
			
			// Now we've worked out the id, we can publish the event in case others were interested
			metadataService.evict(id);
			metadataDependencyRegistry.notifyDownstream(id);
		}
	}
	
	private String obtainPathToIdentifier(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Metadata identification string '" + physicalTypeIdentifier + "' is not valid for this metadata provider");
		Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		JavaType type = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
		PathResolver pathResolver = getPathResolver();
		String relativePath = type.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return pathResolver.getIdentifier(path, relativePath);
	}

	public MetadataItem get(String metadataIdentificationString) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' is not valid for this metadata provider");
		String fileIdentifier = obtainPathToIdentifier(metadataIdentificationString);
		metadataDependencyRegistry.deregisterDependencies(metadataIdentificationString);
		if (!fileManager.exists(fileIdentifier)) {
			// Couldn't find the file, so return null to distinguish from a file that was found but could not be parsed
			return null;
		}
		JavaParserClassMetadata result = new JavaParserClassMetadata(fileManager, fileIdentifier, metadataIdentificationString, metadataService, this);
		if (result.getMemberHoldingTypeDetails() != null && result.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			ClassOrInterfaceTypeDetails details = (ClassOrInterfaceTypeDetails) result.getMemberHoldingTypeDetails();
			if (details.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS && details.getExtendsTypes().size() == 1) {
				// This is a class, and it extends another class
				if (details.getSuperclass() != null) {
					// We have a dependency on the superclass, and there is metadata available for the superclass
					// We won't implement the full MetadataNotificationListener here, but rely on MetadataService's fallback
					// (which is to evict from cache and call get again given JavaParserMetadataProvider doesn't implement MetadataNotificationListener, then notify everyone we've changed)
					String superclassId = details.getSuperclass().getDeclaredByMetadataId();
					metadataDependencyRegistry.registerDependency(superclassId, result.getId());
				} else {
					// We have a dependency on the superclass, but no metadata is available
					// We're left with no choice but to register for every physical type change, in the hope we discover our parent someday (sad, isn't it? :-) )
					for (Path sourcePath : getPathResolver().getSourcePaths()) {
						String possibleSuperclass = PhysicalTypeIdentifier.createIdentifier(details.getExtendsTypes().get(0), sourcePath);
						metadataDependencyRegistry.registerDependency(possibleSuperclass, result.getId());
					}
				}
			}
		}
		
		return result;
	}

	public String getCompilationUnitContents(ClassOrInterfaceTypeDetails cit) {
		Assert.notNull(cit, "Class or interface type details are required");
		return JavaParserMutableClassOrInterfaceTypeDetails.getCompilationUnitContents(cit);
	}

	public ClassOrInterfaceTypeDetails parse(String compilationUnit, String declaredByMetadataId, JavaType javaType) {
		Assert.hasText(compilationUnit, "Compilation unit required");
		Assert.hasText(declaredByMetadataId, "Declaring metadata ID required");
		Assert.notNull(javaType, "Java type to locate required");
		try {
			return new JavaParserClassOrInterfaceTypeDetails(compilationUnit, declaredByMetadataId, javaType, metadataService, this);
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		} catch (CloneNotSupportedException cne) {
			throw new IllegalStateException(cne);
		} catch (ParseException pe) {
			throw new IllegalStateException(pe);
		}
	}
}
