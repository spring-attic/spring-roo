package org.springframework.roo.addon.dbre;

import java.util.Comparator;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Sorts DBRE-managed entities and identifiers by the {@link JavaPackage} they are stored in.
 * 
 * @author Alan Stewart.
 * @since 1.1
 */
public class DbreManagedTypesComparator implements Comparator<JavaType> {

	public int compare(JavaType o1, JavaType o2) {
		return o1.getPackage().getFullyQualifiedPackageName().compareTo(o2.getPackage().getFullyQualifiedPackageName());
	}
}
