package org.springframework.roo.addon.layers.service.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.layers.service.annotations.RooService;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooService}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ServiceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ServiceMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Constructor
   * 
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   * @param entity entity referenced on interface
   * @param identifierType the type of the entity's identifier field
   *            (required)
   * @param readOnly specifies if current entity is defined as readOnly or not
   * @param finders list of finders added to current entity
   * 
   */
  public ServiceMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JavaType identifierType, final boolean readOnly, final List<FinderMethod> finders) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    Validate.notNull(entity, "ERROR: Entity required to generate service interface");
    Validate.notNull(identifierType,
        "ERROR: Entity identifier type required to generate service interface");

    // Generating readOnly methods for every services
    ensureGovernorHasMethod(getFindAllMethod(entity));
    ensureGovernorHasMethod(getFindAllIterableMethod(entity, identifierType));
    ensureGovernorHasMethod(getFindOneMethod(entity, identifierType));

    // Generating persistent methods for not readOnly entities
    if (!readOnly) {
      ensureGovernorHasMethod(getSaveMethod(entity));
      ensureGovernorHasMethod(getDeleteMethod(identifierType));
      ensureGovernorHasMethod(getSaveBatchMethod(entity));
      ensureGovernorHasMethod(getDeleteBatchMethod(identifierType));
    }

    // Generating finders
    for (FinderMethod finder : finders) {
      ensureGovernorHasMethod(getFinderMethod(finder));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates method "findAll" method.
   * 
   * @param entity
   * @return MethodMetadataBuilder with public List <Entity> findAll();
   *         structure
   */
  private MethodMetadataBuilder getFindAllMethod(JavaType entity) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    JavaType listEntityJavaType =
        new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(entity));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "findAll"), listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates method "findAll" with iterable parameter.
   * 
   * @param entity
   * @param identifierType
   * @return MethodMetadataBuilder with public List <Entity> findAll(Iterable
   *         <Long> ids) structure
   */
  private MethodMetadataBuilder getFindAllIterableMethod(JavaType entity, JavaType identifierType) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.lang.Iterable", 0,
        DataType.TYPE, null, Arrays.asList(identifierType))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));


    JavaType listEntityJavaType =
        new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(entity));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "findAll"), listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }


  /**
   * Method that generates method "findOne".
   * 
   * @param entity
   * @param identifierType
   * @return MethodMetadataBuilder with public Entity findOne(Long id);
   *         structure
   */
  private MethodMetadataBuilder getFindOneMethod(JavaType entity, JavaType identifierType) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(identifierType));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "findOne"), entity, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "save" method.
   * 
   * @param entity
   * @return MethodMetadataBuilder with public Entity save(Entity entity);
   *         structure
   */
  private MethodMetadataBuilder getSaveMethod(JavaType entity) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(entity));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("entity"));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "save"), entity, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "delete" method.
   * 
   * @param identifierType
   * @return MethodMetadataBuilder with public void delete(Long id); structure
   */
  private MethodMetadataBuilder getDeleteMethod(JavaType identifierType) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(identifierType));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "delete"), JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "save" batch method.
   * 
   * @param entity
   * @return MethodMetadataBuilder with public List<Entity> save(Iterable
   *         <Entity> entities); structure
   */
  private MethodMetadataBuilder getSaveBatchMethod(JavaType entity) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.lang.Iterable", 0,
        DataType.TYPE, null, Arrays.asList(entity))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("entities"));

    JavaType listEntityJavaType =
        new JavaType("java.util.List", 0, DataType.TYPE, null, Arrays.asList(entity));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "save"), listEntityJavaType, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates "delete" batch method
   * 
   * @param identifierType
   * @return MethodMetadataBuilder with public void delete(Iterable
   *         <Long> ids); structure
   */
  private MethodMetadataBuilder getDeleteBatchMethod(JavaType identifierType) {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.lang.Iterable", 0,
        DataType.TYPE, null, Arrays.asList(identifierType))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, new JavaSymbolName(
            "delete"), JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates finder method on current interface
   * 
   * @param finderMethod
   * @return
   */
  private MethodMetadataBuilder getFinderMethod(FinderMethod finderMethod) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    for (FinderParameter param : finderMethod.getParameters()) {
      parameterTypes.add(AnnotatedJavaType.convertFromJavaType(param.getType()));
      parameterNames.add(param.getName());
    }

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            finderMethod.getMethodName(), finderMethod.getReturnType(), parameterTypes,
            parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }


  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }
}
