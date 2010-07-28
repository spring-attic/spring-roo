package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.Identifier;
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
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Responds to discovery of database structural information from {@link DbreModelService} and creates and manages entities based on this.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class DbreDatabaseListenerImpl implements DbreDatabaseListener {
	private static final JavaSymbolName IDENTIFIER_TYPE = new JavaSymbolName("identifierType");
	private static final String PRIMARY_KEY_SUFFIX = "PK";
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	@Reference private DbreModelService dbreModelService;
	@Reference private DbreTableService dbreTableService;
	@Reference private Shell shell;
	private Map<JavaType, List<Identifier>> identifierResults = null;
	private JavaPackage destinationPackage = null;

	// This method will be called when the database becomes available for the first time and the rest of Roo has startup up OK
	public void notifyDatabaseRefreshed(Database newDatabase) {
		processDatabase(newDatabase);
	}

	private void processDatabase(Database database) {
		if (database == null) {
			return;
		}
		if (database != null && database.hasTables()) {
			identifierResults = new HashMap<JavaType, List<Identifier>>();
			reverseEngineer(database);
		} else {
			identifierResults = null;
			deleteManagedTypes();
		}
	}

	private void reverseEngineer(Database database) {
		// Lookup the relevant destination package if not explicitly given
		JavaPackage destinationToUse = this.destinationPackage;
		if (destinationToUse == null) {
			SortedSet<JavaType> existingDbreManagedEntities = dbreTableService.getDatabaseManagedEntities();
			if (!existingDbreManagedEntities.isEmpty()) {
				// Take the package of the first one
				destinationToUse = existingDbreManagedEntities.first().getPackage();

				// Change the local field, as this means the user really has specified a package as DBRE has entities on disk
				this.destinationPackage = destinationToUse;
			}

			if (destinationToUse == null) {
				// We still don't know the destination package, so the user doesn't want DBRE to be running yet
				return;
			}
		}

		Set<Table> tables = database.getTables();
		for (Table table : tables) {
			// Don't create types from join tables in many-to-many associations
			if (!database.isJoinTable(table)) {
				JavaType javaType = dbreTableService.findTypeForTableName(table.getName(), destinationToUse);
				if (javaType == null) {
					createNewManagedEntityFromTable(table, destinationToUse);
				} else {
					updateExistingManagedEntity(javaType, table);
				}
			}
		}

		deleteManagedTypesNotInModel(tables);
	}

	private void createNewManagedEntityFromTable(Table table, JavaPackage javaPackage) {
		JavaType javaType = dbreTableService.suggestTypeNameForNewTable(table.getName(), javaPackage);

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

		shell.flash(Level.FINE, "Created " + javaType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
		shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
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
		Assert.notNull(entityAnnotation, "@RooEntity annotation not found on " + javaType.getFullyQualifiedTypeName());
		List<AnnotationAttributeValue<?>> entityAttributes = new ArrayList<AnnotationAttributeValue<?>>();

		// Get new @RooEntity attributes
		manageEntityIdentifier(javaType, entityAttributes, table);

		// Update the annotation on disk
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(entityAnnotationType, entityAttributes);
		boolean changed = mutableTypeDetails.updateTypeAnnotation(annotation);
		if (!changed) {
			// Although @RooEntity annotation on disk did not change, other columns may have been added or
			// deleted from the table so we still need to trigger the metadata.
			String dbreMetadataMid = DbreMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
			metadataService.get(dbreMetadataMid, true);
		}
	}

	private void manageEntityIdentifier(JavaType javaType, List<AnnotationAttributeValue<?>> entityAttributes, Table table) {
		JavaType identifierType = getIdentifierType(javaType);
		PhysicalTypeMetadata identifierPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);

		// Process primary keys and add 'identifierType' attribute
		int pkCount = table.getPrimaryKeyCount();
		List<Identifier> identifiers = getIdentifiersFromColumns(table.getPrimaryKeys());

		if (pkCount == 1) {
			// Check for redundant, managed identifier class and delete if found
			if (isIdentifierDeletable(identifierType)) {
				deleteManagedType(identifierType);
			}

			// We don't need a PK class, so we just tell the EntityMetadataProvider via IdentifierService the column name, field type and field name to use
			identifierResults.put(javaType, identifiers);
		} else if (pkCount > 1) {
			// Table has a composite key

			// Check if identifier class already exists and if not, create it
			if (identifierPhysicalTypeMetadata == null || !identifierPhysicalTypeMetadata.isValid() || !(identifierPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(identifierType);
			}

			entityAttributes.add(new ClassAttributeValue(IDENTIFIER_TYPE, identifierType));

			// We need a PK class, so we tell the IdentifierMetadataProvider via IdentifierService the various column names, field types and field names to use
			identifierResults.put(identifierType, identifiers);
		}
	}

	public List<Identifier> getIdentifiers(JavaType pkType) {
		if (identifierResults == null) {
			// Need to populate the identifier results before returning from this method
			processDatabase(dbreModelService.getDatabase(null));
		}
		if (identifierResults == null) {
			// It's still null, so maybe the DBRE XML file isn't available at this time or similar
			return null;
		}
		return identifierResults.get(pkType);
	}

	private void createIdentifierClass(JavaType identifierType) {
		List<AnnotationMetadata> identifierAnnotations = new ArrayList<AnnotationMetadata>();

		List<AnnotationAttributeValue<?>> dbManagedAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		dbManagedAttributes.add(new BooleanAttributeValue(new JavaSymbolName("automaticallyDelete"), true));
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooDbManaged.class.getName()), dbManagedAttributes));

		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooIdentifier.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));

		// Produce identifier itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetails idClassDetails = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, identifierType, Modifier.PUBLIC | Modifier.FINAL, PhysicalTypeCategory.CLASS, null, null, null, null, null, null, identifierAnnotations, null);
		classpathOperations.generateClassFile(idClassDetails);

		shell.flash(Level.FINE, "Created " + identifierType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
		shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
	}

	private List<Identifier> getIdentifiersFromColumns(Set<Column> columns) {
		List<Identifier> result = new ArrayList<Identifier>();

		// Add primary key fields to the identifier class
		for (Column column : columns) {
			if (column.isPrimaryKey()) {
				JavaSymbolName fieldName = new JavaSymbolName(dbreTableService.suggestFieldName(column.getName()));
				JavaType fieldType = column.getType().getJavaType();
				String columnName = column.getName();
				result.add(new Identifier(fieldName, fieldType, columnName));
			}
		}

		return result;
	}

	private void deleteManagedTypes() {
		Set<JavaType> managedIdentifierTypes = dbreTableService.getDatabaseManagedIdentifiers();
		for (JavaType javaType : dbreTableService.getDatabaseManagedEntities()) {
			deleteManagedTypes(javaType, managedIdentifierTypes);
		}
	}

	private void deleteManagedTypesNotInModel(Set<Table> tables) {
		Set<JavaType> managedIdentifierTypes = dbreTableService.getDatabaseManagedIdentifiers();
		for (JavaType javaType : dbreTableService.getDatabaseManagedEntities()) {
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
		String tableNamePattern = dbreTableService.suggestTableNameForNewType(javaType);
		for (Table table : tables) {
			if (table.getName().equalsIgnoreCase(tableNamePattern)) {
				return true;
			}
		}
		return false;
	}

	private boolean isEntityDeletable(JavaType javaType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
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

		boolean hasRequiredAnnotations = true;
		Iterator<? extends AnnotationMetadata> typeAnnotationIterator = typeAnnotations.iterator();
		while (hasRequiredAnnotations && typeAnnotationIterator.hasNext()) {
			JavaType annotationType = typeAnnotationIterator.next().getAnnotationType();
			hasRequiredAnnotations &= (annotationType.getFullyQualifiedTypeName().equals(RooDbManaged.class.getName()) || annotationType.getFullyQualifiedTypeName().equals("javax.persistence.Entity") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.javabean.RooJavaBean") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.tostring.RooToString") || annotationType.getFullyQualifiedTypeName().equals("javax.persistence.Table") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.entity.RooEntity"));
		}

		if (!hasRequiredAnnotations || typeAnnotations.size() != 6) {
			return false;
		}

		// Finally, check for added constructors, fields and methods
		return typeDetails.getDeclaredConstructors().isEmpty() && typeDetails.getDeclaredFields().isEmpty() && typeDetails.getDeclaredMethods().isEmpty();
	}

	private boolean isIdentifierDeletable(JavaType identifierType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);
		if (governorPhysicalTypeMetadata == null) {
			return false;
		}

		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		AnnotationMetadata dbManagedAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName()));
		AnnotationAttributeValue<?> attribute = null;
		if (dbManagedAnnotation == null || (attribute = dbManagedAnnotation.getAttribute(new JavaSymbolName("automaticallyDelete"))) == null || !(Boolean) attribute.getValue()) {
			return false;
		}

		// Check for required type annotations
		List<? extends AnnotationMetadata> typeAnnotations = typeDetails.getTypeAnnotations();

		boolean hasRequiredAnnotations = true;
		Iterator<? extends AnnotationMetadata> typeAnnotationIterator = typeAnnotations.iterator();
		while (hasRequiredAnnotations && typeAnnotationIterator.hasNext()) {
			JavaType annotationType = typeAnnotationIterator.next().getAnnotationType();
			hasRequiredAnnotations &= (annotationType.getFullyQualifiedTypeName().equals(RooDbManaged.class.getName()) || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.entity.RooIdentifier"));
		}

		if (!hasRequiredAnnotations || typeAnnotations.size() != 2) {
			return false;
		}

		// Finally, check for added constructors, fields and methods
		return typeDetails.getDeclaredConstructors().isEmpty() && typeDetails.getDeclaredFields().isEmpty() && typeDetails.getDeclaredMethods().isEmpty();
	}

	private void deleteManagedType(JavaType javaType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		if (governorPhysicalTypeMetadata != null) {
			String filePath = governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath();
			if (fileManager.exists(filePath)) {
				fileManager.delete(filePath);
				shell.flash(Level.FINE, "Deleted " + javaType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
			}
			shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
		}
	}

	private JavaType getIdentifierType(JavaType javaType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(javaType);
		if (governorPhysicalTypeMetadata != null) {
			ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
			AnnotationMetadata entityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooEntity.class.getName()));
			if (entityAnnotation != null) {
				AnnotationAttributeValue<?> identifierTypeAttribute = entityAnnotation.getAttribute(IDENTIFIER_TYPE);
				if (identifierTypeAttribute != null) {
					// Attribute identifierType exists so get the value
					JavaType identifierType = (JavaType) identifierTypeAttribute.getValue();
					if (identifierType != null && !identifierType.getFullyQualifiedTypeName().startsWith("java.lang")) {
						return identifierType;
					}
				}
			}
		}

		// @RooEntity identifierType attribute does not exist or is not a simple type, so return a default
		return new JavaType(javaType.getFullyQualifiedTypeName() + PRIMARY_KEY_SUFFIX);
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}

	public JavaPackage getDestinationPackage() {
		return destinationPackage;
	}

	public void setDestinationPackage(JavaPackage destinationPackage) {
		this.destinationPackage = destinationPackage;
	}
}
