package org.springframework.roo.addon.dto.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dto.addon.DtoOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.ListField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.converters.LastUsed;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Path;

/**
 * Implementation of {@link JpaOperations}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoOperationsImpl implements DtoOperations {

  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;

  @Override
  public boolean isDtoCreationPossible() {
    return projectOperations.isFocusedProjectAvailable();
  }

  @Override
  public ClassOrInterfaceTypeDetailsBuilder createDto(JavaType name, boolean immutable,
      boolean utilityMethods, boolean serializable, boolean fromEntity) {

    Validate.isTrue(!JdkJavaType.isPartOfJavaLang(name.getSimpleTypeName()),
        "Class name '%s' is part of java.lang", name.getSimpleTypeName());

    // Create file
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, name.getModule()));
    final ClassOrInterfaceTypeDetailsBuilder cidBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);

    // Add @RooDTO and @RooJavaBean
    AnnotationMetadataBuilder rooDtoAnnotation = new AnnotationMetadataBuilder(RooJavaType.ROO_DTO);
    AnnotationMetadataBuilder rooJavaBeanAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_JAVA_BEAN);
    if (immutable) {
      rooDtoAnnotation.addBooleanAttribute("immutable", immutable);
      rooJavaBeanAnnotation.addBooleanAttribute("settersByDefault", false);
    }
    cidBuilder.addAnnotation(rooDtoAnnotation);
    cidBuilder.addAnnotation(rooJavaBeanAnnotation);

    // Add utility annotations if necessary
    if (utilityMethods) {
      AnnotationMetadataBuilder rooToStringAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_TO_STRING);
      AnnotationMetadataBuilder rooEqualsAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_EQUALS);
      cidBuilder.addAnnotation(rooToStringAnnotation);
      cidBuilder.addAnnotation(rooEqualsAnnotation);
    }

    // Add @RooSerializable if necessary
    if (serializable) {
      AnnotationMetadataBuilder rooSerializableAnnotation =
          new AnnotationMetadataBuilder(RooJavaType.ROO_SERIALIZABLE);
      cidBuilder.addAnnotation(rooSerializableAnnotation);
    }

    if (!fromEntity) {
      // Write changes to disk
      typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    return cidBuilder;
  }

  @Override
  public void createDtoFromAll(boolean immutable, boolean utilityMethods, boolean serializable) {

    // Get all entities
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            JpaJavaType.ENTITY);
    List<String> entityNames = new ArrayList<String>();

    // Create DTO for each entity
    for (ClassOrInterfaceTypeDetails entity : entities) {
      String name = entity.getType().getSimpleTypeName();

      // Attempt to find a no repeated name
      if (!entityNames.contains(name)) {
        entityNames.add(name);
      } else {
        name =
            StringUtils.capitalize(entity.getType().getPackage().getLastElement()).concat(
                entity.getType().getSimpleTypeName());
        int count = 2;
        while (entityNames.contains(name)) {
          name = entity.getType().getSimpleTypeName().concat(String.valueOf(count));
          count++;
        }
      }

      // Create DTO
      JavaType dtoType =
          new JavaType(String.format("%s.%sDTO", entity.getType().getPackage()
              .getFullyQualifiedPackageName(), name), entity.getType().getModule());
      ClassOrInterfaceTypeDetailsBuilder dtoBuilder =
          createDto(dtoType, immutable, utilityMethods, serializable, true);

      // Get entity fields
      MemberDetails entityDetails =
          memberDetailsScanner.getMemberDetails(this.getClass().getName(), entity);
      List<FieldMetadata> entityFields = entityDetails.getFields();
      addFieldsToDto(immutable, dtoBuilder, entityFields);

      // Build and save changes to disk
      typeManagementService.createOrUpdateTypeOnDisk(dtoBuilder.build());
    }
  }

  @Override
  public void createDtoFromEntity(JavaType name, JavaType entity, String fields,
      String excludeFields, boolean immutable, boolean utilityMethods, boolean serializable) {

    // Get entity details
    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(entity);
    List<FieldMetadata> allFields =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid).getFields();
    List<FieldMetadata> dtoFields = null;

    boolean include = true;
    if (fields != null) {
      include = true;
      dtoFields = buildFieldsFromString(allFields, fields, include);
    } else if (excludeFields != null) {
      include = false;
      dtoFields = buildFieldsFromString(allFields, excludeFields, include);
    } else {
      dtoFields = allFields;
    }

    JavaType dto = null;
    if (name != null) {
      dto = name;
    } else {
      dto =
          new JavaType(String.format("%s.%sDTO",
              entity.getPackage().getFullyQualifiedPackageName(), entity.getSimpleTypeName()),
              entity.getModule());
    }

    // Create DTO and add fields to it
    ClassOrInterfaceTypeDetailsBuilder dtoBuilder =
        createDto(dto, immutable, utilityMethods, serializable, true);
    addFieldsToDto(immutable, dtoBuilder, dtoFields);

    // Build and save changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(dtoBuilder.build());
  }

  /**
   * Returns the definitive list of fields to include in DTO.
   * 
   * @param allFields the fields of the entity
   * @param fieldsString the fields provided by user
   * @param includeMode whether the fields provided should be included or excluded from DTO 
   * @return List<FieldMetadata> with the fields to add in the DTO
   */
  public List<FieldMetadata> buildFieldsFromString(List<FieldMetadata> allFields,
      String fieldsString, boolean includeMode) {

    // Create array of field names from command String
    fieldsString = fieldsString.trim();
    String[] fields = fieldsString.split(",");

    // Create lists with fields which could be in DTO
    List<FieldMetadata> dtoFields = new ArrayList<FieldMetadata>();

    // Iterate over all entity fields
    for (FieldMetadata entityField : allFields) {
      boolean exclude = false;
      for (int i = 0; i < fields.length; i++) {

        // If entity field matches with any provided field, add it
        if (includeMode && fields[i].equals(entityField.getFieldName().getSymbolName())) {
          dtoFields.add(entityField);
          break;
        } else if (!includeMode && fields[i].equals(entityField.getFieldName().getSymbolName())) {
          exclude = true;
          break;
        }
      }

      // If exclude is false and method is in exclude mode, entity field should be added
      if (!exclude && !includeMode) {
        dtoFields.add(entityField);
      }
    }

    return dtoFields;
  }

  /**
   * Removes persistence annotations of provided fields and adds them to a 
   * ClassOrInterfaceTypeDetailsBuilder representing a DTO in construction. 
   * Also adds final modifier to fields if required.
   * 
   * @param immutable whether the fields should be <code>final</code>
   * @param dtoBuilder the ClassOrInterfaceTypeDetailsBuilder for building the DTO class
   * @param entityFields the List<FieldMetadata> to add
   */
  private void addFieldsToDto(boolean immutable, ClassOrInterfaceTypeDetailsBuilder dtoBuilder,
      List<FieldMetadata> entityFields) {
    for (FieldMetadata field : entityFields) {

      // List and Set types require special management
      FieldMetadataBuilder fieldBuilder = null;
      FieldDetails fieldDetails = null;
      if (field.getFieldType().getFullyQualifiedTypeName().equals("java.util.Set")) {
        JavaType fieldType = field.getFieldType().getParameters().get(0);
        fieldDetails =
            new SetField(dtoBuilder.getDeclaredByMetadataId(), new JavaType(
                JdkJavaType.SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(fieldType)), field.getFieldName(), fieldType, null, null);
        fieldBuilder = new FieldMetadataBuilder(fieldDetails);
        fieldBuilder.setModifier(field.getModifier());
        fieldBuilder.setAnnotations(field.getAnnotations());
      } else if (field.getFieldType().getFullyQualifiedTypeName().equals("java.util.List")) {
        JavaType fieldType = field.getFieldType().getParameters().get(0);
        fieldDetails =
            new ListField(dtoBuilder.getDeclaredByMetadataId(), new JavaType(
                JdkJavaType.LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(fieldType)), field.getFieldName(), fieldType, null, null);
        fieldBuilder = new FieldMetadataBuilder(fieldDetails);
        fieldBuilder.setModifier(field.getModifier());
        fieldBuilder.setAnnotations(field.getAnnotations());
      } else {
        // Can't just copy FieldMetadata because field.declaredByMetadataId will be the same
        fieldBuilder = new FieldMetadataBuilder(dtoBuilder.getDeclaredByMetadataId(), field);
      }

      // Add dependency between modules
      typeLocationService.addModuleDependency(dtoBuilder.getName().getModule(),
          field.getFieldType());

      // If it is a CollectionField it needs an initializer
      String initializer = null;
      if (fieldDetails instanceof CollectionField) {
        final CollectionField collectionField = (CollectionField) fieldDetails;
        initializer = "new " + collectionField.getInitializer() + "()";
      } else if (fieldDetails instanceof DateField
          && fieldDetails.getFieldName().getSymbolName().equals("created")) {
        initializer = "new Date()";
      }
      fieldBuilder.setFieldInitializer(initializer);

      // Remove persistence annotations
      List<AnnotationMetadata> annotations = field.getAnnotations();
      for (AnnotationMetadata annotation : annotations) {
        if (annotation.getAnnotationType().getFullyQualifiedTypeName()
            .contains("javax.persistence")) {
          fieldBuilder.removeAnnotation(annotation.getAnnotationType());
        }
      }

      // Add field as final if necessary
      if (immutable) {
        fieldBuilder.setModifier(Modifier.FINAL);
      }

      // Build field
      FieldMetadata dtoField = fieldBuilder.build();

      // Add field to DTO
      dtoBuilder.addField(dtoField);
    }
  }

}
