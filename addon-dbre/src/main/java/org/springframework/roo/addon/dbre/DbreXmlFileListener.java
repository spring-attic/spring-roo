package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.Column;
import org.springframework.roo.addon.dbre.db.DbModel;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.addon.dbre.db.PrimaryKey;
import org.springframework.roo.addon.dbre.db.Table;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
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
	@Reference private TableModelService tableModelService;
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	@Reference private DbModel dbModel;
	
	public void onFileEvent(FileEvent fileEvent) {
		String eventPath = fileEvent.getFileDetails().getCanonicalPath();
		if (eventPath.endsWith(DbrePath.DBRE_XML_FILE.getPath())) {
			createEntities();
			processDetectedEntities();
		}
	}

	private void createEntities() {
		dbModel.deserialize();
		for (Table table : dbModel.getTables()) {
			IdentifiableTable identifiableTable = table.getIdentifiableTable();
	//		System.out.println(identifiableTable.toString());
		//	JavaType javaType = tableModelService.findTypeForTableIdentity(identifiableTable);
		//	System.out.println(identifiableTable.toString() + " : " + javaType.getFullyQualifiedTypeName());
			
			if (tableModelService.findTypeForTableIdentity(identifiableTable) == null) {
				createEntityFromTable(table);
			}
		}
	}

	private void createEntityFromTable(Table table) {
		JavaPackage javaPackage = dbModel.getJavaPackage();
		JavaType javaType = tableModelService.suggestTypeNameForNewTable(table.getIdentifiableTable(), javaPackage);
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);

		// Create entity annotations
		List<AnnotationMetadata> entityAnnotations = new ArrayList<AnnotationMetadata>();
		
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Entity"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.tostring.RooToString"), new ArrayList<AnnotationAttributeValue<?>>()));

		// Find primary key from db metadata and add identifier attributes to @RooEntity 
		List<AnnotationAttributeValue<?>> entityAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		addIdentifierFromPrimaryKey(javaType, entityAttrs, table);
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.entity.RooEntity"), entityAttrs));
		
		// Add @RooDbManaged
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.dbre.RooDbManaged"), new ArrayList<AnnotationAttributeValue<?>>()));

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
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Table"), tableAttrs));

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, javaType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, entityAnnotations, null);
		classpathOperations.generateClassFile(details);
	}

	private void addIdentifierFromPrimaryKey(JavaType javaType, List<AnnotationAttributeValue<?>> entityAttrs, Table table) {
		SortedSet<PrimaryKey> primaryKeys = table.getPrimaryKeys();
		if (primaryKeys.size() == 1) {
			// Table has one primary key column so add the column's type to the 'identifierType' attribute 
			String columnName = primaryKeys.first().getColumnName();
			Column column = getPrimaryKeyColumn(table.getColumns(), columnName);
			if (column != null) {
				entityAttrs.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), new JavaType(column.getType().getName())));
				entityAttrs.add(new StringAttributeValue(new JavaSymbolName("identifierField"), column.getName()));
			}
		} else if (primaryKeys.size() > 1) {
			// Table has a composite key so create the identifier class and add the fields
			
			// Set the identifier's class name to the new entity's class name with 'Pk' appended
			JavaType identifierType = new JavaType(javaType.getFullyQualifiedTypeName() + "Pk");
			
			// Check if identifier class already exists and if not, create it
			String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
			PhysicalTypeMetadata governorPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
			if (governorPhysicalTypeMetadata == null || !governorPhysicalTypeMetadata.isValid() || !(governorPhysicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
				createIdentifierClass(declaredByMetadataId, identifierType, primaryKeys, table.getColumns());
				entityAttrs.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), identifierType));
			} else {
				// TODO add or remove fields from identifier class
			}
		}
	}

	private void createIdentifierClass(String declaredByMetadataId, JavaType identifierType, SortedSet<PrimaryKey> primaryKeys, Set<Column> columns) {
		// Add primary key fields to the new identifier class
		List<FieldMetadata> declaredFields = new LinkedList<FieldMetadata>();
		for (PrimaryKey primaryKey : primaryKeys) {
			String columnName = primaryKey.getColumnName();
			Column column = getPrimaryKeyColumn(columns, columnName);
			if (column != null) {
				JavaSymbolName fieldName = new JavaSymbolName(column.getName());
				JavaType fieldType = new JavaType(column.getType().getName());
			
				// Add annotations to field
				List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
				
				// Add @Column
				List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
				columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), fieldName.getSymbolName()));
				annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes));
				
				// Add @Size for Strings
				if (column.getType().isInstance(new String())) {
					List<AnnotationAttributeValue<?>> sizeAttributes = new ArrayList<AnnotationAttributeValue<?>>();
					sizeAttributes.add(new IntegerAttributeValue(new JavaSymbolName("max"), column.getColumnSize()));
					annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.Size"), sizeAttributes));
				}
				
				declaredFields.add(new DefaultFieldMetadata(declaredByMetadataId, Modifier.PRIVATE, fieldName, fieldType, null, annotations));
			}
		}
		
		// Produce identifier itself
		List<AnnotationMetadata> identifierAnnotations = new ArrayList<AnnotationMetadata>();
		identifierAnnotations.add(new DefaultAnnotationMetadata(new JavaType(RooIdentifier.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));
		// TODO need to comment out next line if entity metadata needs a trigger to @RooDbManaged, othewise and entity ITD will be created for identifier class - NOT REQUIRED
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
	
	private void processDetectedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities = tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			metadataService.get(DbreMetadata.createIdentifier(entry.getValue(), Path.SRC_MAIN_JAVA));
		}
	}
}
