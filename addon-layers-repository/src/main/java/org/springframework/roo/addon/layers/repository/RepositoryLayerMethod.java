package org.springframework.roo.addon.layers.repository;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.support.util.Assert;

/**
 * A method provided by the {@link LayerType#REPOSITORY} layer.
 *
 * @author Andrew Swan
 * @since 1.2
 */
public enum RepositoryLayerMethod {
	
	/* 
	    A Spring Data JPA repository provides the following methods out of the box:
			long        count()
			void        delete(ID)
			void        delete(Iterable<? extends T>)
			void        delete(T)
			void        deleteAll()
			void        deleteInBatch(Iterable<T>)
			boolean     exists(ID)
			List<T>     findAll()
			org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable)
			List<T>     findAll(org.springframework.data.domain.Sort)
			T           findOne(ID)
			void        flush()
	        List<T>     save(Iterable<? extends T>)
			T           save(T)
			T           saveAndFlush(T)
	 */
	
	COUNT ("count", COUNT_ALL_METHOD) {

		@Override
		public String getCall(final List<JavaSymbolName> parameterNames) {
			return "count()";
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}
	},
	
	DELETE ("delete", REMOVE_METHOD) {
		
		@Override
		public String getCall(final List<JavaSymbolName> parameterNames) {
			return "delete(" + parameterNames.get(0).getSymbolName() + ")";
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Arrays.asList(targetEntity);
		}
	},
	
	FIND_ALL ("findAll", FIND_ALL_METHOD) {
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Collections.emptyList();
		}

		@Override
		public String getCall(final List<JavaSymbolName> parameterNames) {
			return "findAll()";
		}
	},

	/**
	 * Finds entities starting from a given zero-based index, up to a given
	 * maximum number of results. This method isn't directly implemented by
	 * Spring Data JPA, so we use its findAll(Pageable) API.
	 */
	FIND_ENTRIES ("findEntries", FIND_ENTRIES_METHOD) {

		@Override
		public String getCall(final List<JavaSymbolName> parameterNames) {
			final JavaSymbolName firstResultParameter = parameterNames.get(0);
			final JavaSymbolName maxResultsParameter = parameterNames.get(1);
			final String pageNumberExpression = firstResultParameter + " / " + maxResultsParameter;
			return "findAll(new org.springframework.data.domain.PageRequest(" + pageNumberExpression + ", " + maxResultsParameter + ")).getContent()";
		}

		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Arrays.asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE);
		}
	},
	
	/**
	 * Spring Data JPA makes no distinction between create/persist/save/update/merge
	 */
	SAVE ("save", MERGE_METHOD, PERSIST_METHOD) {
		
		@Override
		public String getCall(final List<JavaSymbolName> parameterNames) {
			return "save(" + parameterNames.get(0).getSymbolName() + ")";
		}
		
		@Override
		protected List<JavaType> getParameterTypes(final JavaType targetEntity) {
			return Arrays.asList(targetEntity);
		}
	};
	
	/**
	 * Returns the {@link RepositoryLayerMethod} with the given ID and parameter
	 * types.
	 * 
	 * @param methodId the ID to match upon
	 * @param parameterTypes the parameter types to match upon
	 * @param targetEntity the entity type being managed by the repository
	 * @return <code>null</code> if no such method exists
	 */
	public static RepositoryLayerMethod valueOf(final String methodId, final List<JavaType> parameterTypes, final JavaType targetEntity) {
		for (final RepositoryLayerMethod method : values()) {
			if (method.ids.contains(methodId) && method.getParameterTypes(targetEntity).equals(parameterTypes)) {
				return method;
			}
		}
		return null;
	}
	
	// Fields
	private final List<String> ids;
	private final String name;
	
	/**
	 * Constructor
	 *
	 * @param key the unique key for this method (required)
	 * @param name the Java name of this method (required)
	 */
	private RepositoryLayerMethod(final String name, final MethodMetadataCustomDataKey... keys) {
		Assert.hasText(name, "Name is required");
		Assert.isTrue(keys.length > 0, "One or more ids are required");
		this.ids = new ArrayList<String>();
		for (final MethodMetadataCustomDataKey key : keys) {
			this.ids.add(key.name());
		}
		this.name = name;
	}
	
	/**
	 * Returns a Java snippet that invokes this method
	 * 
	 * @param parameterNames the parameter names used by the caller; can be
	 * <code>null</code>
	 * @return a non-blank Java snippet
	 */
	public abstract String getCall(List<JavaSymbolName> parameterNames);
	
	/**
	 * Returns the name of this method
	 * 
	 * @return a non-blank name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Instances must return the types of parameters they take
	 * 
	 * @param targetEntity the type of entity being managed (required)
	 * @return a non-<code>null</code> list
	 */
	protected abstract List<JavaType> getParameterTypes(JavaType targetEntity);
}
