package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DatabaseModelService;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Listens for changes to the DBRE XML file and creates and manages entities based on the database metadata.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreXmlFileListener implements FileEventListener {
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	@Reference private DatabaseModelService databaseModelService;
	@Reference private TableModelService tableModelService;

	public void onFileEvent(FileEvent fileEvent) {
		Assert.notNull(fileEvent, "File event required");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return;
		}

		String dbreXmlPath = projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
		String eventPath = fileEvent.getFileDetails().getCanonicalPath();
		if (!eventPath.equals(dbreXmlPath)) {
			return;
		}

		Database database = null;
		
		if (fileEvent.getFileDetails().getFile().exists()) {
			// The XML is still around, so try to parse it
			database = databaseModelService.deserializeDatabaseMetadata();
		}
		
		if (database != null && database.getTables().size() > 0) {
			reverseEngineer(database);
		} else {
			deleteManagedTypes();
		}
	}

	public void reverseEngineer(Database database) {
		Set<Table> tables = database.getTables();
		for (Table table : tables) {
			// Don't create types from join tables in many-to-many associations
			if (!database.isManyToManyJoinTable(table)) {
				JavaPackage javaPackage = database.getJavaPackage();
				JavaType javaType = tableModelService.findTypeForTableName(table.getName(), javaPackage);
				if (javaType == null) {
					createNewManagedEntityFromTable(table, javaPackage);
				} else {
					updateExistingManagedEntity(javaType, table);
				}
			}
		}

		deleteManagedTypesNotInModel(tables);
	}

	private void createNewManagedEntityFromTable(Table table, JavaPackage javaPackage) {
		JavaType javaType = tableModelService.suggestTypeNameForNewTable(table.getName(), javaPackage);

		// Create type annotations for new entity
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Entity"), new ArrayList<AnnotationAttributeValue<?>>()));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"), new ArrayList<AnnotationAttributeValue<?>>()));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.tostring.RooToString"), new ArrayList<AnnotationAttributeValue<?>>()));

		// Find primary key from db metadata and add identifier attributes to @RooEntity
		List<AnnotationAttributeValue<?>> entityAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		manageEntityIdentifier(javaType, entityAttributes, table);
		annotations.add(new DefaultAnnotationMetadata(new JavaType(RooEntity.class.getName()), entityAttributes));

		// Add @RooDbManaged
		List<AnnotationAttributeValue<?>> dbManagedAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		dbManagedAttributes.add(new BooleanAttributeValue(new JavaSymbolName("automaticallyDelete"), true));
		annotations.add(new DefaultAnnotationMetadata(new JavaType(RooDbManaged.class.getName()), dbManagedAttributes));

		JavaType superclass = new JavaType("java.lang.Object");
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		List<AnnotationAttributeValue<?>> tableAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		tableAttrs.add(new StringAttributeValue(new JavaSymbolName("name"), table.getName()));
		if (StringUtils.hasText(table.getCatalog())) {
			tableAttrs.add(new StringAttributeValue(new JavaSymbolName("catalog"), table.getCatalog()));
		}
		if (table.getSchema() != null && StringUtils.hasText(table.getSchema().getName())) {
			tableAttrs.add(new StringAttributeValue(new JavaSymbolName("schema"), table.getSchema().getName()));
		}
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Table"), tableAttrs));

		// Create entity class
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, javaType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, annotations, null);
		classpathOperations.generateClassFile(details);
	}

	private void updateExistingManagedEntity(JavaType javaType, Table table) {
		// Update changes to @RooEntity attributes
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		if (MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, new JavaType(RooDbManaged.class.getName())) == null) {
			return;
		}

		JavaType entityAnnotationType = new JavaType(RooEntity.class.getName());
		AnnotationMetadata entityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, entityAnnotationType);
		if (entityAnnotation != null) {
			List<AnnotationAttributeValue<?>> entityAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			for (JavaSymbolName attributeName : entityAnnotation.getAttributeNames()) {
				AnnotationAttributeValue<?> attributeValue = entityAnnotation.getAttribute(attributeName);
				if (!("identifierType".equals(attributeName.getSymbolName()) || "identifierField".equals(attributeName.getSymbolName()) || "identifierColumn".equals(attributeName.getSymbolName()))) {
					entityAttributes.add(attributeValue);
				}
			}
			manageEntityIdentifier(javaType, entityAttributes, table);

			AnnotationMetadata annotation = new DefaultAnnotationMetadata(entityAnnotationType, entityAttributes);
			// TODO mutableTypeDetails.updateTypeAnnotation(entityAnnotationType) - should compare values before
			mutableTypeDetails.removeTypeAnnotation(entityAnnotationType);
			mutableTypeDetails.addTypeAnnotation(annotation);
		}
	}

	private void manageEntityIdentifier(JavaType javaType, List<AnnotationAttributeValue<?>> entityAttributes, Table table) {
		JavaType identifierType = getIdentifierType(javaType);
		PhysicalTypeMetadata identifierPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);

		// Process primary keys and add 'identifierType' and 'identifierField' attribute
		int pkCount = table.getPrimaryKeyCount();
		if (pkCount == 1) {
			// Table has one primary key column so add the column's type to the 'identifierType' attribute
			Set<Column> primaryKeys = table.getPrimaryKeys();
			Column primaryKey = primaryKeys.iterator().next();
			String columnName = primaryKey.getName();
			JavaType primaryKeyType = primaryKey.getType().getJavaType();
			// Only add 'identifierType' attribute if it is different from the default, java.lang.Long
			if (!primaryKeyType.equals(JavaType.LONG_OBJECT)) {
				entityAttributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), primaryKeyType));
			}

			// Only add 'identifierField' attribute if it is different from the default, "id"
			String fieldName = tableModelService.suggestFieldName(columnName);
			if (!"id".equals(fieldName)) {
				entityAttributes.add(new StringAttributeValue(new JavaSymbolName("identifierField"), fieldName));
			}

			entityAttributes.add(new StringAttributeValue(new JavaSymbolName("identifierColumn"), columnName));

			// Check for managed identifier class and delete if found
			if (identifierPhysicalTypeMetadata != null && identifierPhysicalTypeMetadata.isValid() && (identifierPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				deleteManagedType(identifierType);
			}
		} else if (pkCount > 1) { // Table has a composite key
			Set<Column> primaryKeys = table.getPrimaryKeys();

			// Check if identifier class already exists and if not, create it
			if (identifierPhysicalTypeMetadata == null || !identifierPhysicalTypeMetadata.isValid() || !(identifierPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(identifierType, primaryKeys);
			} else {
				updateIdentifier(identifierType, primaryKeys);
			}
			entityAttributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), identifierType));
		}
	}

	private void createIdentifierClass(JavaType identifierType, Set<Column> columns) {
		List<AnnotationMetadata> identifierAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotationAttributeValue<?>> dbManagedAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		dbManagedAttributes.add(new BooleanAttributeValue(new JavaSymbolName("automaticallyDelete"), true));
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooDbManaged.class.getName()), dbManagedAttributes));

		List<AnnotationAttributeValue<?>> identifierAttributes = getIdentifierAttributes(columns);
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooIdentifier.class.getName()), identifierAttributes));

		// Produce identifier itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetails idClassDetails = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, identifierType, Modifier.PUBLIC | Modifier.FINAL, PhysicalTypeCategory.CLASS, null, null, null, null, null, null, identifierAnnotations, null);
		classpathOperations.generateClassFile(idClassDetails);
	}

	private List<AnnotationAttributeValue<?>> getIdentifierAttributes(Set<Column> columns) {
		// Add primary key fields to the identifier class
		List<StringAttributeValue> idFields = new ArrayList<StringAttributeValue>();
		List<StringAttributeValue> idTypes = new ArrayList<StringAttributeValue>();
		List<StringAttributeValue> idColumns = new ArrayList<StringAttributeValue>();
		for (Column column : columns) {
			if (column.isPrimaryKey()) {
				String columnName = tableModelService.suggestFieldName(column.getName());
				idFields.add(new StringAttributeValue(new JavaSymbolName("value"), columnName));
				idTypes.add(new StringAttributeValue(new JavaSymbolName("value"), column.getType().getJavaType().getFullyQualifiedTypeName()));
				idColumns.add(new StringAttributeValue(new JavaSymbolName("value"), column.getName()));
			}
		}

		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("idFields"), idFields));
		attributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("idTypes"), idTypes));
		attributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("idColumns"), idColumns));
		return attributes;
	}

	private void updateIdentifier(JavaType identifierType, Set<Column> columns) {
		// Update changes to @RooIdentifier attributes
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		if (MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, new JavaType(RooDbManaged.class.getName())) == null) {
			return;
		}

		JavaType identifierAnnotationType = new JavaType(RooIdentifier.class.getName());
		AnnotationMetadata identifierAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, identifierAnnotationType);
		if (identifierAnnotation != null) {
			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			for (JavaSymbolName attributeName : identifierAnnotation.getAttributeNames()) {
				AnnotationAttributeValue<?> attributeValue = identifierAnnotation.getAttribute(attributeName);
				if (!("idFields".equals(attributeName.getSymbolName()) || "idTypes".equals(attributeName.getSymbolName()) || "idColumns".equals(attributeName.getSymbolName()))) {
					attributes.add(attributeValue);
				}
			}
			attributes.addAll(getIdentifierAttributes(columns));

			AnnotationMetadata annotation = new DefaultAnnotationMetadata(identifierAnnotationType, attributes);
			mutableTypeDetails.removeTypeAnnotation(identifierAnnotationType);
			mutableTypeDetails.addTypeAnnotation(annotation);
		}
	}
	
	private void deleteManagedTypes() {
		Set<JavaType> managedIdentifierTypes = tableModelService.getDatabaseManagedIdentifiers();
		for (JavaType javaType : tableModelService.getDatabaseManagedEntities()) {
			deleteManagedTypes(javaType, managedIdentifierTypes);
		}
	}

	private void deleteManagedTypesNotInModel(Set<Table> tables) {
		Set<JavaType> managedIdentifierTypes = tableModelService.getDatabaseManagedIdentifiers();
		for (JavaType javaType : tableModelService.getDatabaseManagedEntities()) {
			// Check for existence of entity from table model and delete if not in database model
			if (!isDetectedEntityInModel(javaType, tables)) {
				deleteManagedTypes(javaType, managedIdentifierTypes);
			}
		}
	}

	private void deleteManagedTypes(JavaType javaType, Set<JavaType> managedIdentifierTypes) {
		if (isEntityDeletable(javaType)) {
			deleteManagedType(javaType);
			
			JavaType identifierType = getIdentifierType(javaType);
			if (managedIdentifierTypes.contains(identifierType) && isIdentifierDeletable(identifierType)) {
				deleteManagedType(identifierType);
			}
		}
	}

	private boolean isDetectedEntityInModel(JavaType javaType, Set<Table> tables) {
		String tableNamePattern = tableModelService.suggestTableNameForNewType(javaType);
		for (Table table : tables) {
			if (table.getName().equalsIgnoreCase(tableNamePattern)) {
				return true;
			}
		}
		return false;
	}

	private boolean isEntityDeletable(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		if (governorPhysicalTypeMetadata == null) {
			return false;
		}

		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		AnnotationMetadata dbManagedAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName()));
		AnnotationAttributeValue<?> attribute = null;
		if (dbManagedAnnotation == null || (attribute = dbManagedAnnotation.getAttribute(new JavaSymbolName("automaticallyDelete"))) == null || !(Boolean) attribute.getValue()) {
			return false;
		}

		// Check type annotations
		List<? extends AnnotationMetadata> typeAnnotations = typeDetails.getTypeAnnotations();
		if (typeAnnotations.size() != 6) {
			return false;
		}
		
		// Check for required type annotations
		boolean hasRequiredAnnotations = true;
		Iterator<? extends AnnotationMetadata> typeAnnotationIterator = typeAnnotations.iterator();
		while (hasRequiredAnnotations && typeAnnotationIterator.hasNext()) {
			JavaType annotationType = typeAnnotationIterator.next().getAnnotationType();
			hasRequiredAnnotations &= (annotationType.getFullyQualifiedTypeName().equals(RooDbManaged.class.getName()) || annotationType.getFullyQualifiedTypeName().equals("javax.persistence.Entity") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.javabean.RooJavaBean") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.tostring.RooToString") || annotationType.getFullyQualifiedTypeName().equals("javax.persistence.Table") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.entity.RooEntity"));
		}
		
		if (!hasRequiredAnnotations) {
			return false;
		}

		// Finally, check for added constructors, fields and methods
		return typeDetails.getDeclaredConstructors().isEmpty() && typeDetails.getDeclaredFields().isEmpty() && typeDetails.getDeclaredMethods().isEmpty();
	}
	
	private boolean isIdentifierDeletable(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		if (governorPhysicalTypeMetadata == null) {
			return false;
		}

		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		AnnotationMetadata dbManagedAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName()));
		AnnotationAttributeValue<?> attribute = null;
		if (dbManagedAnnotation == null || (attribute = dbManagedAnnotation.getAttribute(new JavaSymbolName("automaticallyDelete"))) == null || !(Boolean) attribute.getValue()) {
			return false;
		}

		// Check type annotations
		List<? extends AnnotationMetadata> typeAnnotations = typeDetails.getTypeAnnotations();
		if (typeAnnotations.size() != 2) {
			return false;
		}
		
		// Check for required type annotations
		boolean hasRequiredAnnotations = true;
		Iterator<? extends AnnotationMetadata> typeAnnotationIterator = typeAnnotations.iterator();
		while (hasRequiredAnnotations && typeAnnotationIterator.hasNext()) {
			JavaType annotationType = typeAnnotationIterator.next().getAnnotationType();
			hasRequiredAnnotations &= (annotationType.getFullyQualifiedTypeName().equals(RooDbManaged.class.getName()) || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.entity.RooIdentifier"));
		}
		
		if (!hasRequiredAnnotations) {
			return false;
		}

		// Finally, check for added constructors, fields and methods
		return typeDetails.getDeclaredConstructors().isEmpty() && typeDetails.getDeclaredFields().isEmpty() && typeDetails.getDeclaredMethods().isEmpty();
	}

	private void deleteManagedType(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		if (governorPhysicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
			if (MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName())) != null) {
				String filePath = governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath();
				fileManager.delete(filePath);
			}
		}
	}
	
	private JavaType getIdentifierType(JavaType javaType) {
		return new JavaType(javaType.getFullyQualifiedTypeName() + "PK");
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}
}
