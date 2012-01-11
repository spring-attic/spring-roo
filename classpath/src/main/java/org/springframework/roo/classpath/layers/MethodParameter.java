package org.springframework.roo.classpath.layers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.Pair;

/**
 * A parameter being passed to a layer method
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MethodParameter extends Pair<JavaType, JavaSymbolName> {

    /**
     * Converts the given list of pairs to a list of {@link MethodParameter}s
     * 
     * @param parameters the pairs to convert (can be <code>null</code>)
     * @return
     */
    public static List<MethodParameter> asList(
            final List<Pair<JavaType, JavaSymbolName>> parameters) {
        final List<MethodParameter> list = new ArrayList<MethodParameter>();
        if (parameters != null) {
            for (final Pair<JavaType, JavaSymbolName> parameter : parameters) {
                list.add(new MethodParameter(parameter.getKey(), parameter
                        .getValue()));
            }
        }
        return list;
    }

    /**
     * Constructor
     * 
     * @param type the parameter's type (required)
     * @param name the parameter's name (required)
     */
    public MethodParameter(final JavaType type, final JavaSymbolName name) {
        super(type, name);
        Assert.notNull(type, "Parameter type is required");
        Assert.notNull(name, "Parameter name is required");
    }

    /**
     * Constructor
     * 
     * @param type the parameter's type (required)
     * @param name the parameter's name (required)
     */
    public MethodParameter(final JavaType type, final String name) {
        this(type, new JavaSymbolName(name));
    }
}
