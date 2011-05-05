package org.springframework.roo.addon.entity;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

/**
 * Represents a parsed {@link RooEntity} annotation.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public class EntityAnnotationValues extends AbstractAnnotationValues {
	// From annotation
	@AutoPopulate private JavaType identifierType;
	@AutoPopulate private String identifierField = "";
	@AutoPopulate private String identifierColumn = "";
	@AutoPopulate private JavaType versionType = JavaType.INT_OBJECT;
	@AutoPopulate private String versionField = "version";
	@AutoPopulate private String versionColumn = "version";
	@AutoPopulate private String persistMethod = "persist";
	@AutoPopulate private String flushMethod = "flush";
	@AutoPopulate private String clearMethod = "clear";
	@AutoPopulate private String mergeMethod = "merge";
	@AutoPopulate private String removeMethod = "remove";
	@AutoPopulate private String countMethod = "count";
	@AutoPopulate private String findAllMethod = "findAll";
	@AutoPopulate private String findMethod = "find";
	@AutoPopulate private String findEntriesMethod = "find";
	@AutoPopulate private String[] finders;
	@AutoPopulate private String persistenceUnit = "";
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
		return versionColumn;
	}

	public String getPersistMethod() {
		return persistMethod;
	}

	public String getFlushMethod() {
		return flushMethod;
	}

	public String getClearMethod() {
		return clearMethod;
	}

	public String getMergeMethod() {
		return mergeMethod;
	}

	public String getRemoveMethod() {
		return removeMethod;
	}

	public String getCountMethod() {
		return countMethod;
	}

	public String getFindAllMethod() {
		return findAllMethod;
	}

	public String getFindMethod() {
		return findMethod;
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
