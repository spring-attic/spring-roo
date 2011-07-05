package org.springframework.roo.addon.web.mvc.controller.converter;

import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.mockito.Mock;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;

@Deprecated
@SuppressWarnings("unused")
public class ConversionServiceOperationsImplTests {
	private static final String WEB_MVC_CONFIG = "/org/springframework/roo/addon/web/mvc/controller/webmvc-config.xml";
	@Mock private FileManager fileManager;
	@Mock private PathResolver pathResolver;
	@Mock private ProjectOperations projectOperations;
	@Mock private TypeLocationService typeLocationService;
	private ConversionServiceOperationsImpl operations;

	@Before
	public void setUp() {
		initMocks(this);
		operations = new ConversionServiceOperationsImpl(fileManager, typeLocationService);
	}
/*	
	@Test
	public void testManageWebMvcConfig() throws Exception {
		String webMvcConfig = WEB_MVC_CONFIG;
		when(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml")).thenReturn(webMvcConfig);
		StubMutableFile webMvcConfigFile = new StubMutableFile(new File(getClass().getResource(webMvcConfig).getFile()));
		when(fileManager.exists(webMvcConfig)).thenReturn(true);
		when(fileManager.updateFile(webMvcConfig)).thenReturn(webMvcConfigFile);
		
		operations.manageWebMvcConfig(new JavaPackage(getClass().getPackage().getName()));
		String output = webMvcConfigFile.getOutputAsString();
		assertThat(output, containsString("<mvc:annotation-driven conversion-service=\"applicationConversionService\"/>"));
		assertThat(output, containsString("<bean class=\"org.springframework.roo.addon.web.mvc.controller.converter.ApplicationConversionServiceFactoryBean\" id=\"applicationConversionService\"/>"));
	}
	
	@Test
	public void testManageWebMvcConfig_ManageTwoTimes() throws Exception {
		String webMvcConfig = WEB_MVC_CONFIG;
		String webMvcConfig2 = "/org/springframework/roo/addon/web/mvc/controller/webmvc-config-conversionService.xml";
		when(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml")).thenReturn(webMvcConfig);
		StubMutableFile webMvcConfigFile2 = new StubMutableFile(new File(getClass().getResource(webMvcConfig2).getFile()));
		when(fileManager.exists(webMvcConfig)).thenReturn(true);
		when(fileManager.updateFile(webMvcConfig)).thenReturn(webMvcConfigFile2);
		
		operations.manageWebMvcConfig(new JavaPackage(getClass().getPackage().getName()));
		assertEquals("Nothing was written out to the OutputStream", 0, webMvcConfigFile2.getOutputAsString().length());
	}
	
	@Test
	public void testManageWebMvcConfig_CustomConversionServiceFound() throws Exception {
		String webMvcConfig = WEB_MVC_CONFIG;
		String webMvcConfig2 = "/org/springframework/roo/addon/web/mvc/controller/webmvc-config-customConversionService.xml";
		when(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml")).thenReturn(webMvcConfig);
		StubMutableFile webMvcConfigFile2 = new StubMutableFile(new File(getClass().getResource(webMvcConfig2).getFile()));
		when(fileManager.exists(webMvcConfig)).thenReturn(true);
		when(fileManager.updateFile(webMvcConfig)).thenReturn(webMvcConfigFile2);

		try {
			operations.manageWebMvcConfig(new JavaPackage(getClass().getPackage().getName()));
			fail("Expected an exception due to the presence of a custom conversion service");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("Found custom ConversionService installed"));
		}
	}
	
	@Test
	public void testInstallJavaClass() throws Exception {
		JavaType javaType = new JavaType(getClass().getPackage().getName() + ".ApplicationConversionServiceFactoryBean");
		String targetPath = "doesntMatter";
		when(typeLocationService.getPhysicalLocationCanonicalPath(javaType , Path.SRC_MAIN_JAVA)).thenReturn(targetPath);
		when(pathResolver.getPath(targetPath)).thenReturn(new Path(targetPath));
		StubMutableFile file = new StubMutableFile();
		when(fileManager.createFile(targetPath)).thenReturn(file);
		
		operations.installConversionService(javaType.getPackage());
		
		String output = file.getOutputAsString();
		assertThat(output, containsString("package org.springframework.roo.addon.web.mvc.controller.converter;"));
		assertThat(output, containsString("import org.springframework.format.FormatterRegistry;"));
		assertThat(output, containsString("import org.springframework.format.support.FormattingConversionServiceFactoryBean;"));
		assertThat(output, containsString("import org.springframework.roo.addon.web.mvc.controller.RooConversionService;"));
		assertThat(output, containsString("@RooConversionService"));
		assertThat(output, containsString("public class ApplicationConversionServiceFactoryBean extends FormattingConversionServiceFactoryBean {"));
		assertThat(output, containsString("@Override"));
		assertThat(output, containsString("protected void installFormatters(FormatterRegistry registry) {"));
		assertThat(output, containsString("super.installFormatters(registry);"));
	}
*/
}
