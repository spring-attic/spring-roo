package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.COLLECTION;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
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
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooSolrSearchable}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = SolrMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    private static final JavaType SOLR_INPUT_DOCUMENT = new JavaType(
            "org.apache.solr.common.SolrInputDocument");
    private static final JavaType SOLR_QUERY = new JavaType(
            "org.apache.solr.client.solrj.SolrQuery");

    private static final JavaType SOLR_QUERY_RESPONSE = new JavaType(
            "org.apache.solr.client.solrj.response.QueryResponse");
    private static final JavaType SOLR_SERVER = new JavaType(
            "org.apache.solr.client.solrj.SolrServer");

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private SolrSearchAnnotationValues annotationValues;
    private String beanPlural;
    private String javaBeanFieldName;

    public SolrMetadata(final String identifier, final JavaType aspectName,
            final SolrSearchAnnotationValues annotationValues,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final MethodMetadata identifierAccessor,
            final FieldMetadata versionField,
            final Map<MethodMetadata, FieldMetadata> accessorDetails,
            final String javaTypePlural) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.notNull(annotationValues,
                "Solr search annotation values required");
        Validate.isTrue(isValid(identifier),
                "Metadata identification string '%s' is invalid", identifier);
        Validate.notNull(identifierAccessor,
                "Persistence identifier method required");
        Validate.notNull(accessorDetails, "Public accessors requred");
        Validate.notBlank(javaTypePlural,
                "Plural representation of java type required");

        if (!isValid()) {
            return;
        }

        javaBeanFieldName = JavaSymbolName.getReservedWordSafeName(destination)
                .getSymbolName();
        this.annotationValues = annotationValues;
        beanPlural = javaTypePlural;

        if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
            valid = false;
            return;
        }

        builder.addField(getSolrServerField());
        if (StringUtils.isNotBlank(annotationValues.getSimpleSearchMethod())) {
            builder.addMethod(getSimpleSearchMethod());
        }
        if (StringUtils.isNotBlank(annotationValues.getSearchMethod())) {
            builder.addMethod(getSearchMethod());
        }
        if (StringUtils.isNotBlank(annotationValues.getIndexMethod())) {
            builder.addMethod(getIndexEntityMethod());
            builder.addMethod(getIndexEntitiesMethod(accessorDetails,
                    identifierAccessor, versionField));
        }
        if (StringUtils.isNotBlank(annotationValues.getDeleteIndexMethod())) {
            builder.addMethod(getDeleteIndexMethod(identifierAccessor));
        }
        if (StringUtils.isNotBlank(annotationValues
                .getPostPersistOrUpdateMethod())) {
            builder.addMethod(getPostPersistOrUpdateMethod());
        }
        if (StringUtils.isNotBlank(annotationValues.getPreRemoveMethod())) {
            builder.addMethod(getPreRemoveMethod());
        }

        builder.addMethod(getSolrServerMethod());

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    public SolrSearchAnnotationValues getAnnotationValues() {
        return annotationValues;
    }

    private MethodMetadataBuilder getDeleteIndexMethod(
            final MethodMetadata identifierAccessor) {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getDeleteIndexMethod());
        final JavaType parameterType = destination;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName(javaBeanFieldName));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(getSimpleName(SOLR_SERVER)
                + " solrServer = solrServer();");
        bodyBuilder.appendFormalLine("try {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("solrServer.deleteById(\""
                + destination.getSimpleTypeName().toLowerCase() + "_\" + "
                + javaBeanFieldName + "."
                + identifierAccessor.getMethodName().getSymbolName() + "());");
        bodyBuilder.appendFormalLine("solrServer.commit();");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("} catch (Exception e) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("e.printStackTrace();");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC | Modifier.STATIC, methodName,
                JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
        return methodBuilder;
    }

    private MethodMetadataBuilder getIndexEntitiesMethod(
            final Map<MethodMetadata, FieldMetadata> accessorDetails,
            final MethodMetadata identifierAccessor,
            final FieldMetadata versionField) {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getIndexMethod() + beanPlural);
        final JavaType parameterType = new JavaType(
                COLLECTION.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(destination));
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName(beanPlural.toLowerCase()));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String sid = getSimpleName(SOLR_INPUT_DOCUMENT);
        final List<JavaType> sidTypeParams = Arrays.asList(SOLR_INPUT_DOCUMENT);

        String listVar = "documents";
        if (listVar.equals(beanPlural.toLowerCase())) {
            listVar += "_";
        }
        bodyBuilder.appendFormalLine(getSimpleName(new JavaType(List.class
                .getName(), 0, DataType.TYPE, null, sidTypeParams))
                + " "
                + listVar
                + " = new "
                + getSimpleName(new JavaType(ArrayList.class.getName(), 0,
                        DataType.TYPE, null, sidTypeParams)) + "();");
        bodyBuilder.appendFormalLine("for (" + destination.getSimpleTypeName()
                + " " + javaBeanFieldName + " : " + beanPlural.toLowerCase()
                + ") {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(sid + " sid = new " + sid + "();");
        bodyBuilder.appendFormalLine("sid.addField(\"id\", \""
                + destination.getSimpleTypeName().toLowerCase() + "_\" + "
                + javaBeanFieldName + "." + identifierAccessor.getMethodName()
                + "());");
        final StringBuilder textField = new StringBuilder("new StringBuilder()");

        for (final Entry<MethodMetadata, FieldMetadata> entry : accessorDetails
                .entrySet()) {
            final FieldMetadata field = entry.getValue();
            if (versionField != null
                    && field.getFieldName().equals(versionField.getFieldName())) {
                continue;
            }
            if (field.getFieldType().isCommonCollectionType()) {
                continue;
            }
            if (!textField.toString().endsWith("StringBuilder()")) {
                textField.append(".append(\" \")");
            }
            final JavaSymbolName accessorMethod = entry.getKey()
                    .getMethodName();
            if (field.getFieldType().equals(CALENDAR)) {
                textField.append(".append(").append(javaBeanFieldName)
                        .append(".").append(accessorMethod)
                        .append("().getTime()").append(")");
            }
            else {
                textField.append(".append(").append(javaBeanFieldName)
                        .append(".").append(accessorMethod).append("()")
                        .append(")");
            }
            String fieldName = javaBeanFieldName
                    + "."
                    + field.getFieldName().getSymbolName().toLowerCase()
                    + SolrUtils
                            .getSolrDynamicFieldPostFix(field.getFieldType());
            for (final AnnotationMetadata annotation : field.getAnnotations()) {
                if (annotation.getAnnotationType()
                        .equals(new JavaType(
                                "org.apache.solr.client.solrj.beans.Field"))) {
                    final AnnotationAttributeValue<?> value = annotation
                            .getAttribute(new JavaSymbolName("value"));
                    if (value != null) {
                        fieldName = value.getValue().toString();
                    }
                }
            }
            if (field.getFieldType().equals(CALENDAR)) {
                bodyBuilder.appendFormalLine("sid.addField(\"" + fieldName
                        + "\", " + javaBeanFieldName + "."
                        + accessorMethod.getSymbolName() + "().getTime());");
            }
            else {
                bodyBuilder.appendFormalLine("sid.addField(\"" + fieldName
                        + "\", " + javaBeanFieldName + "."
                        + accessorMethod.getSymbolName() + "());");
            }
        }
        bodyBuilder
                .appendFormalLine("// Add summary field to allow searching documents for objects of this type");
        bodyBuilder.appendFormalLine("sid.addField(\""
                + destination.getSimpleTypeName().toLowerCase()
                + "_solrsummary_t\", " + textField.toString() + ");");
        bodyBuilder.appendFormalLine(listVar + ".add(sid);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("try {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine(getSimpleName(SOLR_SERVER)
                + " solrServer = solrServer();");
        bodyBuilder.appendFormalLine("solrServer.add(" + listVar + ");");
        bodyBuilder.appendFormalLine("solrServer.commit();");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("} catch (Exception e) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("e.printStackTrace();");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC | Modifier.STATIC, methodName,
                JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
        methodBuilder.addAnnotation(new AnnotationMetadataBuilder(ASYNC));
        return methodBuilder;
    }

    private MethodMetadataBuilder getIndexEntityMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getIndexMethod()
                        + destination.getSimpleTypeName());
        final JavaType parameterType = destination;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final JavaType listType = JavaType.getInstance(List.class.getName(), 0,
                DataType.TYPE, null, parameterType);
        final JavaType arrayListType = JavaType.getInstance(
                ArrayList.class.getName(), 0, DataType.TYPE, null,
                parameterType);
        bodyBuilder.appendFormalLine(getSimpleName(listType) + " "
                + beanPlural.toLowerCase() + " = new "
                + getSimpleName(arrayListType) + "();");
        bodyBuilder.appendFormalLine(beanPlural.toLowerCase() + ".add("
                + javaBeanFieldName + ");");
        bodyBuilder.appendFormalLine(annotationValues.getIndexMethod()
                + beanPlural + "(" + beanPlural.toLowerCase() + ");");

        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName(javaBeanFieldName));

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC
                | Modifier.STATIC, methodName, JavaType.VOID_PRIMITIVE,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private MethodMetadataBuilder getPostPersistOrUpdateMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getPostPersistOrUpdateMethod());
        if (governorHasMethod(methodName)) {
            return null;
        }

        final List<AnnotationMetadataBuilder> annotations = Arrays.asList(
                new AnnotationMetadataBuilder(POST_UPDATE),
                new AnnotationMetadataBuilder(POST_PERSIST));
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(annotationValues.getIndexMethod()
                + destination.getSimpleTypeName() + "(this);");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getPreRemoveMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getPreRemoveMethod());
        if (governorHasMethod(methodName)) {
            return null;
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(PRE_REMOVE));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(annotationValues.getDeleteIndexMethod()
                + "(this);");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PRIVATE, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }

    private MethodMetadataBuilder getSearchMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getSearchMethod());
        final JavaType parameterType = SOLR_QUERY;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final JavaType queryResponse = SOLR_QUERY_RESPONSE;
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("query"));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("try {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("return solrServer().query(query);");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("} catch (Exception e) {");
        bodyBuilder.indent();
        bodyBuilder.appendFormalLine("e.printStackTrace();");
        bodyBuilder.indentRemove();
        bodyBuilder.appendFormalLine("}");
        bodyBuilder.appendFormalLine("return new "
                + getSimpleName(queryResponse) + "();");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC
                | Modifier.STATIC, methodName, queryResponse,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private String getSimpleName(final JavaType type) {
        return type.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
    }

    private MethodMetadataBuilder getSimpleSearchMethod() {
        final JavaSymbolName methodName = new JavaSymbolName(
                annotationValues.getSimpleSearchMethod());
        final JavaType parameterType = JavaType.STRING;
        if (governorHasMethod(methodName, parameterType)) {
            return null;
        }

        final JavaType queryResponse = SOLR_QUERY_RESPONSE;
        final List<JavaSymbolName> parameterNames = Arrays
                .asList(new JavaSymbolName("queryString"));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("String searchString = \""
                + destination.getSimpleTypeName()
                + "_solrsummary_t:\" + queryString;");
        bodyBuilder
                .appendFormalLine("return search(new SolrQuery(searchString.toLowerCase()));");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC
                | Modifier.STATIC, methodName, queryResponse,
                AnnotatedJavaType.convertFromJavaTypes(parameterType),
                parameterNames, bodyBuilder);
    }

    private FieldMetadataBuilder getSolrServerField() {
        final JavaSymbolName fieldName = new JavaSymbolName("solrServer");
        if (governorTypeDetails.getDeclaredField(fieldName) != null) {
            return null;
        }

        return new FieldMetadataBuilder(getId(), Modifier.TRANSIENT,
                Arrays.asList(new AnnotationMetadataBuilder(AUTOWIRED)),
                fieldName, SOLR_SERVER);
    }

    private MethodMetadataBuilder getSolrServerMethod() {
        final JavaSymbolName methodName = new JavaSymbolName("solrServer");
        if (governorHasMethod(methodName)) {
            return null;
        }

        final JavaType returnType = SOLR_SERVER;
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine(getSimpleName(returnType)
                + " _solrServer = new " + destination.getSimpleTypeName()
                + "().solrServer;");
        bodyBuilder
                .appendFormalLine("if (_solrServer == null) throw new IllegalStateException(\"Solr server has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)\");");
        bodyBuilder.appendFormalLine("return _solrServer;");

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC
                | Modifier.STATIC, methodName, returnType, bodyBuilder);
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}