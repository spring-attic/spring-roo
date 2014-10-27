package org.springframework.roo.addon.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.model.JavaType;

public enum Permission {
	// The names of these enum constants are arbitrary; calling code refers to
    // these methods by their String key.

    COUNT(CustomDataKeys.COUNT_ALL_METHOD) {
    	@Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sIsAllowed", getBaseName(), plural); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.COUNT_ALL_PERMISSION;
        }
    },

    DELETE(CustomDataKeys.REMOVE_METHOD) {
    	@Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sIsAllowed", getBaseName(), entityType.getSimpleTypeName()); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.DELETE_PERMISSION;
        }
    },

    FIND(CustomDataKeys.FIND_METHOD) {
    	@Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sIsAllowed", getBaseName(), entityType.getSimpleTypeName()); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.FIND_PERMISSION;
        }
    },

    FIND_ALL(CustomDataKeys.FIND_ALL_METHOD) {
    	@Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sIsAllowed", getBaseName(), plural); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.FIND_ALL_PERMISSION;
        }
    },

    FIND_ENTRIES(CustomDataKeys.FIND_ENTRIES_METHOD) {
        @Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sEntriesIsAllowed", getBaseName(), entityType.getSimpleTypeName()); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.FIND_ENTRIES_PERMISSION;
        }
    },

    SAVE(CustomDataKeys.PERSIST_METHOD) {
    	@Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sIsAllowed", getBaseName(), entityType.getSimpleTypeName()); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.SAVE_PERMISSION;
        }
    },

    UPDATE(CustomDataKeys.MERGE_METHOD) {
        @Override
        public String getName(final JavaType entityType, final String plural) {
            if (StringUtils.isNotBlank(getBaseName())) {
                return String.format("%s%sIsAllowed", getBaseName(), entityType.getSimpleTypeName()); 
            }
            return null;
        }

        @Override
        public String getBaseName() {
            return RooPermissionEvaluator.UPDATE_PERMISSION;
        }
    };

    private final MethodMetadataCustomDataKey key;
    
    public MethodMetadataCustomDataKey getKey() {
    	return key;
    }

    /**
     * Constructor
     * 
     * @param key the internal key for this method (required)
     */
    private Permission(final MethodMetadataCustomDataKey key) {
        Validate.notNull(key, "Method key is required");
        this.key = key;
    }

    public abstract String getBaseName();

    /**
     * Returns the name of this method, based on the given inputs
     * 
     * @param annotationValues the values of the {@link RooService} annotation
     *            on the service
     * @param entityType the type of domain entity managed by the service
     * @param plural the plural form of the entity
     * @return <code>null</code> if the method is not implemented
     */
    public abstract String getName(JavaType entityType, String plural);
}
