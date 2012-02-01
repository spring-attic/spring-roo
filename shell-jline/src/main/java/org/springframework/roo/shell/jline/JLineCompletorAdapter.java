package org.springframework.roo.shell.jline;

import java.util.ArrayList;
import java.util.List;

import jline.Completor;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Parser;

/**
 * An implementation of JLine's {@link Completor} interface that delegates to a
 * {@link Parser}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JLineCompletorAdapter implements Completor {

    private final Parser parser;

    public JLineCompletorAdapter(final Parser parser) {
        Validate.notNull(parser, "Parser required");
        this.parser = parser;
    }

    @SuppressWarnings("all")
    public int complete(final String buffer, final int cursor,
            final List candidates) {
        int result;
        try {
            JLineLogHandler.cancelRedrawProhibition();
            final List<Completion> completions = new ArrayList<Completion>();
            result = parser.completeAdvanced(buffer, cursor, completions);
            for (final Completion completion : completions) {
                candidates
                        .add(new jline.Completion(completion.getValue(),
                                completion.getFormattedValue(), completion
                                        .getHeading()));
            }
        }
        finally {
            JLineLogHandler.prohibitRedraw();
        }
        return result;
    }
}
