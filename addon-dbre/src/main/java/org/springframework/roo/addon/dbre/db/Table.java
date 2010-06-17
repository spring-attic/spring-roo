package org.springframework.roo.addon.dbre.db;

import java.util.Set;

/**
 * An abstract table representation.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface Table {
	
	IdentifiableTable getIdentifiableTable();
		
	Set<Column> getColumns();
	
	Set<PrimaryKey> getPrimaryKeys();
	
	Set<ForeignKey> getForeignKeys();
	
	Set<Index> getIndexes();
}
