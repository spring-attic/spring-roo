package org.springframework.roo.addon.finder;

import java.util.SortedSet;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Interface to {@link FinderOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface FinderOperations {

	boolean isFinderCommandAvailable();

	SortedSet<String> listFindersFor(JavaType typeName, Integer depth);

	void installFinder(JavaType typeName, JavaSymbolName finderName);
}