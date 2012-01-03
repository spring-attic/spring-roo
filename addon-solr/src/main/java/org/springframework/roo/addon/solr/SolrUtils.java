package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.DOUBLE_OBJECT;
import static org.springframework.roo.model.JavaType.DOUBLE_PRIMITIVE;
import static org.springframework.roo.model.JavaType.FLOAT_OBJECT;
import static org.springframework.roo.model.JavaType.FLOAT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JavaType.LONG_PRIMITIVE;
import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;

import org.springframework.roo.model.JavaType;

/**
 * Utils class for solr addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public final class SolrUtils {

    public static String getSolrDynamicFieldPostFix(final JavaType type) {
        if (type.equals(INT_OBJECT) || type.equals(INT_PRIMITIVE)) {
            return "_i";
        }
        else if (type.equals(JavaType.STRING)) {
            return "_s";
        }
        else if (type.equals(LONG_OBJECT) || type.equals(LONG_PRIMITIVE)) {
            return "_l";
        }
        else if (type.equals(BOOLEAN_OBJECT) || type.equals(BOOLEAN_PRIMITIVE)) {
            return "_b";
        }
        else if (type.equals(FLOAT_OBJECT) || type.equals(FLOAT_PRIMITIVE)) {
            return "_f";
        }
        else if (type.equals(DOUBLE_OBJECT) || type.equals(DOUBLE_PRIMITIVE)) {
            return "_d";
        }
        else if (type.equals(DATE) || type.equals(CALENDAR)) {
            return "_dt";
        }
        else {
            return "_t";
        }
    }

    private SolrUtils() {
    }
}
