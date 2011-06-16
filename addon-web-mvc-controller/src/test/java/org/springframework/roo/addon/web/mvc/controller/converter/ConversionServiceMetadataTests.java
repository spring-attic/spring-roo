package org.springframework.roo.addon.web.mvc.controller.converter;

import org.junit.Test;

@Deprecated
public class ConversionServiceMetadataTests {
	@Test public void test() {}
/*
	private JavaTypeWrapper rooJavaType; 
	private ConversionServiceMetadata metadata;
	
	@Mock private MetadataService metadataService;
	@Mock private PhysicalTypeMetadata physicalTypeMetadata;
	@Mock private ClassOrInterfaceTypeDetails typeDetails;

	@Before
	public void setUp() {
		initMocks(this);
		rooJavaType = new JavaTypeWrapper(new JavaType("somepackage.SomeClass") , metadataService);
		when(physicalTypeMetadata.getPhysicalTypeDetails()).thenReturn(typeDetails);
		when(typeDetails.getName()).thenReturn(rooJavaType.getJavaType());
		metadata = new ConversionServiceMetadata("MID:id#path", new JavaType("AspectName"), physicalTypeMetadata);
	}

	@Test
	public void testGetConverterMethod() throws Exception {
		final List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		methods.add(new StubMethodMetadata("getFirstName", String.class));
		methods.add(new StubMethodMetadata("getLastName", String.class));
		methods.add(new StubMethodMetadata("getAge", Integer.class));
		JavaTypeWrapper theType = new JavaTypeWrapper(new JavaType("somepackage.SomeClass"), metadataService) {
			public List<MethodMetadata> getMethodsForLabel() { return methods; }
		};
		
		MethodMetadata actual = metadata.getConverterMethod(theType, "getSomeClassConverter");
		assertEquals(
				"        return new Converter<SomeClass, String>() {\n" +
				"            public String convert(SomeClass source) {\n" +
				"                return new StringBuilder().append(source.getFirstName()).append(\" \").append(source.getLastName()).append(\" \").append(source.getAge()).toString();\n" +
				"            }\n" +
				"        };\n", actual.getBody());
		assertEquals(new JavaType("org.springframework.core.convert.converter.Converter"), actual.getReturnType());
	}
	
	@Test
	public void testGetConverterMethod_EnumField() throws Exception {
		PhysicalTypeMetadata ptmEnum = Mockito.mock(PhysicalTypeMetadata.class);
		ClassOrInterfaceTypeDetails tdEnum = Mockito.mock(ClassOrInterfaceTypeDetails.class);
		String id = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#SRC_MAIN_JAVA?java.lang.String";
		when(metadataService.get(id)).thenReturn(ptmEnum);
		when(ptmEnum.getPhysicalTypeDetails()).thenReturn(tdEnum);
		when(tdEnum.getPhysicalTypeCategory()).thenReturn(PhysicalTypeCategory.ENUMERATION);
		
		final List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		methods.add(new StubMethodMetadata("getEnumType", String.class));

		JavaTypeWrapper theType = new JavaTypeWrapper(new JavaType("somepackage.SomeClass"), metadataService) {
			public List<MethodMetadata> getMethodsForLabel() { return methods; }
		};
		
		MethodMetadata actual = metadata.getConverterMethod(theType, "getSomeClassConverter");
		assertEquals("Enum getter should have .name() appended",
				"        return new Converter<SomeClass, String>() {\n" +
				"            public String convert(SomeClass source) {\n" +
				"                return new StringBuilder().append(source.getEnumType().name()).toString();\n" +
				"            }\n" +
				"        };\n", actual.getBody());
	}
*/
}