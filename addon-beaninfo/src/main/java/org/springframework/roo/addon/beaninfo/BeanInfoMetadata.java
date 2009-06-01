package org.springframework.roo.addon.beaninfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooBeanInfo}.
 * 
 * <p>
 * Represents common information about beans, such as their accessors and mutators.
 * 
 * <p>
 * This metadata will introspect all {@link ItdProviderRole#ACCESSOR_MUTATOR} providers. As such those providers
 * must not rely on details provided by this metadata (it is permitted, however, for those providers to register
 * an additional trigger against this provider).
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class BeanInfoMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = BeanInfoMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	private List<MemberHoldingTypeDetails> memberHoldingTypeDetails = new ArrayList<MemberHoldingTypeDetails>();

	public BeanInfoMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, List<MemberHoldingTypeDetails> memberHoldingTypeDetails) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		
		if (!isValid()) {
			return;
		}
		
		this.memberHoldingTypeDetails = memberHoldingTypeDetails;

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Obtains the property name for the specified JavaBean accessor or mutator method. This is determined by
	 * discarding the first 2 or 3 letters of the method name (depending whether it is a "get", "set" or "is" method).
	 * There is no special searching back to the actual field name.
	 * 
	 * @param methodMetadata to search (required, and must be a "get", "set" or "is" method)
	 * @return the name of the property (never returned null)
	 */
	public JavaSymbolName getPropertyNameForJavaBeanMethod(MethodMetadata methodMetadata) {
		Assert.notNull(methodMetadata, "Method metadata is required");
		String name = methodMetadata.getMethodName().getSymbolName();
		if (name.startsWith("set") || name.startsWith("get")) {
			return new JavaSymbolName(StringUtils.uncapitalize(name.substring(3)));
		}
		if (name.startsWith("is")) {
			return new JavaSymbolName(StringUtils.uncapitalize(name.substring(2)));
		}
		throw new IllegalStateException("Method name '" + name + "' does not observe JavaBean method naming conventions");
	}
	
	/**
	 * Attempts to locate the field which is represented by the presented property name.
	 * 
	 * <p>
	 * Not every JavaBean getter or setter actually backs to a field with an identical name. In such
	 * cases, null will be returned.
	 * 
	 * @param propertyName the property name (required)
	 * @return the field if found, or null if it could not be found
	 */
	public FieldMetadata getFieldForPropertyName(JavaSymbolName propertyName) {
		Assert.notNull(propertyName, "Property name required");
		for (MemberHoldingTypeDetails holder : memberHoldingTypeDetails) {
			FieldMetadata result = MemberFindingUtils.getDeclaredField(holder, propertyName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	/**
	 * @return all public accessor methods defined by the class and all superclasses, sorted alphabetically (never null, but may be empty)
	 */
	public List<MethodMetadata> getPublicAccessors() {
		return getPublicAccessors(true);
	}
	
	/**
	 * @param sortByAccessorName indicated to sort the returned accessors by getter name
	 * @return all public accessor methods defined by the class and all superclasses (never null, but may be empty)
	 */
	public List<MethodMetadata> getPublicAccessors(boolean sortByAccessorName) {
		// We keep these in a TreeMap so the methods are output in alphabetic order
		/** key: string based method name, value: MethodMetadata */
		TreeMap<String, MethodMetadata> map = new TreeMap<String, MethodMetadata>();
		List<MethodMetadata> sortedByDetectionOrder = new ArrayList<MethodMetadata>();
		
		for (MemberHoldingTypeDetails holder : memberHoldingTypeDetails) {
			for (MethodMetadata method : holder.getDeclaredMethods()) {
				String accessorName = method.getMethodName().getSymbolName();
				if (Modifier.isPublic(method.getModifier()) && method.getParameterTypes().size() == 0 && (accessorName.startsWith("get") || accessorName.startsWith("is"))) {
					// We've got a public, no parameter, get|is method
					if (sortByAccessorName) {
						map.put(accessorName, method);
					} else {
						sortedByDetectionOrder.add(method);
					}
				}
			}
		}
		if (sortByAccessorName) {
			return new ArrayList<MethodMetadata>(map.values());
		}
		return sortedByDetectionOrder;
	}
	
	/**
	 * @return all public mutator methods defined by the class and all superclasses, sorted alphabetically (never null, but may be empty)
	 */
	public List<MethodMetadata> getPublicMutators() {
		// We keep these in a TreeMap so the methods are output in alphabetic order
		/** key: string based method name, value: MethodMetadata */
		TreeMap<String, MethodMetadata> map = new TreeMap<String, MethodMetadata>();
		for (MemberHoldingTypeDetails holder : memberHoldingTypeDetails) {
			for (MethodMetadata method : holder.getDeclaredMethods()) {
				String mutatorName = method.getMethodName().getSymbolName();
				if (Modifier.isPublic(method.getModifier()) && method.getParameterTypes().size() == 1 && mutatorName.startsWith("set")) {
					// We've got a public, single parameter, set method
					map.put(mutatorName, method);
				}
			}
		}
		return new ArrayList<MethodMetadata>(map.values());
	}
	
	/**
	 * @return the Java type that this metadata represents information for (never returns null)
	 */
	public JavaType getJavaBean() {
		return governorTypeDetails.getName();
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	
}
