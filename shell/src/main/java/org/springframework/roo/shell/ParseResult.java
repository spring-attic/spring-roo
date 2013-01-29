package org.springframework.roo.shell;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Immutable representation of the outcome of parsing a given shell line.
 * <p>
 * Note that contained objects (the instance and the arguments) may be mutable,
 * as the shell infrastructure has no way of restricting which methods can be
 * the target of CLI commands and nor the arguments they will accept via the
 * {@link Converter} infrastructure.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ParseResult {

    private final Object[] arguments; // May be null if no arguments needed
    private final Object instance;
    private final Method method;

    public ParseResult(final Method method, final Object instance,
            final Object[] arguments) {
        Validate.notNull(method, "Method required");
        Validate.notNull(instance, "Instance required");
        final int length = arguments == null ? 0 : arguments.length;
        Validate.isTrue(method.getParameterTypes().length == length,
                "Required %d arguments, but received %d",
                method.getParameterTypes().length, length);
        this.method = method;
        this.instance = instance;
        this.arguments = arguments;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ParseResult other = (ParseResult) obj;
        if (!Arrays.equals(arguments, other.arguments)) {
            return false;
        }
        if (instance == null) {
            if (other.instance != null) {
                return false;
            }
        }
        else if (!instance.equals(other.instance)) {
            return false;
        }
        if (method == null) {
            if (other.method != null) {
                return false;
            }
        }
        else if (!method.equals(other.method)) {
            return false;
        }
        return true;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Object getInstance() {
        return instance;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(arguments);
        result = prime * result + (instance == null ? 0 : instance.hashCode());
        result = prime * result + (method == null ? 0 : method.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("method", method);
        builder.append("instance", instance);
        builder.append("arguments", StringUtils.join(arguments, ","));
        return builder.toString();
    }
}
