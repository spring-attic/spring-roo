package org.springframework.roo.classpath.itd;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
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
 */
@Component(componentAbstract = true)
public abstract class AbstractItdMetadataProvider extends AbstractHashCodeTrackingMetadataNotifier implements ItdMetadataProvider, MetadataNotificationListener {
	@Reference protected FileManager fileManager;
	@Reference protected MemberDetailsScanner memberDetailsScanner;
	
	/** Cancel production if the governor type details are required, but aren't available */
	private boolean dependsOnGovernorTypeDetailAvailability = true;
	
	/** Requires the governor to be a {@link PhysicalTypeCategory#CLASS} (as opposed to an interface etc) */
	private boolean dependsOnGovernorBeingAClass = true;
	
	/** The annotations which, if present on a class or interface, will cause metadata to be created */
	private List<JavaType> metadataTriggers = new ArrayList<JavaType>();
	
	/** We don't care about trigger annotations; we always produce metadata */
	private boolean ignoreTriggerAnnotations = false;
	
	private boolean isNotificationForJavaType(String mid) {
		return MetadataIdentificationUtils.getMetadataClass(mid).equals(MetadataIdentificationUtils.getMetadataClass(PhysicalTypeIdentifier.getMetadataIdentiferType()));
	}
	
	/**
	 * Designed to handle events originating from a {@link MetadataDependencyRegistry#addNotificationListener(MetadataNotificationListener)}
	 * registration. Such events are always presented with a non-null upstream dependency indicator and a null downstream dependency
	 * indicator. These events differ from events related to {@link PhysicalTypeIdentifier} registrations, as in those cases the downstream
	 * dependency indicator will be the class-level {@link #getProvidesType()}.
	 * 
	 * <p>
	 * This method allows subclasses to specially handle generic {@link MetadataDependencyRegistry} events.
	 * 
	 * @param upstreamDependency the upstream which was modified (guaranteed to be non-null, but could be class-level or instance-level)
	 */
	protected void notifyForGenericListener(String upstreamDependency) {}
	
	/**
	 * Invoked whenever a "class-level" downstream dependency identifier is presented in a metadata notification. An "instance-specific"
	 * downstream dependency identifier is required so that a metadata request can ultimately be made. This method is responsible for
	 * evaluating the upstream dependency identifier and converting it into a valid downstream dependency identifier. The downstream
	 * dependency identifier must be of the same type as this metadata provider's {@link #getProvidesType()}. The downstream dependency
	 * identifier must also be instance-specific. 
	 * 
	 * <p>
	 * The basic implementation offered in this class will only convert a {@link PhysicalTypeIdentifier}. If a subclass registers
	 * a dependency on an upstream (other than {@link PhysicalTypeIdentifier#getMetadataIdentiferType()}) and presents their
	 * {@link #getProvidesType()} as the downstream (thus meaning only class-level downstream dependency identifiers will be presented),
	 * they must override this method and appropriately handle instance-specific downstream dependency identifier resolution.
	 * 
	 * <p>
	 * This method may also return null if it wishes to abort processing of the notification. This may be appropriate if a determination
	 * cannot be made at this time for whatever reason (eg too early in a lifecycle etc).
	 * 
	 * @param upstreamDependency the upstream (never null)
	 * @return an instance-specific MID of type {@link #getProvidesType()} (or null if the metadata notification should be aborted)
	 */
	protected String resolveDownstreamDependencyIdentifier(String upstreamDependency) {
		// We only support analysis of a PhysicalTypeIdentifier upstream MID to convert this to a downstream MID.
		// In any other case the downstream metadata should have registered an instance-specific downstream dependency on a given upstream.
		Assert.isTrue(isNotificationForJavaType(upstreamDependency), "Expected class-level notifications only for physical Java types (not '" + upstreamDependency + "') for metadata provider " + getClass().getName());
		
		// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(upstreamDependency);
		Path path = PhysicalTypeIdentifier.getPath(upstreamDependency);
		return createLocalIdentifier(javaType, path);
	}

	public final void notify(String upstreamDependency, String downstreamDependency) {
		if (downstreamDependency == null) {
			notifyForGenericListener(upstreamDependency);
			return;
		}
		
		// Handle if the downstream dependency is "class level",  meaning we need to figure out the specific downstream MID this metadata provider wants to update/refresh.
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			// We have not identified an instance-specific downstream MID, so we'll need to calculate an instance-specific downstream MID to retrieve.
			downstreamDependency = resolveDownstreamDependencyIdentifier(upstreamDependency);
			
			// We skip if the resolution method returns null, as it doesn't want to continue for some reason
			if (downstreamDependency == null) {
				return;
			}
			
			Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(downstreamDependency), "An instance-specific downstream MID was required by '" + getClass().getName() + "' (not '" + downstreamDependency + "')");
			
			// We only need to proceed if the downstream dependency relationship is not already registered.
			// It is unusual to register a direct downstream relationship given it costs dependency registration memory and class-level notifications will always occur anyway.
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");
		
