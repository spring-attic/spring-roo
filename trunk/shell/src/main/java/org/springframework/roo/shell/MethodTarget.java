package org.springframework.roo.shell;

import java.lang.reflect.Method;

import org.springframework.roo.support.style.ToStringCreator;

public class MethodTarget implements Comparable<MethodTarget> {
	public Object target;
	public Method method;
	public String remainingBuffer;
	public String key;
	
	public int compareTo(MethodTarget o) {
		if (o == null) {
			throw new NullPointerException();
		}
		if (this.equals(o)) {
			return 0;
		}
		return this.remainingBuffer.compareTo(o.remainingBuffer);
	}
	
	public final String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("target", target);
		tsc.append("method", method);
		tsc.append("remainingBuffer", remainingBuffer);
		tsc.append("key", key);
		return tsc.toString();
	}
	
}
