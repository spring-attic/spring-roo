package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.JpaJavaType.POST_PERSIST;
import static org.springframework.roo.model.JpaJavaType.POST_UPDATE;
import static org.springframework.roo.model.JpaJavaType.PRE_REMOVE;
import static org.springframework.roo.model.SpringJavaType.ASYNC;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = SolrMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	// Fields
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
		this.javaBeanFieldName = JavaSymbolName.getReservedWordSafeName(destination).getSymbolName();
		this.annotationValues = annotationValues;
		this.beanPlural = javaTypePlural;
		
		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			valid = false;
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
		autowired.add(new AnnotationMetadataBuilder(AUTOWIRED));
		FieldMetadata fieldMd = MemberFindingUtils.getDeclaredField(governorTypeDetails, fieldName);
		if (fieldMd != null) return fieldMd;
		return new FieldMetadataBuilder(getId(), Modifier.TRANSIENT, autowired, fieldName, new JavaType("org.apache.solr.client.solrj.SolrServer")).build();
	}

	private MethodMetadata getPostPersistOrUpdateMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getPostPersistOrUpdateMethod());
		MethodMetadata postPersistOrUpdateMethod = getGovernorMethod(methodName);
		if (postPersistOrUpdateMethod != null) return postPersistOrUpdateMethod;

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(POST_UPDATE));
		annotations.add(new AnnotationMetadataBuilder(POST_PERSIST));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(annotationValues.getIndexMethod() + destination.getSimpleTypeName() + "(this);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getIndexEntityMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getIndexMethod() + destination.getSimpleTypeName());
		final JavaType parameterType = destination;
		MethodMetadata indexEntityMethod = getGovernorMethod(methodName, parameterType);
		if (indexEntityMethod != null) return indexEntityMethod;

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		final JavaType listType = JavaType.getInstance(List.class.getName(), 0, DataType.TYPE, null, parameterType);
		final JavaType arrayListType = JavaType.getInstance(ArrayList.class.getName(), 0, DataType.TYPE, null, parameterType);
		bodyBuilder.appendFormalLine(getSimpleName(listType) + " " + beanPlural.toLowerCase() + " = new " + getSimpleName(arrayListType) + "();");
		bodyBuilder.appendFormalLine(beanPlural.toLowerCase() + ".add(" + javaBeanFieldName + ");");
		bodyBuilder.appendFormalLine(annotationValues.getIndexMethod() + beanPlural + "(" + beanPlural.toLowerCase() + ");");

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(javaBeanFieldName));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getIndexEntitiesMethod(Map<MethodMetadata, FieldMetadata> accessorDetails, MethodMetadata identifierAccessor, FieldMetadata versionField) {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getIndexMethod() + beanPlural);
		final JavaType parameterType = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Arrays.asList(destination));
		MethodMetadata indexEntitiesMethod = getGovernorMethod(methodName, parameterType);
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

		for (final Entry<MethodMetadata, FieldMetadata> entry : accessorDetails.entrySet()) {
			final FieldMetadata field = entry.getValue();
			if (versionField != null && field.getFieldName().equals(versionField.getFieldName())) {
				continue;
			}
			if (field.getFieldType().isCommonCollectionType()) {
				continue;
			}
			if (!textField.toString().endsWith("StringBuilder()")) {
				textField.append(".append(\" \")");
			}
			final JavaSymbolName accessorMethod = entry.getKey().getMethodName();
			if (field.getFieldType().equals(new JavaType("java.util.Calendar"))) {
				textField.append(".append(").append(javaBeanFieldName).append(".").append(accessorMethod).append("().getTime()").append(")");
			} else {
				textField.append(".append(").append(javaBeanFieldName).append(".").append(accessorMethod).append("()").append(")");
			}
			String fieldName = javaBeanFieldName + "." + field.getFieldName().getSymbolName().toLowerCase() + SolrUtils.getSolrDynamicFieldPostFix(field.getFieldType());
			for (AnnotationMetadata annotation : field.getAnnotations()) {
				if (annotation.getAnnotationType().equals(new JavaType("org.apache.solr.client.solrj.beans.Field"))) {
					AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("value"));
					if (value != null) {
						fieldName = value.getValue().toString();
					}
				}
			}
			if (field.getFieldType().equals(new JavaType("java.util.Calendar"))) {
				bodyBuilder.appendFormalLine("sid.addField(\"" + fieldName + "\", " + javaBeanFieldName + "." + accessorMethod.getSymbolName() + "().getTime());");
			} else {
				bodyBuilder.appendFormalLine("sid.addField(\"" + fieldName + "\", " + javaBeanFieldName + "." + accessorMethod.getSymbolName() + "());");
			}
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

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(beanPlural.toLowerCase()));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
		return methodBuilder.build();
	}

	private MethodMetadata getDeleteIndexMethod(MethodMetadata identifierAccessor) {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getDeleteIndexMethod());
		final JavaType parameterType = destination;
		MethodMetadata deleteIndex = getGovernorMethod(methodName, parameterType);
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

		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName(javaBeanFieldName));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
		return methodBuilder.build();
	}

	private MethodMetadata getPreRemoveMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getPreRemoveMethod());
		MethodMetadata preDeleteMethod = getGovernorMethod(methodName);
		if (preDeleteMethod != null) return preDeleteMethod;

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(PRE_REMOVE));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(annotationValues.getDeleteIndexMethod() + "(this);");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private MethodMetadata getSimpleSearchMethod() {
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSimpleSearchMethod());
		final JavaType parameterType = JavaType.STRING;
		MethodMetadata searchMethod = getGovernorMethod(methodName, parameterType);
		if (searchMethod != null) return searchMethod;

		JavaType queryResponse = new JavaType("org.apache.solr.client.solrj.response.QueryResponse");
		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("queryString"));
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("String searchString = \"" + destination.getSimpleTypeName() + "_solrsummary_t:\" + queryString;");
		bodyBuilder.appendFormalLine("return search(new SolrQuery(searchString.toLowerCase()));");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, queryResponse, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getSearchMethod() {
		JavaType queryResponse = new JavaType("org.apache.solr.client.solrj.response.QueryResponse");
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getSearchMethod());
		final JavaType parameterType = new JavaType("org.apache.solr.client.solrj.SolrQuery");
		MethodMetadata searchMethod = getGovernorMethod(methodName, parameterType);
		if (searchMethod != null) return searchMethod;

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("query"));

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
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, queryResponse, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getSolrServerMethod() {
		JavaSymbolName methodName = new JavaSymbolName("solrServer");
		MethodMetadata solrServerMethod = getGovernorMethod(methodName);
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