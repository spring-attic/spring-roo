package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
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
 * @since 1.0
 *
 */
public final class DefaultItdTypeDetailsBuilder {

	private String declaredByMetadataId;
	private JavaType name;
	private JavaType aspect;
	private boolean privilegedAspect;
	
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
	
	// package protected, as we prefer users to use DefaultItdTypeDetails.getBuilder().
	DefaultItdTypeDetailsBuilder(String declaredByMetadataId, JavaType name, JavaType aspect, boolean privilegedAspect) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(declaredByMetadataId), "Declared by metadata ID must identify a specific instance (not '" + declaredByMetadataId + "')");
		Assert.notNull(name, "Name (to receive the introductions) required");
		Assert.notNull(aspect, "Aspect required");
		this.declaredByMetadataId = declaredByMetadataId;
		this.name = name;
		this.aspect = aspect;
		this.privilegedAspect = privilegedAspect;
	}
	
	/**
	 * @return an immutable {@link DefaultItdTypeDetails} representing the current state of the builder (never null)
	 */
	public DefaultItdTypeDetails build() {
		return new DefaultItdTypeDetails(name, aspect, privilegedAspect, declaredConstructors, declaredFields, declaredMethods, extendsTypes, implementsTypes, typeAnnotations);
	}

	public void addConstructor(ConstructorMetadata md) {
		if (md == null || !declaredByMetadataId.equals(md.getDeclaredByMetadataId())) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredConstructor(build(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Constructor with " + md.getParameterTypes().size() + " parameters already defined");
		Assert.hasText(md.getBody(), "Method '" + md + "' failed to provide a body, despite being identified for ITD inclusion");
		declaredConstructors.add(md);
	}

	public void addField(FieldMetadata md) {
		if (md == null || !declaredByMetadataId.equals(md.getDeclaredByMetadataId())) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredField(build(), md.getFieldName()), "Field '" + md.getFieldName() +"' already defined");
		declaredFields.add(md);
	}

	public void addMethod(MethodMetadata md) {
		if (md == null || !declaredByMetadataId.equals(md.getDeclaredByMetadataId())) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredMethod(build(), md.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(md.getParameterTypes())), "Method '" + md.getMethodName() +"' already defined");
		Assert.hasText(md.getBody(), "Method '" + md + "' failed to provide a body, despite being identified for ITD inclusion");
		declaredMethods.add(md);
	}

	public void addExtendsType(JavaType type) {
		if (type == null) {
			return;
		}
		Assert.isTrue(!extendsTypes.contains(type), "Type '" + type + "' already declared in extends types list");
		extendsTypes.add(type);
	}

	public void addImplementsType(JavaType type) {
		if (type == null) {
			return;
		}
		Assert.isTrue(!implementsTypes.contains(type), "Type '" + type + "' already declared in implements types list");
		implementsTypes.add(type);
	}

	public void addTypeAnnotation(AnnotationMetadata md) {
		if (md == null) {
			return;
		}
		Assert.isNull(MemberFindingUtils.getDeclaredTypeAnnotation(build(), md.getAnnotationType()), "Type annotation '" + md.getAnnotationType() +"' already defined");
		typeAnnotations.add(md);
	}
	
}
