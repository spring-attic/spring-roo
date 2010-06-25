package org.springframework.roo.addon.dbre.db;

import java.util.Set;
import java.util.SortedSet;

/**
 * An abstract table representation.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface Table {
	
	IdentifiableTable getIdentifiableTable();
		
	SortedSet<Column> getColumns();
		
	Set<ForeignKey> getForeignKeys();
	
	Set<Index> getIndexes();
}
