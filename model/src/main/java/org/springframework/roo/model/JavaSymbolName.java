package org.springframework.roo.model;

import java.beans.Introspector;
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
public class JavaSymbolName implements Comparable<JavaSymbolName> {

	/** Constant for keyword "false" */
	public static final JavaSymbolName FALSE = new JavaSymbolName("false");

	/** Constant for keyword "true" */
	public static final JavaSymbolName TRUE = new JavaSymbolName("true");
	
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
	public static void assertJavaNameLegal(final String name) {
		Assert.notNull(name, "Name required");
		
		// Note regular expression for legal characters found to be x5 slower in profiling than this approach
		final char[] value = name.toCharArray();
		for (int i = 0; i < value.length; i++) {
			final char c = value[i];
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
	}

	/**
	 * @return a camel case string in human readable form
	 */
	public static String getReadableSymbolName(final String camelCase) {
	  final Pattern p = Pattern.compile("[A-Z][^A-Z]*");
		final Matcher m = p.matcher(StringUtils.capitalize(camelCase));
		final StringBuilder string = new StringBuilder();
		while (m.find()) {
			string.append(m.group()).append(" ");
		}
		return string.toString().trim();
	}
	
	/**
	 * Construct a Java symbol name which adheres to the strict JavaBean naming
	 * conventions and avoids use of {@link ReservedWords} by suffixing '_'
	 * 
	 * @param javaType the {@link JavaType} for which the symbol name is created
	 * @return a Java symbol name adhering to JavaBean conventions and avoids reserved words
	 * @since 1.2.0
	 */
	public static JavaSymbolName getReservedWordSafeName(final JavaType javaType) {
		String entityNameString = Introspector.decapitalize(StringUtils.capitalize(javaType.getSimpleTypeName()));
		while (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(entityNameString)) {
			// Prefixing can create names that don't work in the Derby DB
			entityNameString += "_";
		}
		return new JavaSymbolName(entityNameString);
	}
	
	/**
	 * Construct a Java symbol name which adheres to the strict JavaBean naming conventions and avoids
	 * use of {@link ReservedWords} by prefixing '_'
	 * 
	 * @param javaType the {@link JavaType} for which the symbol name is created
	 * @return a Java symbol name adhering to JavaBean conventions and avoids reserved words
	 * @deprecated use {@link #getReservedWordSafeName(JavaType)} instead (does the same thing, just better named)
	 */
	@Deprecated
	public static JavaSymbolName getReservedWordSaveName(final JavaType javaType) {
		return getReservedWordSafeName(javaType);
	}
	
	// Fields
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
	public JavaSymbolName(final String symbolName) {
		Assert.hasText(symbolName, "Fully qualified type name required");
		assertJavaNameLegal(symbolName);
		this.symbolName = symbolName;
	}

	
	public int compareTo(final JavaSymbolName o) {
		// NB: If adding more fields to this class ensure the equals(Object) method is updated accordingly 
		if (o == null) return -1;
		return this.symbolName.compareTo(o.symbolName);
	}

	@Override
	public boolean equals(final Object obj) {
		// NB: Not using the normal convention of delegating to compareTo (for efficiency reasons)
		return obj instanceof JavaSymbolName && this.symbolName.equals(((JavaSymbolName) obj).symbolName);
	}
	
	/**
	 * @return the symbol name in human readable form
	 */
	public String getReadableSymbolName() {
		final String camelCase = symbolName;
		return getReadableSymbolName(camelCase);
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
	
	@Override
	public int hashCode() {
		return this.symbolName.hashCode();
	}
	
	@Override
	public String toString() {
		return symbolName;
	}
}
