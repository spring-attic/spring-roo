package org.springframework.roo.addon.entity;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * Represents a parsed {@link RooEntity} annotation.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public class EntityAnnotationValues extends AbstractAnnotationValues {
	public static final String PERSIST_METHOD_DEFAULT = "persist";
	public static final String VERSION_FIELD_DEFAULT = "version";
	public static final String VERSION_COLUMN_DEFAULT = "version";
	public static final String FLUSH_METHOD_DEFAULT = "flush";
	public static final String CLEAR_METHOD_DEFAULT = "clear";
	public static final String MERGE_METHOD_DEFAULT = "merge";
	public static final String REMOVE_METHOD_DEFAULT = "remove";
	public static final String COUNT_METHOD_DEFAULT = "count";
	public static final String FIND_ALL_METHOD_DEFAULT = "findAll";
	public static final String FIND_METHOD_DEFAULT = "find";

	// From annotation
	@AutoPopulate private JavaType identifierType;
	@AutoPopulate private String identifierField = "";
	@AutoPopulate private String identifierColumn = "";
	@AutoPopulate private JavaType versionType = JavaType.INT_OBJECT;
	@AutoPopulate private String versionField = VERSION_FIELD_DEFAULT;
	@AutoPopulate private String versionColumn = VERSION_COLUMN_DEFAULT;
	@AutoPopulate private String persistMethod = PERSIST_METHOD_DEFAULT;
	@AutoPopulate private String flushMethod = FLUSH_METHOD_DEFAULT;
	@AutoPopulate private String clearMethod = CLEAR_METHOD_DEFAULT;
	@AutoPopulate private String mergeMethod = MERGE_METHOD_DEFAULT;
	@AutoPopulate private String removeMethod = REMOVE_METHOD_DEFAULT;
	@AutoPopulate private String countMethod = COUNT_METHOD_DEFAULT;
	@AutoPopulate private String findAllMethod = FIND_ALL_METHOD_DEFAULT;
	@AutoPopulate private String findMethod = FIND_METHOD_DEFAULT;
	@AutoPopulate private String findEntriesMethod = "find";
	@AutoPopulate private String[] finders;
	@AutoPopulate private String persistenceUnit = "";	
	@AutoPopulate private String transactionManager = "";
	@AutoPopulate private boolean mappedSuperclass = false;
	@AutoPopulate private String table = "";
	@AutoPopulate private String schema = "";
	@AutoPopulate private String catalog = "";
	@AutoPopulate private String inheritanceType = "";
	@AutoPopulate private String entityName = "";

	public EntityAnnotationValues(PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(governorPhysicalTypeMetadata, new JavaType(RooEntity.class.getName()));
		AutoPopulationUtils.populate(this, annotationMetadata);
	}

	public JavaType getIdentifierType() {
		return identifierType;
	}

	public String getIdentifierField() {
		return identifierField;
	}

	public String getIdentifierColumn() {
		return identifierColumn;
	}

	public JavaType getVersionType() {
		return versionType;
	}

	public String getVersionField() {
		return versionField;
	}

	public String getVersionColumn() {
		return StringUtils.hasText(versionColumn) ? versionColumn : VERSION_COLUMN_DEFAULT;
	}

	public String getPersistMethod() {
		return StringUtils.hasText(persistMethod) ? persistMethod : PERSIST_METHOD_DEFAULT;
	}

	public String getFlushMethod() {
		return StringUtils.hasText(flushMethod) ? flushMethod : FLUSH_METHOD_DEFAULT;
	}

	public String getClearMethod() {
		return StringUtils.hasText(clearMethod) ? clearMethod : CLEAR_METHOD_DEFAULT;
	}

	public String getMergeMethod() {
		return StringUtils.hasText(mergeMethod) ? mergeMethod : MERGE_METHOD_DEFAULT;
	}

	public String getRemoveMethod() {
		return StringUtils.hasText(removeMethod) ? removeMethod : REMOVE_METHOD_DEFAULT;
	}

	public String getCountMethod() {
		return StringUtils.hasText(countMethod) ? countMethod : COUNT_METHOD_DEFAULT;
	}

	public String getFindAllMethod() {
		return StringUtils.hasText(findAllMethod) ? findAllMethod : FIND_ALL_METHOD_DEFAULT;
	}

	public String getFindMethod() {
		return StringUtils.hasText(findMethod) ? findMethod : FIND_METHOD_DEFAULT;
	}

	public String getFindEntriesMethod() {
		return findEntriesMethod;
	}

	public String[] getFinders() {
		return finders;
	}

	public String getPersistenceUnit() {
		return persistenceUnit;
	}

	public String getTransactionManager() {
		return transactionManager;
	}

	public boolean isMappedSuperclass() {
		return mappedSuperclass;
	}

	public String getTable() {
		return table;
	}

	public String getSchema() {
		return schema;
	}

	public String getCatalog() {
		return catalog;
	}

	public String getInheritanceType() {
		return inheritanceType;
	}

	public String getEntityName() {
		return entityName;
	}
}
