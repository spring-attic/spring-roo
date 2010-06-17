package org.springframework.roo.addon.dbre;

import java.util.Map;

import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link TableModelServiceImpl}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 */
public interface TableModelService {

	/**
	 * Locates the table identity for the presented type.
	 * 
	 * @param type to locate (required)
	 * @return the table name (if known) or null (if not found)
	 */
	IdentifiableTable findTableIdentity(JavaType type);

	/**
	 * Locates the type associated with the presented table name.
	 * 
	 * @param identifiableTable to locate (required)
	 * @return the type (if known) or null (if not found)
	 */
	JavaType findTypeForTableIdentity(IdentifiableTable identifiableTable);
	
	/**
	 * Returns all entity types and their table names.
	 * 
	 * @return An unmodifiable Map of all entities
	 */
	Map<IdentifiableTable, JavaType> getAllDetectedEntities();

	
	/**
	 * Returns a table identity given the JavaType.
	 * 
	 * <p>
	 * Rules are applied in the conversion such as converting capital letters
	 * into underscores. For example, a table name of abstract_person would
	 * be returned as a type called AbstractPerson.
	 * 
	 * @param type to convert to a table identity
	 * @return a new IdentifiableTable
	 */
	IdentifiableTable suggestTableNameForNewType(JavaType type);
	
	/**
	 * Returns a JavaType given a table identity.
	 *  
	 * @param identifiableTable to convert
	 * @param javaPackage the Java package to use for the type.
	 * @return a new JavaType
	 */
	JavaType suggestTypeNameForNewTable(IdentifiableTable identifiableTable, JavaPackage javaPackage);

	JavaSymbolName suggestFieldNameForColumn(String columnName);
	
	/**
	 * Displays all entity types and their table names.
	 * 
	 * @return a String.
	 */
	String dump();
}