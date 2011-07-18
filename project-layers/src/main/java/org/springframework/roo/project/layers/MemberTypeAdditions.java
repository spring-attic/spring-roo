package org.springframework.roo.project.layers;

import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * The required additions to a given type in order to invoke a given
 * application layer method, e.g. <code>findAll()</code>.
 * 
 * Instances are immutable.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
public class MemberTypeAdditions {
	
	/**
	 * Builds the code snippet for a method call with the given properties
	 * 
	 * @param targetName the name of the object or class on which the method is
	 * being invoked (if not blank, must be a valid Java name)
	 * @param methodName the name of the method being invoked (must be a valid
	 * Java name)
	 * @param parameterNames the names of any parameters passed to the method
	 * @return a non-blank Java snippet
	 */
	static String buildMethodCall(final String targetName, final String methodName, final JavaSymbolName... parameterNames) {
		JavaSymbolName.assertJavaNameLegal(methodName);
		final StringBuilder methodCall = new StringBuilder();
		if (StringUtils.hasText(targetName)) {
			JavaSymbolName.assertJavaNameLegal(targetName);
			methodCall.append(targetName);
			methodCall.append(".");
		}
		methodCall.append(methodName);
		methodCall.append("(");
		for (int i = 0; i < parameterNames.length; i++) {
			if (i > 0) {
				methodCall.append(", ");
			}
			methodCall.append(parameterNames[i].getSymbolName());
		}
		methodCall.append(")");
		return methodCall.toString();
	}
	
	// Fields
	private final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceDetailsBuilder;
	private final String methodName;
	private final String methodSignature;
	
	/**
	 * Constructor
	 *
	 * @param methodSignature the snippet of Java code that invokes the method
	 * in question, for example "<code>personService.findAll()</code>" (required)	 * 
	 *
	 * @param classOrInterfaceTypeDetailsBuilder (required)
	 * @param targetName the name of the object or class on which the method is
	 * being invoked (if not blank, must be a valid Java name)
	 * @param methodName the name of the method being invoked (must be a valid
	 * Java name)
	 * @param parameterNames the names of any parameters passed to the method
	 */
	public MemberTypeAdditions(final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder, final String targetName, final String methodName, final JavaSymbolName... parameterNames) {
		Assert.hasText(methodName, "Invalid method name '" + methodName + "'");
		Assert.notNull(classOrInterfaceTypeDetailsBuilder, "Class or member details builder required");
		this.classOrInterfaceDetailsBuilder = classOrInterfaceTypeDetailsBuilder;
		this.methodName = methodName;
		this.methodSignature = buildMethodCall(targetName, methodName, parameterNames);
	}

	/**
	 * Returns the snippet of Java code that invokes the method in question,
	 * for example "<code>personService.findAll()</code>".
	 * 
	 * @return a non-blank String
	 */
	public String getMethodSignature() {
		return methodSignature;
	}
	
	/**
	 * Returns the bare name of the invoked method
	 * 
	 * @return a non-blank name
	 */
	public String getMethodName() {
		return methodName;
	}
	
	/**
	 * Copies this instance's additions into the given builder
	 * 
	 * @param targetBuilder the ITD builder to receive the additions (required)
	 */
	public void copyAdditionsTo(final AbstractMemberHoldingTypeDetailsBuilder<?> targetBuilder) {
		this.classOrInterfaceDetailsBuilder.copyTo(targetBuilder);
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("memberHoldingTypeDetails", classOrInterfaceDetailsBuilder);
		tsc.append("methodSignature", methodSignature);
		return tsc.toString();
	}
}
