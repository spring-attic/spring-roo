package org.springframework.roo.addon.dbre;

import java.util.Set;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Matches tables with java types on disk and also computes new types based on table names.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 */
public interface TableModelService {

	/**
	 * Locates the type associated with the presented table name.
	 * 
	 * @param tableNamePattern the table to locate (required)
	 * @param javaPackage the Java package to use for the type.
	 * @return the type (if known) or null (if not found).
	 */
	JavaType findTypeForTableName(String tableNamePattern, JavaPackage javaPackage);

	/**
	 * Returns a table name given the JavaType.
	 * 
	 * <p>
	 * Rules are applied in the conversion such as converting capital letters into underscores. For example, a table name of abstract_person would be returned as a type called AbstractPerson.
	 * 
	 * @param type to convert to a table name.
	 * @return the table name.
	 */
	String suggestTableNameForNewType(JavaType type);

	/**
	 * Returns a JavaType given a table identity.
	 * 
	 * @param the table name to convert
	 * @param javaPackage the Java package to use for the type.
	 * @return a new JavaType
	 */
	JavaType suggestTypeNameForNewTable(String tableNamePattern, JavaPackage javaPackage);

	/**
	 * Returns a field name for a given database column name;
	 * 
	 * @param columnName the name of the column.
	 * @return a String representing the column.
	 */
	String suggestFieldNameForColumn(String columnName);

	/**
	 * Returns all {@link RooDbManaged} entities.
	 * 
	 * @return An unmodifiable {@link Set} of all database-managed entities
	 */
	Set<JavaType> getDatabaseManagedEntities();
}
