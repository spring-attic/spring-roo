package org.springframework.roo.project.layers;

import java.beans.Introspector;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.support.util.StringUtils;

/**
 * Common tasks performed by Layer related add-ons
 * 
 * @author Stefan Schmidt
 * @since 1.2
 *
 */
public abstract class LayerUtils {

	/**
	 * Obtain a Web save Java Symbol name for a given domain type
	 * @param domainType
	 * @return
	 */
	public static JavaSymbolName getTypeName(JavaType domainType) {
		String entityNameString = Introspector.decapitalize(StringUtils.capitalize(domainType.getSimpleTypeName()));
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(entityNameString)) {
			entityNameString = "_" + entityNameString;
		}
		return new JavaSymbolName(entityNameString);
	}
}
