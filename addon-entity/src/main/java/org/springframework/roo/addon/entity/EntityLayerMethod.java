package org.springframework.roo.addon.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
 * @author Stefan Schmidt
 * @since 1.2
 */
enum EntityLayerMethod {
	
	// The names of these enum constants are arbitrary

	CLEAR (PersistenceCustomDataKeys.CLEAR_METHOD, true) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getClearMethod())) {
				return annotationValues.getClearMethod();
			}
			return null;
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Collections.emptyList();
		}
	},
	
	COUNT_ALL (PersistenceCustomDataKeys.COUNT_ALL_METHOD, true) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getCountMethod())) {
				return annotationValues.getCountMethod() + plural;
			}
			return null;
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Collections.emptyList();
		}
	},
	
	FIND (PersistenceCustomDataKeys.FIND_METHOD, true) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFindMethod())) {
				return annotationValues.getFindMethod() + targetEntity.getSimpleTypeName();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Arrays.asList(idType);
		}
	},
	
	FIND_ALL (PersistenceCustomDataKeys.FIND_ALL_METHOD, true) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFindAllMethod())) {
				return annotationValues.getFindAllMethod() + plural;
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Collections.emptyList();
		}
	},
	
	FIND_ENTRIES (PersistenceCustomDataKeys.FIND_ENTRIES_METHOD, true) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFindEntriesMethod())) {
				return annotationValues.getFindEntriesMethod() + targetEntity.getSimpleTypeName() + "Entries";
			}
			return null;
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Arrays.asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
		}
	},
	
	FLUSH (PersistenceCustomDataKeys.FLUSH_METHOD, false) {

		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getFlushMethod())) {
				return annotationValues.getFlushMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Arrays.asList(targetEntity);
		}
	},
	
	MERGE (PersistenceCustomDataKeys.MERGE_METHOD, false) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getMergeMethod())) {
				return annotationValues.getMergeMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Arrays.asList(targetEntity);
		}
	},
	
	PERSIST (PersistenceCustomDataKeys.PERSIST_METHOD, false) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getPersistMethod())) {
				return annotationValues.getPersistMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
			return Arrays.asList(targetEntity);
		}
	},
	
	REMOVE (PersistenceCustomDataKeys.REMOVE_METHOD, false) {
		
		@Override
		public String getName(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, final String plural) {
			if (StringUtils.hasText(annotationValues.getRemoveMethod())) {
				return annotationValues.getRemoveMethod();
			}
			return null;
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity, final JavaType idType) {
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
	public static EntityLayerMethod valueOf(final String methodIdentifier, final List<JavaType> callerParameters, final JavaType targetEntity, final JavaType idType) {
		// Look for matching method name and parameter types
		for (final EntityLayerMethod method : values()) {
			if (method.id.equals(methodIdentifier) && method.getParameterTypes(targetEntity, idType).equals(callerParameters)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Returns the type of parameters taken by this method
	 * 
	 * @param targetEntity the type of entity being managed
	 * @param idType specifies the ID type used by the target entity (required)
	 * @return a non-<code>null</code> list
	 */
	protected abstract List<JavaType> getParameterTypes(JavaType targetEntity, JavaType idType);

	// Fields
	private final boolean isStatic;
	private final String id;

	/**
	 * Constructor
	 *
	 * @param id a unique id for this method (required)
	 * @param isStatic whether this method is static
	 */
	private EntityLayerMethod(final MethodMetadataCustomDataKey key, final boolean isStatic) {
		Assert.notNull(key, "Key is required");
		this.id = key.name();
		this.isStatic = isStatic;
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
	public abstract String getName(JpaCrudAnnotationValues annotationValues, JavaType targetEntity, String plural);

	/**
	 * Returns the Java snippet that invokes this method, including the target
	 * if any
	 * 
	 * @param annotationValues the CRUD-related values of the {@link RooEntity}
	 * annotation
	 * on the entity type
	 * @param targetEntity the type of entity being managed (required)
	 * @param plural the plural form of the entity (required)
	 * @param parameterNames the caller's names for the method's parameters
	 * (required, must be modifiable)
	 * @return a non-blank Java snippet
	 */
	public String getCall(final JpaCrudAnnotationValues annotationValues, final JavaType targetEntity, String plural, final List<JavaSymbolName> parameterNames) {
		final String target;
		if (this.isStatic) {
			target = targetEntity.getSimpleTypeName();
		}
		else {
			target = parameterNames.get(0).getSymbolName();
			parameterNames.remove(0);
		}
		return getCall(target, getName(annotationValues, targetEntity, plural), parameterNames.iterator());
	}
	
	/**
	 * Generates a method call from the given inputs
	 * 
	 * @param targetName the name of the target on which the method is being invoked (required)
	 * @param methodName the name of the method being invoked (required)
	 * @param parameterNames the names of the parameters (from the caller's POV)
	 * @return a non-blank Java snippet ending in ")"
	 */
	private String getCall(final String targetName, final String methodName, final Iterator<JavaSymbolName> parameterNames) {
		final StringBuilder methodCall = new StringBuilder();
		methodCall.append(targetName);
		methodCall.append(".");
		methodCall.append(methodName);
		methodCall.append("(");
		while (parameterNames.hasNext()) {
			methodCall.append(parameterNames.next().getSymbolName());
			if (parameterNames.hasNext()) {
				methodCall.append(", ");
			}
		}
		methodCall.append(")");
		return methodCall.toString();
	}
	
	/**
	 * Indicates whether this method is static; only for use of unit tests, as
	 * it breaks encapsulation
	 * 
	 * @return
	 */
	boolean isStatic() {
		return isStatic;
	}
}
