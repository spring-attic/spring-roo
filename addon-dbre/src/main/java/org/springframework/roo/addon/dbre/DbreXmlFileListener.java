package org.springframework.roo.addon.dbre;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
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
import org.springframework.roo.addon.dbre.db.PrimaryKey;
import org.springframework.roo.addon.dbre.db.Table;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
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
			if (javaType != null) {
				updateEntity(javaType, table);
			} else {
				createEntityFromTable(table);
			}
		}
	}

	private void deleteManagedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities =  tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			// Check for existence of entity from table model and delete if not in db model, providing the @RooDbManaged annotation is present
			String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(entry.getValue(), Path.SRC_MAIN_JAVA);
			PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
			ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
			AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(typeDetails, new JavaType(RooDbManaged.class.getName()));
			if (!isDetectedEntityInDbModel(entry.getKey()) && annotation != null) {
				deleteUnmanagedEntity(governorPhysicalTypeMetadata.getPhysicalLocationCanonicalPath());
			}
		}
	}

	private void updateEntity(JavaType javaType, Table table) {
		// Update changes to @RooEntity attributes
		JavaType annotationType = new JavaType(RooEntity.class.getName());
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getPhysicalTypeDetails();
		AnnotationMetadata found = MemberFindingUtils.getDeclaredTypeAnnotation(mutableTypeDetails, annotationType);
		if (found == null) {
			logger.warning("Unable to find the @RooEntity annotation on '" + javaType.getFullyQualifiedTypeName() + "'");
			return;
		}
	
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		for (JavaSymbolName attributeName : found.getAttributeNames()) {
			AnnotationAttributeValue<?> attributeValue = found.getAttribute(attributeName);
			if (!("identifierType".equals(attributeName.getSymbolName()) || "identifierField".equals(attributeName.getSymbolName()))) {
				attributes.add(attributeValue);
			}
		}
		attributes.addAll(getAttributes(javaType, table));
		
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(annotationType, attributes);
		mutableTypeDetails.removeTypeAnnotation(annotationType);
		mutableTypeDetails.addTypeAnnotation(annotation);
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
		List<AnnotationAttributeValue<?>> atttributes = getAttributes(javaType, table);
		annotations.add(new DefaultAnnotationMetadata(new JavaType(RooEntity.class.getName()), atttributes));
		
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

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, javaType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, annotations, null);
		classpathOperations.generateClassFile(details);
	}

	private List<AnnotationAttributeValue<?>> getAttributes(JavaType javaType, Table table) {
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		
		// Process primary keys and add 'identifierType' and 'identifierField' attribute
		SortedSet<PrimaryKey> primaryKeys = table.getPrimaryKeys();
		if (primaryKeys.size() == 1) {
			// Table has one primary key column so add the column's type to the 'identifierType' attribute 
			String columnName = primaryKeys.first().getColumnName();
			Column column = getPrimaryKeyColumn(table.getColumns(), columnName);
			if (column != null) {
				JavaType identifierType = column.getType();
				// Only add 'identifierType' attribute if it is different from the default, java.lang.Long
				if (!identifierType.equals(JavaType.LONG_OBJECT)) {
					attributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), identifierType));
				}
				
				// Only add 'identifierField' attribute if it is different from the default, "id"
				String fieldName = tableModelService.suggestFieldNameForColumn(column.getName());
				if (!"id".equals(fieldName)) {
					attributes.add(new StringAttributeValue(new JavaSymbolName("identifierField"), fieldName));
				}
			}
		} else if (primaryKeys.size() > 1) {
			// Table has a composite key so create the identifier class and add the fields
			
			// Set the identifier's class name to the new entity's class name appended with the string 'Pk'
			JavaType identifierType = new JavaType(javaType.getFullyQualifiedTypeName() + "Pk");
			
			// Check if identifier class already exists and if not, create it
			String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
			PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
			if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid() || !(governorPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(declaredByMetadataId, identifierType, primaryKeys, table.getColumns());
			}
			attributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), identifierType));
		}
		
		return attributes;
	}

	private void createIdentifierClass(String declaredByMetadataId, JavaType identifierType, SortedSet<PrimaryKey> primaryKeys, Set<Column> columns) {
		// Add primary key fields to the new identifier class
		List<FieldMetadata> declaredFields = new LinkedList<FieldMetadata>();
		for (PrimaryKey primaryKey : primaryKeys) {
			String columnName = primaryKey.getColumnName();
			Column column = getPrimaryKeyColumn(columns, columnName);
			if (column != null) {
				JavaSymbolName fieldName = new JavaSymbolName(column.getName());
				JavaType fieldType = column.getType();
			
				// Add annotations to field
				List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
				
				// Add @Column
				List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
				columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), fieldName.getSymbolName()));
				
				// Add length attribute for Strings
				if (fieldType.equals(JavaType.STRING_OBJECT)) {
					columnAttributes.add(new IntegerAttributeValue(new JavaSymbolName("length"), column.getColumnSize()));
				}
				annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes));

				declaredFields.add(new DefaultFieldMetadata(declaredByMetadataId, Modifier.PRIVATE, fieldName, fieldType, null, annotations));
			}
		}
		
		// Produce identifier itself
		List<AnnotationMetadata> identifierAnnotations = new ArrayList<AnnotationMetadata>();
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooIdentifier.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));
		// TODO need to comment out next line if entity metadata needs a trigger to @RooDbManaged, otherwise an entity ITD will be created for identifier class - NOT REQUIRED
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooDbManaged.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));
		
		ClassOrInterfaceTypeDetails idClassDetails = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, identifierType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, declaredFields, null, null, null, null, identifierAnnotations, null);
		classpathOperations.generateClassFile(idClassDetails);
	}

	private Column getPrimaryKeyColumn(Set<Column> columns, String columnName) {
		for (Column column : columns) {
			if (column.getName().equals(columnName)) {
				return column;
			}
		}
		return null;
	}
	
	private boolean isDetectedEntityInDbModel(IdentifiableTable identifiableTable) {
		for (Table table : dbModel.getTables()) {
			if (table.getIdentifiableTable().equals(identifiableTable)) {
				return true;
			}
		}
		return false;
	}

	private void deleteUnmanagedEntity(String filePath) {
		if (!FileUtils.deleteRecursively(new File(filePath))) {
			logger.warning("Unable to delete database-managed entity " + filePath);
		}
	}

	private void processAllDetectedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities = tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			metadataService.get(DbreMetadata.createIdentifier(entry.getValue(), Path.SRC_MAIN_JAVA));
		}
	}
}
