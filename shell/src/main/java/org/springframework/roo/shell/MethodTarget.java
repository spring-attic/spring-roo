package org.springframework.roo.shell;

import java.lang.reflect.Method;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Immutable since 1.2.0.
 *
 * @author Ben Alex
 */
public class MethodTarget implements Comparable<MethodTarget> {

	// Fields
	private final Method method;
	private final Object target;
	private final String remainingBuffer;
	private final String key;

	/**
	 * Constructor
	 *
	 * @param method the method (required)
	 * @param target the object on which the method is to be invoked (required)
	 * @param remainingBuffer can be <code>null</code>
	 * @param key can be <code>null</code>
	 * @since 1.2.0
	 */
	public MethodTarget(final Method method, final Object target, final String remainingBuffer, final String key) {
		Assert.notNull(method, "Method is required");
		Assert.notNull(target, "Target is required");
		this.key = key;
		this.method = method;
		this.remainingBuffer = remainingBuffer;
		this.target = target;
	}

	@Override
	public boolean equals(final Object other) {
		return other == this || (other instanceof MethodTarget && compareTo((MethodTarget) other) == 0);
	}

	public int compareTo(final MethodTarget other) {
		if (other == null) {
			throw new NullPointerException();
		}
		if (this == other) {
			return 0;
		}
		return this.remainingBuffer.compareTo(other.remainingBuffer);
	}

	@Override
	public final String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("target", target);
		tsc.append("method", method);
		tsc.append("remainingBuffer", remainingBuffer);
		tsc.append("key", key);
		return tsc.toString();
	}

	/**
	 * @since 1.2.0
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * @return a non-<code>null</code> method
	 * @since 1.2.0
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * @since 1.2.0
	 */
	public String getRemainingBuffer() {
		return this.remainingBuffer;
	}

	/**
	 * @return a non-<code>null</code> Object
	 * @since 1.2.0
	 */
	public Object getTarget() {
		return this.target;
	}
}
