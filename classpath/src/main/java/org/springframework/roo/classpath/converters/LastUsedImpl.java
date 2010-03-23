package org.springframework.roo.classpath.converters;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;

/**
 * Records the last Java package and type used.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class LastUsedImpl implements LastUsed {
	private JavaPackage javaPackage;
	private JavaType javaType;
	@Reference private Shell shell;
	private JavaPackage topLevelPackage;
	
	public void setPackage(JavaPackage javaPackage) {
		Assert.notNull(javaPackage, "JavaPackage required");
		if (javaPackage.getFullyQualifiedPackageName().startsWith("java.")) {
			return;
		}
		this.javaType = null;
		this.javaPackage = javaPackage;
		this.shell.setPromptPath(shorten(javaPackage.getFullyQualifiedPackageName()));
	}
	
	public void setType(JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		if (javaType.getPackage().getFullyQualifiedPackageName().startsWith("java.")) {
			return;
		}
		this.javaType = javaType;
		this.javaPackage = javaType.getPackage();
		this.shell.setPromptPath(shorten(javaType.getFullyQualifiedTypeName()));
	}
	
	private String shorten(String fullyQualifiedName) {
		if (topLevelPackage == null) {
			return fullyQualifiedName;
		}
		return fullyQualifiedName.replace(topLevelPackage.getFullyQualifiedPackageName(), "~");
	}

	public JavaPackage getTopLevelPackage() {
		return topLevelPackage;
	}

	public void setTopLevelPackage(JavaPackage topLevelPackage) {
		this.topLevelPackage = topLevelPackage;
	}

	public JavaType getJavaType() {
		return javaType;
	}
	
	public JavaPackage getJavaPackage() {
		return javaPackage;
	}
	
}
