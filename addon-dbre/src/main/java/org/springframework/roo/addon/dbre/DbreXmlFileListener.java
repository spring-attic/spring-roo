package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.DbModel;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.addon.dbre.db.PrimaryKey;
import org.springframework.roo.addon.dbre.db.Table;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
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
			JavaType javaType = tableModelService.findTypeForTableIdentity(identifiableTable);
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

		// Find primary key from db metadata and add identifier to @RooEntity 
		List<AnnotationAttributeValue<?>> entityAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		addIdentifierFromPrimaryKey(entityAttrs, table);
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

	private void addIdentifierFromPrimaryKey(List<AnnotationAttributeValue<?>> entityAttrs, Table table) {
		// Get primary keys 
		Set<PrimaryKey> primaryKeys = table.getPrimaryKeys();
		if (primaryKeys.size() == 1) {
			
		}
		
	}

//	private IdentifiableTable getIdentifiableTable(Element tableElement) {
//		// TODO catalog can't be used in finding a table
//		// String catalog = StringUtils.trimToNull(tableElement.getAttribute("catalog"));
//		String catalog = null;
//		String schema = StringUtils.trimToNull(tableElement.getAttribute("schema"));
//		String table = StringUtils.trimToNull(tableElement.getAttribute("name"));
//		return new IdentifiableTable(catalog, schema, table);
//	}
	
	private void processDetectedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities = tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			metadataService.get(DbreMetadata.createIdentifier(entry.getValue(), Path.SRC_MAIN_JAVA));
		}
	}
	
	private String getSpecifiedTableXPath(String tableName) {
		return DbrePath.DBRE_TABLE_XPATH.getPath() + "[@name = '" + tableName + "']";
	}
}
