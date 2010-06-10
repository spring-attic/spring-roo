package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
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
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Listens for changes to the dbre xml file and creates entities for new tables. 
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreXmlListener implements FileEventListener {
	@Reference private TableModelService tableModelService;
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	
	public void onFileEvent(FileEvent fileEvent) {
		String eventPath = fileEvent.getFileDetails().getCanonicalPath();
		if (eventPath.endsWith(DbrePath.DBRE_XML_FILE.getPath())) {
			createEntities(fileEvent);
			processDetectedEntities();
		}
	}

	private void createEntities(FileEvent fileEvent) {
		Element dbMetadataElement;
		try {
			Document dbre = XmlUtils.getDocumentBuilder().parse(fileEvent.getFileDetails().getFile());
			dbMetadataElement = dbre.getDocumentElement();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		List<Element> tables = XmlUtils.findElements(DbrePath.DBRE_TABLE_XPATH.getPath(), dbMetadataElement);
		for (Element tableElement : tables) {
			IdentifiableTable tableIdentity = getIdentifiableTable(tableElement);
			if (tableModelService.findTypeForTableIdentity(tableIdentity) == null) {
				createEntityFromTable(dbMetadataElement, tableIdentity);
			}
		}
	}

	private void createEntityFromTable(Element dbMetadataElement, IdentifiableTable tableIdentity) {
		JavaPackage javaPackage = new JavaPackage(dbMetadataElement.getAttribute("package"));
		JavaType javaType = tableModelService.suggestTypeNameForNewTable(tableIdentity, javaPackage);
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);

		// Create entity annotations
		List<AnnotationMetadata> entityAnnotations = new ArrayList<AnnotationMetadata>();
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Entity"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.tostring.RooToString"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.entity.RooEntity"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.dbre.RooDbManaged"), new ArrayList<AnnotationAttributeValue<?>>()));

		JavaType superclass = new JavaType("java.lang.Object");
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
		attrs.add(new StringAttributeValue(new JavaSymbolName("name"), tableIdentity.getTable()));
		if (StringUtils.hasText(tableIdentity.getCatalog())) {
			attrs.add(new StringAttributeValue(new JavaSymbolName("catalog"), tableIdentity.getCatalog()));
		}
		if (StringUtils.hasText(tableIdentity.getSchema())) {
			attrs.add(new StringAttributeValue(new JavaSymbolName("schema"), tableIdentity.getSchema()));
		}
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Table"), attrs));

		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, javaType, Modifier.PUBLIC, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, entityAnnotations, null);
		classpathOperations.generateClassFile(details);
	}

	private IdentifiableTable getIdentifiableTable(Element tableElement) {
		// String catalog = StringUtils.trimToNull(tableElement.getAttribute("catalog"));
		String catalog = null;
		String schema = StringUtils.trimToNull(tableElement.getAttribute("schema"));
		String table = StringUtils.trimToNull(tableElement.getAttribute("name"));
		return new IdentifiableTable(catalog, schema, table);
	}
	
	private void processDetectedEntities() {
		Map<IdentifiableTable, JavaType> allDetectedEntities = tableModelService.getAllDetectedEntities();
		for (Map.Entry<IdentifiableTable, JavaType> entry : allDetectedEntities.entrySet()) {
			metadataService.get(DbreMetadata.createIdentifier(entry.getValue(), Path.SRC_MAIN_JAVA));
		}
	}
}
