package org.springframework.roo.addon.layers.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.PairList;

/**
 * A method provided by a user project's service layer
 * 
 * @author Andrew Swan
 * @author Stefan Schmidt
 * @since 1.2.0
 */
enum ServiceLayerMethod {

    // The names of these enum constants are arbitrary; calling code refers to
    // these methods by their String key.

    COUNT(CustomDataKeys.COUNT_ALL_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getCountAllMethod())) {
                return annotationValues.getCountAllMethod() + plural;
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.LONG_PRIMITIVE;
        }
    },

    DELETE(CustomDataKeys.REMOVE_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getDeleteMethod())) {
                return annotationValues.getDeleteMethod()
                        + entityType.getSimpleTypeName();
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(JavaSymbolName
                    .getReservedWordSafeName(entityType));
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays.asList(entityType);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.VOID_PRIMITIVE;
        }
    },

    FIND(CustomDataKeys.FIND_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFindMethod())) {
                return annotationValues.getFindMethod()
                        + entityType.getSimpleTypeName();
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(new JavaSymbolName("id"));
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays.asList(idType);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return entityType;
        }
    },

    FIND_ALL(CustomDataKeys.FIND_ALL_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFindAllMethod())) {
                return annotationValues.getFindAllMethod() + plural;
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.listOf(entityType);
        }
    },

    FIND_ENTRIES(CustomDataKeys.FIND_ENTRIES_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFindEntriesMethod())) {
                return annotationValues.getFindEntriesMethod()
                        + entityType.getSimpleTypeName() + "Entries";
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(new JavaSymbolName("firstResult"),
                    new JavaSymbolName("maxResults"));
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays
                    .asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.listOf(entityType);
        }
    },

    SAVE(CustomDataKeys.PERSIST_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getSaveMethod())) {
                return annotationValues.getSaveMethod()
                        + entityType.getSimpleTypeName();
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(JavaSymbolName
                    .getReservedWordSafeName(entityType));
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays.asList(entityType);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.VOID_PRIMITIVE;
        }
    },

    UPDATE(CustomDataKeys.MERGE_METHOD) {
        @Override
        public String getName(final ServiceAnnotationValues annotationValues,
                final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getUpdateMethod())) {
                return annotationValues.getUpdateMethod()
                        + entityType.getSimpleTypeName();
            }
            return null;
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(JavaSymbolName
                    .getReservedWordSafeName(entityType));
        }

        @Override
        public List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays.asList(entityType);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return entityType;
        }
    };

    /**
     * Returns the {@link ServiceLayerMethod} with the given properties, if any
     * 
     * @param methodIdentifier the internal ID of the method (can be blank)
     * @param callerParameters the types of parameter to be passed to the method
     *            (required)
     * @param targetEntity the type of entity being managed (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @return <code>null</code> if a blank or unknown ID is given
     */
    public static ServiceLayerMethod valueOf(final String methodIdentifier,
            final List<JavaType> callerParameters, final JavaType targetEntity,
            final JavaType idType) {
        // Look for matching method name and parameter types
        for (final ServiceLayerMethod method : values()) {
            if (method.getKey().equals(methodIdentifier)
                    && method.getParameterTypes(targetEntity, idType).equals(
                            callerParameters)) {
                return method;
            }
        }
        return null;
    }

    private final MethodMetadataCustomDataKey key;

    /**
     * Constructor
     * 
     * @param key the internal key for this method (required)
     */
    private ServiceLayerMethod(final MethodMetadataCustomDataKey key) {
        Validate.notNull(key, "Method key is required");
        this.key = key;
    }

    /**
     * Returns the line(s) of Java code that implement this method
     * 
     * @param lowerLayerAdditions the details of a call to a lower layer, if any
     * @return a non-blank string
     */
    public String getBody(final MemberTypeAdditions lowerLayerAdditions) {
        if (lowerLayerAdditions == null) {
            // No lower layer implements this method; so we stub it
            return "throw new UnsupportedOperationException(\"Implement me!\");";
        }
        // A lower layer implements it; generate a delegation call
        String line = "";
        if (!isVoid()) {
            line = "return ";
        }
        line += lowerLayerAdditions.getMethodCall() + ";";
        return line;
    }

    /**
     * Returns the key identifying this method
     * 
     * @return a non-blank string that's unique within this enum
     */
    public String getKey() {
        return key.name();
    }

    /**
     * Returns the name of this method, based on the given inputs
     * 
     * @param annotationValues the values of the {@link RooService} annotation
     *            on the service
     * @param entityType the type of domain entity managed by the service
     * @param plural the plural form of the entity
     * @return <code>null</code> if the method is not implemented
     */
    public abstract String getName(ServiceAnnotationValues annotationValues,
            JavaType entityType, String plural);

    /**
     * Returns the names of this method's declared parameters
     * 
     * @param entityType the type of domain entity managed by the service
     *            (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @return a non-<code>null</code> list (might be empty)
     */
    public abstract List<JavaSymbolName> getParameterNames(JavaType entityType,
            JavaType idType);

    /**
     * Returns the types and names of the parameters declared by this method for
     * the given domain type
     * 
     * @param domainType the domain type to which the method applies (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @return a non-<code>null</code> list
     */
    public PairList<JavaType, JavaSymbolName> getParameters(
            final JavaType domainType, final JavaType idType) {
        return new PairList<JavaType, JavaSymbolName>(getParameterTypes(
                domainType, idType), getParameterNames(domainType, idType));
    }

    /**
     * Returns the types of parameters taken by this method
     * 
     * @param entityType the type of entity to which this method applies
     *            (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @return a non-<code>null</code> copy of list (might be empty)
     */
    public abstract List<JavaType> getParameterTypes(JavaType entityType,
            JavaType idType);

    /**
     * Returns this method's return type
     * 
     * @param entityType the type of entity being managed
     * @return a non-<code>null</code> type
     */
    public abstract JavaType getReturnType(JavaType entityType);

    /**
     * Returns the name of this method, based on the given inputs
     * 
     * @param annotationValues the values of the {@link RooService} annotation
     *            on the service
     * @param entityType the type of domain entity managed by the service
     * @param plural the plural form of the entity
     * @return <code>null</code> if the method is not implemented
     */
    public JavaSymbolName getSymbolName(
            final ServiceAnnotationValues annotationValues,
            final JavaType entityType, final String plural) {
        final String methodName = getName(annotationValues, entityType, plural);
        if (StringUtils.isNotBlank(methodName)) {
            return new JavaSymbolName(methodName);
        }
        return null;
    }

    /**
     * Indicates whether this method is void, i.e. returns nothing
     * 
     * @return see above
     */
    boolean isVoid() {
        return JavaType.VOID_PRIMITIVE.equals(getReturnType(null));
    }
}
