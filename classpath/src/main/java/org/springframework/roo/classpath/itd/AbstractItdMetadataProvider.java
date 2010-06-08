package org.springframework.roo.classpath.itd;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Provides common functionality used by ITD-based generators.
 * 
 * <p>
 * This abstract class assumes:
 * 
 * <ul>
 * <li>There are one or more annotations which, if present on a physical type, indicate metadata should be created (defined in {@link #metadataTriggers}</li>
 * <li>The default notification facilities offered by {@link MetadataProvider} are sufficient</li>
 * <li>The only class-level notifications that can be processed will related to {@link PhysicalTypeIdentifier}</li>
 * <li>Any instance-level notifications can be processed, but these must contain a downstream identifier consistent with {@link #getProvidesType()}</li>  
 * </ul>
 * 
 * <p>
 * Put differently, this abstract class assumes every ITD will have a corresponding "governor". A "governor" is defined as the type which will eventually receive
 * the introduction. The abstract class assumes all metadata identification strings represent the name of the governor, albeit with a metadata class specific
 * to the add-on. When an instance-specific metadata identification request is received, the governor will be obtained and in turn introspected for one of the
 * trigger annotations. If these are detected, or if there is already an ITD file of the same name as would normally be created had a trigger annotation been
 * found, the metadata will be created. The metadata creation method is expected to create, update or delete the ITD file as appropriate.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(componentAbstract=true)
public abstract class AbstractItdMetadataProvider implements ItdRoleAwareMetadataProvider, MetadataNotificationListener {

	@Reference protected MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference protected FileManager fileManager;
	@Reference protected MetadataService metadataService;

	private boolean dependsOnGovernorTypeDetailAvailability = true;
	private boolean dependsOnGovernorBeingAClass = true;
	private Set<ItdProviderRole> roles = new HashSet<ItdProviderRole>();

	/** The annotations which, if present on a class or interface, will cause metadata to be created */
	private List<JavaType> metadataTriggers = new ArrayList<JavaType>();
	
	/** We don't care about trigger annotations; we always produce metadata */
	private boolean ignoreTriggerAnnotations = false;

	private boolean isNotificationForJavaType(String mid) {
		return MetadataIdentificationUtils.getMetadataClass(mid).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType()));
	}
	
	public final void notify(String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency) && !isNotificationForJavaType(upstreamDependency)) {
			// This notification is NOT for a JavaType, and did NOT identify a specific downstream instance, so we must compute
			// all possible downstream dependencies by a disk scan.
			
			// Scan for every .java file in the project and create a MID specific to this subclass for each
			List<String> allMids = new ArrayList<String>();
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			PathResolver pathResolver = projectMetadata.getPathResolver();
			for (Path path : pathResolver.getSourcePaths()) {
				FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(path)), null);
				String antPath = pathResolver.getRoot(path) + File.separatorChar + "**" + File.separatorChar + "*.java";
				for (FileDetails fileDetails : fileManager.findMatchingAntPath(antPath)) {
					String fullPath = srcRoot.getRelativeSegment(fileDetails.getCanonicalPath());
					fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // ditch the first / and .java
					JavaType javaType = new JavaType(fullPath);
					String mid = createLocalIdentifier(javaType, path);
					allMids.add(mid);
				}
			}
			
			for (String mid : allMids) {
				metadataService.evict(mid);
				if (get(mid) != null) {
					metadataDependencyRegistry.notifyDownstream(mid);
				}
			}
	
			return;
		}
		
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(isNotificationForJavaType(upstreamDependency), "Expected class-level notifications only for physical Java types (not '" + upstreamDependency + "')");
			
			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType javaType = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
			Path path = PhysicalTypeIdentifier.getPath(upstreamDependency);
			downstreamDependency = createLocalIdentifier(javaType, path);
			
			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");
		
		metadataService.evict(downstreamDependency);
		if (get(downstreamDependency) != null) {
			metadataDependencyRegistry.notifyDownstream(downstreamDependency);
		}
	}

	/**
	 * Called whenever there is a requirement to produce a local identifier (ie an instance identifier consistent with
	 * {@link #getProvidesType()}) for the indicated {@link JavaType} and {@link Path}.
	 * 
	 * @param javaType the type (required)
	 * @param path the path (required)
	 * @return an instance-specific identifier that is compatible with {@link #getProvidesType()} (never null or empty)
	 */
	protected abstract String createLocalIdentifier(JavaType javaType, Path path);
	
	/**
	 * Called whenever there is a requirement to convert a local metadata identification string (ie an instance identifier
	 * consistent with {@link #getProvidesType()}) into the corresponding governor physical type identifier.
	 * 
	 * @param metadataIdentificationString the local identifier (required)
	 * @return the physical type identifier of the governor (required)
	 */
	protected abstract String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString);

	/**
	 * Called when it is time to create the actual metadata instance.
	 * 
	 * @param metadataIdentificationString the local identifier (non-null and consistent with {@link #getProvidesType()})
	 * @param aspectName the Java type name for the ITD (non-null and obtained via {@link PhysicalTypeMetadata#getItdJavaType(ItdMetadataProvider)})
	 * @param governorPhysicalTypeMetadata the governor metadata (non-null and obtained via {@link #getGovernorPhysicalTypeIdentifier(String)})
	 * @param itdFilename the canonical filename for the ITD (non-null and obtained via {@link PhysicalTypeMetadata#getItdCanoncialPath(ItdMetadataProvider)})
	 * @return the new metadata (may return null if there is a problem processing)
	 */
	protected abstract ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename);
	
	/**
	 * Registers an additional {@link JavaType} that will trigger metadata registration.
	 * 
	 * @param javaType the type-level annotation to detect that will cause metadata creation (required)
	 */
	public void addMetadataTrigger(JavaType javaType) {
		Assert.notNull(javaType, "Java type required for metadata trigger registration");
		this.metadataTriggers.add(javaType);
	}
	
	/**
	 * Removes a {@link JavaType} metadata trigger registration. If the type was never registered, the method returns without an error.
	 * 
	 * @param javaType to remove (required)
	 */
	public void removeMetadataTrigger(JavaType javaType) {
		Assert.notNull(javaType, "Java type required for metadata trigger deregistration");
		this.metadataTriggers.remove(javaType);
	}
	
	protected final void addProviderRole(ItdProviderRole role) {
		Assert.notNull(role, "Provider role required");
		this.roles.add(role);
	}
	
	protected final void removeProviderRole(ItdProviderRole role) {
		Assert.notNull(role, "Provider role required");
		this.roles.remove(role);
	}
	
	protected boolean isIgnoreTriggerAnnotations() {
		return ignoreTriggerAnnotations;
	}

	protected void setIgnoreTriggerAnnotations(boolean ignoreTriggerAnnotations) {
		this.ignoreTriggerAnnotations = ignoreTriggerAnnotations;
	}

	public final MetadataItem get(String metadataIdentificationString) {
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(metadataIdentificationString).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected request for '" + metadataIdentificationString + "' to this provider (which uses '" + getProvidesType() + "'");
		
		// Remove the upstream dependencies for this instance (we'll be recreating them later, if needed) 
		metadataDependencyRegistry.deregisterDependencies(metadataIdentificationString);
		
		// Compute the identifier for the Physical Type Metadata we're correlated with
		String governorPhysicalTypeIdentifier = getGovernorPhysicalTypeIdentifier(metadataIdentificationString);
		
		// Obtain the physical type
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(governorPhysicalTypeIdentifier);
		
		if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid()) {
			// We can't get even basic information about the physical type, so abort (the ITD will be deleted by ItdFileDeletionService)
			return null;
		}

		// Determine ITD details
		String itdFilename = governorPhysicalTypeMetadata.getItdCanoncialPath(this);
		JavaType aspectName = governorPhysicalTypeMetadata.getItdJavaType(this);

		// Flag to indicate whether we'll even try to create this metadata
		boolean produceMetadata = false;

		// Determine if we should generate the metadata on the basis of it containing a trigger annotation
		ClassOrInterfaceTypeDetails cid = null;
		if (governorPhysicalTypeMetadata.getPhysicalTypeDetails() != null && governorPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			cid = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		
			// Only create metadata if the type is annotated with one of the metadata triggers
			for (JavaType trigger : metadataTriggers) {
				if (MemberFindingUtils.getDeclaredTypeAnnotation(cid, trigger) != null) {
					produceMetadata = true;
					break;
				}
			}
		}
	
		// Fallback to ignoring trigger annotations
		if (ignoreTriggerAnnotations) {
			produceMetadata = true;
		}
		
		// Cancel production if the governor type details are required, but aren't available
		if (dependsOnGovernorTypeDetailAvailability && cid == null) {
			produceMetadata = false;
		}
		
		// Cancel production if the governor is not a class, and the subclass only wants to know about classes
		if (cid != null && dependsOnGovernorBeingAClass && cid.getPhysicalTypeCategory() != PhysicalTypeCategory.CLASS) {
			produceMetadata = false;
		}
		
		if (fileManager.exists(itdFilename) && !produceMetadata) {
			// We don't seem to want metadata anymore, yet the ITD physically exists, so get rid of it
			// This might be because the trigger annotation has been removed, the governor is missing a class declaration etc
			fileManager.delete(itdFilename);
		}
		
		if (produceMetadata) {
			// This type contains an annotation we were configured to detect, or there is an ITD (which may need deletion), so we need to produce the metadata
			ItdTypeDetailsProvidingMetadataItem metadata;
			metadata = getMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, itdFilename);
			
			// Register a direct connection between the physical type and this metadata
			// (this is needed so changes to the inheritance hierarchies are eventually notified to us)
			metadataDependencyRegistry.registerDependency(governorPhysicalTypeMetadata.getId(), metadataIdentificationString);
			
			// Quit if the subclass returned null; it might not have experienced issues parsing etc
			if (metadata == null) {
				return null;
			}
			
			// Handle the management of the ITD file
			if (metadata.getItdTypeDetails() != null) {
				// Construct the source file composer
				ItdSourceFileComposer itdSourceFileComposer = new ItdSourceFileComposer(metadata.getItdTypeDetails());

				// Output the ITD if there is actual content involved
				// (if there is no content, we continue on to the deletion phase at the bottom of this conditional block)
				if (itdSourceFileComposer.isContent()) {
					String itd = itdSourceFileComposer.getOutput();
					
					fileManager.createOrUpdateTextFileIfRequired(itdFilename, itd);
					// Important to exit here, so we don't proceed onto the delete operation below
					// (as we have a valid ITD that has been written out by now)
					return metadata;
				} 

			}

			// Delete the ITD if we determine deletion is appropriate
			if (metadata.isValid() && fileManager.exists(itdFilename)) {
				fileManager.delete(itdFilename);
			}
			
			return metadata;
		}
		
		return null;
	}

	public final String getIdForPhysicalJavaType(String physicalJavaTypeIdentifier) {
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(physicalJavaTypeIdentifier).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType())), "Expected a valid physical Java type instance identifier (not '" + physicalJavaTypeIdentifier + "')");
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalJavaTypeIdentifier);
		Path path = PhysicalTypeIdentifier.getPath(physicalJavaTypeIdentifier);
		return createLocalIdentifier(javaType, path);
	}

	/**
	 * If set to true (default is true), ensures subclass not called unless the governor type details are available.
	 * 
	 * @param dependsOnGovernorTypeDetailAvailability true means governor type details must be available
	 */
	public void setDependsOnGovernorTypeDetailAvailability(boolean dependsOnGovernorTypeDetailAvailability) {
		this.dependsOnGovernorTypeDetailAvailability = dependsOnGovernorTypeDetailAvailability;
	}

	/**
	 * If set to true (default is true), ensures the governor type details represent a class. Note that 
	 * {@link #setDependsOnGovernorTypeDetailAvailability(boolean)} must also be true to ensure this can be relied upon. 
	 * 
	 * @param dependsOnGovernorBeingAClass true means governor type detail must represent a class
	 */
	public void setDependsOnGovernorBeingAClass(boolean dependsOnGovernorBeingAClass) {
		this.dependsOnGovernorBeingAClass = dependsOnGovernorBeingAClass;
	}

	public final Set<ItdProviderRole> getRoles() {
		return Collections.unmodifiableSet(this.roles);
	}

}