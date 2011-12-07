package org.springframework.roo.classpath.layers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
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
 * @since 1.2.0
 */
public class MemberTypeAdditions {

	// Fields
	private final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceDetailsBuilder;
	private final String methodName;
	private final String methodCall;

	/**
	 * Factory method that builds the method call for you from the given target,
	 * method, and array of parameter names.
	 *
	 * @param builder stores any changes the caller should make in order to make
	 * the given method call, e.g. the field that is the method target (required)
	 * @param targetName the name of the object or class on which the method is
	 * being invoked (if not blank, must be a valid Java name)
	 * @param methodName the name of the method being invoked (must be a valid
	 * Java name)
	 * @param parameterNames the names of any parameters passed to the method
	 * (required)
	 */
	public static MemberTypeAdditions getInstance(final ClassOrInterfaceTypeDetailsBuilder builder, final String targetName, final String methodName, final JavaSymbolName... parameterNames) {
		return getInstance(builder, targetName, methodName, Arrays.asList(parameterNames));
	}

	/**
	 * Factory method that builds the method call for you from the given target,
	 * method, and list of parameter names.
	 *
	 * @param builder stores any changes the caller should make in order to make
	 * the given method call, e.g. the field that is the method target (required)
	 * @param targetName the name of the object or class on which the method is
	 * being invoked (if not blank, must be a valid Java name)
	 * @param methodName the name of the method being invoked (must be a valid
	 * Java name)
	 * @param parameterNames the names of any parameters passed to the method
	 * (required)
	 */
	public static MemberTypeAdditions getInstance(final ClassOrInterfaceTypeDetailsBuilder builder, final String targetName, final String methodName, final List<JavaSymbolName> parameterNames) {
		return new MemberTypeAdditions(builder, methodName, buildMethodCall(targetName, methodName, parameterNames.iterator()));
	}

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
	static String buildMethodCall(final String targetName, final String methodName, final Iterator<JavaSymbolName> parameterNames) {
		JavaSymbolName.assertJavaNameLegal(methodName);
		final StringBuilder methodCall = new StringBuilder();
		if (StringUtils.hasText(targetName)) {
			JavaSymbolName.assertJavaNameLegal(targetName);
			methodCall.append(targetName);
			methodCall.append(".");
		}
		methodCall.append(methodName);
		methodCall.append("(");
		while (parameterNames.hasNext()) {
			methodCall.append(parameterNames.next().getSymbolName());
			if (parameterNames.hasNext()) {
				methodCall.append(", ");
			}
		}
		methodCall.append(")");
		return methodCall.toString();
	}

	/**
	 * Constructor that accepts a pre-built method call
	 *
	 * @param builder stores any changes the caller should make in order to make
	 * the given method call, e.g. the field that is the method target; can be
	 * <code>null</code> if the caller requires no changes other than the given
	 * method call
	 * @param methodName the bare name of the method being invoked (required)
	 * @param methodCall a valid Java snippet that calls the method,
	 * including any required target and parameters, for example "foo.bar(baz)"
	 * (required)
	 */
	public MemberTypeAdditions(final ClassOrInterfaceTypeDetailsBuilder builder, final String methodName, final String methodCall) {
		Assert.hasText(methodName, "Invalid method name '" + methodName + "'");
		Assert.hasText(methodCall, "Invalid method signature '" + methodCall + "'");
		this.classOrInterfaceDetailsBuilder = builder;
		this.methodName = methodName;
		this.methodCall = methodCall;
	}

	/**
	 * Copies this instance's additions (if any) into the given builder
	 *
	 * @param targetBuilder the ITD builder to receive the additions (required)
	 * @param governorClassOrInterfaceTypeDetails the {@link ClassOrInterfaceTypeDetails} of the governor (required)
	 */
	public void copyAdditionsTo(final AbstractMemberHoldingTypeDetailsBuilder<?> targetBuilder, final ClassOrInterfaceTypeDetails governorClassOrInterfaceTypeDetails) {
		if (this.classOrInterfaceDetailsBuilder != null) {
			this.classOrInterfaceDetailsBuilder.copyTo(targetBuilder, governorClassOrInterfaceTypeDetails);
		}
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
	 * Returns the snippet of Java code that calls the method in question, for
	 * example "<code>personService.findAll()</code>".
	 *
	 * @return a non-blank String
	 */
	public String getMethodCall() {
		return methodCall;
	}
	
	/**
	 * Returns the field on which this method is invoked
	 * 
	 * @return <code>null</code> if it's a static method call
	 * @throws IllegalStateException if there's more than one field in the builder
	 */
	public FieldMetadata getInvokedField() {
		if (classOrInterfaceDetailsBuilder == null) {
			return null;
		}
		final List<FieldMetadataBuilder> declaredFields = classOrInterfaceDetailsBuilder.getDeclaredFields();
		switch (declaredFields.size()) {
			case 0:
				return null;
			case 1:
				return declaredFields.get(0).build();
			default:
				throw new IllegalStateException("Multiple fields introduced for " + this);
		}
	}
	
	/**
	 * Indicates whether this is a static method call
	 * 
	 * @return see above
	 */
	public boolean isStatic() {
		return getInvokedField() == null;
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("classOrInterfaceDetailsBuilder", classOrInterfaceDetailsBuilder);
		tsc.append("methodName", methodName);
		tsc.append("methodCall", methodCall);
		return tsc.toString();
	}
}
