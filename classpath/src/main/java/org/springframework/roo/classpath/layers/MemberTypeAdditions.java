package org.springframework.roo.classpath.layers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * The required additions to a given type in order to invoke a given application
 * layer method, e.g. <code>findAll()</code>. Instances are immutable.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MemberTypeAdditions {

    /**
     * Builds the code snippet for a method call with the given properties
     * 
     * @param targetName the name of the object or class on which the method is
     *            being invoked (if not blank, must be a valid Java name)
     * @param methodName the name of the method being invoked (must be a valid
     *            Java name)
     * @param parameterNames the names of any parameters passed to the method
     * @return a non-blank Java snippet
     */
    static String buildMethodCall(final String targetName,
            final String methodName,
            final Collection<MethodParameter> parameters) {
        JavaSymbolName.assertJavaNameLegal(methodName);
        final StringBuilder methodCall = new StringBuilder();
        if (StringUtils.isNotBlank(targetName)) {
            JavaSymbolName.assertJavaNameLegal(targetName);
            methodCall.append(targetName);
            methodCall.append(".");
        }
        methodCall.append(methodName);
        methodCall.append("(");
        for (final Iterator<MethodParameter> iter = parameters.iterator(); iter
                .hasNext();) {
            final MethodParameter parameter = iter.next();
            methodCall.append(parameter.getValue());
            if (iter.hasNext()) {
                methodCall.append(", ");
            }
        }
        methodCall.append(")");
        return methodCall.toString();
    }

    /**
     * Factory method that builds the method call from the given target, method,
     * and list of parameter names.
     * 
     * @param builder stores any changes the caller should make in order to make
     *            the given method call, e.g. the field that is the method
     *            target (required)
     * @param targetName the name of the object or class on which the method is
     *            being invoked (if not blank, must be a valid Java name)
     * @param methodName the name of the method being invoked (must be a valid
     *            Java name)
     * @param isStatic whether the invoked method is static
     * @param parameterNames the names of any parameters passed to the method
     *            (required)
     */
    public static MemberTypeAdditions getInstance(
            final ClassOrInterfaceTypeDetailsBuilder builder,
            final String targetName, final String methodName,
            final boolean isStatic, final List<MethodParameter> parameters) {
        return new MemberTypeAdditions(builder, methodName, buildMethodCall(
                targetName, methodName, parameters), isStatic, parameters);
    }

    /**
     * Factory method that builds the method call from the given target, method,
     * and array of parameter names.
     * 
     * @param builder stores any changes the caller should make in order to make
     *            the given method call, e.g. the field that is the method
     *            target (required)
     * @param targetName the name of the object or class on which the method is
     *            being invoked (if not blank, must be a valid Java name)
     * @param methodName the name of the method being invoked (must be a valid
     *            Java name)
     * @param isStatic whether the invoked method is static
     * @param parameterNames the names of any parameters passed to the method
     *            (required)
     */
    public static MemberTypeAdditions getInstance(
            final ClassOrInterfaceTypeDetailsBuilder builder,
            final String targetName, final String methodName,
            final boolean isStatic, final MethodParameter... parameters) {
        return getInstance(builder, targetName, methodName, isStatic,
                Arrays.asList(parameters));
    }

    private final ClassOrInterfaceTypeDetailsBuilder classOrInterfaceDetailsBuilder;
    private final boolean isStatic;

    private final String methodCall;

    private final String methodName;

    private final List<MethodParameter> methodParameters;

    /**
     * Constructor that takes a pre-built method call.
     * 
     * @param builder stores any changes the caller should make in order to make
     *            the given method call, e.g. the field that is the method
     *            target; can be <code>null</code> if the caller requires no
     *            changes other than the given method call
     * @param methodName the bare name of the method being invoked (required)
     * @param methodCall a valid Java snippet that calls the method, including
     *            any required target and parameters, for example "foo.bar(baz)"
     *            (required)
     * @param isStatic whether the invoked method is static
     * @param methodParameters the parameters taken by the invoked method (can
     *            be <code>null</code>)
     */
    public MemberTypeAdditions(
            final ClassOrInterfaceTypeDetailsBuilder builder,
            final String methodName, final String methodCall,
            final boolean isStatic, final List<MethodParameter> methodParameters) {
        Validate.notBlank(methodName, "Invalid method name '" + methodName
                + "'");
        Validate.notBlank(methodCall, "Invalid method signature '" + methodCall
                + "'");
        classOrInterfaceDetailsBuilder = builder;
        this.methodCall = methodCall;
        this.methodName = methodName;
        this.methodParameters = new ArrayList<MethodParameter>();
        CollectionUtils.populate(this.methodParameters, methodParameters);
        this.isStatic = isStatic;
    }

    /**
     * Copies this instance's additions (if any) into the given builder
     * 
     * @param targetBuilder the ITD builder to receive the additions (required)
     * @param governorClassOrInterfaceTypeDetails the
     *            {@link ClassOrInterfaceTypeDetails} of the governor (required)
     */
    public void copyAdditionsTo(
            final AbstractMemberHoldingTypeDetailsBuilder<?> targetBuilder,
            final ClassOrInterfaceTypeDetails governorClassOrInterfaceTypeDetails) {
        if (classOrInterfaceDetailsBuilder != null) {
            classOrInterfaceDetailsBuilder.copyTo(targetBuilder,
                    governorClassOrInterfaceTypeDetails);
        }
    }

    /**
     * Returns the field on which this method is invoked
     * 
     * @return <code>null</code> if it's a static method call
     * @throws IllegalStateException if there's more than one field in the
     *             builder
     */
    public FieldMetadata getInvokedField() {
        if (classOrInterfaceDetailsBuilder == null) {
            return null;
        }
        final List<FieldMetadataBuilder> declaredFields = classOrInterfaceDetailsBuilder
                .getDeclaredFields();
        switch (declaredFields.size()) {
        case 0:
            return null;
        case 1:
            return declaredFields.get(0).build();
        default:
            throw new IllegalStateException("Multiple fields introduced for "
                    + this);
        }
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
     * Returns the bare name of the invoked method
     * 
     * @return a non-blank name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the parameters taken by the invoked method
     * 
     * @return a non-<code>null</code> copy of this list
     */
    public List<MethodParameter> getMethodParameters() {
        return new ArrayList<MethodParameter>(methodParameters);
    }

    /**
     * Indicates whether this is a static method call
     * 
     * @return see above
     */
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("classOrInterfaceDetailsBuilder",
                classOrInterfaceDetailsBuilder);
        builder.append("methodName", methodName);
        builder.append("methodCall", methodCall);
        return builder.toString();
    }
}
