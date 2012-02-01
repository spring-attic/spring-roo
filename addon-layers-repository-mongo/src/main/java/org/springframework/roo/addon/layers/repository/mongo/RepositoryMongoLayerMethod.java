package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * A method provided by the {@link LayerType#REPOSITORY} layer.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public enum RepositoryMongoLayerMethod {

    COUNT("count", COUNT_ALL_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "count()";
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.LONG_PRIMITIVE;
        }
    },

    /**
     * Deletes the passed-in entity (does not delete by ID).
     */
    DELETE("delete", REMOVE_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "delete(" + parameters.get(0).getValue() + ")";
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(JavaSymbolName
                    .getReservedWordSafeName(entityType));
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.VOID_PRIMITIVE;
        }
    },

    FIND("find", FIND_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "findOne(" + parameters.get(0).getValue() + ")";
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(new JavaSymbolName("id"));
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays.asList(idType);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return entityType;
        }
    },

    FIND_ALL("findAll", FIND_ALL_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "findAll()";
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.listOf(entityType);
        }
    },

    /**
     * Finds entities starting from a given zero-based index, up to a given
     * maximum number of results. This method isn't directly implemented by
     * Spring Data JPA, so we use its findAll(Pageable) API.
     */
    FIND_ENTRIES("findEntries", FIND_ENTRIES_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            final JavaSymbolName firstResultParameter = parameters.get(0)
                    .getValue();
            final JavaSymbolName maxResultsParameter = parameters.get(1)
                    .getValue();
            final String pageNumberExpression = firstResultParameter + " / "
                    + maxResultsParameter;
            return "findAll(new org.springframework.data.domain.PageRequest("
                    + pageNumberExpression + ", " + maxResultsParameter
                    + ")).getContent()";
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(new JavaSymbolName("firstResult"),
                    new JavaSymbolName("maxResults"));
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays
                    .asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.listOf(entityType);
        }
    },

    /**
     * Spring Data JPA makes no distinction between
     * create/persist/save/update/merge
     */
    SAVE("save", MERGE_METHOD, PERSIST_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "save(" + parameters.get(0).getValue() + ")";
        }

        @Override
        public List<JavaSymbolName> getParameterNames(
                final JavaType entityType, final JavaType idType) {
            return Arrays.asList(JavaSymbolName
                    .getReservedWordSafeName(entityType));
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }

        @Override
        public JavaType getReturnType(final JavaType entityType) {
            return JavaType.VOID_PRIMITIVE;
        }
    };

    /**
     * Returns the {@link RepositoryMongoLayerMethod} with the given ID and
     * parameter types.
     * 
     * @param methodId the ID to match upon
     * @param parameterTypes the parameter types to match upon
     * @param targetEntity the entity type being managed by the repository
     * @param idType specifies the ID type used by the target entity (required)
     * @return <code>null</code> if no such method exists
     */
    public static RepositoryMongoLayerMethod valueOf(final String methodId,
            final List<JavaType> parameterTypes, final JavaType targetEntity,
            final JavaType idType) {
        for (final RepositoryMongoLayerMethod method : values()) {
            if (method.ids.contains(methodId)
                    && method.getParameterTypes(targetEntity, idType).equals(
                            parameterTypes)) {
                return method;
            }
        }
        return null;
    }

    private final List<String> ids;
    private final String name;

    /**
     * Constructor
     * 
     * @param key the unique key for this method (required)
     * @param name the Java name of this method (required)
     */
    private RepositoryMongoLayerMethod(final String name,
            final MethodMetadataCustomDataKey... keys) {
        Validate.notBlank(name, "Name is required");
        Validate.isTrue(keys.length > 0, "One or more ids are required");
        ids = new ArrayList<String>();
        for (final MethodMetadataCustomDataKey key : keys) {
            ids.add(key.name());
        }
        this.name = name;
    }

    /**
     * Returns a Java snippet that invokes this method (minus the target)
     * 
     * @param parameters the parameters used by the caller; can be
     *            <code>null</code>
     * @return a non-blank Java snippet
     */
    public abstract String getCall(List<MethodParameter> parameters);

    /**
     * Returns the name of this method
     * 
     * @return a non-blank name
     */
    public String getName() {
        return name;
    }

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
     * Instances must return the types of parameters they take
     * 
     * @param targetEntity the type of entity being managed (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @return a non-<code>null</code> list
     */
    protected abstract List<JavaType> getParameterTypes(JavaType targetEntity,
            JavaType idType);

    /**
     * Returns this method's return type
     * 
     * @param entityType the type of entity being managed
     * @return a non-<code>null</code> type
     */
    public abstract JavaType getReturnType(JavaType entityType);
}
