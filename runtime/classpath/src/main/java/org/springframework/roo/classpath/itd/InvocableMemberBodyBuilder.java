package org.springframework.roo.classpath.itd;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.InvocableMemberMetadata;

/**
 * A simple way of producing method bodies for
 * {@link InvocableMemberMetadata#getBody()}.
 * <p>
 * Method bodies immediately assume they are indented two levels.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class InvocableMemberBodyBuilder {

    public static InvocableMemberBodyBuilder getInstance() {
        return new InvocableMemberBodyBuilder();
    }

    private int indentLevel;
    private boolean reset;
    private final StringBuilder stringBuilder = new StringBuilder();

    /**
     * Constructor for an empty body
     */
    public InvocableMemberBodyBuilder() {
        indentLevel++;
        indentLevel++;
    }

    /**
     * Prints the message, WITHOUT ANY INDENTATION.
     */
    public InvocableMemberBodyBuilder append(final String message) {
        if (message != null && !"".equals(message)) {
            stringBuilder.append(message);
        }
        return this;
    }

    /**
     * Prints the message, after adding indents and returns to a new line. This
     * is the most commonly used method.
     */
    public InvocableMemberBodyBuilder appendFormalLine(final String message) {
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
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append("    ");
        }
        return this;
    }

    public String getOutput() {
        if (reset) {
            Validate.isTrue(
                    indentLevel == 0,
                    "Indent level must be 0 (not %d) to terminate following a reset",
                    indentLevel);
        }
        else {
            Validate.isTrue(
                    indentLevel == 2,
                    "Indent level must be 2 (not %d) to terminate (use reset to indent to level 0)",
                    indentLevel);
        }
        return stringBuilder.toString();
    }

    /**
     * Increases the indent by one level.
     */
    public InvocableMemberBodyBuilder indent() {
        indentLevel++;
        return this;
    }

    /**
     * Decreases the indent by one level.
     */
    public InvocableMemberBodyBuilder indentRemove() {
        indentLevel--;
        return this;
    }

    public InvocableMemberBodyBuilder newLine() {
        newLine(true);
        return this;
    }

    /**
     * Prints a blank line, ensuring any indent is included before doing so.
     */
    public InvocableMemberBodyBuilder newLine(final boolean indentBefore) {
        if (indentBefore) {
            appendIndent();
        }
        // We use \n for consistency with JavaParser's DumpVisitor, which always
        // uses \n
        stringBuilder.append("\n");
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
}
