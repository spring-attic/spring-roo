package org.springframework.roo.addon.layers.repository.jpa;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FLUSH_METHOD;
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
 * <p>
 * A Spring Data JPA repository provides the following methods out of the box,
 * of which those marked * are not implemented below:
 * 
 * <pre>
 *     long         count()
 *     *void        delete(ID)
 *     *void        delete(Iterable<? extends T>)
 *     void         delete(T)
 *     *void        deleteAll()
 *     *void        deleteInBatch(Iterable<T>)
 *     *boolean     exists(ID)
 *     List<T>      findAll()
 *     *org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable)
 *     *List<T>     findAll(org.springframework.data.domain.Sort)
 *     T            findOne(ID)
 *     void         flush()
 *     *List<T>     save(Iterable<? extends T>)
 *     T            save(T)
 *     *T           saveAndFlush(T)
 * </pre>
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public enum RepositoryJpaLayerMethod {

    COUNT("count", COUNT_ALL_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "count()";
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
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
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }
    },

    FIND("find", FIND_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "findOne(" + parameters.get(0).getValue() + ")";
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType entityType,
                final JavaType idType) {
            return Arrays.asList(idType);
        }
    },

    FIND_ALL("findAll", FIND_ALL_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "findAll()";
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
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
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays
                    .asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
        }
    },

    FLUSH("flush", FLUSH_METHOD) {

        @Override
        public String getCall(final List<MethodParameter> parameters) {
            return "flush()";
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            // Even though Spring Data JPA's flush() method doesn't take a
            // parameter, the caller provides one, so we list it here.
            return Arrays.asList(targetEntity);
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
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }
    };

    /**
     * Returns the {@link RepositoryJpaLayerMethod} with the given ID and
     * parameter types.
     * 
     * @param methodId the ID to match upon
     * @param parameterTypes the parameter types to match upon
     * @param targetEntity the entity type being managed by the repository
     * @param idType specifies the ID type used by the target entity (required)
     * @return <code>null</code> if no such method exists
     */
    public static RepositoryJpaLayerMethod valueOf(final String methodId,
            final List<JavaType> parameterTypes, final JavaType targetEntity,
            final JavaType idType) {
        for (final RepositoryJpaLayerMethod method : values()) {
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
    private RepositoryJpaLayerMethod(final String name,
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
     * Instances must return the types of parameters they take
     * 
     * @param targetEntity the type of entity being managed (required)
     * @param idType specifies the ID type used by the target entity (required)
     * @return a non-<code>null</code> list
     */
    protected abstract List<JavaType> getParameterTypes(JavaType targetEntity,
            JavaType idType);
}
