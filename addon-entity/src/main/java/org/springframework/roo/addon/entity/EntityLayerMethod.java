package org.springframework.roo.addon.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Methods implemented by a user project entity.
 *
 * @author Andrew Swan
 * @since 1.2
 */
enum EntityLayerMethod {
	
	// The names of these enum constants are arbitrary

	CLEAR (PersistenceCustomDataKeys.CLEAR_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return targetEntity.getFullyQualifiedTypeName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// Static call
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getClearMethod())) {
				return annotationValues.getClearMethod();
			}
			return null;
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	COUNT_ALL (PersistenceCustomDataKeys.COUNT_ALL_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return targetEntity.getFullyQualifiedTypeName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// Static call
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getCountMethod())) {
				return annotationValues.getCountMethod() + plural;
			}
			return null;
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	FIND_ALL (PersistenceCustomDataKeys.FIND_ALL_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return targetEntity.getFullyQualifiedTypeName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// Static call
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFindAllMethod())) {
				return annotationValues.getFindAllMethod() + plural;
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	FIND_ENTRIES (PersistenceCustomDataKeys.FIND_ENTRIES_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			final String firstIndex = parameterNames.get(0).getSymbolName();
			final String maxResults = parameterNames.get(1).getSymbolName();
			return targetEntity.getFullyQualifiedTypeName() + "." + getName(annotationValues, targetEntity, plural) + "(" + firstIndex + ", " + maxResults + ")";	// static
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFindEntriesMethod())) {
				return annotationValues.getFindEntriesMethod() + targetEntity.getSimpleTypeName() + "Entries";
			}
			return null;
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Arrays.asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
		}
	},
	
	FLUSH (PersistenceCustomDataKeys.FLUSH_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return targetEntity.getFullyQualifiedTypeName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// static
		}

		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFlushMethod())) {
				return annotationValues.getFlushMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	MERGE (PersistenceCustomDataKeys.MERGE_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return parameterNames.get(0).getSymbolName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// Instance call
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getMergeMethod())) {
				return annotationValues.getMergeMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	PERSIST (PersistenceCustomDataKeys.PERSIST_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return parameterNames.get(0).getSymbolName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// Instance call
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getPersistMethod())) {
				return annotationValues.getPersistMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	REMOVE (PersistenceCustomDataKeys.REMOVE_METHOD) {
		
		@Override
		public String getCall(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural, final List<JavaSymbolName> parameterNames) {
			return parameterNames.get(0).getSymbolName() + "." + getName(annotationValues, targetEntity, plural) + "()";	// Instance call
		}
		
		@Override
		public String getName(final EntityAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getRemoveMethod())) {
				return annotationValues.getRemoveMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	};

	/**
	 * Returns the {@link EntityLayerMethod} with the given ID and parameter
	 * types
	 * 
	 * @param methodIdentifier the ID to seek; will not match if blank
	 * @param callerParameters will not match if <code>null</code>
	 * @param targetEntity
	 * @return
	 */
	public static EntityLayerMethod valueOf(final String methodIdentifier, final List<JavaType> callerParameters, final JavaType targetEntity) {
		// Look for matching method name and parameter types
		for (final EntityLayerMethod method : values()) {
			if (method.id.equals(methodIdentifier) && method.getParameterTypes(targetEntity).equals(callerParameters)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Returns the type of parameters taken by this method
	 * 
	 * @param targetEntity the type of entity being managed
	 * @return a non-<code>null</code> list
	 */
	protected abstract List<JavaType> getParameterTypes(JavaType targetEntity);

	// Fields
	private final String id;

	/**
	 * Constructor
	 *
	 * @param id a unique id for this method (required)
	 */
	private EntityLayerMethod(MethodMetadataCustomDataKey key) {
		Assert.notNull(key, "Key is required");
		this.id = key.name();
	}

	/**
	 * Returns the desired name of this method based on the given annotation
	 * values
	 * 
	 * @param annotationValues the values of the {@link RooEntity} annotation
	 * on the entity type
	 * @param targetEntity the entity type (required)
	 * @param plural the plural form of the entity (required)
	 * @return <code>null</code> if the method isn't desired for that entity
	 */
	public abstract String getName(EntityAnnotationValues annotationValues, JavaType targetEntity, String plural);

	/**
	 * Returns the Java snippet that invokes this method, including the target
	 * if any
	 * 
	 * @param annotationValues the values of the {@link RooEntity} annotation
	 * on the entity type
	 * @param targetEntity the type of entity being managed (required)
	 * @param plural the plural form of the entity (required)
	 * @param parameterNames the caller's names for the method's parameters
	 * (required)
	 * 
	 * @return a non-blank Java snippet
	 */
	public abstract String getCall(EntityAnnotationValues annotationValues, JavaType targetEntity, String plural, List<JavaSymbolName> parameterNames);
}
