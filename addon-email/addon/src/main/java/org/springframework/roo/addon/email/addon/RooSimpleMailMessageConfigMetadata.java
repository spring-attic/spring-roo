package org.springframework.roo.addon.email.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.email.annotations.RooSimpleMailMessageConfig;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooSimpleMailMessageConfig} annotation.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class RooSimpleMailMessageConfigMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = RooSimpleMailMessageConfigMetadata.class
      .getName();
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
   * @param identifier
   *            the ID of the metadata to create (must be a valid ID)
   * @param aspectName
   *            the name of the ITD to be created (required)
   * @param governorPhysicalTypeMetadata
   *            the governor (required)
   */
  public RooSimpleMailMessageConfigMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {

    super(identifier, aspectName, governorPhysicalTypeMetadata);

    // Add all necessary fields
    addFields();

    // Including @Bean method
    builder.addMethod(getSimpleMailMessageMethod());

    // Create a representation of the desired output ITD
    itdTypeDetails = builder.build();

  }

  /**
   * Method to add all necessary fields
   */
  private void addFields() {

    // Generating host field
    FieldMetadataBuilder hostField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName("host"),
            JavaType.STRING, null);
    AnnotationMetadataBuilder hostValueAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.VALUE);
    hostValueAnnotation.addStringAttribute("value", "${spring.mail.host}");
    hostField.addAnnotation(hostValueAnnotation);
    builder.addField(hostField);

    // Generating from field
    FieldMetadataBuilder fromField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName("from"),
            JavaType.STRING, null);
    AnnotationMetadataBuilder fromValueAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.VALUE);
    fromValueAnnotation.addStringAttribute("value", "${email.from}");
    fromField.addAnnotation(fromValueAnnotation);
    builder.addField(fromField);

    // Generating subject field
    FieldMetadataBuilder subjectField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName("subject"),
            JavaType.STRING, null);
    AnnotationMetadataBuilder subjectValueAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.VALUE);
    subjectValueAnnotation.addStringAttribute("value", "${email.subject}");
    subjectField.addAnnotation(subjectValueAnnotation);
    builder.addField(subjectField);

  }

  /**
   * Method that generates simpleMailMessage method
   * 
   * @return
   */
  private MethodMetadataBuilder getSimpleMailMessageMethod() {
    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Create the method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    buildSimpleMailMessageMethodBody(bodyBuilder);

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
            new JavaSymbolName("simpleMailMessage"), SpringJavaType.SIMPLE_MAIL_MESSAGE,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(
        "org.springframework.context.annotation.Bean")));

    return methodBuilder; // Build and return a MethodMetadata
  }

  /**
   * Method that generates simpleMailMessage method body
   * 
   * @param bodyBuilder
   */
  private void buildSimpleMailMessageMethodBody(InvocableMemberBodyBuilder bodyBuilder) {

    // SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
    bodyBuilder.appendFormalLine("SimpleMailMessage simpleMailMessage = new SimpleMailMessage();");

    // simpleMailMessage.setFrom(from);
    bodyBuilder.appendFormalLine("simpleMailMessage.setFrom(from);");

    // simpleMailMessage.setSubject(subject);
    bodyBuilder.appendFormalLine("simpleMailMessage.setSubject(subject);");

    // return simpleMailMessage;
    bodyBuilder.appendFormalLine("return simpleMailMessage;");

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
