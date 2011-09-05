package org.springframework.roo.classpath.itd;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaSymbolName;
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
 */
public abstract class AbstractItdTypeDetailsProvidingMetadataItem extends AbstractMetadataItem implements ItdTypeDetailsProvidingMetadataItem {
	protected ClassOrInterfaceTypeDetails governorTypeDetails;
	protected ItdTypeDetails itdTypeDetails;
	protected ItdTypeDetailsBuilder builder;
	protected JavaType destination;
	protected JavaType aspectName;
	protected PhysicalTypeMetadata governorPhysicalTypeMetadata;
	
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
	
	/**
	 * Returns the metadata for an annotation of the given type if the governor
	 * does not already have one.
	 * 
	 * @param annotationType the type of annotation to generate (required)
	 * @return <code>null</code> if the governor already has that annotation
	 */
	protected AnnotationMetadata getTypeAnnotation(final JavaType annotationType) {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, annotationType) != null) {
			return null;
		}
		return new AnnotationMetadataBuilder(annotationType).build();
	}

	/**
	 * Determines if the presented class (or any of its superclasses) implements the target interface.
	 * 
	 * @param clazz to search
	 * @param interfaceTarget the interface to locate
	 * @return true if the class or any of its superclasses contains the specified interface
	 */
	protected boolean isImplementing(final ClassOrInterfaceTypeDetails clazz, final JavaType interfaceTarget) {
		if (clazz.getImplementsTypes().contains(interfaceTarget)) {
			return true;
		}
		if (clazz.getSuperclass() != null) {
			return isImplementing(clazz.getSuperclass(), interfaceTarget);
		}
		return false;
	}
	
	protected MethodMetadata methodExists(final JavaSymbolName methodName, final List<JavaType> parameterTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, parameterTypes);
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
	
	/**
	 * Ensures that the governor extends the given type, i.e. introduces that
	 * type as a supertype iff it's not already one
	 * 
	 * @param javaType the type to extend (required)
	 * @since 1.2.0
	 */
	protected final void ensureGovernorExtends(final JavaType javaType) {
		if (!governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().extendsType(javaType)) {
			builder.addExtendsTypes(javaType);
		}		
	}
}
