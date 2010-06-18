package org.springframework.roo.addon.dbre.db;

import java.util.Set;

import org.springframework.roo.model.JavaPackage;

/**
 * Specifies methods to retrieve database metadata and to serialize and deserialize the metadata to and from an XML file.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbModel {

	String getDbMetadata();

	String getDbMetadata(IdentifiableTable identifiableTable);
	
	JavaPackage getJavaPackage();
	
	void setJavaPackage(JavaPackage javaPackage);

	void serialize();

	void deserialize();

	Set<Table> getTables();
	
	Table getTable(IdentifiableTable identifiableTable);
}
