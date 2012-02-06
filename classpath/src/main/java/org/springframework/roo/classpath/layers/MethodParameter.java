package org.springframework.roo.classpath.layers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * A parameter being passed to a layer method.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MethodParameter extends MutablePair<JavaType, JavaSymbolName> {

    private static final long serialVersionUID = -3652851128787182692L;

    /**
     * Converts the given list of pairs to a list of {@link MethodParameter}s
     * 
     * @param parameters the pairs to convert (can be <code>null</code>)
     * @return
     */
    public static List<MethodParameter> asList(
            final List<MutablePair<JavaType, JavaSymbolName>> parameters) {
        final List<MethodParameter> list = new ArrayList<MethodParameter>();
        if (parameters != null) {
            for (final MutablePair<JavaType, JavaSymbolName> parameter : parameters) {
                list.add(new MethodParameter(parameter.getKey(), parameter
                        .getValue()));
            }
        }
        return list;
    }

    private final JavaType type;

    private final JavaSymbolName name;

    /**
     * Constructor.
     * 
     * @param type the parameter's type (required)
     * @param name the parameter's name (required)
     */
    public MethodParameter(final JavaType type, final JavaSymbolName name) {
        Validate.notNull(type, "Parameter type is required");
        Validate.notNull(name, "Parameter name is required");
        this.type = type;
        this.name = name;
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

    @Override
    public JavaType getLeft() {
        return type;
    }

    @Override
    public JavaSymbolName getRight() {
        return name;
    }

    @Override
    public void setLeft(final JavaType left) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRight(final JavaSymbolName right) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaSymbolName setValue(final JavaSymbolName value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("key: ").append(type.getFullyQualifiedTypeName());
        builder.append(", value: ").append(name.getSymbolName());
        return builder.toString();
    }
}
