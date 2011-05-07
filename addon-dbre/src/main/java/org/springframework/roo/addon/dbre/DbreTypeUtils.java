package org.springframework.roo.addon.dbre;

import java.util.Set;

import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides methods to find types based on table names and to suggest type and field 
 * names from table and column names respectively.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public abstract class DbreTypeUtils {

	/**
	 * Locates the type associated with the presented table name.
	 * 
	 * @param managedEntities a set of database-managed entities to search.
	 * @param tableNamePattern the table to locate (required).
	 * @return the type (if known) or null (if not found).
	 */
	public static JavaType findTypeForTableName(Set<ClassOrInterfaceTypeDetails> managedEntities, String tableNamePattern) {
		Assert.hasText(tableNamePattern, "Table name required");

		for (ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
			String tableName = getTableName(managedEntity);
			if (tableNamePattern.equals(tableName)) {
				return managedEntity.getName();
			}
		}

		return null;
	}

	/**
	 * Locates the type associated with the presented table.
	 * 
	 * @param managedEntities a set of database-managed entities to search.
	 * @param table the table to locate (required).
	 * @return the type (if known) or null (if not found).
	 */
	public static JavaType findTypeForTable(Set<ClassOrInterfaceTypeDetails> managedEntities, Table table) {
		Assert.notNull(table, "Table required");
		return findTypeForTableName(managedEntities, table.getName());
	}

	/**
	 * Locates the table name using the presented ClassOrInterfaceTypeDetails.
	 * 
	 * <p>
	 * The search for the table names starts on the @Table annotation and if not present, the
	 * {@link RooEntity @RooEntity} "table" attribute is checked. If not present on either, the method returns null.
	 * 
	 * @param classOrInterfaceTypeDetails the type to search.
	 * @return the table name (if known) or null (if not found).
	 */
	public static String getTableName(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		// Try to locate a table name, which can be specified either via the "name" attribute on
		// @Table, eg @Table(name = "foo") or via the "table" attribute on @RooEntity, eg @RooEntity(table = "foo")
		String tableName = null;

		AnnotationMetadata annotation = MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, new JavaType("javax.persistence.Table"));
		if (annotation != null) {
			AnnotationAttributeValue<?> nameAttribute = annotation.getAttribute(new JavaSymbolName("name"));
			if (nameAttribute != null) {
				tableName = (String) nameAttribute.getValue();
			}
		}

		if (!StringUtils.hasText(tableName)) {
			// The search continues...
			annotation = MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, new JavaType("org.springframework.roo.addon.entity.RooEntity"));
			if (annotation != null) {
				AnnotationAttributeValue<?> tableAttribute = annotation.getAttribute(new JavaSymbolName("table"));
				if (tableAttribute != null) {
					tableName = (String) tableAttribute.getValue();
				}
			}
		}

		return StringUtils.trimToNull(tableName);
	}

	/**
	 * Returns a JavaType given a table identity.
	 * 
	 * @param tableNamePattern the table name to convert
	 * @param javaPackage the Java package to use for the type.
	 * @return a new JavaType
	 */
	public static JavaType suggestTypeNameForNewTable(String tableNamePattern, JavaPackage javaPackage) {
		Assert.hasText(tableNamePattern, "Table name required");

		StringBuilder result = new StringBuilder();
		if (javaPackage != null && StringUtils.hasText(javaPackage.getFullyQualifiedPackageName())) {
			result.append(javaPackage.getFullyQualifiedPackageName());
			result.append(".");
		}
		result.append(getName(tableNamePattern, false));
		return new JavaType(result.toString());
	}

	/**
	 * Returns a field name for a given database table or column name;
	 * 
	 * @param name the name of the table or column.
	 * @return a String representing the table or column.
	 */
	public static String suggestFieldName(String name) {
		Assert.hasText(name, "Table or column name required");
		return getName(name, true);
	}

	/**
	 * Returns a field name for a given database table;
	 * 
	 * @param table the the table.
	 * @return a String representing the table or column.
	 */
	public static String suggestFieldName(Table table) {
		Assert.notNull(table, "Table required");
		return getName(table.getName(), true);
	}

	private static String getName(String str, boolean isField) {
		StringBuilder result = new StringBuilder();
		boolean isDelimChar = false;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (i == 0) {
				if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') {
					result.append(isField ? "f" : "T");
					result.append(c);
				} else {
					result.append(isField ? Character.toLowerCase(c) : Character.toUpperCase(c));
				}
				continue;
			} else if (i > 0 && (c == '_' || c == '-' || c == '\\' || c == '/') || c == '.') {
				isDelimChar = true;
				continue;
			}
			
			if (isDelimChar) {
				result.append(Character.toUpperCase(c));
				isDelimChar = false;
			} else {
				if (i > 1 && Character.isLowerCase(str.charAt(i - 1)) && Character.isUpperCase(c)) {
					result.append(c);
				} else {
					result.append(Character.toLowerCase(c));
				}
			}
		}
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(result.toString())) {
			result.append("1");
		}
		return result.toString();
	}
}
