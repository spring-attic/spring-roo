package org.springframework.roo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

public class ImportRegistrationResolverImpl implements ImportRegistrationResolver {

	private static final List<String> javaLangSimpleTypeNames = new ArrayList<String>();
	private Set<JavaType> registeredImports = new HashSet<JavaType>();
	private JavaPackage compilationUnitPackage;
	
	static {
		javaLangSimpleTypeNames.add("Appendable");
		javaLangSimpleTypeNames.add("CharSequence");
		javaLangSimpleTypeNames.add("Cloneable");
		javaLangSimpleTypeNames.add("Comparable");
		javaLangSimpleTypeNames.add("Iterable");
		javaLangSimpleTypeNames.add("Readable");
		javaLangSimpleTypeNames.add("Runnable");
		javaLangSimpleTypeNames.add("Boolean");
		javaLangSimpleTypeNames.add("Byte");
		javaLangSimpleTypeNames.add("Character");
		javaLangSimpleTypeNames.add("Class");
		javaLangSimpleTypeNames.add("ClassLoader");
		javaLangSimpleTypeNames.add("Compiler");
		javaLangSimpleTypeNames.add("Double");
		javaLangSimpleTypeNames.add("Enum");
		javaLangSimpleTypeNames.add("Float");
		javaLangSimpleTypeNames.add("InheritableThreadLocal");
		javaLangSimpleTypeNames.add("Integer");
		javaLangSimpleTypeNames.add("Long");
		javaLangSimpleTypeNames.add("Math");
		javaLangSimpleTypeNames.add("Number");
		javaLangSimpleTypeNames.add("Object");
		javaLangSimpleTypeNames.add("Package");
		javaLangSimpleTypeNames.add("Process");
		javaLangSimpleTypeNames.add("ProcessBuilder");
		javaLangSimpleTypeNames.add("Runtime");
		javaLangSimpleTypeNames.add("RuntimePermission");
		javaLangSimpleTypeNames.add("SecurityManager");
		javaLangSimpleTypeNames.add("Short");
		javaLangSimpleTypeNames.add("StackTraceElement");
		javaLangSimpleTypeNames.add("StrictMath");
		javaLangSimpleTypeNames.add("String");
		javaLangSimpleTypeNames.add("StringBuffer");
		javaLangSimpleTypeNames.add("StringBuilder");
		javaLangSimpleTypeNames.add("System");
		javaLangSimpleTypeNames.add("Thread");
		javaLangSimpleTypeNames.add("ThreadGroup");
		javaLangSimpleTypeNames.add("ThreadLocal");
		javaLangSimpleTypeNames.add("Throwable");
		javaLangSimpleTypeNames.add("Void");
		javaLangSimpleTypeNames.add("ArithmeticException");
		javaLangSimpleTypeNames.add("ArrayIndexOutOfBoundsException");
		javaLangSimpleTypeNames.add("ArrayStoreException");
		javaLangSimpleTypeNames.add("ClassCastException");
		javaLangSimpleTypeNames.add("ClassNotFoundException");
		javaLangSimpleTypeNames.add("CloneNotSupportedException");
		javaLangSimpleTypeNames.add("EnumConstantNotPresentException");
		javaLangSimpleTypeNames.add("Exception");
		javaLangSimpleTypeNames.add("IllegalAccessException");
		javaLangSimpleTypeNames.add("IllegalArgumentException");
		javaLangSimpleTypeNames.add("IllegalMonitorStateException");
		javaLangSimpleTypeNames.add("IllegalStateException");
		javaLangSimpleTypeNames.add("IllegalThreadStateException");
		javaLangSimpleTypeNames.add("IndexOutOfBoundsException");
		javaLangSimpleTypeNames.add("InstantiationException");
		javaLangSimpleTypeNames.add("InterruptedException");
		javaLangSimpleTypeNames.add("NegativeArraySizeException");
		javaLangSimpleTypeNames.add("NoSuchFieldException");
		javaLangSimpleTypeNames.add("NoSuchMethodException");
		javaLangSimpleTypeNames.add("NullPointerException");
		javaLangSimpleTypeNames.add("NumberFormatException");
		javaLangSimpleTypeNames.add("RuntimeException");
		javaLangSimpleTypeNames.add("SecurityException");
		javaLangSimpleTypeNames.add("StringIndexOutOfBoundsException");
		javaLangSimpleTypeNames.add("TypeNotPresentException");
		javaLangSimpleTypeNames.add("UnsupportedOperationException");
		javaLangSimpleTypeNames.add("AbstractMethodError");
		javaLangSimpleTypeNames.add("AssertionError");
		javaLangSimpleTypeNames.add("ClassCircularityError");
		javaLangSimpleTypeNames.add("ClassFormatError");
		javaLangSimpleTypeNames.add("Error");
		javaLangSimpleTypeNames.add("ExceptionInInitializerError");
		javaLangSimpleTypeNames.add("IllegalAccessError");
		javaLangSimpleTypeNames.add("IncompatibleClassChangeError");
		javaLangSimpleTypeNames.add("InstantiationError");
		javaLangSimpleTypeNames.add("InternalError");
		javaLangSimpleTypeNames.add("LinkageError");
		javaLangSimpleTypeNames.add("NoClassDefFoundError");
		javaLangSimpleTypeNames.add("NoSuchFieldError");
		javaLangSimpleTypeNames.add("NoSuchMethodError");
		javaLangSimpleTypeNames.add("OutOfMemoryError");
		javaLangSimpleTypeNames.add("StackOverflowError");
		javaLangSimpleTypeNames.add("ThreadDeath");
		javaLangSimpleTypeNames.add("UnknownError");
		javaLangSimpleTypeNames.add("UnsatisfiedLinkError");
		javaLangSimpleTypeNames.add("UnsupportedClassVersionError");
		javaLangSimpleTypeNames.add("VerifyError");
		javaLangSimpleTypeNames.add("VirtualMachineError");
	}
	
