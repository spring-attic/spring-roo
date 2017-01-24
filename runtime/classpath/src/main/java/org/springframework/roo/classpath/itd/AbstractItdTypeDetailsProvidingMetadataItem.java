package org.springframework.roo.classpath.itd;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;

/**
 * Abstract implementation of {@link ItdTypeDetailsProvidingMetadataItem}, which
 * assumes the subclass will require a non-null
 * {@link ClassOrInterfaceTypeDetails} representing the governor and wishes to
 * build an ITD via the {@link ItdTypeDetailsBuilder} mechanism.
 *
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.0
 */
public abstract class AbstractItdTypeDetailsProvidingMetadataItem extends AbstractMetadataItem
    implements ItdTypeDetailsProvidingMetadataItem {

  protected JavaType aspectName;
  protected ItdTypeDetailsBuilder builder;
  protected JavaType destination;
  protected PhysicalTypeMetadata governorPhysicalTypeMetadata;
  protected ClassOrInterfaceTypeDetails governorTypeDetails;
  protected ItdTypeDetails itdTypeDetails;

  protected Map<FieldMetadata, MethodMetadataBuilder> accessorMethods;
  protected Map<FieldMetadata, MethodMetadataBuilder> mutatorMethods;

  /**
   * Validates input and constructs a superclass that implements
   * {@link ItdTypeDetailsProvidingMetadataItem}.
   * <p>
   * Exposes the {@link ClassOrInterfaceTypeDetails} of the governor, if
   * available. If they are not available, ensures {@link #isValid()} returns
   * false.
   * <p>
   * Subclasses should generally return immediately if {@link #isValid()} is
   * false. Subclasses should also attempt to set the {@link #itdTypeDetails}
   * to contain the output of their ITD where {@link #isValid()} is true.
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   */
  protected AbstractItdTypeDetailsProvidingMetadataItem(final String identifier,
      final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(identifier);
    Validate.notNull(aspectName, "Aspect name required");
    Validate.notNull(governorPhysicalTypeMetadata, "Governor physical type metadata required");

    this.aspectName = aspectName;
    this.governorPhysicalTypeMetadata = governorPhysicalTypeMetadata;

    accessorMethods = new HashMap<FieldMetadata, MethodMetadataBuilder>();
    mutatorMethods = new HashMap<FieldMetadata, MethodMetadataBuilder>();

    final Object physicalTypeDetails = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    if (physicalTypeDetails instanceof ClassOrInterfaceTypeDetails) {
      // We have reliable physical type details
      governorTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
    } else {
      // There is a problem
      valid = false;
    }

    destination = governorTypeDetails.getName();

    // Provide the subclass a builder, to make preparing an ITD even easier
    builder = new ItdTypeDetailsBuilder(getId(), governorTypeDetails, aspectName, true);
  }

  private void addToImports(final List<JavaType> parameterTypes) {
    if (parameterTypes != null) {
      final List<JavaType> typesToImport = new ArrayList<JavaType>();
      for (final JavaType parameterType : parameterTypes) {
        if (!JdkJavaType.isPartOfJavaLang(parameterType)) {
          typesToImport.add(parameterType);
        }
      }
      builder.getImportRegistrationResolver().addImports(typesToImport);
    }
  }

  /**
   * Generates the {@link ItdTypeDetails} from the current contents of this
   * instance's {@link ItdTypeDetailsBuilder}.
   *
   * @since 1.2.0
   */
  protected void buildItd() {
    itdTypeDetails = builder.build();
  }

  /**
   * Ensures that the governor extends the given type, i.e. introduces that
   * type as a supertype iff it's not already one
   *
   * @param javaType the type to extend (required)
   * @since 1.2.0
   */
  protected final void ensureGovernorExtends(final JavaType javaType) {
    if (!governorTypeDetails.extendsType(javaType)) {
      builder.addExtendsTypes(javaType);
    }
  }

  /**
   * Ensures that the governor implements the given type.
   *
   * @param javaType the type to implement (required)
   * @since 1.2.0
   */
  protected final void ensureGovernorImplements(final JavaType javaType) {
    if (!governorTypeDetails.implementsType(javaType)) {
      builder.addImplementsType(javaType);
    }
  }

  /**
   * Ensures that the governor is annotated with the given annotation
   *
   * @param AnnotationMetadataBuilder the annotation to use(required)
   * @since 2.0
   */
  protected final void ensureGovernorIsAnnotated(final AnnotationMetadataBuilder annotationMetadata) {
    if (governorTypeDetails.getAnnotation(annotationMetadata.getAnnotationType()) == null) {
      builder.addAnnotation(annotationMetadata);
    }
  }

  /**
   * Ensures that the governor has provided field. If the provided field is private, 
   * this method always includes the accessor method. The mutator method will not be 
   * generated if the provided field is annotated with @Autowired annotation 
   *
   * @param FieldMetadataBuilder the field to include(required)
   * @since 2.0
   */
  protected final void ensureGovernorHasField(final FieldMetadataBuilder fieldMetadata) {
    boolean includeAccessor = false;
    boolean includeMutator = false;
    int modifier = fieldMetadata.getModifier();
    if (modifier >= Modifier.PRIVATE
        && modifier <= Modifier.PRIVATE + Modifier.STATIC + Modifier.FINAL && modifier % 2 == 0) {
      includeAccessor = true;
      if (fieldMetadata.getDeclaredTypeAnnotation(SpringJavaType.AUTOWIRED) == null
          && modifier < Modifier.FINAL) {
        includeMutator = true;
      }
    }
    ensureGovernorHasField(fieldMetadata, includeAccessor, includeMutator);
  }

  /**
   * Ensures that the governor has provided field. You could indicates if
   * is necessary to generate the accessor and the mutator methods for this new field manually.
   *
   * @param FieldMetadataBuilder the field to include(required)
   * @since 2.0
   */
  protected final void ensureGovernorHasField(final FieldMetadataBuilder fieldMetadata,
      boolean includeAccessor, boolean includeMutator) {
    if (governorTypeDetails.getField(fieldMetadata.getFieldName()) == null) {
      builder.addField(fieldMetadata);
    }

    if (includeAccessor) {
      MethodMetadata accessorMethod = getAccessorMethod(fieldMetadata.build());
      if (accessorMethod != null
          && governorTypeDetails.getMethod(accessorMethod.getMethodName()) == null) {
        builder.addMethod(accessorMethod);
      }
    }

    if (includeMutator) {
      MethodMetadata mutatorMethod = getMutatorMethod(fieldMetadata.build());
      if (mutatorMethod != null
          && governorTypeDetails.getMethod(mutatorMethod.getMethodName()) == null) {
        builder.addMethod(mutatorMethod);
      }
    }

  }


  /**
   * Ensures that the governor has provided method
   *
   * @param MethodMetadataBuilder the method to include(required)
   * @since 2.0
   */
  protected final void ensureGovernorHasMethod(final MethodMetadataBuilder methodMetadata) {
    if (governorTypeDetails.getMethod(methodMetadata.getMethodName(),
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes())) == null
        && methodMetadata.getDeclaredByMetadataId().equals(getId())
        && MemberFindingUtils.getDeclaredMethod(builder.build(), methodMetadata.getMethodName(),
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes())) == null) {
      builder.addMethod(methodMetadata);
    }
  }

  /**
   * Ensures that the governor has provided constructor
   *
   * @param ConstructorMetadataBuilder the constructor to include(required)
   * @since 2.0
   */
  protected final void ensureGovernorHasConstructor(
      final ConstructorMetadataBuilder constructorMetadata) {
    if (governorTypeDetails.getDeclaredConstructor(AnnotatedJavaType
        .convertFromAnnotatedJavaTypes(constructorMetadata.getParameterTypes())) == null) {
      builder.addConstructor(constructorMetadata);
    }
  }

  protected MethodMetadata getAccessorMethod(final FieldMetadata field) {
    // Check if this method has been cached
    MethodMetadataBuilder accessor = accessorMethods.get(field);
    if (accessor == null) {
      accessor =
          getAccessorMethod(
              field,
              InvocableMemberBodyBuilder.getInstance().appendFormalLine(
                  "return " + field.getFieldName().getSymbolName() + ";"));
      accessorMethods.put(field, accessor);
    }

    // Return governor method if exists
    if (accessor == null) {
      return getGovernorMethod(BeanInfoUtils.getAccessorMethodName(field));
    }

    return accessor.build();
  }

  protected MethodMetadataBuilder getAccessorMethod(final FieldMetadata field,
      final InvocableMemberBodyBuilder bodyBuilder) {
    return getMethod(PUBLIC, BeanInfoUtils.getAccessorMethodName(field), field.getFieldType(),
        null, null, bodyBuilder);
  }

  protected MethodMetadataBuilder getAccessorMethod(final JavaSymbolName fieldName,
      final JavaType fieldType) {
    return getAccessorMethod(fieldName, fieldType, InvocableMemberBodyBuilder.getInstance()
        .appendFormalLine("return " + fieldName + ";"));
  }

  protected MethodMetadataBuilder getAccessorMethod(final JavaSymbolName fieldName,
      final JavaType fieldType, final InvocableMemberBodyBuilder bodyBuilder) {
    return getMethod(PUBLIC, BeanInfoUtils.getAccessorMethodName(fieldName, fieldType), fieldType,
        null, null, bodyBuilder);
  }

  /**
   * Convenience method for returning a simple private field based on the
   * field name, type, and initializer.
   *
   * @param fieldName the field name
   * @param fieldType the field type
   * @param fieldInitializer the string to initialize the field with
   * @return null if the field exists on the governor, otherwise a new field
   *         with the given field name and type
   */
  protected FieldMetadataBuilder getField(final int modifier, final JavaSymbolName fieldName,
      final JavaType fieldType, final String fieldInitializer) {
    if (governorTypeDetails.getField(fieldName) != null) {
      return null;
    }

    addToImports(Arrays.asList(fieldType));
    return new FieldMetadataBuilder(getId(), modifier, fieldName, fieldType, fieldInitializer);
  }

  protected FieldMetadataBuilder getField(final JavaSymbolName fieldName, final JavaType fieldType) {
    return getField(PRIVATE, fieldName, fieldType, null);
  }

  /**
   * Returns the given method of the governor.
   *
   * @param methodName the name of the method for which to search
   * @param parameterTypes the method's parameter types
   * @return null if there was no such method
   * @see MemberFindingUtils#getDeclaredMethod(org.springframework.roo.classpath.details.MemberHoldingTypeDetails,
   *      JavaSymbolName, List)
   * @since 1.2.0
   */
  protected MethodMetadata getGovernorMethod(final JavaSymbolName methodName,
      final JavaType... parameterTypes) {
    return getGovernorMethod(methodName, Arrays.asList(parameterTypes));
  }

  /**
   * Returns the given method of the governor.
   *
   * @param methodName the name of the method for which to search
   * @param parameterTypes the method's parameter types
   * @return null if there was no such method
   * @see MemberFindingUtils#getDeclaredMethod(org.springframework.roo.classpath.details.MemberHoldingTypeDetails,
   *      JavaSymbolName, List)
   * @since 1.2.0 (previously called methodExists)
   */
  protected MethodMetadata getGovernorMethod(final JavaSymbolName methodName,
      final List<JavaType> parameterTypes) {
    return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, parameterTypes);
  }

  public final ItdTypeDetails getMemberHoldingTypeDetails() {
    return itdTypeDetails;
  }

  /**
   * Returns a public method given the method name, return type, parameter
   * types, parameter names, and method body.
   *
   * @param modifier the method modifier
   * @param methodName the method name
   * @param returnType the return type
   * @param parameterTypes a list of parameter types
   * @param parameterNames a list of parameter names
   * @param bodyBuilder the method body
   * @return null if the method exists on the governor, otherwise a new method
   *         is returned
   */
  protected MethodMetadataBuilder getMethod(final int modifier, final JavaSymbolName methodName,
      final JavaType returnType, final List<JavaType> parameterTypes,
      final List<JavaSymbolName> parameterNames, final InvocableMemberBodyBuilder bodyBuilder) {
    final MethodMetadata method = getGovernorMethod(methodName, parameterTypes);
    if (method != null) {
      return null;
    }

    addToImports(parameterTypes);
    return new MethodMetadataBuilder(getId(), modifier, methodName, returnType,
        AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
  }

  protected MethodMetadata getMutatorMethod(final FieldMetadata field) {
    // Check if the mutator has been cached
    MethodMetadataBuilder mutator = mutatorMethods.get(field);
    if (mutator == null) {
      mutator = getMutatorMethod(field.getFieldName(), field.getFieldType());
      mutatorMethods.put(field, mutator);
    }

    // Return governor method
    if (mutator == null) {
      return getGovernorMethod(BeanInfoUtils.getMutatorMethodName(field.getFieldName()),
          field.getFieldType());
    }

    return mutator.build();
  }

  protected MethodMetadataBuilder getMutatorMethod(final JavaSymbolName fieldName,
      final JavaType parameterType) {
    return getMutatorMethod(
        fieldName,
        parameterType,
        InvocableMemberBodyBuilder.getInstance().appendFormalLine(
            "this." + fieldName.getSymbolName() + " = " + fieldName.getSymbolName() + ";"));
  }

  protected MethodMetadataBuilder getMutatorMethod(final JavaSymbolName fieldName,
      final JavaType parameterType, final InvocableMemberBodyBuilder bodyBuilder) {
    return getMethod(PUBLIC, BeanInfoUtils.getMutatorMethodName(fieldName),
        JavaType.VOID_PRIMITIVE, Arrays.asList(parameterType), Arrays.asList(fieldName),
        bodyBuilder);
  }

  /**
   * Returns the metadata for an annotation of the given type if the governor
   * does not already have one.
   *
   * @param annotationType the type of annotation to generate (required)
   * @return <code>null</code> if the governor already has that annotation
   */
  protected AnnotationMetadata getTypeAnnotation(final JavaType annotationType) {
    if (governorTypeDetails.getAnnotation(annotationType) != null) {
      return null;
    }
    return new AnnotationMetadataBuilder(annotationType).build();
  }

  /**
   * Indicates whether the governor has a method with the given signature.
   *
   * @param methodName the name of the method for which to search
   * @param parameterTypes the method's parameter types
   * @return see above
   * @since 1.2.0
   */
  protected boolean governorHasMethod(final JavaSymbolName methodName,
      final JavaType... parameterTypes) {
    return getGovernorMethod(methodName, parameterTypes) != null;
  }

  /**
   * Indicates whether the governor has a method with the given signature.
   *
   * @param methodName the name of the method for which to search
   * @param parameterTypes the method's parameter types
   * @return see above
   * @since 1.2.1
   */
  protected boolean governorHasMethod(final JavaSymbolName methodName,
      final List<JavaType> parameterTypes) {
    return getGovernorMethod(methodName, parameterTypes) != null;
  }

  /**
   * Indicates whether the governor has a method with the given method name
   * regardless of method parameters.
   *
   * @param methodName the name of the method for which to search
   * @return see above
   * @since 1.2.0
   */
  protected boolean governorHasMethodWithSameName(final JavaSymbolName methodName) {
    return MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName) != null;
  }

  @Override
  public int hashCode() {
    if (itdTypeDetails != null) {
      return itdTypeDetails.hashCode();
    }
    return builder.build().hashCode();
  }

  /**
   * Determines if the presented class (or any of its superclasses) implements
   * the target interface.
   *
   * @param clazz the cid to search
   * @param interfaceTarget the interface to locate
   * @return true if the class or any of its superclasses contains the
   *         specified interface
   */
  protected boolean isImplementing(final ClassOrInterfaceTypeDetails clazz,
      final JavaType interfaceTarget) {
    if (clazz.getImplementsTypes().contains(interfaceTarget)) {
      return true;
    }
    if (clazz.getSuperclass() != null) {
      return isImplementing(clazz.getSuperclass(), interfaceTarget);
    }
    return false;
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }

  /**
   * Return current aspect name
   * @return
   */
  public JavaType getAspectName() {
    return aspectName;
  }

  public JavaType getDestination() {
    return destination;
  }

  /**
   * Returns String which represents type inside ITD
   *
   * Delegates on {@link JavaType#getNameIncludingTypeParameters(boolean, org.springframework.roo.model.ImportRegistrationResolver)}
   * (asStatic = false)
   *
   * @param type
   * @return
   */
  protected String getNameOfJavaType(JavaType type) {
    return getNameOfJavaType(type, false);
  }

  /**
   * Returns String which represents type inside ITD
   *
   * Delegates on {@link JavaType#getNameIncludingTypeParameters(boolean, org.springframework.roo.model.ImportRegistrationResolver)}
   *
   * @param type
   * @param asStatic
   * @return
   */
  protected String getNameOfJavaType(JavaType type, boolean asStatic) {
    return type.getNameIncludingTypeParameters(asStatic, builder.getImportRegistrationResolver());
  }
}
