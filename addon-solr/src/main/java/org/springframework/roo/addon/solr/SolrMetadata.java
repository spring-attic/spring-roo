package org.springframework.roo.addon.solr;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.tostring.ToStringMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooSolrSearchable}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class SolrMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = SolrMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private EntityMetadata entityMetadata;
	private BeanInfoMetadata beanInfoMetadata;
	private SolrSearchAnnotationValues annotationValues;
	private String beanPlural;
	private MetadataService metadataService;
	private PathResolver pathResolver;
	private FileManager fileManager;

	public SolrMetadata(String identifier, JavaType aspectName, SolrSearchAnnotationValues annotationValues, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata entityMetadata, BeanInfoMetadata beanInfoMetadata, ToStringMetadata toStringMetadata, MetadataService metadataService, PathResolver pathResolver, FileManager fileManager) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(annotationValues, "Solr search annotation values required");
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(pathResolver, "PathResolver required");
		Assert.notNull(fileManager, "Filemanager required");
		
		if (!isValid()) {
			return;
		}
		this.entityMetadata = entityMetadata;
		this.beanInfoMetadata = beanInfoMetadata;
		this.annotationValues = annotationValues;
		this.metadataService = metadataService;
		this.pathResolver = pathResolver;
		this.fileManager = fileManager;
		
		PluralMetadata plural = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(beanInfoMetadata.getJavaBean(), Path.SRC_MAIN_JAVA));
		Assert.notNull(plural, "Could not obtain plural metadata for type " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName());
		beanPlural = plural.getPlural();
		
		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			//do something with supertype
		} else {
			builder.addField(getSolrServerField());
			if (annotationValues.getSimpleSearchMethod() != null && annotationValues.getSimpleSearchMethod().length() > 0) {
				builder.addMethod(getSimpleSearchMethod());
			}
			if (annotationValues.getSearchMethod() != null && annotationValues.getSearchMethod().length() > 0) {
				builder.addMethod(getSearchMethod());
			}
			if (annotationValues.getIndexMethod() != null && annotationValues.getIndexMethod().length() > 0) {
				builder.addMethod(getIndexEntityMethod());
				builder.addMethod(getIndexEntitiesMethod());
			}
			if (annotationValues.getDeleteIndexMethod() != null && annotationValues.getDeleteIndexMethod().length() > 0) {
				builder.addMethod(getDeleteIndexMethod());
			}
			if (annotationValues.getPostPersistOrUpdateMethod() != null && annotationValues.getPostPersistOrUpdateMethod().length() > 0) {
				builder.addMethod(getPostPersistOrUpdateMethod());
			}
			if (annotationValues.getPreRemoveMethod() != null && annotationValues.getPreRemoveMethod().length() > 0) {
				builder.addMethod(getPreRemoveMethod());
			}
			
			builder.addMethod(getSolrServerMethod());
			
			managePointcuts();
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	private void managePointcuts() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Could not obtain project metadata");
		
		String aspectLocation = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, StringUtils.replace(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName(), ".", "/") + "/SolrSearchAsyncTaskExecutor.aj");
		if (fileManager.exists(aspectLocation)) {
			try {
				String contents = FileCopyUtils.copyToString(new FileReader(aspectLocation));
				boolean writeNeeded = false;
				if (annotationValues.getIndexMethod() != null || annotationValues.getIndexMethod().length() != 0) {
					if (!contents.contains(annotationValues.getIndexMethod() + beanInfoMetadata.getJavaBean().getSimpleTypeName())) {
						contents = StringUtils.replace(contents, "asyncMethods():", "asyncMethods(): execution(void " + annotationValues.getIndexMethod() + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "()) ||");
						writeNeeded = true;
					}
					if (!contents.contains(annotationValues.getIndexMethod() + beanPlural)) {
						contents = StringUtils.replace(contents, "asyncMethods():", "asyncMethods(): execution(void " + annotationValues.getIndexMethod() + beanPlural + "()) ||");
						writeNeeded = true;
					}
				}
				
				if (writeNeeded) {
					MutableFile file = fileManager.updateFile(aspectLocation);
				    Writer output = new BufferedWriter(new OutputStreamWriter(file.getOutputStream()));
				    try {
				      output.write(contents.toString());
				    }
				    finally {
				      output.close();
				    }
				}
			} catch (IOException e) {
				new IllegalStateException("Could not copy SolrSearchAsyncTaskExecutor.aj into project", e);
			}
		}
	}
	
	public SolrSearchAnnotationValues getAnnotationValues() {
		return annotationValues;
	}
	
	private FieldMetadata getSolrServerField() {
		List<AnnotationMetadata> autowired = new ArrayList<AnnotationMetadata>();
		autowired.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));
		return new DefaultFieldMetadata(getId(), Modifier.TRANSIENT, new JavaSymbolName("solrServer"), new JavaType("org.apache.solr.client.solrj.SolrServer"), null, autowired);
	}
	
	private MethodMetadata getPostPersistOrUpdateMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getPostPersistOrUpdateMethod());
		MethodMetadata postPersistOrUpdate = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (postPersistOrUpdate != null)
			return postPersistOrUpdate;

		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.PostUpdate"), new ArrayList<AnnotationAttributeValue<?>>()));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.PostPersist"), new ArrayList<AnnotationAttributeValue<?>>()));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(annotationValues.getIndexMethod() + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "(this);");
		return new DefaultMethodMetadata(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, null, null, annotations, null, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getIndexEntityMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getIndexMethod() + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		MethodMetadata indexEntityMethod = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (indexEntityMethod != null)
			return indexEntityMethod;
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(beanInfoMetadata.getJavaBean());
		bodyBuilder.appendFormalLine(getSimleName(new JavaType(List.class.getName(), 0, DataType.TYPE, null, typeParams)) + " " + beanPlural.toLowerCase() + " = new " + getSimleName(new JavaType(ArrayList.class.getName(), 0, DataType.TYPE, null, typeParams)) + "();");
		bodyBuilder.appendFormalLine(beanPlural.toLowerCase() + ".add(" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + ");");
		bodyBuilder.appendFormalLine(annotationValues.getIndexMethod() + beanPlural + "(" + beanPlural.toLowerCase() + ");");
		
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(beanInfoMetadata.getJavaBean(), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase()));
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		return new DefaultMethodMetadata(getId(), modifier, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	@SuppressWarnings("static-access")
	private MethodMetadata getIndexEntitiesMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getIndexMethod() + beanPlural);
		MethodMetadata indexEntitiesMethod = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (indexEntitiesMethod != null)
			return indexEntitiesMethod;
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		JavaType solrInputDocument = new JavaType("org.apache.solr.common.SolrInputDocument");
		String sid = getSimleName(solrInputDocument);
		String simpleBeanName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		List<JavaType> sidTypeParams = new ArrayList<JavaType>();
		sidTypeParams.add(solrInputDocument);
		bodyBuilder.appendFormalLine(getSimleName(new JavaType(List.class.getName(), 0, DataType.TYPE, null, sidTypeParams)) + " documents = new " + getSimleName(new JavaType(ArrayList.class.getName(), 0, DataType.TYPE, null, sidTypeParams)) + "();");
		bodyBuilder.appendFormalLine("for (" + beanInfoMetadata.getJavaBean().getSimpleTypeName() + " " + simpleBeanName + " : " + beanPlural.toLowerCase() + ") {");	
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(sid + " sid = new " + sid + "();");
		bodyBuilder.appendFormalLine("sid.addField(\"id\", \"" + simpleBeanName + ".\" + " + simpleBeanName + "." + entityMetadata.getIdentifierAccessor().getMethodName() + "());");
		StringBuilder textField = new StringBuilder("new StringBuilder()");
		
		for (MethodMetadata method: beanInfoMetadata.getPublicAccessors()) {
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(beanInfoMetadata.getPropertyNameForJavaBeanMethod(method));
			Assert.notNull(field, "Could not determine field '" + method.getMethodName().getSymbolName().substring(3) + "' for method " + method.getMethodName().getSymbolName());
			if(field.getFieldType().isCommonCollectionType() || // not interested in collections for now
					field.getFieldName().equals(entityMetadata.getVersionField().getFieldName())) { // not interested in the version field
				continue;
			} 
			if (!textField.toString().endsWith("StringBuilder()")) {
				textField.append(".append(\" \")");
			}
			textField.append(".append(").append(simpleBeanName).append(".").append(method.getMethodName()).append("()").append(")");
			String fieldName = simpleBeanName + "." + field.getFieldName().getSymbolName().toLowerCase() + SolrUtils.getSolrDynamicFieldPostFix(field.getFieldType());
			for (AnnotationMetadata annotation: field.getAnnotations()) {
				if (annotation.getAnnotationType().equals(new JavaType("org.apache.solr.client.solrj.beans.Field"))) {
					AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("value"));
					if (value != null) {
						fieldName = value.getValue().toString();
					}
				}		
			}
			bodyBuilder.appendFormalLine("sid.addField(\"" + fieldName + "\", " + simpleBeanName + "." + method.getMethodName().getSymbolName() + "());");
		}
		bodyBuilder.appendFormalLine("//add summary field to allow searching documents for objects of this type");
		bodyBuilder.appendFormalLine("sid.addField(\"" + simpleBeanName + ".solrsummary_t\", " + textField.toString() + ");");
		bodyBuilder.appendFormalLine("documents.add(sid);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(getSimleName(new JavaType("org.apache.solr.client.solrj.SolrServer")) + " solrServer = solrServer();");
		bodyBuilder.appendFormalLine("solrServer.add(documents);");
		bodyBuilder.appendFormalLine("solrServer.commit();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (Exception e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("e.printStackTrace();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
//		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.scheduling.annotation.Async"), new ArrayList<AnnotationAttributeValue<?>>()));
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(beanInfoMetadata.getJavaBean());
		paramTypes.add(new AnnotatedJavaType(new JavaType("java.util.Collection", 0, DataType.TYPE, null, typeParams), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(beanPlural.toLowerCase()));
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		return new DefaultMethodMetadata(getId(), modifier, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, annotations, null, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getDeleteIndexMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getDeleteIndexMethod());
		MethodMetadata deleteIndex = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (deleteIndex != null)
			return deleteIndex;
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getSimleName(new JavaType("org.apache.solr.client.solrj.SolrServer")) + " solrServer = solrServer();");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("solrServer.deleteById(\"" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + ".\" + " + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + "." + entityMetadata.getIdentifierAccessor().getMethodName().getSymbolName() + "());");
		bodyBuilder.appendFormalLine("solrServer.commit();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (Exception e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("e.printStackTrace();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(beanInfoMetadata.getJavaBean(), new ArrayList<AnnotationMetadata>()));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase()));
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		return new DefaultMethodMetadata(getId(), modifier, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	private MethodMetadata getPreRemoveMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getPreRemoveMethod());
		MethodMetadata preDelete = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (preDelete != null)
			return preDelete;
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
//		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.scheduling.annotation.Async"), new ArrayList<AnnotationAttributeValue<?>>()));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.PreRemove"), new ArrayList<AnnotationAttributeValue<?>>()));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(annotationValues.getDeleteIndexMethod() + "(this);");
		return new DefaultMethodMetadata(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, null, null, annotations, null, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getSimpleSearchMethod() {
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType(String.class.getName()), new ArrayList<AnnotationMetadata>()));
		
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSimpleSearchMethod());
		MethodMetadata searchMethod = methodExists(methodName, paramTypes);
		if (searchMethod != null)
			return searchMethod;
		
		JavaType queryResponse = new JavaType("org.apache.solr.client.solrj.response.QueryResponse");
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("queryString"));
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(governorTypeDetails.getName());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();	
		bodyBuilder.appendFormalLine("return search(new SolrQuery(\"" + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase() + ".solrsummary_t:\" + queryString.toLowerCase()));");
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		return new DefaultMethodMetadata(getId(), modifier, methodName, queryResponse, paramTypes, paramNames, annotations, null, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getSearchMethod() {
		JavaType queryResponse = new JavaType("org.apache.solr.client.solrj.response.QueryResponse");
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSearchMethod());
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("org.apache.solr.client.solrj.SolrQuery"), new ArrayList<AnnotationMetadata>()));
		MethodMetadata searchMethod = methodExists(methodName, paramTypes);
		if (searchMethod != null)
			return searchMethod;
		
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("query"));
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(governorTypeDetails.getName());
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(queryResponse + " rsp = solrServer().query(query);");
		bodyBuilder.appendFormalLine("return rsp;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (Exception e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("e.printStackTrace();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new QueryResponse();");
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		return new DefaultMethodMetadata(getId(), modifier, methodName, queryResponse, paramTypes, paramNames, annotations, null, bodyBuilder.getOutput());
	}
	
	private MethodMetadata getSolrServerMethod() {
		JavaSymbolName methodName = new JavaSymbolName("solrServer");
		MethodMetadata solrServerMethod = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (solrServerMethod != null)
			return solrServerMethod;
		
		JavaType solrServer = new JavaType("org.apache.solr.client.solrj.SolrServer");
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getSimleName(solrServer) + " _solrServer = new " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "().solrServer;");
		bodyBuilder.appendFormalLine("if (_solrServer == null) throw new IllegalStateException(\"Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)\");");
		bodyBuilder.appendFormalLine("return _solrServer;");
		
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		modifier = modifier |= Modifier.FINAL;
		
		return new DefaultMethodMetadata(getId(), modifier, methodName, solrServer, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> paramTypes) {
		// We have no access to method parameter information, so we scan by name alone and treat any match as authoritative
		// We do not scan the superclass, as the caller is expected to know we'll only scan the current class
		for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName) && method.getParameterTypes().equals(paramTypes)) {
				// Found a method of the expected name; we won't check method parameters though
				return method;
			}
		}
		return null;
	}
	
	private String getSimleName(JavaType type) {
		return type.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}