package org.springframework.roo.layers;

import java.util.List;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class MemberTypeAdditions {
	
	private ClassOrInterfaceTypeDetailsBuilder classOrInterfaceDetailsBuilder;
	private List<String> methodCall;
	
	public MemberTypeAdditions(ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder, List<String> methodBody) {
		super();
		Assert.notNull(classOrInterfaceTypeDetailsBuilder, "Class or member details builder required");
		
		this.classOrInterfaceDetailsBuilder = classOrInterfaceTypeDetailsBuilder;
		this.methodCall = methodBody;
	}

	public ClassOrInterfaceTypeDetailsBuilder getClassOrInterfaceTypeDetailsBuilder() {
		return classOrInterfaceDetailsBuilder;
	}

	public List<String> getMethodBody() {
		return methodCall;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("memberHoldingTypeDetails", classOrInterfaceDetailsBuilder);
		tsc.append("methodCall", methodCall);
		return tsc.toString();
	}
}
