package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.ImportRegistrationResolverImpl;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Assists in the building of an {@link ItdTypeDetails} instance.
 * <p>
 * All methods on this class (which does NOT include the constructor) accept
 * null arguments, and will automatically ignore any attempt to add an
 * {@link IdentifiableJavaStructure} that is not use the same
 * declaredByMetadataId as when the instance was constructed.
 * <p>
 * In addition, any method on this class which accepts an
 * {@link InvocableMemberMetadata} will verify a
 * {@link InvocableMemberMetadata#getBody()} is provided. This therefore detects
 * programming errors which result from requesting a member to be included in an
 * ITD but without providing the actual executable body for that member.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public class ItdTypeDetailsBuilder extends
        AbstractMemberHoldingTypeDetailsBuilder<ItdTypeDetails> {

    private final JavaType aspect;
    private final List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
    private final ClassOrInterfaceTypeDetails governor;
    private final ImportRegistrationResolver importRegistrationResolver;
    private final List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();
    private final boolean privilegedAspect;
    private final Set<JavaType> declarePrecedence;

    /**
     * Constructor based on an existing ITD
     * 
     * @param existing (required)
     */
    public ItdTypeDetailsBuilder(final ItdTypeDetails existing) {
        super(existing.getDeclaredByMetadataId(), existing);
        aspect = existing.getAspect();
        governor = existing.getGovernor();
        importRegistrationResolver = new ImportRegistrationResolverImpl(
                aspect.getPackage());
        privilegedAspect = existing.isPrivilegedAspect();
        declarePrecedence = existing.getDeclarePrecedence();
    }

    /**
     * Constructor
     * 
     * @param declaredByMetadataId
     * @param governor (required)
     * @param aspect (required)
     * @param privilegedAspect
     */
    public ItdTypeDetailsBuilder(final String declaredByMetadataId,
            final ClassOrInterfaceTypeDetails governor, final JavaType aspect,
            final boolean privilegedAspect) {
        super(declaredByMetadataId);
        Validate.notNull(governor,
                "Name (to receive the introductions) required");
        Validate.notNull(aspect, "Aspect required");
        this.aspect = aspect;
        this.governor = governor;
        importRegistrationResolver = new ImportRegistrationResolverImpl(
                aspect.getPackage());
        this.privilegedAspect = privilegedAspect;
        this.declarePrecedence = new LinkedHashSet<JavaType>();
    }

    public void addFieldAnnotation(
            final DeclaredFieldAnnotationDetails declaredFieldAnnotationDetails) {
        if (declaredFieldAnnotationDetails == null) {
            return;
        }
        final JavaType declaredBy = PhysicalTypeIdentifier
                .getJavaType(declaredFieldAnnotationDetails.getField()
                        .getDeclaredByMetadataId());
        final boolean hasAnnotation = MemberFindingUtils.getAnnotationOfType(
                declaredFieldAnnotationDetails.getField().getAnnotations(),
                declaredFieldAnnotationDetails.getFieldAnnotation()
                        .getAnnotationType()) != null;
        if (!declaredFieldAnnotationDetails.isRemoveAnnotation()) {
            Validate.isTrue(
                    !hasAnnotation,
                    "Field annotation '@%s' is already present on the target field '%s.%s' (ITD target '%s')",
                    declaredFieldAnnotationDetails.getFieldAnnotation()
                            .getAnnotationType().getSimpleTypeName(),
                    declaredBy.getFullyQualifiedTypeName(),
                    declaredFieldAnnotationDetails.getField().getFieldName()
                            .getSymbolName(),
                    aspect.getFullyQualifiedTypeName());
        }
        else {
            Validate.isTrue(
                    hasAnnotation,
                    "Field annotation '@%s' cannot be removed as it is not present on the target field '%s.%s' (ITD target '%s')",
                    declaredFieldAnnotationDetails.getFieldAnnotation()
                            .getAnnotationType().getSimpleTypeName(),
                    declaredBy.getFullyQualifiedTypeName(),
                    declaredFieldAnnotationDetails.getField().getFieldName()
                            .getSymbolName(),
                    aspect.getFullyQualifiedTypeName());
        }
        fieldAnnotations.add(declaredFieldAnnotationDetails);
    }

    @Override
    public void addImports(final Collection<ImportMetadata> imports) {
        if (imports != null) {
            for (final ImportMetadata anImport : imports) {
                importRegistrationResolver.addImport(anImport.getImportType());
            }
        }
    }

    public void addMethodAnnotation(
            final DeclaredMethodAnnotationDetails declaredMethodAnnotationDetails) {
        if (declaredMethodAnnotationDetails == null) {
            return;
        }
        final JavaType declaredBy = PhysicalTypeIdentifier
                .getJavaType(declaredMethodAnnotationDetails
                        .getMethodMetadata().getDeclaredByMetadataId());
        final boolean hasAnnotation = MemberFindingUtils.getAnnotationOfType(
                declaredMethodAnnotationDetails.getMethodMetadata()
                        .getAnnotations(), declaredMethodAnnotationDetails
                        .getMethodAnnotation().getAnnotationType()) != null;
        Validate.isTrue(
                !hasAnnotation,
                "Method annotation '@%s' is already present on the target field '%s.%s' (ITD target '%s')",
                declaredMethodAnnotationDetails.getMethodAnnotation()
                        .getAnnotationType().getSimpleTypeName(),
                declaredBy.getFullyQualifiedTypeName(),
                declaredMethodAnnotationDetails.getMethodMetadata()
                        .getMethodName().getSymbolName(),
                aspect.getFullyQualifiedTypeName());
        methodAnnotations.add(declaredMethodAnnotationDetails);
    }

    @Deprecated
    // Should use addAnnotation() instead
    public void addTypeAnnotation(final AnnotationMetadata annotationMetadata) {
        addAnnotation(annotationMetadata);
    }
    
    /**
     * Set the aspects to use on {@code declare precedence}
     * AspectJ declaration.
     *  
     * @param aspects
     */
    public void setDeclarePrecedence(JavaType...aspects) {
    	if (aspects != null && aspects.length > 0 ){
    		Validate.isTrue(aspects.length > 1,"precedence must contain, at least, 2 aspects");
    	}
    	CollectionUtils.populate(declarePrecedence, Arrays.asList(aspects));
    }

    public ItdTypeDetails build() {
        return new DefaultItdTypeDetails(getCustomData().build(),
                getDeclaredByMetadataId(), getModifier(), governor, aspect,
                privilegedAspect,
                importRegistrationResolver.getRegisteredImports(),
                buildConstructors(), buildFields(), buildMethods(),
                getExtendsTypes(), getImplementsTypes(), buildAnnotations(),
                fieldAnnotations, methodAnnotations, buildInnerTypes(), 
                declarePrecedence);
    }

    public ImportRegistrationResolver getImportRegistrationResolver() {
        return importRegistrationResolver;
    }

    @Override
    protected void onAddAnnotation(final AnnotationMetadataBuilder md) {
        Validate.isTrue(
                governor.getAnnotation(md.getAnnotationType()) == null,
                "Type annotation '%s' already defined in target type '%s' (ITD target '%s')",
                md.getAnnotationType(), governor.getName()
                        .getFullyQualifiedTypeName(), aspect
                        .getFullyQualifiedTypeName());
        Validate.isTrue(
                build().getAnnotation(md.getAnnotationType()) == null,
                "Type annotation '%s' already defined in ITD (ITD target '%s')",
                md.getAnnotationType(), aspect.getFullyQualifiedTypeName());
    }

    @Override
    protected void onAddConstructor(final ConstructorMetadataBuilder md) {
        Validate.isTrue(
                governor.getDeclaredConstructor(AnnotatedJavaType
                        .convertFromAnnotatedJavaTypes(md.getParameterTypes())) == null,
                "Constructor with %d parameters already defined in target type '%s' (ITD target '%s')",
                md.getParameterTypes().size(), governor.getName()
                        .getFullyQualifiedTypeName(), aspect
                        .getFullyQualifiedTypeName());
        Validate.isTrue(
                build().getDeclaredConstructor(
                        AnnotatedJavaType.convertFromAnnotatedJavaTypes(md
                                .getParameterTypes())) == null,
                "Constructor with %d parameters already defined in ITD (ITD target '%s')",
                md.getParameterTypes().size(), aspect
                        .getFullyQualifiedTypeName());
        Validate.notBlank(
                md.getBody(),
                "Constructor '%s' failed to provide a body, despite being identified for ITD inclusion",
                md);
    }

    @Override
    protected void onAddExtendsTypes(final JavaType type) {
        Validate.isTrue(
                !governor.getExtendsTypes().contains(type),
                "Type '%s' already declared in extends types list in target type '%s' (ITD target '%s')",
                type, governor.getName().getFullyQualifiedTypeName(),
                aspect.getFullyQualifiedTypeName());
        Validate.isTrue(
                !getExtendsTypes().contains(type),
                "Type '%s' already declared in extends types list in ITD (ITD target '%s')",
                type, aspect.getFullyQualifiedTypeName());
    }

    @Override
    protected void onAddField(final FieldMetadataBuilder md) {
        Validate.isTrue(
                governor.getDeclaredField(md.getFieldName()) == null,
                "Field '%s' already defined in target type '%s' (ITD target '%s')",
                md.getFieldName(), governor.getName()
                        .getFullyQualifiedTypeName(), aspect
                        .getFullyQualifiedTypeName());
        Validate.isTrue(build().getDeclaredField(md.getFieldName()) == null,
                "Field '%s' already defined in ITD (ITD target '%s')",
                md.getFieldName(), aspect.getFullyQualifiedTypeName());
    }

    @Override
    protected void onAddImplementType(final JavaType type) {
        Validate.isTrue(
                !governor.getImplementsTypes().contains(type),
                "Type '%s' already declared in implements types list in target type '%s' (ITD target '%s')",
                type, governor.getName().getFullyQualifiedTypeName(),
                aspect.getFullyQualifiedTypeName());
        Validate.isTrue(
                !getImplementsTypes().contains(type),
                "Type '%s' already declared in implements types list in ITD (ITD target '%s')",
                type, aspect.getFullyQualifiedTypeName());
    }

    @Override
    public void onAddInnerType(final ClassOrInterfaceTypeDetailsBuilder cid) {
        if (cid == null) {
            return;
        }
        Validate.isTrue(Modifier.isStatic(cid.getModifier()),
                "Currently only static inner types are supported by AspectJ");
    }

    @Override
    protected void onAddMethod(final MethodMetadataBuilder md) {
        Validate.isTrue(
                MemberFindingUtils.getDeclaredMethod(governor, md
                        .getMethodName(), AnnotatedJavaType
                        .convertFromAnnotatedJavaTypes(md.getParameterTypes())) == null,
                "Method '%s' already defined in target type '%s' (ITD target '%s')",
                md.getMethodName(), governor.getName()
                        .getFullyQualifiedTypeName(), aspect
                        .getFullyQualifiedTypeName());
        Validate.isTrue(
                MemberFindingUtils.getDeclaredMethod(build(), md
                        .getMethodName(), AnnotatedJavaType
                        .convertFromAnnotatedJavaTypes(md.getParameterTypes())) == null,
                "Method '%s' already defined in ITD (ITD target '%s')", md
                        .getMethodName(), aspect.getFullyQualifiedTypeName());
        if (!Modifier.isAbstract(md.getModifier())) {
            Validate.notBlank(
                    md.getBody(),
                    "Method '%s' failed to provide a body, despite being identified for ITD inclusion",
                    md);
        }
    }
}
