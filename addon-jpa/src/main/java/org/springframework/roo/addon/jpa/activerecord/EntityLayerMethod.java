package org.springframework.roo.addon.jpa.activerecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.model.JavaType;

/**
 * Methods implemented by a user project entity.
 * 
 * @author Andrew Swan
 * @author Stefan Schmidt
 * @since 1.2.0
 */
enum EntityLayerMethod {

    // The names of these enum constants are arbitrary

    CLEAR(CustomDataKeys.CLEAR_METHOD, true) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getClearMethod())) {
                return annotationValues.getClearMethod();
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
        }
    },

    COUNT_ALL(CustomDataKeys.COUNT_ALL_METHOD, true) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getCountMethod())) {
                return annotationValues.getCountMethod() + plural;
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
        }
    },

    FIND(CustomDataKeys.FIND_METHOD, true) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFindMethod())) {
                return annotationValues.getFindMethod()
                        + targetEntity.getSimpleTypeName();
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(idType);
        }
    },

    FIND_ALL(CustomDataKeys.FIND_ALL_METHOD, true) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFindAllMethod())) {
                return annotationValues.getFindAllMethod() + plural;
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Collections.emptyList();
        }
    },

    FIND_ENTRIES(CustomDataKeys.FIND_ENTRIES_METHOD, true) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFindEntriesMethod())) {
                return annotationValues.getFindEntriesMethod()
                        + targetEntity.getSimpleTypeName() + "Entries";
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays
                    .asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
        }
    },

    FLUSH(CustomDataKeys.FLUSH_METHOD, false) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getFlushMethod())) {
                return annotationValues.getFlushMethod();
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }
    },

    MERGE(CustomDataKeys.MERGE_METHOD, false) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getMergeMethod())) {
                return annotationValues.getMergeMethod();
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }
    },

    PERSIST(CustomDataKeys.PERSIST_METHOD, false) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getPersistMethod())) {
                return annotationValues.getPersistMethod();
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }
    },

    REMOVE(CustomDataKeys.REMOVE_METHOD, false) {
        @Override
        public String getName(final JpaCrudAnnotationValues annotationValues,
                final JavaType targetEntity, final String plural) {
            if (StringUtils.isNotBlank(annotationValues.getRemoveMethod())) {
                return annotationValues.getRemoveMethod();
            }
            return null;
        }

        @Override
        protected List<JavaType> getParameterTypes(final JavaType targetEntity,
                final JavaType idType) {
            return Arrays.asList(targetEntity);
        }
    };

    /**
     * Returns the {@link EntityLayerMethod} with the given ID and parameter
     * types
     * 
     * @param methodIdentifier the ID to seek; will not match if blank
     * @param callerParameters will not match if <code>null</code>
     * @param targetEntity
     * @param idType specifies the ID type used by the target entity (required)
     * @return
     */
    public static EntityLayerMethod valueOf(final String methodIdentifier,
            final List<JavaType> callerParameters, final JavaType targetEntity,
            final JavaType idType) {
        // Look for matching method name and parameter types
        for (final EntityLayerMethod method : values()) {
            if (method.id.equals(methodIdentifier)
                    && method.getParameterTypes(targetEntity, idType).equals(
                            callerParameters)) {
                return method;
            }
        }
        return null;
    }

    private final String id;

    private final boolean isStatic;

    /**
     * Constructor
     * 
     * @param id a unique id for this method (required)
     * @param isStatic whether this method is static
     */
    private EntityLayerMethod(final MethodMetadataCustomDataKey key,
            final boolean isStatic) {
        Validate.notNull(key, "Key is required");
        id = key.name();
        this.isStatic = isStatic;
    }

    /**
     * Returns the Java snippet that invokes this method, including the target
     * if any
     * 
     * @param annotationValues the CRUD-related values of the
     *            {@link RooJpaActiveRecord} annotation on the entity type
     * @param targetEntity the type of entity being managed (required)
     * @param plural the plural form of the entity (required)
     * @param callerParameters the caller's method's parameters (required)
     * @return a non-blank Java snippet
     */
    public String getCall(final JpaCrudAnnotationValues annotationValues,
            final JavaType targetEntity, final String plural,
            final List<MethodParameter> callerParameters) {
        final String target;
        if (isStatic) {
            target = targetEntity.getSimpleTypeName();
        }
        else {
            target = callerParameters.get(0).getValue().getSymbolName();
        }
        final List<MethodParameter> parameters = getParameters(callerParameters);
        return getCall(target, getName(annotationValues, targetEntity, plural),
                parameters.iterator());
    }

    /**
     * Generates a method call from the given inputs
     * 
     * @param targetName the name of the target on which the method is being
     *            invoked (required)
     * @param methodName the name of the method being invoked (required)
     * @param parameterNames the names of the parameters (from the caller's POV)
     * @return a non-blank Java snippet ending in ")"
     */
    private String getCall(final String targetName, final String methodName,
            final Iterator<MethodParameter> parameters) {
        final StringBuilder methodCall = new StringBuilder();
        methodCall.append(targetName);
        methodCall.append(".");
        methodCall.append(methodName);
        methodCall.append("(");
        while (parameters.hasNext()) {
            methodCall.append(parameters.next().getValue().getSymbolName());
            if (parameters.hasNext()) {
                methodCall.append(", ");
            }
        }
        methodCall.append(")");
        return methodCall.toString();
    }

    /**
     * Returns the desired name of this method based on the given annotation
     * values
     * 
     * @param annotationValues the values of the {@link RooJpaActiveRecord}
     *            annotation on the entity type
     * @param targetEntity the entity type (required)
     * @param plural the plural form of the entity (required)
     * @return <code>null</code> if the method isn't desired for that entity
     */
    public abstract String getName(JpaCrudAnnotationValues annotationValues,
            JavaType targetEntity, String plural);

    /**
     * Returns the parameters to be passed when this method is invoked
     * 
     * @param callerParameters the parameters provided by the caller (required)
     * @return a non-<code>null</code> List
     */
    public List<MethodParameter> getParameters(
            final Collection<MethodParameter> callerParameters) {
        final List<MethodParameter> parameters = new ArrayList<MethodParameter>(
                callerParameters);
        if (!isStatic) {
            parameters.remove(0); // the instance doesn't need itself as a
                                  // parameter
        }
        return parameters;
    }

    /**
     * Returns the type of parameters taken by this method
     * 
     * @param targetEntity the type of entity being managed
     * @param idType specifies the ID type used by the target entity (required)
     * @return a non-<code>null</code> list
     */
    protected abstract List<JavaType> getParameterTypes(JavaType targetEntity,
            JavaType idType);

    /**
     * Indicates whether this method is static
     * 
     * @return
     */
    public boolean isStatic() {
        return isStatic;
    }
}
