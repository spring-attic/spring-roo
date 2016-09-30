package org.springframework.roo.addon.finder.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.finder.annotations.RooFinder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for {@link RooFinder}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class FinderMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = FinderMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private List<MethodMetadata> finders = new ArrayList<MethodMetadata>();
  private List<MethodMetadata> countMethods = new ArrayList<MethodMetadata>();

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
   * @param annotationValues (required)
   * @param identifierType the type of the entity's identifier field
   *            (required)
   * @param finders the list of finders to be included
   */
  public FinderMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, List<FinderMethod> finders) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    // Including finders methods
    for (FinderMethod finderMethod : finders) {
      MethodMetadata method = getFinderMethod(finderMethod).build();
      ensureGovernorHasMethod(getFinderMethod(finderMethod));
      this.finders.add(method);

      // Generate a count method for each finder if they aren't count methods
      if (!StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
        MethodMetadata countMethod =
            getCountMethod(finderMethod.getParameters().get(0).getType(),
                finderMethod.getMethodName());
        ensureGovernorHasMethod(new MethodMetadataBuilder(countMethod));
        if (!countMethods.contains(countMethod)) {
          countMethods.add(countMethod);
        }
      }
    }

    // Build the ITD
    itdTypeDetails = builder.build();
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

    // Add additional Pageable method
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));
    parameterNames.add(new JavaSymbolName("pageable"));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT,
            finderMethod.getMethodName(), finderMethod.getReturnType(), parameterTypes,
            parameterNames, null);

    return methodBuilder; // Build and return a MethodMetadata
    // instance
  }

  /**
   * Method that generates count methods for finders.
   * 
   * @param formBean the object containing the properties to search to
   * @param javaSymbolName the finder name
   * @return
   */
  public MethodMetadata getCountMethod(JavaType formBean, JavaSymbolName finderName) {

    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(formBean));
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("formBean"));

    // Create count method name
    String countName = finderName.getSymbolName();
    if (StringUtils.startsWith(countName, "find")) {
      countName = StringUtils.removeStart(countName, "find");
    } else if (StringUtils.startsWith(countName, "query")) {
      countName = StringUtils.removeStart(countName, "query");
    } else if (StringUtils.startsWith(countName, "read")) {
      countName = StringUtils.removeStart(countName, "read");
    }
    countName = "count".concat(countName);
    JavaSymbolName countMethodName = new JavaSymbolName(countName);

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.ABSTRACT, countMethodName,
            JavaType.LONG_PRIMITIVE, parameterTypes, parameterNames, null);

    return methodBuilder.build(); // Build and return a MethodMetadata
  }

  /**
   * Return all finder methods
   * 
   * @return
   */
  public List<MethodMetadata> getFinders() {
    return this.finders;
  }

  /**
   * Return all count methods
   * 
   * @return
   */
  public List<MethodMetadata> getCountMethods() {
    return this.countMethods;
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
