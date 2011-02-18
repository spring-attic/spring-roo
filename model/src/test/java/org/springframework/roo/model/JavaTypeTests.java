package org.springframework.roo.model;

import junit.framework.Assert;

import org.junit.Test;

public class JavaTypeTests {
	
	@Test 
	public void testEnclosingTypeDetection() {
		
		// No enclosing types
		Assert.assertNull(new JavaType("BarBar").getEnclosingType());
		Assert.assertNull(new JavaType("com.foo.Car").getEnclosingType());
		Assert.assertNull(new JavaType("foo.Sar").getEnclosingType());
		Assert.assertNull(new JavaType("bob").getEnclosingType());
		
		// Enclosing type in default package
		Assert.assertEquals(new JavaType("Bob"), new JavaType("Bob.Smith").getEnclosingType());
		Assert.assertEquals(new JavaPackage("Bob"), new JavaType("Bob.Smith").getEnclosingType().getPackage());
		
		// Enclosing type in declared package
		Assert.assertEquals(new JavaType("foo.My"), new JavaType("foo.My.Sar").getEnclosingType());
		
		// Enclosing type in declared package several levels deep
		Assert.assertEquals(new JavaType("foo.bar.My"), new JavaType("foo.bar.My.Sar").getEnclosingType());
		Assert.assertEquals("com.foo._MyBar", new JavaType("com.foo._MyBar").getFullyQualifiedTypeName());
		Assert.assertEquals(new JavaType("com.Foo.Bar"), new JavaType("com.Foo.Bar.My").getEnclosingType());
		Assert.assertEquals(new JavaType("com.foo.BAR"), new JavaType("com.foo.BAR.My").getEnclosingType());
	}
}
