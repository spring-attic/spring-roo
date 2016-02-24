package org.springframework.roo.shell;

import java.lang.reflect.Method;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A method that can be executed via a shell command.
 * <p>
 * Immutable since 1.2.0.
 * 
 * @author Ben Alex
 */
public class MethodTarget {

    private final String key;
    private final Method method;
    private final String remainingBuffer;
    private final Object target;

    /**
     * Constructor for a <code>null remainingBuffer</code> and <code>key</code>
     * 
     * @param method the method to invoke (required)
     * @param target the object on which the method is to be invoked (required)
     * @since 1.2.0
     */
    public MethodTarget(final Method method, final Object target) {
        this(method, target, null, null);
    }

    /**
     * Constructor that allows all fields to be set
     * 
     * @param method the method to invoke (required)
     * @param target the object on which the method is to be invoked (required)
     * @param remainingBuffer can be blank
     * @param key can be blank
     * @since 1.2.0
     */
    public MethodTarget(final Method method, final Object target,
            final String remainingBuffer, final String key) {
        Validate.notNull(method, "Method is required");
        Validate.notNull(target, "Target is required");
        this.key = StringUtils.stripToEmpty(key);
        this.method = method;
        this.remainingBuffer = StringUtils.stripToEmpty(remainingBuffer);
        this.target = target;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MethodTarget)) {
            return false;
        }
        final MethodTarget otherMethodTarget = (MethodTarget) other;
        return method.equals(otherMethodTarget.getMethod())
                && target.equals(otherMethodTarget.getTarget());
    }

    /**
     * @since 1.2.0
     */
    public String getKey() {
        return key;
    }

    /**
     * @return a non-<code>null</code> method
     * @since 1.2.0
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @since 1.2.0
     */
    public String getRemainingBuffer() {
        return remainingBuffer;
    }

    /**
     * @return a non-<code>null</code> Object
     * @since 1.2.0
     */
    public Object getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCodeMulti(method, target);
    }

    @Override
    public final String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("target", target);
        builder.append("method", method);
        builder.append("remainingBuffer", remainingBuffer);
        builder.append("key", key);
        return builder.toString();
    }
}
