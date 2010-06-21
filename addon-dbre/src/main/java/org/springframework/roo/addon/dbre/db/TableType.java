package org.springframework.roo.addon.dbre.db;

/**
 * SQL table types.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum TableType {
	TABLE, VIEW;
	
	public static TableType getTableType(String typeName) {
		try {
			return TableType.valueOf(typeName);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