		// We no longer notify downstreams here, as the "get" operation with eviction will ensure the main get(String) method below will be fired and it
		// directly notified downstreams as part of that method (BPA 10 Dec 2010)
		metadataService.get(downstreamDependency, true);
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
		if (governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() != null && governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			cid = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		
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
		
		if (!produceMetadata && fileManager.exists(itdFilename)) {
			// We don't seem to want metadata anymore, yet the ITD physically exists, so get rid of it
			// This might be because the trigger annotation has been removed, the governor is missing a class declaration etc
			// TODO: Overload fileManager.delete(..) so we can give a message so the console output is more meaningful
			fileManager.delete(itdFilename);
		}
		
		if (produceMetadata) {
			// This type contains an annotation we were configured to detect, or there is an ITD (which may need deletion), so we need to produce the metadata
			ItdTypeDetailsProvidingMetadataItem metadata = getMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, itdFilename);
			
			// There is no requirement to register a direct connection with the physical type and this metadata because changes will
			// trickle down via the class-level notification registered by convention by AbstractItdMetadataProvider subclasses (BPA 10 Dec 2010)
			
			// Quit if the subclass returned null or a metadata item they're not happy with; it might have experienced issues parsing etc
			if (metadata == null || !metadata.isValid()) {
				return null;
			}
			
			// By this point we have a valid MetadataItem, but it might not contain any members for the resulting ITD etc

			// Handle the management of the ITD file
			boolean deleteItdFile = false;
			
			if (metadata.getMemberHoldingTypeDetails() == null) {
				// We have no members in this ITD, so its on-disk existence falls into question... :-)
				// Exterminate it.
				deleteItdFile = true;
			}
			
			if (!deleteItdFile) {
				// We have some members in the ITD, so decide if we're to write something to disk
				ItdSourceFileComposer itdSourceFileComposer = new ItdSourceFileComposer(metadata.getMemberHoldingTypeDetails());
				
				// Decide whether the get an ITD on-disk based on whether there is physical content to write
				if (itdSourceFileComposer.isContent()) {
					// We have content to write
					String itd = itdSourceFileComposer.getOutput();
					
					fileManager.createOrUpdateTextFileIfRequired(itdFilename, itd, false);
				} else {
					// We don't have content to write
					deleteItdFile = true;
				}
			}
			
			if (deleteItdFile) {
				// Notice we use the createOrUpdateTextFileIfRequired method, as a zero byte payload will delete the file.
				// This is better than calling fileManager.delete(..) in this case, as we can ensure if a subsequent update happens
				// in the same process manager operation it will not physically delete the file or display console messages to that effect.
				// We also need not perform an FileManager.exists(..) call, which is more efficient as it might update several times.
				fileManager.createOrUpdateTextFileIfRequired(itdFilename, "", false);
			}
			
			// Eagerly notify that the metadata has been updated; this also registers the metadata hash code in the superclass' cache to avoid
			// unnecessary subsequent notifications if it hasn't changed
			notifyIfRequired(metadata);
			
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
	
	/**
	 * Assists creating a local metadata identification string (MID) from any presented {@link MemberHoldingTypeDetails} implementation.
	 * This is achieved by extracting the {@link IdentifiableJavaStructure#getDeclaredByMetadataId()} and converting it into a {@link JavaType}
	 * and {@link Path}, then calling {@link #createLocalIdentifier(JavaType, Path)}.
	 * 
	 * @param memberHoldingTypeDetails the member holder from which the declaring type information should be extracted (required)
	 * @return a MID produced by {@link #createLocalIdentifier(JavaType, Path)} for the extracted Java type in the extract Path (never null)
	 */
	protected String getLocalMid(MemberHoldingTypeDetails memberHoldingTypeDetails) {
		JavaType governorType = memberHoldingTypeDetails.getName();
		
		// Extract out the metadata provider class (we need this later to extract just the Path it is located in)
		String providesType = MetadataIdentificationUtils.getMetadataClass(memberHoldingTypeDetails.getDeclaredByMetadataId());
		Path path = PhysicalTypeIdentifierNamingUtils.getPath(providesType, memberHoldingTypeDetails.getDeclaredByMetadataId());
		
		// Produce the local MID we're going to use to make the request
		return createLocalIdentifier(governorType, path);
	}

	protected MemberDetails getMemberDetails(JavaType type) {
		// We need to lookup the metadata we depend on
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		return getMemberDetails(physicalTypeMetadata);
	}

	protected MemberDetails getMemberDetails(PhysicalTypeMetadata physicalTypeMetadata) {
		// We need to abort if we couldn't find dependent metadata
		if (physicalTypeMetadata == null || !physicalTypeMetadata.isValid()) {
			return null;
		}

		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (classOrInterfaceTypeDetails == null) {
			// Abort if the type's class details aren't available (parse error etc)
			return null;
		}
		return memberDetailsScanner.getMemberDetails(getClass().getName(), classOrInterfaceTypeDetails);
	}
}