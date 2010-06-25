package org.springframework.roo.addon.dbre;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.Column;
import org.springframework.roo.addon.dbre.db.DbModel;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.addon.dbre.db.Table;
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
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Listens for changes to the dbre xml file and creates entities for new tables.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreXmlFileListener implements FileEventListener {
	private static final Logger logger = HandlerUtils.getLogger(DbreXmlFileListener.class);
	@Reference private TableModelService tableModelService;
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	@Reference private DbModel dbModel;

	public void onFileEvent(FileEvent fileEvent) {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return;
		}

		String dbrePath = projectMetadata.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, DbrePath.DBRE_XML_FILE.getPath());
		String eventPath = fileEvent.getFileDetails().getCanonicalPath();
		if (!eventPath.equals(dbrePath) || !(fileEvent.getOperation() == FileOperation.CREATED || fileEvent.getOperation() == FileOperation.UPDATED)) {
			return;
		}

		createOrUpdateManagedEntities();
		deleteManagedEntities();
		processAllDetectedEntities();
	}

	private void createOrUpdateManagedEntities() {
		dbModel.deserialize();
		for (Table table : dbModel.getTables()) {
			IdentifiableTable identifiableTable = table.getIdentifiableTable();
			JavaType javaType = tableModelService.findTypeForTableIdentity(identifiableTable);
			if (javaType == null) {
				createEntityFromTable(table);
			} else {
				updateEntity(javaType, table);
			}
		}
	}

	private void deleteManagedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities = tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			// Check for existence of entity from table model and delete if not in db model
			if (!isDetectedEntityInDbModel(entry.getKey())) {
				deleteManagedType(entry.getValue());
			}
		}
	}

	private void createEntityFromTable(Table table) {
		JavaPackage javaPackage = dbModel.getJavaPackage();
		JavaType javaType = tableModelService.suggestTypeNameForNewTable(table.getIdentifiableTable(), javaPackage);

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
		annotations.add(new DefaultAnnotationMetadata(new JavaType(RooDbManaged.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));

		JavaType superclass = new JavaType("java.lang.Object");
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		List<AnnotationAttributeValue<?>> tableAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		tableAttrs.add(new StringAttributeValue(new JavaSymbolName("name"), table.getIdentifiableTable().getTable()));
		if (StringUtils.hasText(table.getIdentifiableTable().getCatalog())) {
			tableAttrs.add(new StringAttributeValue(new JavaSymbolName("catalog"), table.getIdentifiableTable().getCatalog()));
		}
		if (StringUtils.hasText(table.getIdentifiableTable().getSchema())) {
			tableAttrs.add(new StringAttributeValue(new JavaSymbolName("schema"), table.getIdentifiableTable().getSchema()));
		}
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Table"), tableAttrs));

		// Create entity class
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, javaType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, annotations, null);
		classpathOperations.generateClassFile(details);
	}

	private void updateEntity(JavaType javaType, Table table) {
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
		SortedSet<Column> columns = table.getColumns();
		int pkCount = getPkCount(columns);
		if (pkCount == 1) {
			// Table has one primary key column so add the column's type to the 'identifierType' attribute
			Column column = columns.first();
			if (column != null) {
				String columnName = column.getName();
				JavaType primaryKeyType = column.getType();
				// Only add 'identifierType' attribute if it is different from the default, java.lang.Long
				if (!primaryKeyType.equals(JavaType.LONG_OBJECT)) {
					entityAttributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), primaryKeyType));
				}

				// Only add 'identifierField' attribute if it is different from the default, "id"
				String fieldName = tableModelService.suggestFieldNameForColumn(columnName);
				if (!"id".equals(fieldName)) {
					entityAttributes.add(new StringAttributeValue(new JavaSymbolName("identifierField"), fieldName));
				}

				entityAttributes.add(new StringAttributeValue(new JavaSymbolName("identifierColumn"), columnName));
			}

			// Check for managed identifier class and delete if found
			if (identifierPhysicalTypeMetadata != null && identifierPhysicalTypeMetadata.isValid() && (identifierPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				deleteManagedType(identifierType);
			}
		} else if (pkCount > 1) { // Table has a composite key
			// Check if identifier class already exists and if not, create it
			if (identifierPhysicalTypeMetadata == null || !identifierPhysicalTypeMetadata.isValid() || !(identifierPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(identifierType, columns);
			} else {
				updateIdentifier(identifierType, columns);
			}
			entityAttributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), identifierType));
		}
	}

	private int getPkCount(SortedSet<Column> columns) {
		int pkCount = 0;
		for (Column column : columns) {
			if (column.isPrimaryKey()) {
				pkCount++;
			}
		}
		return pkCount;
	}

	private void createIdentifierClass(JavaType identifierType, Set<Column> columns) {
		List<AnnotationMetadata> identifierAnnotations = new ArrayList<AnnotationMetadata>();
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooDbManaged.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));

		List<AnnotationAttributeValue<?>> attributes = getIdentifierAttributes(columns);
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooIdentifier.class.getName()), attributes));

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
				String columnName = column.getName();
				idFields.add(new StringAttributeValue(new JavaSymbolName("ignored"), columnName));
				idTypes.add(new StringAttributeValue(new JavaSymbolName("ignored"), column.getType().getFullyQualifiedTypeName()));
				idColumns.add(new StringAttributeValue(new JavaSymbolName("ignored"), columnName));
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

	private JavaType getIdentifierType(JavaType javaType) {
		return new JavaType(javaType.getFullyQualifiedTypeName() + "Pk");
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}

	private boolean isDetectedEntityInDbModel(IdentifiableTable identifiableTable) {
		for (Table table : dbModel.getTables()) {
			if (table.getIdentifiableTable().equals(identifiableTable)) {
				return true;
			}
		}
		return false;
	}

	private void deleteManagedType(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		if (MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName())) != null) {
			String filePath = governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath();
			if (!FileUtils.deleteRecursively(new File(filePath))) {
				logger.warning("Unable to delete database-managed class " + filePath);
			}
		}
	}

	private void processAllDetectedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities = tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			metadataService.get(DbreMetadata.createIdentifier(entry.getValue(), Path.SRC_MAIN_JAVA));
		}
	}
}
