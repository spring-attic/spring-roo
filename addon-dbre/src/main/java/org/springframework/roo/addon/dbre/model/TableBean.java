package org.springframework.roo.addon.dbre.model;

/**
 * Holder for table.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class TableBean {
	private String tableName;
	private String schemaName;

	public TableBean(String tableName, String schemaName) {
		this.tableName = tableName;
		this.schemaName = schemaName;
	}

	public String getName() {
		return tableName;
	}

	public String getSchemaName() {
		return schemaName;
	}
}
