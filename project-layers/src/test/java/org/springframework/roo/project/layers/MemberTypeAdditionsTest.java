package org.springframework.roo.project.layers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Unit test of {@link MemberTypeAdditions}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class MemberTypeAdditionsTest {

	@Test
	public void testGetMethodCallWithBlankTargetAndNoParameters() {
		assertMethodCall("foo()", null, "foo");
	}
	
	@Test
	public void testGetMethodCallWithTargetAndNoParameters() {
		assertMethodCall("Foo.bar()", "Foo", "bar");
	}
	
	@Test
	public void testGetMethodCallWithBlankTargetAndTwoParameters() {
		assertMethodCall("matchmakingService.marry(julia, zemiro)", "matchmakingService", "marry", "julia", "zemiro");
	}
	
	/**
	 * Asserts that {@link MemberTypeAdditions#buildMethodCall(String, String, java.util.Iterator)}
	 * builds the expected method call from the given parameters
	 * 
	 * @param expectedMethodCall
	 * @param target
	 * @param method
	 * @param parameterNames
	 */
	private void assertMethodCall(final String expectedMethodCall, final String target, final String method, final String... parameterNames) {
		final List<JavaSymbolName> parameterSymbols = new ArrayList<JavaSymbolName>();
		for (final String parameterName : parameterNames) {
			parameterSymbols.add(new JavaSymbolName(parameterName));
		}
		assertEquals(expectedMethodCall, MemberTypeAdditions.buildMethodCall(target, method, parameterSymbols.iterator()));
	}
}
