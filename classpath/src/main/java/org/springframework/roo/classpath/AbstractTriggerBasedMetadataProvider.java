package org.springframework.roo.classpath;

import java.util.Collection;
import java.util.HashSet;

import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Base class for implementing {@link TriggerBasedMetadataProvider}s.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class AbstractTriggerBasedMetadataProvider<M extends MetadataItem> implements TriggerBasedMetadataProvider {

	// Fields
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	
	private final Collection<JavaType> triggers;
	private final String metadataTypeId;

	/**
	 * Constructor
	 *
	 * @param metadataClass the class of metadata being provided (required)
	 */
	protected AbstractTriggerBasedMetadataProvider(final Class<M> metadataClass) {
		this.metadataTypeId = MetadataIdentificationUtils.create(metadataClass);
		this.triggers = new HashSet<JavaType>();
	}
	
	protected void activate(final ComponentContext context) {
		// Listen for changes to physical types in order to detect new or removed trigger annotations
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), metadataTypeId);
	}
	
	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), metadataTypeId);
	}

	public String getProvidesType() {
		return metadataTypeId;
	}

	public void addMetadataTrigger(final JavaType trigger) {
		if (trigger != null) {
			this.triggers.add(trigger);
		}
	}

	public void removeMetadataTrigger(final JavaType trigger) {
		this.triggers.remove(trigger);
	}

	public MetadataItem get(final String metadataId) {
		assertCorrectMetadataTypeIsBeingRequested(metadataId);

		final PhysicalTypeMetadata governorMetadata = getGovernorMetadata(metadataId);
		if (!shouldProduceMetadataFor(governorMetadata)) {
			return null;
		}

		final MetadataItem metadata = getMetadata(metadataId, governorMetadata);
		
		return convertInvalidToNull(metadata);
	}

	private void assertCorrectMetadataTypeIsBeingRequested(final String metadataId) {
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(metadataId).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected request for '" + metadataId + "' to this provider (which provides '" + getProvidesType() + "'");
	}

	private PhysicalTypeMetadata getGovernorMetadata(final String metadataId) {
		final JavaType domainType = PhysicalTypeIdentifierNamingUtils.getJavaType(metadataTypeId, metadataId);
		final Path path = PhysicalTypeIdentifierNamingUtils.getPath(metadataTypeId, metadataId);
		final String physicalTypeMetadataId = PhysicalTypeIdentifier.createIdentifier(domainType, path);
		return (PhysicalTypeMetadata) metadataService.get(physicalTypeMetadataId);
	}

	private boolean shouldProduceMetadataFor(final PhysicalTypeMetadata metadata) {
		return isComplete(metadata) && annotatedWithAnyTriggers(metadata.getMemberHoldingTypeDetails());
	}

	private boolean isComplete(final PhysicalTypeMetadata metadata) {
		return metadata != null && metadata.isValid() && metadata.getMemberHoldingTypeDetails() != null;
	}
	
	private boolean annotatedWithAnyTriggers(final IdentifiableAnnotatedJavaStructure governor) {
		for (final AnnotationMetadata annotation : governor.getAnnotations()) {
			if (this.triggers.contains(annotation.getAnnotationType())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Subclasses should generate the metadata for the given governor, if possible.
	 * 
	 * @param metadataId the ID of the metadata to produce (a valid metadata instance ID)
	 * @param governor the user project type that contained one or more of the trigger annotations (never <code>null</code>)
	 * @return <code>null</code> or an invalid {@link MetadataItem} if the metadata could not be produced
	 */
	protected abstract M getMetadata(String metadataId, PhysicalTypeMetadata governor);
	
	private MetadataItem convertInvalidToNull(final MetadataItem metadata) {
		if (metadata == null || !metadata.isValid()) {
			return null;
		}
		return metadata;
	}
}
