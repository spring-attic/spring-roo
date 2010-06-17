package org.springframework.roo.addon.dbre.db;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Specifies methods to convert between Java types and database metadata attributes.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbMetadataConverter {

	IdentifiableTable convertTypeToTableType(JavaType type);
	
	JavaType convertTableIdentityToType(IdentifiableTable identifiableTable, JavaPackage javaPackage);
	
	String getFieldName(String columnName);
}