	public ImportRegistrationResolverImpl(JavaPackage compilationUnitPackage) {
		Assert.notNull(compilationUnitPackage, "Compilation unit package required");
		this.compilationUnitPackage = compilationUnitPackage;
	}

	public void addImport(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		registeredImports.add(javaType);
	}

	public JavaPackage getCompilationUnitPackage() {
		return compilationUnitPackage;
	}

	public Set<JavaType> getRegisteredImports() {
		return Collections.unmodifiableSet(registeredImports);
	}

	public boolean isAdditionLegal(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		
		if (javaType.getDataType() != DataType.TYPE) {
			// It's a type variable or primitive
			return false;
		}
		
		if (javaType.isDefaultPackage()) {
			// Cannot import types from the default package
			return false;
		}
		
		// Must be a class, so it's legal if there isn't an existing registration that conflicts
		for (JavaType candidate : registeredImports) {
			if (candidate.getSimpleTypeName().equals(javaType.getSimpleTypeName())) {
				// conflict detected
				return false;
			}
		}
		
		return true;
	}
	
	public boolean isFullyQualifiedFormRequired(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		
		if (javaType.getDataType() == DataType.PRIMITIVE || javaType.getDataType() == DataType.VARIABLE) {
			// Primitives and type variables do not need to be used in fully-qualified form
			return false;
		}

		if (registeredImports.contains(javaType)) {
			// Already know about this one
			return false;
		}
		
		if (compilationUnitPackage.equals(javaType.getPackage())) {
			// No need for an explicit registration, given it's in the same package
			return false;
		}
		
		// To get this far, it must need a fully-qualified name
		return true;
	}
	
	public boolean isFullyQualifiedFormRequiredAfterAutoImport(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		
		// Try to add import if possible
		if (isAdditionLegal(javaType)) {
			addImport(javaType);
		}
		
		// Indicate whether we can use in a simple or need a fully-qualified form
		return isFullyQualifiedFormRequired(javaType);
	}

	/**
	 * Determines whether the presented simple type name is part of java.lang or not.
	 *  
	 * @param simpleTypeName the simple type name (required)
	 * @return whether the type is declared as part of java.lang
	 */
	public static boolean isPartOfJavaLang(String simpleTypeName) {
		Assert.hasText(simpleTypeName, "Simple type name required");
		return javaLangSimpleTypeNames.contains(simpleTypeName);
	}

}
