package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.ImportRegistrationResolverImpl;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Assists in the building of an {@link ItdTypeDetails} instance.
 * 
 * <p>
 * All methods on this class (which does NOT include the constructor) accept null arguments,
 * and will automatically ignore any attempt to add an {@link IdentifiableMember} that is
 * not use the same {@link #declaredByMetadataId} as when the instance was constructed.
 * 
 * <p>
 * In addition, any method on this class which accepts an {@link InvocableMemberMetadata} will
 * verify a {@link InvocableMemberMetadata#getBody()} is provided. This therefore detects
 * programming errors which result from requesting a member to be included in an ITD but
 * without providing the actual executable body for that member.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public final class DefaultItdTypeDetailsBuilder {

	private String declaredByMetadataId;
	private ClassOrInterfaceTypeDetails governor;
	private JavaType aspect;
	private boolean privilegedAspect;
	
	private ImportRegistrationResolver importRegistrationResolver;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private List<ClassOrInterfaceTypeDetails> innerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
	private List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
	private List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();
	
	// package protected, as we prefer users to use DefaultItdTypeDetails.getBuilder().
	DefaultItdTypeDetailsBuilder(String declaredByMetadataId, ClassOrInterfaceTypeDetails governor, JavaType aspect, boolean privilegedAspect) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(declaredByMetadataId), "Declared by metadata ID must identify a specific instance (not '" + declaredByMetadataId + "')");
		Assert.notNull(governor, "Name (to receive the introductions) required");
		Assert.notNull(aspect, "Aspect required");
		this.declaredByMetadataId = declaredByMetadataId;
		this.governor = governor;
		this.aspect = aspect;
		this.privilegedAspect = privilegedAspect;
		this.importRegistrationResolver = new ImportRegistrationResolverImpl(aspect.getPackage());
	}
	
	/**
	 * @return an immutable {@link DefaultItdTypeDetails} representing the current state of the builder (never null)
	 */
	public DefaultItdTypeDetails build() {
		return new DefaultItdTypeDetails(governor, aspect, privilegedAspect, importRegistrationResolver.getRegisteredImports(), declaredConstructors, declaredFields, declaredMethods, extendsTypes, implementsTypes, typeAnnotations, fieldAnnotations, methodAnnotations, innerTypes);
	}

	public ImportRegistrationResolver getImportRegistrationResolver() {
		return importRegistrationResolver;
	}
	
	public void addConstructor(ConstructorMetadata md) {
		if (md == null || !declaredByMetadataId.equals(md.getDeclaredByMetadataId())) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredConstructor(governor, AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Constructor with " + md.getParameterTypes().size() + " parameters already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredConstructor(build(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Constructor with " + md.getParameterTypes().size() + " parameters already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		Assert.hasText(md.getBody(), "Method '" + md + "' failed to provide a body, despite being identified for ITD inclusion");
		declaredConstructors.add(md);
	}

	public void addField(FieldMetadata md) {
		if (md == null || !declaredByMetadataId.equals(md.getDeclaredByMetadataId())) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredField(governor, md.getFieldName()), "Field '" + md.getFieldName() +"' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredField(build(), md.getFieldName()), "Field '" + md.getFieldName() +"' already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		declaredFields.add(md);
	}

	public void addMethod(MethodMetadata md) {
		if (md == null || !declaredByMetadataId.equals(md.getDeclaredByMetadataId())) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredMethod(governor, md.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Method '" + md.getMethodName() +"' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredMethod(build(), md.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Method '" + md.getMethodName() +"' already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		Assert.hasText(md.getBody(), "Method '" + md + "' failed to provide a body, despite being identified for ITD inclusion");
		declaredMethods.add(md);
	}

	public void addExtendsType(JavaType type) {
		if (type == null) {
			return;
		}
		Assert.isTrue(!governor.getExtendsTypes().contains(type), "Type '" + type + "' already declared in extends types list in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isTrue(!extendsTypes.contains(type), "Type '" + type + "' already declared in extends types list in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		extendsTypes.add(type);
	}

	public void addImplementsType(JavaType type) {
		if (type == null) {
			return;
		}
		Assert.isTrue(!governor.getImplementsTypes().contains(type), "Type '" + type + "' already declared in implements types list in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isTrue(!implementsTypes.contains(type), "Type '" + type + "' already declared in implements types list in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		implementsTypes.add(type);
	}

	public void addTypeAnnotation(AnnotationMetadata md) {
		if (md == null) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredTypeAnnotation(governor, md.getAnnotationType()), "Type annotation '" + md.getAnnotationType() +"' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredTypeAnnotation(build(), md.getAnnotationType()), "Type annotation '" + md.getAnnotationType() +"' already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		typeAnnotations.add(md);
	}

	public void addFieldAnnotation(DeclaredFieldAnnotationDetails declaredFieldAnnotationDetails) {
		if (declaredFieldAnnotationDetails == null) {
			return;
		}
		Assert.isTrue(!declaredFieldAnnotationDetails.getFieldMetadata().getAnnotations().contains(declaredFieldAnnotationDetails.getFieldAnnotation()), "Field annotation '@" + declaredFieldAnnotationDetails.getFieldAnnotation().getAnnotationType().getSimpleTypeName() +"' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "." + declaredFieldAnnotationDetails.getFieldMetadata().getFieldName().getSymbolName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		fieldAnnotations.add(declaredFieldAnnotationDetails);
	}
	
	public void addMethodAnnotation(DeclaredMethodAnnotationDetails declaredMethodAnnotationDetails) {
		if (declaredMethodAnnotationDetails == null) {
			return;
		}
		Assert.isTrue(!declaredMethodAnnotationDetails.getMethodMetadata().getAnnotations().contains(declaredMethodAnnotationDetails.getMethodAnnotation()), "Method annotation '@" + declaredMethodAnnotationDetails.getMethodAnnotation().getAnnotationType().getSimpleTypeName() +"' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "." + declaredMethodAnnotationDetails.getMethodMetadata().getMethodName().getSymbolName()+ "()' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		methodAnnotations.add(declaredMethodAnnotationDetails);
	}
	
	public void addInnerType(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		if (classOrInterfaceTypeDetails == null) {
			return;
		}
		Assert.isTrue(Modifier.isStatic(classOrInterfaceTypeDetails.getModifier()), "Currently only static inner types are supported by AspectJ");
		innerTypes.add(classOrInterfaceTypeDetails);
	}
}
