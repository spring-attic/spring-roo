package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.ImportRegistrationResolverImpl;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Assists in the building of an {@link ItdTypeDetails} instance.
 * 
 * <p>
 * All methods on this class (which does NOT include the constructor) accept null arguments,
 * and will automatically ignore any attempt to add an {@link IdentifiableJavaStructure} that is
 * not use the same declaredByMetadataId as when the instance was constructed.
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
 */
public final class ItdTypeDetailsBuilder extends AbstractMemberHoldingTypeDetailsBuilder<ItdTypeDetails> {
	private ClassOrInterfaceTypeDetails governor;
	private JavaType aspect;
	private boolean privilegedAspect;
	private ImportRegistrationResolver importRegistrationResolver;
	private List<ClassOrInterfaceTypeDetails> innerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	private List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
	private List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();

	public ItdTypeDetailsBuilder(ItdTypeDetails existing) {
		super(existing.getDeclaredByMetadataId(), existing);
		this.governor = existing.getGovernor();
		this.aspect = existing.getAspect();
		this.privilegedAspect = existing.isPrivilegedAspect();
		this.importRegistrationResolver = new ImportRegistrationResolverImpl(aspect.getPackage());
	}
	
	public ItdTypeDetailsBuilder(String declaredByMetadataId, ClassOrInterfaceTypeDetails governor, JavaType aspect, boolean privilegedAspect) {
		super(declaredByMetadataId);
		Assert.notNull(governor, "Name (to receive the introductions) required");
		Assert.notNull(aspect, "Aspect required");
		this.governor = governor;
		this.aspect = aspect;
		this.privilegedAspect = privilegedAspect;
		this.importRegistrationResolver = new ImportRegistrationResolverImpl(aspect.getPackage());
	}

