package org.springframework.roo.project.layers;

import static org.junit.Assert.assertEquals;

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
		assertEquals("foo()", MemberTypeAdditions.buildMethodCall(null, "foo"));
	}
	
	@Test
	public void testGetMethodCallWithTargetAndNoParameters() {
		assertEquals("Foo.bar()", MemberTypeAdditions.buildMethodCall("Foo", "bar"));
	}
	
	@Test
	public void testGetMethodCallWithBlankTargetAndTwoParameters() {
		final JavaSymbolName parameter1 = new JavaSymbolName("Julia");
		final JavaSymbolName parameter2 = new JavaSymbolName("Zemiro");
		assertEquals("marry(Julia, Zemiro)", MemberTypeAdditions.buildMethodCall(null, "marry", parameter1, parameter2));
	}
}
