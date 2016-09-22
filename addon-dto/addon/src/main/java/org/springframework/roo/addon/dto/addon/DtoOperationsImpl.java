package org.springframework.roo.addon.dto.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.ListField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.ShellContext;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of {@link JpaOperations}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class DtoOperationsImpl implements DtoOperations {

  protected final static Logger LOGGER = HandlerUtils.getLogger(DtoOperationsImpl.class);

  @Reference
  private ProjectOperations projectOperations;
  @Reference
  private TypeManagementService typeManagementService;
  @Reference
  private TypeLocationService typeLocationService;
  @Reference
  private MemberDetailsScanner memberDetailsScanner;
  @Reference
  private PathResolver pathResolver;
  @Reference
  private FileManager fileManager;

  @Override
  public boolean isDtoCreationPossible() {
    return projectOperations.isFocusedProjectAvailable();
  }

  @Override
  public boolean isEntityProjectionPossible() {
    return typeLocationService.findTypesWithAnnotation(RooJavaType.ROO_JPA_ENTITY).size() > 0;
  }

  @Override
  public void createDto(JavaType name, boolean immutable, boolean utilityMethods,
      boolean serializable) {

    Validate.isTrue(!JdkJavaType.isPartOfJavaLang(name.getSimpleTypeName()),
        "Class name '%s' is part of java.lang", name.getSimpleTypeName());

    // Set focus on dto module
    projectOperations.setModule(projectOperations.getPomFromModuleName(name.getModule()));

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

    typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
  }

  @Override
  public void createProjection(JavaType entity, JavaType name, String fields, String suffix) {
    Validate.notNull(name, "Use --class to select the name of the Projection.");

    // Set focus on projection module
    projectOperations.setModule(projectOperations.getPomFromModuleName(name.getModule()));

    Map<String, FieldMetadata> fieldsToAdd = new HashMap<String, FieldMetadata>();
    boolean onlyMainEntityFields = true;
    if (fields != null) {
      onlyMainEntityFields = false;
      fieldsToAdd = buildFieldsFromString(fields, entity);
    } else {
      List<FieldMetadata> allFields =
          memberDetailsScanner.getMemberDetails(this.getClass().getName(),
              typeLocationService.getTypeDetails(entity)).getFields();
      for (FieldMetadata field : allFields) {
        fieldsToAdd.put(field.getFieldName().getSymbolName(), field);
      }
    }

    // Create projection
    final String declaredByMetadataId =
        PhysicalTypeIdentifier.createIdentifier(name,
            LogicalPath.getInstance(Path.SRC_MAIN_JAVA, name.getModule()));
    final ClassOrInterfaceTypeDetailsBuilder projectionBuilder =
        new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name,
            PhysicalTypeCategory.CLASS);

    // Add fields to projection
    addFieldsToProjection(projectionBuilder, fieldsToAdd);

    // @RooJavaBean, @RooToString and @RooEquals
    AnnotationMetadataBuilder rooJavaBeanAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_JAVA_BEAN);
    rooJavaBeanAnnotation.addBooleanAttribute("settersByDefault", false);
    projectionBuilder.addAnnotation(rooJavaBeanAnnotation);
    AnnotationMetadataBuilder rooToStringAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_TO_STRING);
    AnnotationMetadataBuilder rooEqualsAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_EQUALS);
    projectionBuilder.addAnnotation(rooToStringAnnotation);
    projectionBuilder.addAnnotation(rooEqualsAnnotation);

    // Add @RooEntityProjection
    AnnotationMetadataBuilder projectionAnnotation =
        new AnnotationMetadataBuilder(RooJavaType.ROO_ENTITY_PROJECTION);
    projectionAnnotation.addClassAttribute("entity", entity);
    List<StringAttributeValue> fieldNames = new ArrayList<StringAttributeValue>();

    if (onlyMainEntityFields) {

      // Should add all entity fields
      for (FieldMetadata field : fieldsToAdd.values()) {
        fieldNames.add(new StringAttributeValue(new JavaSymbolName("fields"), field.getFieldName()
            .getSymbolName()));
      }
    } else {

      // --fields option has been completed and validated, so build annotation 'fields' 
      // param from selected fields
      String[] fieldsFromCommand = StringUtils.split(fields, ",");
      for (int i = 0; i < fieldsFromCommand.length; i++) {
        fieldNames
            .add(new StringAttributeValue(new JavaSymbolName("fields"), fieldsFromCommand[i]));
      }
    }
    projectionAnnotation.addAttribute(new ArrayAttributeValue<StringAttributeValue>(
        new JavaSymbolName("fields"), fieldNames));
    projectionBuilder.addAnnotation(projectionAnnotation);

    // Build and save changes to disk
    typeManagementService.createOrUpdateTypeOnDisk(projectionBuilder.build());
  }

  @Override
  public void createAllProjections(String suffix, ShellContext shellContext) {

    // Get all entities
    Set<ClassOrInterfaceTypeDetails> entities =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JPA_ENTITY,
            JpaJavaType.ENTITY);
    List<String> projectionNames = new ArrayList<String>();

    // Get all Projections to check names
    Set<ClassOrInterfaceTypeDetails> currentProjections =
        typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_DTO);
    for (ClassOrInterfaceTypeDetails projection : currentProjections) {
      projectionNames.add(projection.getType().getFullyQualifiedTypeName());
    }

    // Create Projection for each entity
    for (ClassOrInterfaceTypeDetails entity : entities) {

      String name = entity.getType().getFullyQualifiedTypeName().concat(suffix);

      if (projectionNames.contains(name) && !shellContext.isForce()) {
        throw new IllegalArgumentException(
            "One or more Projections with pre-generated names already "
                + "exist and cannot be created. Use --force parameter to overwrite them. Suffix for pre-generated names is "
                    .concat(suffix));
      }

      // Create Projection if doesn't exists, already
      JavaType projectionType = new JavaType(name, entity.getType().getModule());
      final String entityFilePathIdentifier =
          pathResolver.getCanonicalPath(projectionType.getModule(), Path.SRC_MAIN_JAVA,
              projectionType);
      if (!fileManager.exists(entityFilePathIdentifier)) {
        createProjection(entity.getType(), projectionType, null, suffix);
      }
    }
  }

  /**
   * Returns the list of fields to include in Projection.
   * 
   * @param fieldsString the fields provided by user.
   * @param entity the associated entity to use for searching the fields.
   * @return Map<String, FieldMetadata> with the field name to add in the Projection 
   *            and its metadata.
   */
  public Map<String, FieldMetadata> buildFieldsFromString(String fieldsString, JavaType entity) {

    // Create Map for storing FieldMetadata and it's future new name
    Map<String, FieldMetadata> fieldsToAdd = new HashMap<String, FieldMetadata>();

    // Create array of field names from command String
    fieldsString = fieldsString.trim();
    String[] fields = fieldsString.split(",");

    ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(entity);
    List<FieldMetadata> allFields =
        memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid).getFields();

    // Iterate over all specified fields
    for (int i = 0; i < fields.length; i++) {
      String fieldName = "";
      boolean found = false;

      // Iterate over all entity fields
      for (FieldMetadata field : allFields) {
        if (field.getFieldName().getSymbolName().equals(fields[i])) {
          // If found, add field to returned map
          fieldsToAdd.put(field.getFieldName().getSymbolName(), field);
          found = true;
          break;
        }
      }

      if (!found) {

        // The field isn't in the entity, should be in one of its relations
        String[] splittedByDot = StringUtils.split(fields[i], ".");
        JavaType currentEntity = entity;
        if (fields[i].contains(".")) {

          // Search a matching relation field
          for (int t = 0; t < splittedByDot.length; t++) {
            ClassOrInterfaceTypeDetails currentEntityCid =
                typeLocationService.getTypeDetails(currentEntity);
            List<FieldMetadata> currentEntityFields =
                memberDetailsScanner.getMemberDetails(this.getClass().getName(), currentEntityCid)
                    .getFields();
            boolean relationFieldFound = false;

            // Iterate to build the field-levels of the relation field
            for (FieldMetadata field : currentEntityFields) {
              if (field.getFieldName().getSymbolName().equals(splittedByDot[t])
                  && t != splittedByDot.length - 1
                  && typeLocationService.getTypeDetails(field.getFieldType()) != null
                  && typeLocationService.getTypeDetails(field.getFieldType()).getAnnotation(
                      RooJavaType.ROO_JPA_ENTITY) != null) {

                // Field is an entity and we should look into its fields
                currentEntity = field.getFieldType();
                found = true;
                relationFieldFound = true;
                if (t == 0) {
                  fieldName = fieldName.concat(field.getFieldName().getSymbolName());
                } else {
                  fieldName =
                      fieldName
                          .concat(StringUtils.capitalize(field.getFieldName().getSymbolName()));
                }
                break;
              } else if (field.getFieldName().getSymbolName().equals(splittedByDot[t])) {

                // Add field to projection fields
                fieldName =
                    fieldName.concat(StringUtils.capitalize(field.getFieldName().getSymbolName()));
                fieldsToAdd.put(fieldName, field);
                found = true;
                relationFieldFound = true;
                break;
              }
            }

            // If not found, relation field is bad written
            if (!relationFieldFound) {
              throw new IllegalArgumentException(String.format(
                  "Field %s couldn't be located in %s. Please, be sure that it is well written.",
                  splittedByDot[t], currentEntity.getFullyQualifiedTypeName()));
            }
          }
        } else {

          // Not written as a relation field
          throw new IllegalArgumentException(String.format(
              "Field %s couldn't be located in entity %s", fields[i],
              entity.getFullyQualifiedTypeName()));
        }
      }

      // If still not found, field is bad written
      if (!found) {
        throw new IllegalArgumentException(String.format(
            "Field %s couldn't be located. Please, be sure that it is well written.", fields[i]));
      }
    }

    return fieldsToAdd;
  }

  /**
   * Removes persistence annotations of provided fields and adds them to a 
   * ClassOrInterfaceTypeDetailsBuilder representing a Projection in construction. 
   * Also adds final modifier to fields if required.
   * 
   * @param projectionBuilder the ClassOrInterfaceTypeDetailsBuilder for building the 
   *            Projection class.
   * @param fieldsToAdd the List<FieldMetadata> to add.
   */
  private void addFieldsToProjection(ClassOrInterfaceTypeDetailsBuilder projectionBuilder,
      Map<String, FieldMetadata> fieldsToAdd) {
    Iterator<Entry<String, FieldMetadata>> iterator = fieldsToAdd.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, FieldMetadata> entry = iterator.next();
      FieldMetadata field = entry.getValue();

      // List and Set types require special management
      FieldMetadataBuilder fieldBuilder = null;
      FieldDetails fieldDetails = null;
      if (field.getFieldType().getFullyQualifiedTypeName().equals("java.util.Set")) {
        JavaType fieldType = field.getFieldType().getParameters().get(0);
        fieldDetails =
            new SetField(projectionBuilder.getDeclaredByMetadataId(), new JavaType(
                JdkJavaType.SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(fieldType)), field.getFieldName(), fieldType, null, null, true);
        fieldBuilder = new FieldMetadataBuilder(fieldDetails);
        fieldBuilder.setModifier(field.getModifier());
        fieldBuilder.setAnnotations(field.getAnnotations());
      } else if (field.getFieldType().getFullyQualifiedTypeName().equals("java.util.List")) {
        JavaType fieldType = field.getFieldType().getParameters().get(0);
        fieldDetails =
            new ListField(projectionBuilder.getDeclaredByMetadataId(), new JavaType(
                JdkJavaType.LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(fieldType)), field.getFieldName(), fieldType, null, null, true);
        fieldBuilder = new FieldMetadataBuilder(fieldDetails);
        fieldBuilder.setModifier(field.getModifier());
        fieldBuilder.setAnnotations(field.getAnnotations());
      } else {
        // Can't just copy FieldMetadata because field.declaredByMetadataId will be the same
        fieldBuilder = new FieldMetadataBuilder(projectionBuilder.getDeclaredByMetadataId(), field);
      }

      // Add dependency between modules
      typeLocationService.addModuleDependency(projectionBuilder.getName().getModule(),
          field.getFieldType());

      // Set new fieldName
      fieldBuilder.setFieldName(new JavaSymbolName(entry.getKey()));

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

        } else if (annotation.getAnnotationType().getFullyQualifiedTypeName()
            .startsWith("javax.validation")) {

          // Add validation dependency
          projectOperations.addDependency(projectionBuilder.getName().getModule(), new Dependency(
              "javax.validation", "validation-api", null));
        }
      }

      projectOperations.addDependency(projectionBuilder.getName().getModule(), new Dependency(
          "org.springframework.boot", "spring-boot-starter-data-jpa", null));

      fieldBuilder.setModifier(Modifier.PRIVATE);

      // Build field
      FieldMetadata projectionField = fieldBuilder.build();

      // Add field to DTO
      projectionBuilder.addField(projectionField);
    }
  }

}
