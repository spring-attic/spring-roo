package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.details.InvocableMemberMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * A simple way of producing method bodies for {@link InvocableMemberMetadata#getBody()}.
 * 
 * <p>
 * Method bodies immediately assume they are indented two levels.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class InvocableMemberBodyBuilder {
	private boolean reset = false;
	private int indentLevel = 0;
	private StringBuilder stringBuilder = new StringBuilder();

	public InvocableMemberBodyBuilder() {
		indentLevel++;
		indentLevel++;
	}

	/**
	 * Increases the indent by one level.
	 */
	public InvocableMemberBodyBuilder indent() {
		indentLevel++;
		return this;
	}
	
	/**
	 * Resets the indent to zero.
	 */
	public InvocableMemberBodyBuilder reset() {
		indentLevel = 0;
		reset = true;
		return this;
	}

	/**
	 * Decreases the indent by one level.
	 */
	public InvocableMemberBodyBuilder indentRemove() {
		indentLevel--;
		return this;
	}

	/**
	 * Prints a blank line, ensuring any indent is included before doing so.
	 */
	public InvocableMemberBodyBuilder newLine(boolean indentBefore) {
		if (indentBefore) {
			appendIndent();
		}
        // We use \n for consistency with JavaParser's DumpVisitor, which always uses \n
		stringBuilder.append("\n");
		// stringBuilder.append(System.getProperty("line.separator"));
		return this;
	}
	
	public InvocableMemberBodyBuilder newLine() {
		newLine(true);
		return this;
	}
	
	/**
	 * Prints the message, WITHOUT ANY INDENTATION.
	 */
	public InvocableMemberBodyBuilder append(String message) {
		if (message != null && !"".equals(message)) {
			stringBuilder.append(message);
		}
		return this;
	}

	/**
	 * Prints the message, after adding indents and returns to a new line. This is the most commonly used method.
	 */
	public InvocableMemberBodyBuilder appendFormalLine(String message) {
		appendIndent();
		if (message != null && !"".equals(message)) {
			stringBuilder.append(message);
		}
		return newLine(false);
	}

	/**
	 * Prints the relevant number of indents.
	 */
	public InvocableMemberBodyBuilder appendIndent() {
		for (int i = 0 ; i < indentLevel; i++) {
			stringBuilder.append("    ");
		}
		return this;
	}
	
	public String getOutput() {
		if (reset) {
			Assert.isTrue(this.indentLevel == 0, "Indent level must be 0 (not " + indentLevel + ") to terminate following a reset!");
		} else {
			Assert.isTrue(this.indentLevel == 2, "Indent level must be 2 (not " + indentLevel + ") to terminate (use reset to indent to level 0)!");
		}
		return stringBuilder.toString();
	}
}
