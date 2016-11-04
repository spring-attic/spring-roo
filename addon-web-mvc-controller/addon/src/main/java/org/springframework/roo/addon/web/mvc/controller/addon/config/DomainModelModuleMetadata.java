package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooDomainModelModule;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooDomainModelModule}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class DomainModelModuleMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
  private static final JavaType JACKSON_SIMPLE_MODULE = new JavaType(
      "com.fasterxml.jackson.databind.module.SimpleModule");
  private static final JavaType BOOT_JACKSON_JSON_COMPONENT = new JavaType(
      "org.springframework.boot.jackson.JsonComponent");

  private static final String PROVIDES_TYPE_STRING = DomainModelModuleMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);


  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
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


  private final Map<JavaType, JavaType> mixins;

  /**
   * Constructor
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   *
   */
  public DomainModelModuleMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final Map<JavaType, JavaType> mixins) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);


    Map<JavaType, JavaType> orderedMixins = new TreeMap<JavaType, JavaType>(mixins);
    this.mixins = Collections.unmodifiableMap(orderedMixins);

    // Add @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(BOOT_JACKSON_JSON_COMPONENT));

    // Add extends WebMvcConfigurerAdapter
    ensureGovernorExtends(JACKSON_SIMPLE_MODULE);

    // Add constructor
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(getConstructor()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private ConstructorMetadata getConstructor() {
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.setModifier(Modifier.PUBLIC);

    bodyBuilder.appendFormalLine("// Mixin registration");
    bodyBuilder.newLine();

    // Generating body
    for (Entry<JavaType, JavaType> item : mixins.entrySet()) {
      bodyBuilder.appendFormalLine("setMixInAnnotation(%s.class, %s.class);",
          getNameOfJavaType(item.getKey()), getNameOfJavaType(item.getValue()));

    }

    // Adding body
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();
  }

  /**
   * @return registered mixins
   */
  public Map<JavaType, JavaType> getMixins() {
    return this.mixins;
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
