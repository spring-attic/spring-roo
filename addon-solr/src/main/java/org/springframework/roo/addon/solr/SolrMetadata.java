package org.springframework.roo.addon.solr;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooSolrSearchable}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = SolrMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private SolrSearchAnnotationValues annotationValues;
	private String beanPlural;
	private String javaBeanFieldName;

	public SolrMetadata(String identifier, JavaType aspectName, SolrSearchAnnotationValues annotationValues, PhysicalTypeMetadata governorPhysicalTypeMetadata, MethodMetadata identifierAccessor, FieldMetadata versionField, Map<MethodMetadata, FieldMetadata> accessorDetails, String javaTypePlural) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(annotationValues, "Solr search annotation values required");
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(identifierAccessor, "Persistence identifier method metadata required");
		Assert.notNull(accessorDetails, "Metadata for public accessors requred");
		Assert.hasText(javaTypePlural, "Plural representation of java type required");

		if (!isValid()) {
			return;
		}
		this.javaBeanFieldName = destination.getSimpleTypeName().toLowerCase();
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(javaBeanFieldName)) {
			this.javaBeanFieldName = "_" + javaBeanFieldName;
		}
		this.annotationValues = annotationValues;
		this.beanPlural = javaTypePlural;
		
		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			// TODO Do something with supertype
			return;
		}
		
		builder.addField(getSolrServerField());
		if (StringUtils.hasText(annotationValues.getSimpleSearchMethod())) {
			builder.addMethod(getSimpleSearchMethod());
		}
		if (StringUtils.hasText(annotationValues.getSearchMethod())) {
			builder.addMethod(getSearchMethod());
		}
		if (StringUtils.hasText(annotationValues.getIndexMethod())) {
			builder.addMethod(getIndexEntityMethod());
			builder.addMethod(getIndexEntitiesMethod(accessorDetails, identifierAccessor, versionField));
		}
		if (StringUtils.hasText(annotationValues.getDeleteIndexMethod())) {
			builder.addMethod(getDeleteIndexMethod(identifierAccessor));
		}
		if (StringUtils.hasText(annotationValues.getPostPersistOrUpdateMethod())) {
			builder.addMethod(getPostPersistOrUpdateMethod());
		}
		if (StringUtils.hasText(annotationValues.getPreRemoveMethod())) {
			builder.addMethod(getPreRemoveMethod());
		}

		builder.addMethod(getSolrServerMethod());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	public SolrSearchAnnotationValues getAnnotationValues() {
		return annotationValues;
	}

	private FieldMetadata getSolrServerField() {
		JavaSymbolName fieldName = new JavaSymbolName("solrServer");
		List<AnnotationMetadataBuilder> autowired = new ArrayList<AnnotationMetadataBuilder>();
		autowired.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.beans.factory.annotation.Autowired")));
		FieldMetadata fieldMd = MemberFindingUtils.getDeclaredField(governorTypeDetails, fieldName);
		if (fieldMd != null) return fieldMd;
		return new FieldMetadataBuilder(getId(), Modifier.TRANSIENT, autowired, fieldName, new JavaType("org.apache.solr.client.solrj.SolrServer")).build();
	}

	private MethodMetadata getPostPersistOrUpdateMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getPostPersistOrUpdateMethod());
		MethodMetadata postPersistOrUpdate = MemberFindingUtils.getMethod(governorTypeDetails, methodName, new ArrayList<JavaType>());
		if (postPersistOrUpdate != null) return postPersistOrUpdate;

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.PostUpdate")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.PostPersist")));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(annotationValues.getIndexMethod() + destination.getSimpleTypeName() + "(this);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getIndexEntityMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getIndexMethod() + destination.getSimpleTypeName());
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(destination, new ArrayList<AnnotationMetadata>()));
		MethodMetadata indexEntityMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(paramTypes));
		if (indexEntityMethod != null) return indexEntityMethod;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(destination);
		bodyBuilder.appendFormalLine(getSimpleName(new JavaType(List.class.getName(), 0, DataType.TYPE, null, typeParams)) + " " + beanPlural.toLowerCase() + " = new " + getSimpleName(new JavaType(ArrayList.class.getName(), 0, DataType.TYPE, null, typeParams)) + "();");
		bodyBuilder.appendFormalLine(beanPlural.toLowerCase() + ".add(" + javaBeanFieldName + ");");
		bodyBuilder.appendFormalLine(annotationValues.getIndexMethod() + beanPlural + "(" + beanPlural.toLowerCase() + ");");

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaBeanFieldName));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getIndexEntitiesMethod(Map<MethodMetadata, FieldMetadata> accessorDetails, MethodMetadata identifierAccessor, FieldMetadata versionField) {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getIndexMethod() + beanPlural);
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(destination);
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(new JavaType("java.util.Collection", 0, DataType.TYPE, null, typeParams), new ArrayList<AnnotationMetadata>()));
		MethodMetadata indexEntitiesMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(paramTypes));
		if (indexEntitiesMethod != null) return indexEntitiesMethod;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		JavaType solrInputDocument = new JavaType("org.apache.solr.common.SolrInputDocument");
		String sid = getSimpleName(solrInputDocument);
		List<JavaType> sidTypeParams = new ArrayList<JavaType>();
		sidTypeParams.add(solrInputDocument);
		bodyBuilder.appendFormalLine(getSimpleName(new JavaType(List.class.getName(), 0, DataType.TYPE, null, sidTypeParams)) + " documents = new " + getSimpleName(new JavaType(ArrayList.class.getName(), 0, DataType.TYPE, null, sidTypeParams)) + "();");
		bodyBuilder.appendFormalLine("for (" + destination.getSimpleTypeName() + " " + javaBeanFieldName + " : " + beanPlural.toLowerCase() + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(sid + " sid = new " + sid + "();");
		bodyBuilder.appendFormalLine("sid.addField(\"id\", \"" + destination.getSimpleTypeName().toLowerCase() + "_\" + " + javaBeanFieldName + "." + identifierAccessor.getMethodName() + "());");
		StringBuilder textField = new StringBuilder("new StringBuilder()");

		for (MethodMetadata method : accessorDetails.keySet()) {
			FieldMetadata field = accessorDetails.get(method);
			if (versionField != null && field.getFieldName().equals(versionField.getFieldName())) {
				continue;
			}
			if (field.getFieldType().isCommonCollectionType()) {
				continue;
			}
			if (!textField.toString().endsWith("StringBuilder()")) {
				textField.append(".append(\" \")");
			}
			textField.append(".append(").append(javaBeanFieldName).append(".").append(method.getMethodName()).append("()").append(")");
			String fieldName = javaBeanFieldName + "." + field.getFieldName().getSymbolName().toLowerCase() + SolrUtils.getSolrDynamicFieldPostFix(field.getFieldType());
			for (AnnotationMetadata annotation : field.getAnnotations()) {
				if (annotation.getAnnotationType().equals(new JavaType("org.apache.solr.client.solrj.beans.Field"))) {
					AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("value"));
					if (value != null) {
						fieldName = value.getValue().toString();
					}
				}
			}
			bodyBuilder.appendFormalLine("sid.addField(\"" + fieldName + "\", " + javaBeanFieldName + "." + method.getMethodName().getSymbolName() + "());");
		}
		bodyBuilder.appendFormalLine("// Add summary field to allow searching documents for objects of this type");
		bodyBuilder.appendFormalLine("sid.addField(\"" + destination.getSimpleTypeName().toLowerCase() + "_solrsummary_t\", " + textField.toString() + ");");
		bodyBuilder.appendFormalLine("documents.add(sid);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(getSimpleName(new JavaType("org.apache.solr.client.solrj.SolrServer")) + " solrServer = solrServer();");
		bodyBuilder.appendFormalLine("solrServer.add(documents);");
		bodyBuilder.appendFormalLine("solrServer.commit();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (Exception e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("e.printStackTrace();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(beanPlural.toLowerCase()));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.scheduling.annotation.Async")));
		return methodBuilder.build();
	}

	private MethodMetadata getDeleteIndexMethod(MethodMetadata identifierAccessor) {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getDeleteIndexMethod());
		List<AnnotatedJavaType> paramTypes = new ArrayList<AnnotatedJavaType>();
		paramTypes.add(new AnnotatedJavaType(destination, new ArrayList<AnnotationMetadata>()));
		MethodMetadata deleteIndex = MemberFindingUtils.getMethod(governorTypeDetails, methodName, AnnotatedJavaType.convertFromAnnotatedJavaTypes(paramTypes));
		if (deleteIndex != null) return deleteIndex;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getSimpleName(new JavaType("org.apache.solr.client.solrj.SolrServer")) + " solrServer = solrServer();");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("solrServer.deleteById(\"" + destination.getSimpleTypeName().toLowerCase() + "_\" + " + javaBeanFieldName + "." + identifierAccessor.getMethodName().getSymbolName() + "());");
		bodyBuilder.appendFormalLine("solrServer.commit();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (Exception e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("e.printStackTrace();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(javaBeanFieldName));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, paramTypes, paramNames, bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType("org.springframework.scheduling.annotation.Async")));
		return methodBuilder.build();
	}

	private MethodMetadata getPreRemoveMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getPreRemoveMethod());
		MethodMetadata preDelete = MemberFindingUtils.getMethod(governorTypeDetails, methodName, new ArrayList<JavaType>());
		if (preDelete != null) return preDelete;

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		// annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.scheduling.annotation.Async")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.PreRemove")));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(annotationValues.getDeleteIndexMethod() + "(this);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getSimpleSearchMethod() {
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.STRING_OBJECT);

		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSimpleSearchMethod());
		MethodMetadata searchMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (searchMethod != null) return searchMethod;

		JavaType queryResponse = new JavaType("org.apache.solr.client.solrj.response.QueryResponse");
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("queryString"));
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(destination);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String searchString = \"" + destination.getSimpleTypeName() + "_solrsummary_t:\" + queryString;");
		bodyBuilder.appendFormalLine("return search(new SolrQuery(searchString.toLowerCase()));");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, queryResponse, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getSearchMethod() {
		JavaType queryResponse = new JavaType("org.apache.solr.client.solrj.response.QueryResponse");
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSearchMethod());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(new JavaType("org.apache.solr.client.solrj.SolrQuery"));
		MethodMetadata searchMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (searchMethod != null) return searchMethod;

		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("query"));
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(destination);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return solrServer().query(query);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch (Exception e) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("e.printStackTrace();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return new " + getSimpleName(queryResponse) + "();");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, queryResponse, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getSolrServerMethod() {
		JavaSymbolName methodName = new JavaSymbolName("solrServer");
		MethodMetadata solrServerMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, new ArrayList<JavaType>());
		if (solrServerMethod != null) return solrServerMethod;

		JavaType solrServer = new JavaType("org.apache.solr.client.solrj.SolrServer");
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(getSimpleName(solrServer) + " _solrServer = new " + destination.getSimpleTypeName() + "().solrServer;");
		bodyBuilder.appendFormalLine("if (_solrServer == null) throw new IllegalStateException(\"Solr server has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)\");");
		bodyBuilder.appendFormalLine("return _solrServer;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, methodName, solrServer, bodyBuilder);
		return methodBuilder.build();
	}

	private String getSimpleName(JavaType type) {
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