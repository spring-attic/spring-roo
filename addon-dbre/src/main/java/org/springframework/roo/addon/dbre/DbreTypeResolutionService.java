package org.springframework.roo.addon.dbre;

import java.util.Set;
import java.util.SortedSet;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Matches tables with java types on disk and also computes new types based on table names.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreTypeResolutionService {

	/**
	 * Locates the type associated with the presented table name.
	 * 
	 * @param managedEntities a set of database-managed entities to search.
	 * @param tableNamePattern the table to locate (required).
	 * @param javaPackage the Java package to use for the type.
	 * @return the type (if known) or null (if not found).
	 */
	JavaType findTypeForTableName(SortedSet<JavaType> managedEntities, String tableNamePattern, JavaPackage javaPackage);

	/**
	 * Locates the type associated with the presented table name.
	 * 
	 * @param tableNamePattern the table to locate (required).
	 * @param javaPackage the Java package to use for the type.
	 * @return the type (if known) or null (if not found).
	 */
	JavaType findTypeForTableName(String tableNamePattern, JavaPackage javaPackage);

	/**
	 * Returns a JavaType given a table identity.
	 * 
	 * @param tableNamePattern the table name to convert
	 * @param javaPackage the Java package to use for the type.
	 * @return a new JavaType
	 */
	JavaType suggestTypeNameForNewTable(String tableNamePattern, JavaPackage javaPackage);

	/**
	 * Returns a field name for a given database table or column name;
	 * 
	 * @param name the name of the table or column.
	 * @return a String representing the table or column.
	 */
	String suggestFieldName(String name);
	
	/**
	 * Returns the table name from either the @RooEntity annotation or @Table annotation
	 * @param javaType the type to search
	 * @return the name of the table. Can be null;
	 */
	String findTableName(JavaType javaType);

	/**
	 * Returns all {@link RooDbManaged} {@link RooEntity entities}.
	 * 
	 * @return An unmodifiable {@link Set} of all database-managed entities.
	 */
	SortedSet<JavaType> getManagedEntityTypes();

	/**
	 * Returns all {@link RooIdentifier identifiers} with the dbManaged attribute value equal to true.
	 * 
	 * @return An unmodifiable {@link Set} of database-managed identifiers.
	 */
	SortedSet<JavaType> getManagedIdentifierTypes();
}
