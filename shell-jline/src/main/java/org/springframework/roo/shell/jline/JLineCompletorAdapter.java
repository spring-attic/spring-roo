package org.springframework.roo.shell.jline;

import java.util.List;

import jline.Completor;

import org.springframework.roo.shell.Parser;
import org.springframework.roo.shell.SimpleParser;
import org.springframework.roo.support.util.Assert;

/**
 * An implementation of JLine's {@link Completor} interface that delegates to ROO's {@link SimpleParser}.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class JLineCompletorAdapter implements Completor {

	// Fields
	private final Parser simpleParser;

	public JLineCompletorAdapter(final Parser simpleParser) {
		Assert.notNull(simpleParser, "Simple Parser required");
		this.simpleParser = simpleParser;
	}

	@SuppressWarnings("all")
	public int complete(final String buffer, final int cursor, final List candidates) {
		int result;
		try {
			JLineLogHandler.cancelRedrawProhibition();
			result = simpleParser.complete(buffer, cursor, candidates);
		} finally {
			JLineLogHandler.prohibitRedraw();
		}
		return result;
	}
}
