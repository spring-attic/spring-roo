package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.AbstractHashCodeTrackingMetadataNotifier;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
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
public class DbreDatabaseListenerImpl extends AbstractHashCodeTrackingMetadataNotifier implements DbreDatabaseListener {
	private static final JavaType ROO_ENTITY = new JavaType(RooEntity.class.getName());
	private static final JavaType ROO_IDENTIFIER = new JavaType(RooIdentifier.class.getName());
	private static final String IDENTIFIER_TYPE = "identifierType";
	private static final String VERSION_FIELD = "versionField";
	private static final String VERSION = "version";
	private static final String PRIMARY_KEY_SUFFIX = "PK";
	@Reference private DbreModelService dbreModelService;
	@Reference private FileManager fileManager;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	@Reference private Shell shell;
	
	private Map<JavaType, List<Identifier>> identifierResults = null;

	// This method will be called when the database becomes available for the first time and the rest of Roo has started up OK
	public void notifyDatabaseRefreshed(Database newDatabase) {
		processDatabase(newDatabase);
	}

	private void processDatabase(Database database) {
		if (database == null) {
			return;
		}
		if (database.hasTables()) {
			identifierResults = new HashMap<JavaType, List<Identifier>>();
			reverseEngineer(database);
		}
	}

	private void reverseEngineer(Database database) {
		// Lookup the relevant destination package if not explicitly given
		Set<ClassOrInterfaceTypeDetails> managedEntities = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooDbManaged.class.getName()));
		
		JavaPackage destinationPackage = database.getDestinationPackage();
		if (destinationPackage == null) {
			if (!managedEntities.isEmpty()) {
				// Take the package of the first one
				destinationPackage = managedEntities.iterator().next().getName().getPackage();
			}
		}

		// Fall back to project's top level package
		if (destinationPackage == null) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			destinationPackage = projectMetadata.getTopLevelPackage();
		}

		// Get tables from database
		Set<Table> tables = new LinkedHashSet<Table>(database.getTables());

		// Manage existing entities with @RooDbManaged annotation
		for (ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
			// Remove table from set as each managed entity is processed.
			// The tables that remain in the set will be used for creation of new entities later
			Table table = updateOrDeleteManagedEntity(managedEntity, database);
			if (table != null) {
				tables.remove(table);
			}
		}

		// Create new entities from tables
		List<ClassOrInterfaceTypeDetails> newEntities = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (Table table : tables) {
			// Don't create types from join tables in many-to-many associations
			if (!table.isJoinTable()) {
				newEntities.add(createNewManagedEntityFromTable(table, destinationPackage));
			}
		}

		// Notify
		managedEntities.addAll(newEntities);
		for (ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
			MetadataItem metadataItem = metadataService.get(managedEntity.getDeclaredByMetadataId(), true);
			if (metadataItem != null) {
				notifyIfRequired(metadataItem);
			}
		}

		for (ClassOrInterfaceTypeDetails managedIdentifierType : getManagedIdentifiers()) {
			MetadataItem metadataItem = metadataService.get(managedIdentifierType.getDeclaredByMetadataId(), true);
			if (metadataItem != null) {
				notifyIfRequired(metadataItem);
			}
		}
	}
	
	private Set<ClassOrInterfaceTypeDetails> getManagedIdentifiers() {
		Set<ClassOrInterfaceTypeDetails> managedIdentifierTypes = new LinkedHashSet<ClassOrInterfaceTypeDetails>();

		Set<ClassOrInterfaceTypeDetails> identifierTypes = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_IDENTIFIER);
		for (ClassOrInterfaceTypeDetails managedIdentifierType : identifierTypes) {
			AnnotationMetadata identifierAnnotation = MemberFindingUtils.getTypeAnnotation(managedIdentifierType, ROO_IDENTIFIER);
			AnnotationAttributeValue<?> attrValue = identifierAnnotation.getAttribute(new JavaSymbolName("dbManaged"));
			if (attrValue != null && (Boolean) attrValue.getValue()) {
				managedIdentifierTypes.add(managedIdentifierType);
			}
		}
		return managedIdentifierTypes;
	}

	private Table updateOrDeleteManagedEntity(ClassOrInterfaceTypeDetails managedEntity, Database database) {
		// Update changes to @RooEntity attributes
		AnnotationMetadata rooEntityAnnotation =  MemberFindingUtils.getDeclaredTypeAnnotation(managedEntity, ROO_ENTITY);
		Assert.notNull(rooEntityAnnotation, "@RooEntity annotation not found on " + managedEntity.getName().getFullyQualifiedTypeName());
		AnnotationMetadataBuilder rooEntityBuilder = new AnnotationMetadataBuilder(rooEntityAnnotation);

		// Find table in database using 'table' attribute from @RooEntity
		AnnotationAttributeValue<?> tableAttribute = rooEntityAnnotation.getAttribute(new JavaSymbolName("table"));
		String errMsg = "Unable to maintain database-managed entity " + managedEntity.getName().getFullyQualifiedTypeName() + " because its associated table could not be found";
		Assert.notNull(tableAttribute, errMsg);
		String tableName = (String) tableAttribute.getValue();
		Assert.hasText(tableName, errMsg);
		Table table = database.getTable(tableName);
		if (table == null) {
			// Table has been dropped so delete managed type, and its identifier if applicable
			deleteManagedType(managedEntity);
			return null;
		}

		// Get new @RooEntity attributes
		Set<JavaSymbolName> attributesToDeleteIfPresent = new LinkedHashSet<JavaSymbolName>();
		manageIdentifier(managedEntity.getName(), rooEntityBuilder, attributesToDeleteIfPresent, table);

		// Manage versionField attribute
		AnnotationAttributeValue<?> versionFieldAttribute = rooEntityAnnotation.getAttribute(new JavaSymbolName(VERSION_FIELD));
		if (versionFieldAttribute != null) {
			String versionFieldValue = (String) versionFieldAttribute.getValue();
			if (hasVersionField(table) && (!StringUtils.hasText(versionFieldValue) || VERSION.equals(versionFieldValue))) {
				attributesToDeleteIfPresent.add(new JavaSymbolName(VERSION_FIELD));
			}
		} else {
			if (hasVersionField(table)) {
				attributesToDeleteIfPresent.add(new JavaSymbolName(VERSION_FIELD));
			} else {
				rooEntityBuilder.addStringAttribute(VERSION_FIELD, "");
			}
		}

		// Update the annotation on disk
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) managedEntity;
		mutableTypeDetails.updateTypeAnnotation(rooEntityBuilder.build(), attributesToDeleteIfPresent);
		return table;
	}

	private ClassOrInterfaceTypeDetails createNewManagedEntityFromTable(Table table, JavaPackage destinationPackage) {
		JavaType javaType = DbreTypeUtils.suggestTypeNameForNewTable(table.getName(), destinationPackage);

		// Create type annotations for new entity
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));

		// Find primary key from db metadata and add identifier attributes to @RooEntity
		AnnotationMetadataBuilder rooEntityBuilder = new AnnotationMetadataBuilder(ROO_ENTITY);
		manageIdentifier(javaType, rooEntityBuilder, new HashSet<JavaSymbolName>(), table);

		if (!hasVersionField(table)) {
			rooEntityBuilder.addStringAttribute(VERSION_FIELD, "");
		}
		if (StringUtils.hasText(table.getName())) {
			rooEntityBuilder.addStringAttribute("table", table.getName());
		}
		if (table.getSchema() != null && StringUtils.hasText(table.getSchema().getName())) {
			rooEntityBuilder.addStringAttribute("schema", table.getSchema().getName());
		}

		annotations.add(rooEntityBuilder);

		// Add @RooDbManaged
		AnnotationMetadataBuilder rooDbManagedBuilder = new AnnotationMetadataBuilder(new JavaType(RooDbManaged.class.getName()));
		rooDbManagedBuilder.addBooleanAttribute("automaticallyDelete", true);
		annotations.add(rooDbManagedBuilder);

		JavaType superclass = new JavaType("java.lang.Object");
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		// Create entity class
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, javaType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		typeDetailsBuilder.setAnnotations(annotations);

		ClassOrInterfaceTypeDetails entityType = typeDetailsBuilder.build();
		typeManagementService.generateClassFile(entityType);
		
		shell.flash(Level.FINE, "Created " + javaType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
		shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
		
		return entityType;
	}

	private boolean hasVersionField(Table table) {
		for (Column column : table.getColumns()) {
			if (VERSION.equalsIgnoreCase(column.getName())) {
				return true;
			}
		}
		return false;
	}

	private void manageIdentifier(JavaType javaType, AnnotationMetadataBuilder rooEntityBuilder, Set<JavaSymbolName> attributesToDeleteIfPresent, Table table) {
		JavaType identifierType = getIdentifierType(javaType);
		PhysicalTypeMetadata identifierPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);

		// Process primary keys and add 'identifierType' attribute
		int pkCount = table.getPrimaryKeyCount();
		if (pkCount == 1) {
			// Table has one primary key
			// Check for redundant, managed identifier class and delete if found
			if (isIdentifierDeletable(identifierType)) {
				deleteJavaType(identifierType);
			}

			attributesToDeleteIfPresent.add(new JavaSymbolName(IDENTIFIER_TYPE));

			// We don't need a PK class, so we just tell the EntityMetadataProvider via IdentifierService the column name, field type and field name to use
			List<Identifier> identifiers = getIdentifiersFromPrimaryKeys(table.getName(), table.getPrimaryKeys());
			identifierResults.put(javaType, identifiers);
		} else if (pkCount == 0 || pkCount > 1) {
			// Table has either no primary keys or more than one primary key so create a composite key

			// Check if identifier class already exists and if not, create it
			if (identifierPhysicalTypeMetadata == null || !identifierPhysicalTypeMetadata.isValid() || !(identifierPhysicalTypeMetadata.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(identifierType);
			}

			rooEntityBuilder.addClassAttribute(IDENTIFIER_TYPE, identifierType);

			// We need a PK class, so we tell the IdentifierMetadataProvider via IdentifierService the various column names, field types and field names to use
			// For tables with no primary keys, create a composite key using all the table's columns
			List<Identifier> identifiers = pkCount == 0 ? getIdentifiersFromColumns(table.getName(), table.getColumns()) : getIdentifiersFromPrimaryKeys(table.getName(), table.getPrimaryKeys());
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
		List<AnnotationMetadataBuilder> identifierAnnotations = new ArrayList<AnnotationMetadataBuilder>();

		AnnotationMetadataBuilder identifierBuilder = new AnnotationMetadataBuilder(ROO_IDENTIFIER);
		identifierBuilder.addBooleanAttribute("dbManaged", true);
		identifierAnnotations.add(identifierBuilder);

		// Produce identifier itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder idTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL, identifierType, PhysicalTypeCategory.CLASS);
		idTypeDetailsBuilder.setAnnotations(identifierAnnotations);
		typeManagementService.generateClassFile(idTypeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + identifierType.getFullyQualifiedTypeName(), DbreDatabaseListenerImpl.class.getName());
		shell.flash(Level.FINE, "", DbreDatabaseListenerImpl.class.getName());
	}

	private List<Identifier> getIdentifiersFromPrimaryKeys(String tableName, Set<Column> primaryKeys) {
		return getIdentifiersFromColumns(tableName, primaryKeys);
	}

	private List<Identifier> getIdentifiersFromColumns(String tableName, Set<Column> columns) {
		List<Identifier> result = new ArrayList<Identifier>();

		// Add fields to the identifier class
		for (Column column : columns) {
			String columnName = column.getName();
			JavaSymbolName fieldName;
			try {
				fieldName = new JavaSymbolName(DbreTypeUtils.suggestFieldName(columnName));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Failed to create field name for column '" + columnName + "' in table '" + tableName + "': " + e.getMessage());
			}
			JavaType fieldType = column.getJavaType();
			result.add(new Identifier(fieldName, fieldType, columnName));
		}

		return result;
	}

	private void deleteManagedType(ClassOrInterfaceTypeDetails managedEntity) {
		if (isEntityDeletable(managedEntity)) {
			deleteJavaType(managedEntity.getName());

			JavaType identifierType = getIdentifierType(managedEntity.getName());
			for (ClassOrInterfaceTypeDetails managedIdentifier : getManagedIdentifiers()) {
				if (managedIdentifier.getName().equals(identifierType)) {
					deleteJavaType(identifierType);
					break;
				}
			}
		}
	}

	private boolean isEntityDeletable(ClassOrInterfaceTypeDetails managedEntity) {
		AnnotationMetadata dbManagedAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(managedEntity, new JavaType(RooDbManaged.class.getName()));
		AnnotationAttributeValue<?> attribute = null;
		if (dbManagedAnnotation == null || (attribute = dbManagedAnnotation.getAttribute(new JavaSymbolName("automaticallyDelete"))) == null || !(Boolean) attribute.getValue()) {
			return false;
		}

		// Check type annotations
		List<? extends AnnotationMetadata> typeAnnotations = managedEntity.getAnnotations();

		boolean hasRequiredAnnotations = true;
		Iterator<? extends AnnotationMetadata> typeAnnotationIterator = typeAnnotations.iterator();
		while (hasRequiredAnnotations && typeAnnotationIterator.hasNext()) {
			JavaType annotationType = typeAnnotationIterator.next().getAnnotationType();
			hasRequiredAnnotations &= (annotationType.getFullyQualifiedTypeName().equals(RooDbManaged.class.getName()) || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.javabean.RooJavaBean") || annotationType.getFullyQualifiedTypeName().equals("org.springframework.roo.addon.tostring.RooToString") || annotationType.getFullyQualifiedTypeName().equals(RooEntity.class.getName()));
		}

		if (!hasRequiredAnnotations || typeAnnotations.size() != 4) {
			return false;
		}

		// Finally, check for added constructors, fields and methods
		return managedEntity.getDeclaredConstructors().isEmpty() && managedEntity.getDeclaredFields().isEmpty() && managedEntity.getDeclaredMethods().isEmpty();
	}

	private boolean isIdentifierDeletable(JavaType identifierType) {
		PhysicalTypeMetadata governorPhysicalTypeMetadata = getPhysicalTypeMetadata(identifierType);
		if (governorPhysicalTypeMetadata == null) {
			return false;
		}

		// Check for added constructors, fields and methods
		ClassOrInterfaceTypeDetails managedIdentifier = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		return managedIdentifier.getDeclaredConstructors().isEmpty() && managedIdentifier.getDeclaredFields().isEmpty() && managedIdentifier.getDeclaredMethods().isEmpty();
	}

	private void deleteJavaType(JavaType javaType) {
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
			ClassOrInterfaceTypeDetails governorTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
			AnnotationMetadata rooEntityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, ROO_ENTITY);
			if (rooEntityAnnotation != null) {
				AnnotationAttributeValue<?> identifierTypeAttribute = rooEntityAnnotation.getAttribute(new JavaSymbolName(IDENTIFIER_TYPE));
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
}
