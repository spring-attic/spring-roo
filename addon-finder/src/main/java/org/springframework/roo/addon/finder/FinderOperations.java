package org.springframework.roo.addon.finder;

import java.util.SortedSet;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Provides Finder add-on operations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface FinderOperations {

    void installFinder(JavaType typeName, JavaSymbolName finderName);

    boolean isFinderInstallationPossible();

    SortedSet<String> listFindersFor(JavaType typeName, Integer depth);
}