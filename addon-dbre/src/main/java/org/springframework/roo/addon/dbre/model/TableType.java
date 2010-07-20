package org.springframework.roo.addon.dbre.model;

/**
 * SQL table types.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum TableType {
	TABLE, VIEW, ALIAS, SYNONYM, UNKNOWN;

	public static TableType getTableType(String typeName) {
		try {
			return TableType.valueOf(typeName);
		} catch (IllegalArgumentException e) {
			return UNKNOWN;
		}
	}
}
