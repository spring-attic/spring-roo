package org.springframework.roo.addon.finder;

import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JpaJavaType.ENTITY_MANAGER;
import static org.springframework.roo.model.JpaJavaType.TYPED_QUERY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJpaActiveRecord#finders()}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class FinderMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = FinderMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

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

    private final List<MethodMetadata> dynamicFinderMethods = new ArrayList<MethodMetadata>();

    private Map<JavaSymbolName, QueryHolder> queryHolders;

    public FinderMetadata(final String identifier, final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final MethodMetadata entityManagerMethod,
            final Map<JavaSymbolName, QueryHolder> queryHolders) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.isTrue(entityManagerMethod != null || queryHolders.isEmpty(),
                "EntityManager method required if any query holders are provided");
        Validate.notNull(queryHolders, "Query holders required");

        if (!isValid()) {
            return;
        }

        this.queryHolders = queryHolders;

        for (final JavaSymbolName finderName : queryHolders.keySet()) {
           
        	// finder and count
        	final MethodMetadataBuilder methodBuilder = getDynamicFinderMethod(
                    finderName, entityManagerMethod, false);
            builder.addMethod(methodBuilder);
            dynamicFinderMethods.add(methodBuilder.build());
            
            // sorted finder
            if(!finderName.getSymbolName().startsWith("count")) {
	            final MethodMetadataBuilder methodBuilderSorted = getDynamicFinderMethod(
	                    finderName, entityManagerMethod, true);
	            builder.addMethod(methodBuilderSorted);
	            dynamicFinderMethods.add(methodBuilderSorted.build());
            }
        }

        // Create a representation of the desired output ITD
        itdTypeDetails = builder.build();
    }

    /**
     * Obtains all the currently-legal dynamic finders known to this metadata
     * instance. This may be a subset (or even completely empty) versus those
     * requested via the {@link RooJpaActiveRecord} annotation, as the user may
     * have made a typing error in representing the requested dynamic finder,
     * the field may have been deleted by the user, or an add-on which produces
     * the field (or its mutator) might not yet be loaded or in error or other
     * similar conditions.
     * 
     * @return a non-null, immutable representation of currently-available
     *         finder methods (never returns null, but may be empty)
     */
    public List<MethodMetadata> getAllDynamicFinders() {
        return Collections.unmodifiableList(dynamicFinderMethods);
    }

    /**
     * Locates a dynamic finder method of the specified name, or creates one on
     * demand if not present.
     * <p>
     * It is required that the requested name was defined in the
     * {@link RooJpaActiveRecord#finders()}. If it is not present, an exception
     * is thrown.
     * 
     * @param finderName the dynamic finder method name
     * @param entityManagerMethod required
     * @return the user-defined method, or an ITD-generated method (never
     *         returns null)
     */
    private MethodMetadataBuilder getDynamicFinderMethod(
            final JavaSymbolName finderName,
            final MethodMetadata entityManagerMethod, final Boolean sorted) {
        Validate.notNull(finderName, "Dynamic finder method name is required");
        Validate.isTrue(queryHolders.containsKey(finderName),
                "Undefined method name '%s'", finderName.getSymbolName());

   
        // To get this far we need to create the method...
        final List<JavaType> parameters = new ArrayList<JavaType>();
        parameters.add(destination);
        JavaType typedQueryType = new JavaType(
                TYPED_QUERY.getFullyQualifiedTypeName(), 0, DataType.TYPE,
                null, parameters);
        if(finderName.getSymbolName().startsWith("count")) {
            typedQueryType = new JavaType("Long");
        }

        final QueryHolder queryHolder = queryHolders.get(finderName);
        final String jpaQuery = queryHolder.getJpaQuery();
        final List<JavaType> parameterTypes = queryHolder.getParameterTypes();
        final List<JavaSymbolName> parameterNames = queryHolder
                .getParameterNames();
        
        
        // Now we have parameters types, we can scan by name
        // AND with parameters types
        // We do not scan the superclass, as the caller is expected to know
        // we'll only scan the current class
    	List<JavaType> parameterTypes4Test = new ArrayList<JavaType>(parameterTypes);
    	if(!finderName.getSymbolName().startsWith("count") && sorted) {
    		parameterTypes4Test.add(STRING);
    		parameterTypes4Test.add(STRING);
    	}
    	final MethodMetadata userMethod = MemberFindingUtils.getDeclaredMethod(governorTypeDetails,
    			finderName, parameterTypes4Test);
         if (userMethod != null) {
        	 return new MethodMetadataBuilder(userMethod);
         }
         

        // We declared the field in this ITD, so produce a public accessor for
        // it
        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        final String methodName = finderName.getSymbolName();
        boolean containsCollectionType = false;

        for (int i = 0; i < parameterTypes.size(); i++) {
            final String name = parameterNames.get(i).getSymbolName();

            final StringBuilder length = new StringBuilder();
            if (parameterTypes.get(i).equals(STRING)) {
                length.append(" || ").append(parameterNames.get(i))
                        .append(".length() == 0");
            }

            if (!parameterTypes.get(i).isPrimitive()) {
                bodyBuilder.appendFormalLine("if (" + name + " == null"
                        + length.toString()
                        + ") throw new IllegalArgumentException(\"The " + name
                        + " argument is required\");");
            }

            if (length.length() > 0
                    && methodName.substring(
                            methodName.indexOf(parameterNames.get(i)
                                    .getSymbolNameCapitalisedFirstLetter())
                                    + name.length()).startsWith("Like")) {
                bodyBuilder.appendFormalLine(name + " = " + name
                        + ".replace('*', '%');");
                bodyBuilder.appendFormalLine("if (" + name
                        + ".charAt(0) != '%') {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(name + " = \"%\" + " + name + ";");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                bodyBuilder.appendFormalLine("if (" + name + ".charAt(" + name
                        + ".length() - 1) != '%') {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine(name + " = " + name + " + \"%\";");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
            }

            if (parameterTypes.get(i).isCommonCollectionType()) {
                containsCollectionType = true;
            }
        }
        
        
        // Get the entityManager() method (as per ROO-216)
        bodyBuilder.appendFormalLine(ENTITY_MANAGER
                .getNameIncludingTypeParameters(false,
                        builder.getImportRegistrationResolver())
                + " em = "
                + destination.getSimpleTypeName()
                + "."
                + entityManagerMethod.getMethodName().getSymbolName() + "();");

        String typeNameIncludingTypeParameters = typedQueryType.getNameIncludingTypeParameters(false,
                builder.getImportRegistrationResolver());
        String typeName = destination.getSimpleTypeName();
        if(methodName.startsWith("count")) {
            final List<JavaType> parametersCount = new ArrayList<JavaType>();
            parameters.add(JavaType.LONG_OBJECT);
            JavaType typedQueryTypeCount = new JavaType(
                    TYPED_QUERY.getFullyQualifiedTypeName(), 0, DataType.TYPE,
                    null, parametersCount);
            typeNameIncludingTypeParameters = typedQueryTypeCount.getNameIncludingTypeParameters(false,
                    builder.getImportRegistrationResolver());
            typeName = "Long";
        }
        
        final List<JavaSymbolName> collectionTypeNames = new ArrayList<JavaSymbolName>();
        if (containsCollectionType) {
            bodyBuilder
                    .appendFormalLine("StringBuilder queryBuilder = new StringBuilder(\""
                            + jpaQuery + "\");");
            boolean jpaQueryComplete = false;
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (!jpaQueryComplete && !jpaQuery.trim().endsWith("WHERE")
                        && !jpaQuery.trim().endsWith("AND")
                        && !jpaQuery.trim().endsWith("OR")) {
                    bodyBuilder.appendFormalLine("queryBuilder.append(\""
                            + (methodName.substring(
                                    methodName.toLowerCase().indexOf(
                                            parameterNames.get(i)
                                                    .getSymbolName()
                                                    .toLowerCase())
                                            + parameterNames.get(i)
                                                    .getSymbolName().length())
                                    .startsWith("And") ? " AND" : " OR")
                            + "\");");
                    jpaQueryComplete = true;
                }
                if (parameterTypes.get(i).isCommonCollectionType()) {
                    collectionTypeNames.add(parameterNames.get(i));
                }
            }
            int position = 0;
            for (final JavaSymbolName name : collectionTypeNames) {
                bodyBuilder.appendFormalLine("for (int i = 0; i < " + name
                        + ".size(); i++) {");
                bodyBuilder.indent();
                bodyBuilder
                        .appendFormalLine("if (i > 0) queryBuilder.append(\" AND\");");
                bodyBuilder.appendFormalLine("queryBuilder.append(\" :" + name
                        + "_item\").append(i).append(\" MEMBER OF o." + name
                        + "\");");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                if (collectionTypeNames.size() > ++position) {
                    bodyBuilder.appendFormalLine("queryBuilder.append(\""
                            + (methodName.substring(
                                    methodName.toLowerCase().indexOf(
                                            name.getSymbolName().toLowerCase())
                                            + name.getSymbolName().length())
                                    .startsWith("And") ? " AND" : " OR")
                            + "\");");
                }
            }
                    
            // sorting part
            if(!methodName.startsWith("count") && sorted) {
                bodyBuilder.appendFormalLine("if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine("queryBuilder.append(\" ORDER BY \").append(sortFieldName);");
                bodyBuilder.appendFormalLine("if (\"ASC\".equalsIgnoreCase(sortOrder) || \"DESC\".equalsIgnoreCase(sortOrder)) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine("queryBuilder.append(\" \" + sortOrder);");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
            }
            
            bodyBuilder.appendFormalLine(typeNameIncludingTypeParameters
                    + " q = em.createQuery(queryBuilder.toString(), "
                    + typeName + ".class);");

            for (int i = 0; i < parameterTypes.size(); i++) {
                if (parameterTypes.get(i).isCommonCollectionType()) {
                    bodyBuilder.appendFormalLine("int " + parameterNames.get(i)
                            + "Index = 0;");
                    bodyBuilder.appendFormalLine("for ("
                            + parameterTypes.get(i).getParameters().get(0)
                                    .getSimpleTypeName()
                            + " _"
                            + parameterTypes.get(i).getParameters().get(0)
                                    .getSimpleTypeName().toLowerCase() + ": "
                            + parameterNames.get(i) + ") {");
                    bodyBuilder.indent();
                    bodyBuilder.appendFormalLine("q.setParameter(\""
                            + parameterNames.get(i)
                            + "_item\" + "
                            + parameterNames.get(i)
                            + "Index++, _"
                            + parameterTypes.get(i).getParameters().get(0)
                                    .getSimpleTypeName().toLowerCase() + ");");
                    bodyBuilder.indentRemove();
                    bodyBuilder.appendFormalLine("}");
                }
                else {
                    bodyBuilder.appendFormalLine("q.setParameter(\""
                            + parameterNames.get(i) + "\", "
                            + parameterNames.get(i) + ");");
                }
            }
        }
        else {        
            // sorting part
            if(!methodName.startsWith("count") && sorted) {
                bodyBuilder.appendFormalLine("String jpaQuery = \"" + jpaQuery + "\";");
                bodyBuilder.appendFormalLine("if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine("jpaQuery = jpaQuery + \" ORDER BY \" + sortFieldName;");
                bodyBuilder.appendFormalLine("if (\"ASC\".equalsIgnoreCase(sortOrder) || \"DESC\".equalsIgnoreCase(sortOrder)) {");
                bodyBuilder.indent();
                bodyBuilder.appendFormalLine("jpaQuery = jpaQuery + \" \" + sortOrder;");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                bodyBuilder.indentRemove();
                bodyBuilder.appendFormalLine("}");
                bodyBuilder.appendFormalLine(typeNameIncludingTypeParameters
                        + " q = em.createQuery(jpaQuery, "
                        + typeName + ".class);");
            } else {
                bodyBuilder.appendFormalLine(typeNameIncludingTypeParameters
                    + " q = em.createQuery(\""
                    + jpaQuery
                    + "\", "
                    + typeName + ".class);");
            }
            
            for (final JavaSymbolName name : parameterNames) {
                bodyBuilder.appendFormalLine("q.setParameter(\"" + name
                        + "\", " + name + ");");
            }
        }

        if(methodName.startsWith("count")) {
            bodyBuilder.appendFormalLine("return ((Long) q.getSingleResult());");
        } else {
            bodyBuilder.appendFormalLine("return q;");
        }
        
    	List<JavaType> methodParameterTypes = new ArrayList<JavaType>(parameterTypes);
    	List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>(parameterNames);
    	
        // sort parameters : sortFieldName & sortOrder
        if(!methodName.startsWith("count")  && sorted) {
        	methodParameterTypes.add(STRING);
        	methodParameterTypes.add(STRING);
        	methodParameterNames.add(new JavaSymbolName("sortFieldName"));
        	methodParameterNames.add(new JavaSymbolName("sortOrder"));
        }

        return new MethodMetadataBuilder(getId(), Modifier.PUBLIC
                | Modifier.STATIC, finderName, typedQueryType,
                AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes),
                methodParameterNames, bodyBuilder);
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
