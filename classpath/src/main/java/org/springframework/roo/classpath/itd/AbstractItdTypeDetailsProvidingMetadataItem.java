package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract implementation of {@link ItdTypeDetailsProvidingMetadataItem}, which assumes the subclass will require
 * a non-null {@link ClassOrInterfaceTypeDetails} representing the governor and wishes to build an ITD via the
 * {@link ItdTypeDetailsBuilder} mechanism. 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class AbstractItdTypeDetailsProvidingMetadataItem extends AbstractMetadataItem implements ItdTypeDetailsProvidingMetadataItem {

	protected ClassOrInterfaceTypeDetails governorTypeDetails;
	protected ItdTypeDetails itdTypeDetails;
	protected JavaType destination;
	
	protected JavaType aspectName;
	protected PhysicalTypeMetadata governorPhysicalTypeMetadata;
	
	protected ItdTypeDetailsBuilder builder;
	
	/**
	 * Validates input and constructs a superclass that implements {@link ItdTypeDetailsProvidingMetadataItem}.
	 * 
	 * <p>
	 * Exposes the {@link ClassOrInterfaceTypeDetails} of the governor, if available. If they are not available, ensures
	 * {@link #isValid()} returns false.
	 * 
	 * <p>
	 * Subclasses should generally return immediately if {@link #isValid()} is false. Subclasses should also attempt to set the
	 * {@link #itdTypeDetails} to contain the output of their ITD where {@link #isValid()} is true.
	 * 
	 * @param identifier the identifier for this item of metadata (required)
	 * @param aspectName the Java type of the ITD (required)
	 * @param governorPhysicalTypeMetadata the governor, which is expected to contain a {@link ClassOrInterfaceTypeDetails} (required)
	 */
	public AbstractItdTypeDetailsProvidingMetadataItem(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier);
		Assert.notNull(aspectName, "Aspect name required");
		Assert.notNull(governorPhysicalTypeMetadata, "Governor physical type metadata required");
		
		this.aspectName = aspectName;
		this.governorPhysicalTypeMetadata = governorPhysicalTypeMetadata;

		PhysicalTypeDetails physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		
		if (physicalTypeDetails == null || !(physicalTypeDetails instanceof ClassOrInterfaceTypeDetails)) {
			// There is a problem
			valid = false;
		} else {
			// We have reliable physical type details
			governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
		}

		this.destination = governorTypeDetails.getName();
		
		// Provide the subclass a builder, to make preparing an ITD even easier
		this.builder = new ItdTypeDetailsBuilder(getId(), governorTypeDetails, aspectName, true);
	}
	
	public final ItdTypeDetails getMemberHoldingTypeDetails() {
		return itdTypeDetails;
	}

	@Override
	public int hashCode() {
		return builder.build().hashCode();
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

}
