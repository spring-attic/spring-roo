package org.springframework.roo.addon.dbre.model;

/**
 * SQL table types.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum TableType {
    ALIAS, SYNONYM, TABLE, UNKNOWN, VIEW;

    public static TableType getTableType(final String typeName) {
        try {
            return TableType.valueOf(typeName);
        }
        catch (final IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
