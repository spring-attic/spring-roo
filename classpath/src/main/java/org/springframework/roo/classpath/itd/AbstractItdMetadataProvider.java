package org.springframework.roo.classpath.itd;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.ItdDiscoveryService;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
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
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;

/**
 * Provides common functionality used by ITD-based generators.
 * <p>
 * This abstract class assumes:
 * <ul>
 * <li>There are one or more annotations which, if present on a physical type,
 * indicate metadata should be created (defined in {@link #metadataTriggers}</li>
 * <li>The default notification facilities offered by {@link MetadataProvider}
 * are sufficient</li>
 * <li>The only class-level notifications that can be processed will related to
 * {@link PhysicalTypeIdentifier}</li>
 * <li>Any instance-level notifications can be processed, but these must contain
 * a downstream identifier consistent with {@link #getProvidesType()}</li>
 * </ul>
 * <p>
 * Put differently, this abstract class assumes every ITD will have a
 * corresponding "governor". A "governor" is defined as the type which will
 * eventually receive the introduction. The abstract class assumes all metadata
 * identification strings represent the name of the governor, albeit with a
 * metadata class specific to the add-on. When an instance-specific metadata
 * identification request is received, the governor will be obtained and in turn
 * introspected for one of the trigger annotations. If these are detected, or if
 * there is already an ITD file of the same name as would normally be created
 * had a trigger annotation been found, the metadata will be created. The
 * metadata creation method is expected to create, update or delete the ITD file
 * as appropriate.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(componentAbstract = true)
public abstract class AbstractItdMetadataProvider extends
        AbstractHashCodeTrackingMetadataNotifier implements
        ItdTriggerBasedMetadataProvider, MetadataNotificationListener {

    /**
     * Requires the governor to be a {@link PhysicalTypeCategory#CLASS} (as
     * opposed to an interface etc)
     */
    // TODO change the type of this field to PhysicalTypeCategory and allow
    // subclasses to pass it via a new constructor
    private boolean dependsOnGovernorBeingAClass = true;
    /**
     * Cancel production if the governor type details are required, but aren't
     * available
     */
    private boolean dependsOnGovernorTypeDetailAvailability = true;
    @Reference protected FileManager fileManager;
    /** We don't care about trigger annotations; we always produce metadata */
    private boolean ignoreTriggerAnnotations = false;
    @Reference protected ItdDiscoveryService itdDiscoveryService;

    @Reference protected MemberDetailsScanner memberDetailsScanner;

    /**
     * The annotations which, if present on a class or interface, will cause
     * metadata to be created
     */
    private final List<JavaType> metadataTriggers = new ArrayList<JavaType>();

    @Reference protected PersistenceMemberLocator persistenceMemberLocator;

    @Reference protected TypeLocationService typeLocationService;

    /**
     * Registers an additional {@link JavaType} that will trigger metadata
     * registration.
     * 
     * @param javaType the type-level annotation to detect that will cause
     *            metadata creation (required)
     */
    public void addMetadataTrigger(final JavaType javaType) {
        Validate.notNull(javaType,
                "Java type required for metadata trigger registration");
        metadataTriggers.add(javaType);
    }

    /**
     * Registers the given {@link JavaType}s as triggering metadata
     * registration.
     * 
     * @param triggerTypes the type-level annotations to detect that will cause
     *            metadata creation
     * @since 1.2.0
     */
    public void addMetadataTriggers(final JavaType... triggerTypes) {
        for (final JavaType triggerType : triggerTypes) {
            addMetadataTrigger(triggerType);
        }
    }

    /**
     * Called whenever there is a requirement to produce a local identifier (ie
     * an instance identifier consistent with {@link #getProvidesType()}) for
     * the indicated {@link JavaType} and {@link Path}.
     * 
     * @param javaType the type (required)
     * @param path the path (required)
     * @return an instance-specific identifier that is compatible with
     *         {@link #getProvidesType()} (never null or empty)
     */
    protected abstract String createLocalIdentifier(JavaType javaType,
            LogicalPath path);

    /**
     * Deletes the given ITD, either now or later.
     * 
     * @param metadataIdentificationString the ITD's metadata ID
     * @param itdFilename the ITD's filename
     * @param reason the reason for deletion; ignored if now is
     *            <code>false</code>
     * @param now whether to delete the ITD immediately; <code>false</code>
     *            schedules it for later deletion; this is preferable when it's
     *            possible that the ITD might need to be re-created in the
     *            meantime (e.g. because some ancestor metadata has changed to
     *            that effect), otherwise there will be spurious console
     *            messages about the ITD being deleted and created
     */
    private void deleteItd(final String metadataIdentificationString,
            final String itdFilename, final String reason, final boolean now) {
        if (now) {
            fileManager.delete(itdFilename, reason);
        }
        else {
            fileManager
                    .createOrUpdateTextFileIfRequired(itdFilename, "", false);
        }
        itdDiscoveryService.removeItdTypeDetails(metadataIdentificationString);
        // TODO do we need to notify downstream dependencies that this ITD has
        // gone away?
    }

    public final MetadataItem get(final String metadataIdentificationString) {
        Validate.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        metadataIdentificationString).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected request for '%s' to this provider (which uses '%s')",
                metadataIdentificationString, getProvidesType());

        // Remove the upstream dependencies for this instance (we'll be
        // recreating them later, if needed)
        metadataDependencyRegistry
                .deregisterDependencies(metadataIdentificationString);

        // Compute the identifier for the Physical Type Metadata we're
        // correlated with
        final String governorPhysicalTypeIdentifier = getGovernorPhysicalTypeIdentifier(metadataIdentificationString);

        // Obtain the physical type
        final PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService
                .get(governorPhysicalTypeIdentifier);
        if (governorPhysicalTypeMetadata == null
                || !governorPhysicalTypeMetadata.isValid()) {
            // We can't get even basic information about the physical type, so
            // abort (the ITD will be deleted by ItdFileDeletionService)
            return null;
        }

        // Flag to indicate whether we'll even try to create this metadata
        boolean produceMetadata = false;

        // Determine if we should generate the metadata on the basis of it
        // containing a trigger annotation
        final ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (cid != null) {
            // Only create metadata if the type is annotated with one of the
            // metadata triggers
            for (final JavaType trigger : metadataTriggers) {
                if (cid.getAnnotation(trigger) != null) {
                    produceMetadata = true;
                    break;
                }
            }
        }

        // Fall back to ignoring trigger annotations
        if (ignoreTriggerAnnotations) {
            produceMetadata = true;
        }

        // Cancel production if the governor type details are required, but
        // aren't available
        if (dependsOnGovernorTypeDetailAvailability && cid == null) {
            produceMetadata = false;
        }

        // Cancel production if the governor is not a class, and the subclass
        // only wants to know about classes
        if (cid != null && dependsOnGovernorBeingAClass
                && cid.getPhysicalTypeCategory() != PhysicalTypeCategory.CLASS) {
            produceMetadata = false;
        }

        final String itdFilename = governorPhysicalTypeMetadata
                .getItdCanonicalPath(this);
        if (!produceMetadata && isGovernor(cid)
                && fileManager.exists(itdFilename)) {
            // We don't seem to want metadata anymore, yet the ITD physically
            // exists, so get rid of it
            // This might be because the trigger annotation has been removed,
            // the governor is missing a class declaration, etc.
            deleteItd(metadataIdentificationString, itdFilename,
                    "not required for governor " + cid.getName(), true);
            return null;
        }

        if (produceMetadata) {
            // This type contains an annotation we were configured to detect, or
            // there is an ITD (which may need deletion), so we need to produce
            // the metadata
            final JavaType aspectName = governorPhysicalTypeMetadata
                    .getItdJavaType(this);
            final ItdTypeDetailsProvidingMetadataItem metadata = getMetadata(
                    metadataIdentificationString, aspectName,
                    governorPhysicalTypeMetadata, itdFilename);

            // There is no requirement to register a direct connection with the
            // physical type and this metadata because changes will
            // trickle down via the class-level notification registered by
            // convention by AbstractItdMetadataProvider subclasses (BPA 10 Dec
            // 2010)

            if (metadata == null || !metadata.isValid()) {
                // The metadata couldn't be created properly
                deleteItd(metadataIdentificationString, itdFilename, "", false);
                return null;
            }

            // By this point we have a valid MetadataItem, but it might not
            // contain any members for the resulting ITD etc

            // Handle the management of the ITD file
            boolean deleteItdFile = false;
            final ItdTypeDetails itdTypeDetails = metadata
                    .getMemberHoldingTypeDetails();

            if (itdTypeDetails == null) {
                // The ITD has no members
                deleteItdFile = true;
            }

            if (!deleteItdFile) {
                // We have some members in the ITD, so decide if we're to write
                // something to disk
                final ItdSourceFileComposer itdSourceFileComposer = new ItdSourceFileComposer(
                        metadata.getMemberHoldingTypeDetails());

                // Decide whether the get an ITD on-disk based on whether there
                // is physical content to write
                if (itdSourceFileComposer.isContent()) {
                    // We have content to write
                    itdDiscoveryService.addItdTypeDetails(itdTypeDetails);
                    final String itd = itdSourceFileComposer.getOutput();
                    fileManager.createOrUpdateTextFileIfRequired(itdFilename,
                            itd, false);
                }
                else {
                    // We don't have content to write
                    deleteItdFile = true;
                }
            }

            if (deleteItdFile) {
                deleteItd(metadataIdentificationString, itdFilename, null,
                        false);
            }

            // Eagerly notify that the metadata has been updated; this also
            // registers the metadata hash code in the superclass' cache to
            // avoid
            // unnecessary subsequent notifications if it hasn't changed
            notifyIfRequired(metadata);

            return metadata;
        }
        return null;
    }

    /**
     * Called whenever there is a requirement to convert a local metadata
     * identification string (ie an instance identifier consistent with
     * {@link #getProvidesType()}) into the corresponding governor physical type
     * identifier.
     * 
     * @param metadataIdentificationString the local identifier (required)
     * @return the physical type identifier of the governor (required)
     */
    protected abstract String getGovernorPhysicalTypeIdentifier(
            String metadataIdentificationString);

    public final String getIdForPhysicalJavaType(
            final String physicalJavaTypeIdentifier) {
        Validate.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        physicalJavaTypeIdentifier).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(PhysicalTypeIdentifier
                                        .getMetadataIdentiferType())),
                "Expected a valid physical Java type instance identifier (not '%s')",
                physicalJavaTypeIdentifier);
        final JavaType javaType = PhysicalTypeIdentifier
                .getJavaType(physicalJavaTypeIdentifier);
        final LogicalPath path = PhysicalTypeIdentifier
                .getPath(physicalJavaTypeIdentifier);
        return createLocalIdentifier(javaType, path);
    }

    /**
     * Assists creating a local metadata identification string (MID) from any
     * presented {@link MemberHoldingTypeDetails} implementation. This is
     * achieved by extracting the
     * {@link IdentifiableJavaStructure#getDeclaredByMetadataId()} and
     * converting it into a {@link JavaType} and {@link Path}, then calling
     * {@link #createLocalIdentifier(JavaType, Path)}.
     * 
     * @param memberHoldingTypeDetails the member holder from which the
     *            declaring type information should be extracted (required)
     * @return a MID produced by {@link #createLocalIdentifier(JavaType, Path)}
     *         for the extracted Java type in the extract Path (never null)
     */
    protected String getLocalMid(
            final MemberHoldingTypeDetails memberHoldingTypeDetails) {
        final JavaType governorType = memberHoldingTypeDetails.getName();

        // Extract out the metadata provider class (we need this later to
        // extract just the Path it is located in)
        final String providesType = MetadataIdentificationUtils
                .getMetadataClass(memberHoldingTypeDetails
                        .getDeclaredByMetadataId());
        final LogicalPath path = PhysicalTypeIdentifierNamingUtils.getPath(
                providesType,
                memberHoldingTypeDetails.getDeclaredByMetadataId());
        // Produce the local MID we're going to use to make the request
        return createLocalIdentifier(governorType, path);
    }

    /**
     * Returns details of the given class or interface type's members
     * 
     * @param cid the physical type for which to get the members (can be
     *            <code>null</code>)
     * @return <code>null</code> if the member details are unavailable
     */
    protected MemberDetails getMemberDetails(
            final ClassOrInterfaceTypeDetails cid) {
        if (cid == null) {
            return null;
        }
        return memberDetailsScanner.getMemberDetails(getClass().getName(), cid);
    }

    /**
     * Returns details of the given Java type's members
     * 
     * @param type the type for which to get the members (required)
     * @return <code>null</code> if the member details are unavailable
     */
    protected MemberDetails getMemberDetails(final JavaType type) {
        final String physicalTypeIdentifier = typeLocationService
                .getPhysicalTypeIdentifier(type);
        if (physicalTypeIdentifier == null) {
            return null;
        }
        // We need to lookup the metadata we depend on
        final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService
                .get(physicalTypeIdentifier);
        return getMemberDetails(physicalTypeMetadata);
    }

    /**
     * Returns details of the given physical type's members
     * 
     * @param physicalTypeMetadata the physical type for which to get the
     *            members (can be <code>null</code>)
     * @return <code>null</code> if the member details are unavailable
     */
    protected MemberDetails getMemberDetails(
            final PhysicalTypeMetadata physicalTypeMetadata) {
        // We need to abort if we couldn't find dependent metadata
        if (physicalTypeMetadata == null || !physicalTypeMetadata.isValid()) {
            return null;
        }

        final ClassOrInterfaceTypeDetails cid = physicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (cid == null) {
            // Abort if the type's class details aren't available (parse error
            // etc)
            return null;
        }
        return memberDetailsScanner.getMemberDetails(getClass().getName(), cid);
    }

    /**
     * Called when it is time to create the actual metadata instance.
     * 
     * @param metadataIdentificationString the local identifier (non-null and
     *            consistent with {@link #getProvidesType()})
     * @param aspectName the Java type name for the ITD (non-null and obtained
     *            via
     *            {@link PhysicalTypeMetadata#getItdJavaType(ItdMetadataProvider)}
     *            )
     * @param governorPhysicalTypeMetadata the governor metadata (non-null and
     *            obtained via
     *            {@link #getGovernorPhysicalTypeIdentifier(String)})
     * @param itdFilename the canonical filename for the ITD (non-null and
     *            obtained via
     *            {@link PhysicalTypeMetadata#getItdCanoncialPath(ItdMetadataProvider)}
     *            )
     * @return the new metadata (may return null if there is a problem
     *         processing)
     */
    protected abstract ItdTypeDetailsProvidingMetadataItem getMetadata(
            String metadataIdentificationString, JavaType aspectName,
            PhysicalTypeMetadata governorPhysicalTypeMetadata,
            String itdFilename);

    /**
     * Looks up the given type's inheritance hierarchy for metadata of the given
     * type, starting with the given type's parent and going upwards until the
     * first such instance is found (i.e. lower level metadata takes priority
     * over higher level metadata)
     * 
     * @param <T> the type of metadata to look for
     * @param child the child type whose parents to search (required)
     * @return <code>null</code> if there is no such metadata
     */
    @SuppressWarnings("unchecked")
    protected <T extends MetadataItem> T getParentMetadata(
            final ClassOrInterfaceTypeDetails child) {
        T parentMetadata = null;
        ClassOrInterfaceTypeDetails superCid = child.getSuperclass();
        while (parentMetadata == null && superCid != null) {
            final String superCidPhysicalTypeIdentifier = superCid
                    .getDeclaredByMetadataId();
            final LogicalPath path = PhysicalTypeIdentifier
                    .getPath(superCidPhysicalTypeIdentifier);
            final String superCidLocalIdentifier = createLocalIdentifier(
                    superCid.getName(), path);
            parentMetadata = (T) metadataService.get(superCidLocalIdentifier);
            superCid = superCid.getSuperclass();
        }
        return parentMetadata; // Could be null
    }

    /**
     * Indicates whether the given type is the governor for this provider. This
     * implementation simply checks whether the given type is either a class or
     * an interface, based on the value of {@link #dependsOnGovernorBeingAClass}
     * . A more sophisticated implementation could check for the presence of
     * particular annotations or the implementation of particular interfaces.
     * 
     * @param type can be <code>null</code>
     * @return <code>false</code> if the given type is <code>null</code>
     */
    protected boolean isGovernor(final ClassOrInterfaceTypeDetails type) {
        if (type == null) {
            return false;
        }
        if (dependsOnGovernorBeingAClass) {
            return type.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS;
        }
        return type.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE;
    }

    protected boolean isIgnoreTriggerAnnotations() {
        return ignoreTriggerAnnotations;
    }

    private boolean isNotificationForJavaType(final String mid) {
        return MetadataIdentificationUtils.getMetadataClass(mid).equals(
                MetadataIdentificationUtils
                        .getMetadataClass(PhysicalTypeIdentifier
                                .getMetadataIdentiferType()));
    }

    public final void notify(final String upstreamDependency,
            String downstreamDependency) {
        if (downstreamDependency == null) {
            notifyForGenericListener(upstreamDependency);
            return;
        }

        // Handle if the downstream dependency is "class level", meaning we need
        // to figure out the specific downstream MID this metadata provider
        // wants to update/refresh.
        if (MetadataIdentificationUtils
                .isIdentifyingClass(downstreamDependency)) {
            // We have not identified an instance-specific downstream MID, so
            // we'll need to calculate an instance-specific downstream MID to
            // retrieve.
            downstreamDependency = resolveDownstreamDependencyIdentifier(upstreamDependency);

            // We skip if the resolution method returns null, as it doesn't want
            // to continue for some reason
            if (downstreamDependency == null) {
                return;
            }

            Validate.isTrue(
                    MetadataIdentificationUtils
                            .isIdentifyingInstance(downstreamDependency),
                    "An instance-specific downstream MID was required by '%s' (not '%s')",
                    getClass().getName(), downstreamDependency);

            // We only need to proceed if the downstream dependency relationship
            // is not already registered.
            // It is unusual to register a direct downstream relationship given
            // it costs dependency registration memory and class-level
            // notifications will always occur anyway.
            if (metadataDependencyRegistry.getDownstream(upstreamDependency)
                    .contains(downstreamDependency)) {
                return;
            }
        }

        // We should now have an instance-specific "downstream dependency" that
        // can be processed by this class
        Validate.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        downstreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected downstream notification for '%s' to this provider (which uses '%s')",
                downstreamDependency, getProvidesType());

        // We no longer notify downstreams here, as the "get" operation with
        // eviction will ensure the main get(String) method below will be fired
        // and it
        // directly notified downstreams as part of that method (BPA 10 Dec
        // 2010)
        metadataService.evictAndGet(downstreamDependency);
    }

    /**
     * Designed to handle events originating from a
     * {@link MetadataDependencyRegistry#addNotificationListener(MetadataNotificationListener)}
     * registration. Such events are always presented with a non-null upstream
     * dependency indicator and a null downstream dependency indicator. These
     * events differ from events related to {@link PhysicalTypeIdentifier}
     * registrations, as in those cases the downstream dependency indicator will
     * be the class-level {@link #getProvidesType()}.
     * <p>
     * This method allows subclasses to specially handle generic
     * {@link MetadataDependencyRegistry} events.
     * 
     * @param upstreamDependency the upstream which was modified (guaranteed to
     *            be non-null, but could be class-level or instance-level)
     */
    protected void notifyForGenericListener(final String upstreamDependency) {
    }

    /**
     * Removes a {@link JavaType} metadata trigger registration. If the type was
     * never registered, the method returns without an error.
     * 
     * @param javaType to remove (required)
     */
    public void removeMetadataTrigger(final JavaType javaType) {
        Validate.notNull(javaType,
                "Java type required for metadata trigger deregistration");
        metadataTriggers.remove(javaType);
    }

    /**
     * Removes the given {@link JavaType}s as triggering metadata registration.
     * 
     * @param triggerTypes the type-level annotations to remove as triggers
     * @since 1.2.0
     */
    public void removeMetadataTriggers(final JavaType... triggerTypes) {
        for (final JavaType triggerType : triggerTypes) {
            removeMetadataTrigger(triggerType);
        }
    }

    /**
     * Invoked whenever a "class-level" downstream dependency identifier is
     * presented in a metadata notification. An "instance-specific" downstream
     * dependency identifier is required so that a metadata request can
     * ultimately be made. This method is responsible for evaluating the
     * upstream dependency identifier and converting it into a valid downstream
     * dependency identifier. The downstream dependency identifier must be of
     * the same type as this metadata provider's {@link #getProvidesType()}. The
     * downstream dependency identifier must also be instance-specific.
     * <p>
     * The basic implementation offered in this class will only convert a
     * {@link PhysicalTypeIdentifier}. If a subclass registers a dependency on
     * an upstream (other than
     * {@link PhysicalTypeIdentifier#getMetadataIdentiferType()}) and presents
     * their {@link #getProvidesType()} as the downstream (thus meaning only
     * class-level downstream dependency identifiers will be presented), they
     * must override this method and appropriately handle instance-specific
     * downstream dependency identifier resolution.
     * <p>
     * This method may also return null if it wishes to abort processing of the
     * notification. This may be appropriate if a determination cannot be made
     * at this time for whatever reason (eg too early in a lifecycle etc).
     * 
     * @param upstreamDependency the upstream (never null)
     * @return an instance-specific MID of type {@link #getProvidesType()} (or
     *         null if the metadata notification should be aborted)
     */
    protected String resolveDownstreamDependencyIdentifier(
            final String upstreamDependency) {
        // We only support analysis of a PhysicalTypeIdentifier upstream MID to
        // convert this to a downstream MID.
        // In any other case the downstream metadata should have registered an
        // instance-specific downstream dependency on a given upstream.
        Validate.isTrue(isNotificationForJavaType(upstreamDependency),
                "Expected class-level notifications only for physical Java types (not '"
                        + upstreamDependency + "') for metadata provider "
                        + getClass().getName());

        // A physical Java type has changed, and determine what the
        // corresponding local metadata identification string would have been
        final JavaType javaType = PhysicalTypeIdentifier
                .getJavaType(upstreamDependency);
        final LogicalPath path = PhysicalTypeIdentifier
                .getPath(upstreamDependency);
        return createLocalIdentifier(javaType, path);
    }

    /**
     * If set to true (default is true), ensures the governor type details
     * represent a class. Note that
     * {@link #setDependsOnGovernorTypeDetailAvailability(boolean)} must also be
     * true to ensure this can be relied upon.
     * 
     * @param dependsOnGovernorBeingAClass true means governor type detail must
     *            represent a class
     */
    public void setDependsOnGovernorBeingAClass(
            final boolean dependsOnGovernorBeingAClass) {
        this.dependsOnGovernorBeingAClass = dependsOnGovernorBeingAClass;
    }

    /**
     * If set to true (default is true), ensures subclass not called unless the
     * governor type details are available.
     * 
     * @param dependsOnGovernorTypeDetailAvailability true means governor type
     *            details must be available
     */
    public void setDependsOnGovernorTypeDetailAvailability(
            final boolean dependsOnGovernorTypeDetailAvailability) {
        this.dependsOnGovernorTypeDetailAvailability = dependsOnGovernorTypeDetailAvailability;
    }

    protected void setIgnoreTriggerAnnotations(
            final boolean ignoreTriggerAnnotations) {
        this.ignoreTriggerAnnotations = ignoreTriggerAnnotations;
    }
}