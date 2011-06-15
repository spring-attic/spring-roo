package org.springframework.roo.addon.jsf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfManagedBean}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfManagedBeanFormMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = JsfManagedBeanFormMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private JavaType entityType;
	private String plural;
	private List<MethodMetadata> locatedAccessors;
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata mergeMethod;
	private MethodMetadata removeMethod;
	private MethodMetadata findAllMethod;

	public JsfManagedBeanFormMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, JsfAnnotationValues annotationValues, MemberDetails memberDetails, String plural, List<MethodMetadata> locatedAccessors) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(memberDetails, "Member details required");
		Assert.isTrue(StringUtils.hasText(plural), "Plural required");
		
		if (!isValid()) {
			return;
		}
		
		entityType = annotationValues.getEntity();
		this.plural = plural;
		this.locatedAccessors = locatedAccessors;

		identifierAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		persistMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.PERSIST_METHOD);
		mergeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.MERGE_METHOD);
		removeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.REMOVE_METHOD);
		findAllMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_ALL_METHOD);
		if (identifierAccessorMethod == null || persistMethod == null || mergeMethod == null || removeMethod == null || findAllMethod == null) {
			return;
		}

		// Add methods
		builder.addMethod(getHeadingMethod());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private MethodMetadata getHeadingMethod() {
		JavaSymbolName methodName = new JavaSymbolName("getName");
		MethodMetadata method = methodExists(methodName, new ArrayList<JavaType>());
		if (method != null) return method;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return \"" + StringUtils.uncapitalize(entityType.getSimpleTypeName()) + "\";");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.STRING_OBJECT, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName, List<JavaType> paramTypes) {
		return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, paramTypes);
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