	public ItdTypeDetails build() {
		return new DefaultItdTypeDetails(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), governor, aspect, privilegedAspect, importRegistrationResolver.getRegisteredImports(), buildConstructors(), buildFields(), buildMethods(), getExtendsTypes(), getImplementsTypes(), buildAnnotations(), fieldAnnotations, methodAnnotations, innerTypes);
	}

	public ImportRegistrationResolver getImportRegistrationResolver() {
		return importRegistrationResolver;
	}

	@Override 
	protected void onAddConstructor(ConstructorMetadataBuilder md) {
		Assert.isNull(MemberFindingUtils.getDeclaredConstructor(governor, AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Constructor with " + md.getParameterTypes().size() + " parameters already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredConstructor(build(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Constructor with " + md.getParameterTypes().size() + " parameters already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		Assert.hasText(md.getBody(), "Method '" + md + "' failed to provide a body, despite being identified for ITD inclusion");
	}

	@Override 
	protected void onAddField(FieldMetadataBuilder md) {
		Assert.isNull(MemberFindingUtils.getDeclaredField(governor, md.getFieldName()), "Field '" + md.getFieldName() + "' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredField(build(), md.getFieldName()), "Field '" + md.getFieldName() + "' already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + ")'");
	}

	@Override 
	protected void onAddMethod(MethodMetadataBuilder md) {
		Assert.isNull(MemberFindingUtils.getDeclaredMethod(governor, md.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Method '" + md.getMethodName() + "' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredMethod(build(), md.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Method '" + md.getMethodName() + "' already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
		Assert.hasText(md.getBody(), "Method '" + md + "' failed to provide a body, despite being identified for ITD inclusion");
	}

	@Override 
	protected void onAddExtendsTypes(JavaType type) {
		Assert.isTrue(!governor.getExtendsTypes().contains(type), "Type '" + type + "' already declared in extends types list in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isTrue(!getExtendsTypes().contains(type), "Type '" + type + "' already declared in extends types list in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
	}

	@Override 
	protected void onAddImplementType(JavaType type) {
		Assert.isTrue(!governor.getImplementsTypes().contains(type), "Type '" + type + "' already declared in implements types list in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isTrue(!getImplementsTypes().contains(type), "Type '" + type + "' already declared in implements types list in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
	}

	@Override 
	protected void onAddAnnotation(AnnotationMetadataBuilder md) {
		Assert.isNull(MemberFindingUtils.getDeclaredTypeAnnotation(governor, md.getAnnotationType()), "Type annotation '" + md.getAnnotationType() + "' already defined in target type '" + governor.getName().getFullyQualifiedTypeName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		Assert.isNull(MemberFindingUtils.getDeclaredTypeAnnotation(build(), md.getAnnotationType()), "Type annotation '" + md.getAnnotationType() + "' already defined in ITD (ITD target '" + aspect.getFullyQualifiedTypeName() + "'");
	}

	public void addFieldAnnotation(DeclaredFieldAnnotationDetails declaredFieldAnnotationDetails) {
		if (declaredFieldAnnotationDetails == null) {
			return;
		}
		JavaType declaredBy = PhysicalTypeIdentifier.getJavaType(declaredFieldAnnotationDetails.getFieldMetadata().getDeclaredByMetadataId());
		boolean hasAnnotation = MemberFindingUtils.getAnnotationOfType(declaredFieldAnnotationDetails.getFieldMetadata().getAnnotations(), declaredFieldAnnotationDetails.getFieldAnnotation().getAnnotationType()) != null;
		if (!declaredFieldAnnotationDetails.isRemoveAnnotation()) {
			Assert.isTrue(!hasAnnotation, "Field annotation '@" + declaredFieldAnnotationDetails.getFieldAnnotation().getAnnotationType().getSimpleTypeName() + "' is already present on the target field '" + declaredBy.getFullyQualifiedTypeName() + "." + declaredFieldAnnotationDetails.getFieldMetadata().getFieldName().getSymbolName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		} else {
			Assert.isTrue(hasAnnotation, "Field annotation '@" + declaredFieldAnnotationDetails.getFieldAnnotation().getAnnotationType().getSimpleTypeName() + "' cannot be removed as it is not present on the target field '" + declaredBy.getFullyQualifiedTypeName() + "." + declaredFieldAnnotationDetails.getFieldMetadata().getFieldName().getSymbolName() + "' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		}
		fieldAnnotations.add(declaredFieldAnnotationDetails);
	}

	public void addMethodAnnotation(DeclaredMethodAnnotationDetails declaredMethodAnnotationDetails) {
		if (declaredMethodAnnotationDetails == null) {
			return;
		}
		JavaType declaredBy = PhysicalTypeIdentifier.getJavaType(declaredMethodAnnotationDetails.getMethodMetadata().getDeclaredByMetadataId());
		boolean hasAnnotation = MemberFindingUtils.getAnnotationOfType(declaredMethodAnnotationDetails.getMethodMetadata().getAnnotations(), declaredMethodAnnotationDetails.getMethodAnnotation().getAnnotationType()) != null;
		Assert.isTrue(!hasAnnotation, "Method annotation '@" + declaredMethodAnnotationDetails.getMethodAnnotation().getAnnotationType().getSimpleTypeName() + "' is already present on the target method '" + declaredBy.getFullyQualifiedTypeName() + "." + declaredMethodAnnotationDetails.getMethodMetadata().getMethodName().getSymbolName() + "()' (ITD target '" + aspect.getFullyQualifiedTypeName() + "')");
		methodAnnotations.add(declaredMethodAnnotationDetails);
	}

	public void addInnerType(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		if (classOrInterfaceTypeDetails == null) {
			return;
		}
		Assert.isTrue(Modifier.isStatic(classOrInterfaceTypeDetails.getModifier()), "Currently only static inner types are supported by AspectJ");
		innerTypes.add(classOrInterfaceTypeDetails);
	}

	@Deprecated
	// Should use addAnnotation() instead
	public void addTypeAnnotation(AnnotationMetadata annotationMetadata) {
		addAnnotation(annotationMetadata);
	}
}
