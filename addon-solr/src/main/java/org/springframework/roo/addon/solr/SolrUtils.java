package org.springframework.roo.addon.solr;

import org.springframework.roo.model.JavaType;

/**
 * Utils class for solr addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public abstract class SolrUtils {
	
	public static String getSolrDynamicFieldPostFix(JavaType type) {
		if (type.equals(JavaType.INT_OBJECT) || type.equals(JavaType.INT_PRIMITIVE)) {
			return "_i";
		} else if (type.equals(JavaType.STRING_OBJECT)) {
			return "_s";
		} else if (type.equals(JavaType.LONG_OBJECT) || type.equals(JavaType.LONG_PRIMITIVE)) {
			return "_l";
		} else if (type.equals(JavaType.BOOLEAN_OBJECT) || type.equals(JavaType.BOOLEAN_PRIMITIVE)) {
			return "_b";
		} else if (type.equals(JavaType.FLOAT_OBJECT) || type.equals(JavaType.FLOAT_PRIMITIVE)) {
			return "_f";
		} else if (type.equals(JavaType.DOUBLE_OBJECT) || type.equals(JavaType.DOUBLE_PRIMITIVE)) {
			return "_d";
		} else if (type.equals(new JavaType("java.util.Date")) || type.equals(new JavaType("java.util.Calendar"))) {
			return "_dt";
		} else {
			return "_t";
		}
	}
}
