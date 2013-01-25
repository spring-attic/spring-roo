package org.springframework.roo.classpath.javaparser;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.TypeManagementServiceImpl;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Functional test of
 * {@link JavaParserTypeParsingService#updateAndGetCompilationUnitContents(String, org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails)}
 * 
 * @author DiSiD Technologies
 * @since 1.2.1
 */
@RunWith(PowerMockRunner.class)
public class NewUpdateCompilationUnitTest {

	private static final String SIMPLE_INTERFACE_FILE_PATH = "SimpleInterface.java.test";
	private static final String SIMPLE_CLASS_FILE_PATH = "SimpleClass.java.test";
	private static final String SIMPLE_CLASS2_FILE_PATH = "SimpleClass2.java.test";
	private static final String SIMPLE_CLASS3_FILE_PATH = "SimpleClass3.java.test";
	private static final String ROO1505_CLASS_FILE_PATH = "Roo_1505.java.test";
	private static final String ENUM_FILE_PATH = "AEnumerate.java.test";

	private static final JavaType SIMPLE_INTERFACE_TYPE = new JavaType(
			"org.myPackage.SimpleInterface");
	private static final JavaType SIMPLE_CLASS_TYPE = new JavaType(
			"org.myPackage.SimpleClass");
	private static final JavaType SIMPLE_CLASS2_TYPE = new JavaType(
			"org.myPackage.SimpleClass2");
	private static final JavaType SIMPLE_CLASS3_TYPE = new JavaType(
			"org.myPackage.SimpleClass3");
	private static final JavaType ROO1505_CLASS_TYPE = new JavaType(
			"com.pet.Roo_1505");
	private static final JavaType ENUM_TYPE = new JavaType(
			"org.myPackage.AEnumerate");

	private static final String SIMPLE_INTERFACE_DECLARED_BY_MID = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#bar?SimpleInterface";
	private static final String SIMPLE_CLASS_DECLARED_BY_MID = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#SRC_MAIN_JAVA?SimpleClass";
	private static final String SIMPLE_CLASS2_DECLARED_BY_MID = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#SRC_MAIN_JAVA?SimpleClass2";
	private static final String SIMPLE_CLASS3_DECLARED_BY_MID = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#SRC_MAIN_JAVA?SimpleClass3";
	private static final String ROO1505_CLASS_DECLARED_BY_MID = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#SRC_MAIN_JAVA?Roo_1505";
	private static final String ENUM_DECLARED_BY_MID = "MID:org.springframework.roo.classpath.PhysicalTypeIdentifier#SRC_MAIN_JAVA?AEnumerate";

	private static final String SIMPLE_INTERFACE_NAME = "SimpleInterface";
	private static final String SIMPLE_CLASS_NAME = "SimpleClass";
	private static final String SIMPLE_CLASS2_NAME = "SimpleClass2";
	private static final String SIMPLE_CLASS3_NAME = "SimpleClass3";
	private static final String ROO1505_CLASS_NAME = "Roo_1505";
	private static final String ENUM_NAME = "AEnumerate";

	@Mock
	private MetadataService mockMetadataService;
	@Mock
	private TypeLocationService mockTypeLocationService;

	private TypeManagementService typeManagementService;

	// Fixture
	private JavaParserTypeParsingService typeParsingService;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		typeParsingService = new JavaParserTypeParsingService();
		typeParsingService.metadataService = mockMetadataService;
		typeParsingService.typeLocationService = mockTypeLocationService;
		typeManagementService = new TypeManagementServiceImpl();
	}

	@Test
	public void testSimpleInterfaceNoChanges() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_INTERFACE_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents,
						SIMPLE_INTERFACE_DECLARED_BY_MID, SIMPLE_INTERFACE_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						simpleInterfaceDetails);

		saveResult(file, result);

		checkSimpleInterface(result, true);
	}

	@Test
	public void testEnumNoChanges() throws Exception {

		// Set up
		final File file = getResource(ENUM_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, ENUM_DECLARED_BY_MID,
						ENUM_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						simpleInterfaceDetails);

		saveResult(file, result);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails2 = typeParsingService
				.getTypeFromString(result, ENUM_DECLARED_BY_MID, ENUM_TYPE);

		// invoke
		final String result2 = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						simpleInterfaceDetails2);

		checkEnum(result);
	}

	@Test
	public void testEnumAddElement() throws Exception {

		// Set up
		final File file = getResource(ENUM_FILE_PATH);
		final String fileContents = getResourceContents(file);
		final String filePath = file.getCanonicalPath();

		final ClassOrInterfaceTypeDetails enumDetails = typeParsingService
				.getTypeFromString(fileContents, ENUM_DECLARED_BY_MID,
						ENUM_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(filePath, enumDetails);

		saveResult(file, result);

		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
				(ClassOrInterfaceTypeDetails) enumDetails);

		cidBuilder.addEnumConstant(new JavaSymbolName("ALIEN"));

		final ClassOrInterfaceTypeDetails enumDetails2 = cidBuilder.build();

		// invoke
		final String result2 = typeParsingService
				.updateAndGetCompilationUnitContents(filePath, enumDetails2);

		saveResult(file, result2, "addedConst");

		checkEnum(result2);

		assertTrue(result2.contains("MALE, FEMALE, ALIEN"));

	}

	@Test
	public void testSimpleInterfaceVoidFile() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_INTERFACE_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents,
						SIMPLE_INTERFACE_DECLARED_BY_MID, SIMPLE_INTERFACE_TYPE);

		final File voidFile = new File(file.getCanonicalFile() + ".void");

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(
						voidFile.getCanonicalPath(), simpleInterfaceDetails);

		saveResult(file, result, "-void");

		checkSimpleInterface(result, false);
	}

	@Test
	public void testSimpleClassNoChanges() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_CLASS_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, SIMPLE_CLASS_DECLARED_BY_MID,
						SIMPLE_CLASS_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						simpleInterfaceDetails);

		saveResult(file, result);

		checkSimpleClass(result);
	}

	@Test
	public void testSimpleClass2NoChanges() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_CLASS2_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, SIMPLE_CLASS2_DECLARED_BY_MID,
						SIMPLE_CLASS2_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getAbsolutePath(),
						simpleInterfaceDetails);

		saveResult(file, result);

		checkSimple2Class(result);
	}

	@Test
	public void testSimpleClass3NoChanges() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_CLASS3_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, SIMPLE_CLASS3_DECLARED_BY_MID,
						SIMPLE_CLASS3_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getAbsolutePath(),
						simpleInterfaceDetails);

		saveResult(file, result);

		checkSimple3Class(result);
	}

	@Test
	public void testRegresion_ROO_1505() throws Exception {

		// Set up
		final File file = getResource(ROO1505_CLASS_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, ROO1505_CLASS_DECLARED_BY_MID,
						ROO1505_CLASS_TYPE);

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getAbsolutePath(),
						simpleInterfaceDetails);

		saveResult(file, result);

		check_ROO_1505_Class(result);
	}

	@Test
	public void testSimpleClassAddField() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_CLASS_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, SIMPLE_CLASS_DECLARED_BY_MID,
						SIMPLE_CLASS_TYPE);

		final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(
				SIMPLE_CLASS_DECLARED_BY_MID, Modifier.PRIVATE,
				new JavaSymbolName("newFieldAddedByCode"), new JavaType(
						String.class), "\"Create by code\"");
		final ClassOrInterfaceTypeDetails newSimpleInterfaceDetails = addField(
				simpleInterfaceDetails, fieldBuilder.build());

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						newSimpleInterfaceDetails);

		saveResult(file, result, "-addedField");

		checkSimpleClass(result);

		assertTrue(result
				.contains("private String newFieldAddedByCode = \"Create by code\";"));
	}

	@Test
	public void testSimpleClassAddAnnotation() throws Exception {

		// Set up
		final File file = getResource(SIMPLE_CLASS_FILE_PATH);
		final String fileContents = getResourceContents(file);

		final ClassOrInterfaceTypeDetails simpleInterfaceDetails = typeParsingService
				.getTypeFromString(fileContents, SIMPLE_CLASS_DECLARED_BY_MID,
						SIMPLE_CLASS_TYPE);

		final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
				new JavaType(
						"org.springframework.roo.addon.tostring.RooToString"));
		final ClassOrInterfaceTypeDetails newSimpleInterfaceDetails = addAnnotation(
				simpleInterfaceDetails, annotationBuilder.build());

		// invoke
		final String result = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						newSimpleInterfaceDetails);

		saveResult(file, result, "-addedAnnotation");

		checkSimpleClass(result);

		assertTrue(result
				.contains("import org.springframework.roo.addon.tostring.RooToString;"));
		assertTrue(result.contains("@RooToString"));

		// invoke2
		final ClassOrInterfaceTypeDetails simpleInterfaceDetails2 = typeParsingService
				.getTypeFromString(result, SIMPLE_CLASS_DECLARED_BY_MID,
						SIMPLE_CLASS_TYPE);

		final String result2 = typeParsingService
				.updateAndGetCompilationUnitContents(file.getCanonicalPath(),
						simpleInterfaceDetails2);

		saveResult(file, result2, "-addedAnnotation2");

		checkSimpleClass(result2);

		assertTrue(result2
				.contains("import org.springframework.roo.addon.tostring.RooToString;"));
		assertTrue(result2.contains("@RooToString"));

	}

	public static ClassOrInterfaceTypeDetails addField(
			final ClassOrInterfaceTypeDetails ptd, final FieldMetadata field) {
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
				ptd);
		cidBuilder.addField(field);
		return cidBuilder.build();
	}

	public static ClassOrInterfaceTypeDetails addAnnotation(
			final ClassOrInterfaceTypeDetails ptd,
			final AnnotationMetadata annotation) {
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
				ptd);
		cidBuilder.addAnnotation(annotation);
		return cidBuilder.build();
	}

	public static void checkSimpleClass(final String result) {
		// check headers and import
		assertTrue(result.contains("* File header"));
		assertTrue(result.contains("package org.myPackage;"));
		assertTrue(result.contains("// Simple comment1"));
		assertTrue(result.contains("import test.importtest.pkg;"));
		assertTrue(result.contains("Comment about import"));
		assertTrue(result
				.contains("import static test.importtest.pkg.Hola.proofMethod;"));
		assertTrue(result.contains("import java.util.*;"));

		// check class declaration
		assertTrue(result.contains("* @author DiSiD Technologies"));
		assertTrue(result.contains("public class SimpleClass"));
		assertTrue(result.contains("extends OtheClass<String, Boolean>"));
		assertTrue(result.contains("implements SimpleInterface"));

		assertTrue(result.contains("== Comment in code =="));

		// Check fields
		assertTrue(result.contains("* Javadoc for field"));
		assertTrue(result.contains("private final String[] params;"));
		assertTrue(result.contains("protected Double param1 = new Double(12);"));
		assertTrue(result
				.contains("private List<String>[] listArray = new List<String>[3];"));
		assertTrue(result
				.contains("Set<String>[] setArray = new Set<String>[] { null, null, null };"));
		assertTrue(result.contains("* Enum javaDoc"));
		assertTrue(result.contains("public enum theNumbers"));
		assertTrue(result.contains("uno, dos, tres"));

		// Check constructors
		assertTrue(result.contains("@Documented(\"aaadbbbdd\")"));
		// XXX Not supported Variable Args in Spring Rooo 'public
		// SimpleClass(Double param1, String
		// params)'
		// assertTrue(result.contains("public SimpleClass(Double param1, String... params)"));
		assertTrue(result
				.contains("public SimpleClass(Double param1, String[] params, Set<String>[] setArrayParam)"));
		assertTrue(result.contains("* Public constructor"));
		assertTrue(result.contains("// Comment in public constructor"));
		assertTrue(result.contains("this.param1 = param1;"));
		assertTrue(result.contains("this.params = params;"));
		assertTrue(result.contains("* Private constructor"));
		assertTrue(result.contains("private SimpleClass()"));
		assertTrue(result.contains("* Comment in private constructor"));
		assertTrue(result.contains("this.param1 = null;"));
		assertTrue(result.contains("// Comment inline1"));
		assertTrue(result.contains("// other comment between inline"));
		assertTrue(result.contains("this.params = null;"));
		// XXX JavaParser Bug
		// assertTrue(result.contains("// Comment inline2"));

		// Check method hello declaration
		assertTrue(result.contains("* Javadoc of hello method"));
		assertTrue(result
				.contains("@Deprecated(message = \"Do not use\", more = \"Nothing\")"));
		assertTrue(result.contains("@Override"));
		assertTrue(result.contains("public Sting hello(String value)"));

		// Check method methodVoid
		assertTrue(result.contains("* methodVoid JavaDoc"));
		assertTrue(result
				.contains("void methodVoid(Double param1, String[] params, Map<String, Object>[] mapArrayParam)"));
		assertTrue(result.contains("// Comment before for"));
		assertTrue(result
				.contains("for (Map<String, Object> map : mapArrayParam)"));
		assertTrue(result.contains("// comment inside for"));
		assertTrue(result.contains("map.isEmpty()"));

		// Check content method hello
		assertTrue(result.contains("Comment block inside method"));
		assertTrue(result.contains("Simple comment inside method"));
		assertTrue(result.contains("Simple comment inside method (2nd line)"));
		assertTrue(result.contains("return \"Hello\";"));

		// Check private method declaration
		assertTrue(result.contains("<T, X> Map<T, X>"));
		assertTrue(result.contains("privateMethod"));
		assertTrue(result.contains("@ParameterAnnotation(\"xXX\")"));
		assertTrue(result.contains("Second method comment"));

		// Check subclass
		assertTrue(result.contains("* SubClass JavaDoc"));
		assertTrue(result.contains("private static class SubClass"));
		assertTrue(result.contains("String string1;"));
		assertTrue(result.contains("final theNumbers enumValue = dos;"));
		assertTrue(result.contains("int aInteger = 2;"));
		assertTrue(result.contains("long aLong = 10l;"));
		assertTrue(result.contains("SubClass(theNumbers enumValue)"));
		assertTrue(result.contains("super();"));
		assertTrue(result.contains("// if comment"));
		assertTrue(result.contains("if (enumValue.equals(this.enumValue))"));
		assertTrue(result.contains("// comment 'if' true"));
		assertTrue(result.contains("string1 = \"equals\";"));
		// XXX JavaParser Bug
		// assertTrue(result.contains("// comment inline 'if' true"));
		assertTrue(result
				.contains("else if (theNumbers.tres.equals(enumValue))"));
		assertTrue(result.contains("// comment 'elseif' true"));
		assertTrue(result.contains("string1 = \"elseif\";"));
		// XXX JavaParser Bug
		// assertTrue(result.contains("// comment inline elseif true"));
		assertTrue(result.contains("* comment in 'else'"));
		assertTrue(result.contains("string1 = \"else\";"));
		// XXX JavaParser Bug
		// assertTrue(result.contains("* comment inline else"));

		// Check newList method
		assertTrue(result
				.contains("List<List<Map<String, Iterator<Long>>>> newList(List<Map<String, Iterator<Long>>> theList)"));
		assertTrue(result
				.contains("List<List<Map<String, Iterator<Long>>>> newListResult = new ArrayList<List<Map<String, Iterator<Long>>>>();"));
		assertTrue(result.contains("newListResult.add(theList);"));
		assertTrue(result.contains("return newListResult;"));

	}

	public static void checkSimple2Class(final String result) {
		// check headers and import
		assertTrue(result.contains("package org.myPackage;"));
		assertTrue(result.contains("import java.util.*;"));

		// check class declaration
		// assertTrue(result.contains("* @author DiSiD Technologies"));
		assertTrue(result.contains("public class SimpleClass2"));
		assertTrue(result.contains("extends OtheClass<String, Boolean>"));
		assertTrue(result.contains("implements SimpleInterface"));

		// Check newList method
		assertTrue(result
				.contains("List<List<Map<String, Iterator<Long>>>> newList(List<Map<String, Iterator<Long>>> theList)"));
		assertTrue(result
				.contains("List<List<Map<String, Iterator<Long>>>> newListResult = new ArrayList<List<Map<String, Iterator<Long>>>>();"));
		assertTrue(result.contains("newListResult.add(theList);"));
		assertTrue(result.contains("return newListResult;"));
	}

	public static void checkSimple3Class(final String result) {
		// check headers and import
		assertTrue(result.contains("package org.myPackage;"));
		assertTrue(result.contains("import java.util.*;"));

		// check class declaration
		assertTrue(result.contains("public class SimpleClass3"));

		// Check int
		assertTrue(result.contains("int mInteger = 0;"));
		assertTrue(result.contains("int[] mIntegerArray;"));
		assertTrue(result.contains("int[][] mIntegerArray2;"));
		assertTrue(result.contains("int[][][] mIntegerArray3;"));

		// Check byte
		assertTrue(result.contains("byte mByte;"));
		assertTrue(result.contains("byte[] mByteArray;"));
		assertTrue(result.contains("byte[][] mByteArray2;"));
		assertTrue(result.contains("byte[][][] mByteArray3;"));

		// Check Long
		assertTrue(result.contains("Long mLongObject;"));
		assertTrue(result.contains("Long[] mLongObjectArray;"));
		assertTrue(result.contains("Long[][] mLongObjectArray2;"));
		assertTrue(result.contains("Long[][][] mLongObjectArray3;"));

		// Check Set of Strings
		assertTrue(result.contains("Set<String> mSetString;"));
		assertTrue(result.contains("Set<String>[] mSetStringArray;"));
		assertTrue(result.contains("Set<String>[][] mSetStringArray2;"));
		assertTrue(result.contains("Set<String>[][][] mSetStringArray3;"));

		// Check Map of Strings and Doubles
		assertTrue(result.contains("Map<String, Double> mMapStringDouble;"));
		assertTrue(result
				.contains("Map<String, Double>[] mMapStringDoubleArray;"));
		assertTrue(result
				.contains("Map<String, Double>[][] mMapStringDoubleArray2;"));
		assertTrue(result
				.contains("Map<String, Double>[][][] mMapStringDoubleArray3;"));

		// Check Map of Strings and Doubles
		assertTrue(result
				.contains("List<Map<String, Iterator<Double>>> mListMapStringIteratorDouble;"));
		assertTrue(result
				.contains("List<Map<String, Iterator<Double>>>[] mListMapStringIteratorDoubleArray;"));
		assertTrue(result
				.contains("List<Map<String, Iterator<Double>>>[][] mListMapStringIteratorDoubleArray2;"));
		assertTrue(result
				.contains("List<Map<String, Iterator<Double>>>[][][] mListMapStringIteratorDoubleArray3;"));
	}

	public static void check_ROO_1505_Class(final String result) {
		// Check package
		assertTrue(result.contains("package com.pet;"));

		// Check imports
		assertTrue(result.contains("import javax.persistence.Entity;"));
		assertTrue(result
				.contains("import org.springframework.roo.addon.javabean.RooJavaBean;"));
		assertTrue(result
				.contains("import org.springframework.roo.addon.tostring.RooToString;"));
		assertTrue(result
				.contains("import org.springframework.roo.addon.entity.RooEntity;"));
		assertTrue(result
				.contains("import javax.validation.constraints.NotNull;"));
		assertTrue(result.contains("import java.util.Set;"));
		assertTrue(result.contains("import javax.persistence.OneToMany;"));
		assertTrue(result.contains("import javax.persistence.CascadeType;"));

		assertTrue(result.contains("@Entity"));
		assertTrue(result.contains("@RooJavaBean"));
		assertTrue(result.contains("@RooToString"));
		assertTrue(result.contains("@RooEntity"));
		assertTrue(result.contains("public class Roo_1505 {"));
		assertTrue(result.contains("@NotNull"));
		assertTrue(result.contains("private String name;"));

		assertTrue(result
				.contains("@OneToMany(cascade = CascadeType.ALL, mappedBy = \"owner\")"));
		assertTrue(result.contains("private Set<Pet> pets = new HashSet();"));

	}

	public static void checkSimpleInterface(String result, boolean testComments) {
		if (testComments)
			assertTrue(result.contains("* File header"));
		assertTrue(result.contains("package org.myPackage;"));
		if (testComments)
			assertTrue(result.contains("// Simple comment1"));
		assertTrue(result.contains("import test.importtest.pkg;"));
		// assertTrue(result.contains("Comment about import"));
		assertTrue(result
				.contains("import static test.importtest.pkg.Hola.proofMethod;"));
		if (testComments)
			assertTrue(result.contains("* @author DiSiD Technologies"));
		assertTrue(result.contains("public interface SimpleInterface"));
		assertTrue(result
				.contains("extends Comparable<SimpleInterface>, Iterable<SimpleInterface>"));
		if (testComments)
			assertTrue(result.contains("* Javadoc of hello method"));
		assertTrue(result.contains("@Deprecated"));
		assertTrue(result.contains("String hello(String value);"));
	}

	public static void checkEnum(final String result) {
		assertTrue(result.contains("package org.myPackage;"));
		assertTrue(result.contains("public enum AEnumerate"));
		assertTrue(result.contains("MALE, FEMALE"));
	}

	private File getResource(String pathname) {
		URL res = this.getClass().getClassLoader().getResource(pathname);
		return new File(res.getPath());
	}

	private String getResourceContents(String pathName) throws IOException {
		return getResourceContents(getResource(pathName));
	}

	private String getResourceContents(File file) throws IOException {
		return FileUtils.readFileToString(file);
	}

	private void saveResult(File orgininalFile, String result, String suffix)
			throws IOException {
		if (suffix == null) {
			suffix = ".update.result";
		} else {
			suffix = ".update" + suffix + ".result";
		}
		final File resultFile = new File(orgininalFile.getParentFile(),
				FilenameUtils.getName(orgininalFile.getName()) + suffix);
		FileUtils.write(resultFile, result);
	}

	private void saveResult(File orgininalFile, String result)
			throws IOException {
		saveResult(orgininalFile, result, null);
	}

}
