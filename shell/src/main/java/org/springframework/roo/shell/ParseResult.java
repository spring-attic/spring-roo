package org.springframework.roo.shell;

import java.lang.reflect.Method;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

public class ParseResult {
	private Method method;
	private Object instance;
	private Object[] arguments; // may be null if no arguments needed
	
	public ParseResult(Method method, Object instance, Object[] arguments) {
		Assert.notNull(method, "Method required");
		Assert.notNull(instance, "Instance required");
		int length = arguments == null ? 0 : arguments.length;
		Assert.isTrue(method.getParameterTypes().length == length, "Required " + method.getParameterTypes().length + " arguments, but received " + length);
		this.method = method;
		this.instance = instance;
		this.arguments = arguments;
	}

	public Method getMethod() {
		return method;
	}

	public Object getInstance() {
		return instance;
	}

	public Object[] getArguments() {
		return arguments;
	}
	
	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("method", method);
		tsc.append("instance", instance);
		tsc.append("arguments", StringUtils.arrayToCommaDelimitedString(arguments));
		return tsc.toString();
	}

}
