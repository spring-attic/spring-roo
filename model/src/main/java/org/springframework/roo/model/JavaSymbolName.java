package org.springframework.roo.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Immutable representation of a Java field name, method name, or other common legal Java identifier.
 * 
 * <p>
 * Ensures the field is properly formed.
 * 
 * @author Ben Alex
 * @author Greg Turnquist
 * @since 1.0
 */
public final class JavaSymbolName implements Comparable<JavaSymbolName> {

	/** Constant for keyword "true" */
	public static final JavaSymbolName TRUE = new JavaSymbolName("true");

	/** Constant for keyword "false" */
	public static final JavaSymbolName FALSE = new JavaSymbolName("false");
	
	private final String symbolName;

	/**
	 * Construct a Java symbol name.
	 * 
	 * <p>
	 * The name will be enforced as follows:
	 * 
	 * <ul>
	 * <li>The rules listed in {@link #assertJavaNameLegal(String)}
	 * </ul>
     *
	 * @param symbolName the name (mandatory)
	 */
	public JavaSymbolName(String symbolName) {
		Assert.hasText(symbolName, "Fully qualified type name required");
		assertJavaNameLegal(symbolName);
		this.symbolName = symbolName;
	}
	
	/**
	 * @return the symbol name (never null or empty)
	 */
	public String getSymbolName() {
		return symbolName;
	}
	
	/**
	 * @return the symbol name, capitalising the first letter (never null or empty)
	 */
	public String getSymbolNameCapitalisedFirstLetter() {
		return StringUtils.capitalize(symbolName);
	}
	
	/**
	 * @return the name of a setter for the symbol
	 */
	public String getSymbolNameTurnedIntoMutatorMethodName() {
		return "set" + getSymbolNameCapitalisedFirstLetter();
	}

	
	/**
	 * @return the symbol name in human readable form
	 */
	public String getReadableSymbolName() {
		String camelCase = symbolName;
		return getReadableSymbolName(camelCase);
	}

	/**
	 * @return a camel case string in human readable form
	 */
	public static String getReadableSymbolName(String camelCase) {
	  Pattern p = Pattern.compile("[A-Z][^A-Z]*");
		Matcher m = p.matcher(StringUtils.capitalize(camelCase));
		StringBuilder string = new StringBuilder();
		while (m.find()) {
			string.append(m.group()).append(" ");
		}
		return string.toString().trim();
  }
	
	public final int hashCode() {
		return this.symbolName.hashCode();
	}

	public final boolean equals(Object obj) {
		// NB: Not using the normal convention of delegating to compareTo (for efficiency reasons)
		return obj != null && obj instanceof JavaSymbolName && this.symbolName.equals(((JavaSymbolName) obj).symbolName);
	}

	public final int compareTo(JavaSymbolName o) {
		// NB: If adding more fields to this class ensure the equals(Object) method is updated accordingly 
		if (o == null) return -1;
		return this.symbolName.compareTo(o.symbolName);
	}
	
	public final String toString() {
		return symbolName;
	}
	
	/**
	 * Verifies the presented name is a valid Java name. Specifically, the following is enforced:
	 * 
	 * <ul>
	 * <li>Textual content must be provided in the name</li>
	 * <li>Must not have any slashes in the name</li>
	 * <li>Must not start with a number</li>
	 * <li>Must not have any spaces or other illegal characters in the name</li>
	 * <li>Must not start or end with a period</li>
	 * </ul>
	 * 
	 * @param name to the package name to evaluate (required)
	 */
	public static final void assertJavaNameLegal(String name) {
		Assert.notNull(name, "Name required");
		
		// Note regular expression for legal characters found to be x5 slower in profiling than this approach
		char[] value = name.toCharArray();
		for (int i = 0; i < value.length; i++) {
			char c = value[i];
			if ('/' == c || ' ' == c || '*' == c || '>' == c || '<' == c || '!' == c || '@' == c || '%' == c || '^' == c ||
				'?' == c || '(' == c || ')' == c || '~' == c || '`' == c || '{' == c || '}' == c || '[' == c || ']' == c ||
				'|' == c || '\\' == c || '\'' == c || '+' == c || '-' == c)  {
				throw new IllegalArgumentException("Illegal name '" + name + "' (illegal character)");
			}
			if (i == 0) {
				if ('1' == c || '2' == c || '3' == c || '4' == c || '5' == c || '6' == c || '7' == c || '8' == c || '9' == c || '0' == c) {
					throw new IllegalArgumentException("Illegal name '" + name + "' (cannot start with a number)");
				}
			}
			if (i + 1 == value.length || i == 0) {
				if ('.' == c) {
					throw new IllegalArgumentException("Illegal name '" + name + "' (cannot start or end with a period)");
				}
			}
		}
		/*
		Assert.notNull(name, "Name required");
		Assert.isTrue(!name.contains("/"), "Slashes are prohibited in the name");
		Assert.isTrue(!name.contains(" "), "Spaces are prohibited in the name");
		Assert.isTrue(!name.contains("*"), "Illegal name");
		Assert.isTrue(!name.contains(">"), "Illegal name");
		Assert.isTrue(!name.contains("<"), "Illegal name");
		Assert.isTrue(!name.contains("!"), "Illegal name");
		Assert.isTrue(!name.contains("@"), "Illegal name");
		Assert.isTrue(!name.contains("%"), "Illegal name");
		Assert.isTrue(!name.contains("^"), "Illegal name");
		Assert.isTrue(!name.contains("?"), "Illegal name");
		Assert.isTrue(!name.contains("("), "Illegal name");
		Assert.isTrue(!name.contains(")"), "Illegal name");
		Assert.isTrue(!name.contains("~"), "Illegal name");
		Assert.isTrue(!name.contains("`"), "Illegal name");
		Assert.isTrue(!name.contains("{"), "Illegal name");
		Assert.isTrue(!name.contains("}"), "Illegal name");
		Assert.isTrue(!name.contains("["), "Illegal name");
		Assert.isTrue(!name.contains("]"), "Illegal name");
		Assert.isTrue(!name.contains("|"), "Illegal name");
		Assert.isTrue(!name.contains("\""), "Illegal name");
		Assert.isTrue(!name.contains("'"), "Illegal name");
		Assert.isTrue(!name.contains("+"), "Illegal name");
		Assert.isTrue(!name.startsWith("1"), "Illegal name");
		Assert.isTrue(!name.startsWith("2"), "Illegal name");
		Assert.isTrue(!name.startsWith("3"), "Illegal name");
		Assert.isTrue(!name.startsWith("4"), "Illegal name");
		Assert.isTrue(!name.startsWith("5"), "Illegal name");
		Assert.isTrue(!name.startsWith("6"), "Illegal name");
		Assert.isTrue(!name.startsWith("7"), "Illegal name");
		Assert.isTrue(!name.startsWith("8"), "Illegal name");
		Assert.isTrue(!name.startsWith("9"), "Illegal name");
		Assert.isTrue(!name.startsWith("0"), "Illegal name");
		Assert.isTrue(!name.startsWith("."), "The name cannot begin with a period");
		Assert.isTrue(!name.endsWith("."), "The name cannot end with a period");
		*/
	}
}
