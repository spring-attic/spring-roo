package org.springframework.roo.shell;

public final class CliSimpleParserContext {
	private static ThreadLocal<SimpleParser> simpleParserContextHolder = new ThreadLocal<SimpleParser>();

	public static Parser getSimpleParserContext() {
		return simpleParserContextHolder.get();
	}
	
	public static void setSimpleParserContext(SimpleParser simpleParserContext) {
		simpleParserContextHolder.set(simpleParserContext);
	}
	
	public static void resetSimpleParserContext() {
		simpleParserContextHolder.remove();
	}
	
}
