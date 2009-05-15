package org.springframework.roo.shell;

public final class CliOptionContext {
	private static ThreadLocal<String> optionContextHolder = new ThreadLocal<String>();

	public static String getOptionContext() {
		return optionContextHolder.get();
	}
	
	public static void setOptionContext(String optionContext) {
		optionContextHolder.set(optionContext);
	}
	
	public static void resetOptionContext() {
		optionContextHolder.remove();
	}
	
}
