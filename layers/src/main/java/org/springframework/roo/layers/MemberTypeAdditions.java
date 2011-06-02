package org.springframework.roo.layers;

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
	private String methodSignature;
	
	public MemberTypeAdditions(ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder, String methodSignature) {
		super();
		Assert.notNull(classOrInterfaceTypeDetailsBuilder, "Class or member details builder required");
		
		this.classOrInterfaceDetailsBuilder = classOrInterfaceTypeDetailsBuilder;
		this.methodSignature = methodSignature;
	}

	public ClassOrInterfaceTypeDetailsBuilder getClassOrInterfaceTypeDetailsBuilder() {
		return classOrInterfaceDetailsBuilder;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("memberHoldingTypeDetails", classOrInterfaceDetailsBuilder);
		tsc.append("methodSignature", methodSignature);
		return tsc.toString();
	}
}
