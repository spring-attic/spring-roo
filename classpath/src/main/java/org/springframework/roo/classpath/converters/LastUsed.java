package org.springframework.roo.classpath.converters;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Records the last Java package and type used.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class LastUsed {
	private JavaPackage javaPackage;
	private JavaType javaType;
	private Shell shell;
	
	public LastUsed(Shell shell) {
		Assert.notNull(shell, "Shell required");
		this.shell = shell;
	}

	/**
	 * Sets the package, and clears the type field. Ignores attempts to set to java.*.
	 */
	public void setPackage(JavaPackage javaPackage) {
		Assert.notNull(javaPackage, "JavaPackage required");
		if (javaPackage.getFullyQualifiedPackageName().startsWith("java.")) {
			return;
		}
		this.javaType = null;
		this.javaPackage = javaPackage;
		this.shell.setPromptPath(javaPackage.getFullyQualifiedPackageName());
	}
	
	/**
	 * Sets the type, and also sets the package field. Ignores attempts to set to java.*.
	 */
	public void setType(JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		if (javaType.getPackage().getFullyQualifiedPackageName().startsWith("java.")) {
			return;
		}
		this.javaType = javaType;
		this.javaPackage = javaType.getPackage();
		this.shell.setPromptPath(javaType.getFullyQualifiedTypeName());
	}

	/**
	 * @return the type or null
	 */
	public JavaType getJavaType() {
		return javaType;
	}
	
	/**
	 * @return the package, either explicitly set or via a type set (may also be null if never set)
	 */
	public JavaPackage getJavaPackage() {
		return javaPackage;
	}
	
}
