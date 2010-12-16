package org.springframework.roo.addon.web.mvc.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

public class JavaTypeWrapperTests {

	@Mock private MetadataService metadataService;
	@Mock private BeanInfoMetadata beanInfoMetadata;
	
	@Before
	public void setUp() {
		initMocks(this);
	}

	@Test
	public void testMethodsForLabelWithNoSuitableMethods() throws Exception {
		String beanInfoMid = "MID:org.springframework.roo.addon.beaninfo.BeanInfoMetadata#SRC_MAIN_JAVA?somepackage.SomeClass";
		when(metadataService.get(beanInfoMid)).thenReturn(beanInfoMetadata);

		JavaTypeWrapper type = new JavaTypeWrapper(new JavaType("somepackage.SomeClass") , metadataService);
		List<MethodMetadata> actual = type.getMethodsForLabel();
		
		assertEquals(1, actual.size());
		assertEquals("toString", actual.get(0).getMethodName().getSymbolName());
		assertEquals("java.lang.String", actual.get(0).getReturnType().getNameIncludingTypeParameters());
	}
	
}
