package org.springframework.roo.model;
import junit.framework.Assert;

import org.junit.Test;


public class JavaTypeTests {
	@Test
	public void testEnclosingTypeDetection() {
		// no enclosing types
		Assert.assertEquals(null, new JavaType("BarBar").getEnclosingType());
		Assert.assertEquals(null, new JavaType("com.foo.Car").getEnclosingType());
		Assert.assertEquals(null, new JavaType("foo.Sar").getEnclosingType());
		// enclosing type in default package
		Assert.assertEquals(new JavaType("Bob"), new JavaType("Bob.Smith").getEnclosingType());
		// enclosing type in declared package
		Assert.assertEquals(new JavaType("foo.My"), new JavaType("foo.My.Sar").getEnclosingType());
		// enclosing type in declared package several levels deep
		Assert.assertEquals(new JavaType("foo.bar.My"), new JavaType("foo.bar.My.Sar").getEnclosingType());
	}
	
}
