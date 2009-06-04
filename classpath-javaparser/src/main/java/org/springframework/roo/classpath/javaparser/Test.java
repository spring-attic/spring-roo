package org.springframework.roo.classpath.javaparser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

//public class Test<T> {
public class Test<T extends JavaParserUtils, W extends Map<? extends T[], ? super List<? extends Float>[]>> {
	
	private Collection<List<?>> dcd;
	
	private List<? super T> list2;
	
	public <K extends T> Test(Class<K> whatever) {}
	
	private Map<? extends Boolean, ? extends Long> map;
	
	private Map<Boolean, ? super W> anotherMap;
	
	public <K extends JavaParserUtils> void foo(Class<K> clazz) {}

	public <W extends JavaParserUtils> void foobar(Class<W> clazz) {}
	
	public void blah(Class<?> dc) {}
	
}
