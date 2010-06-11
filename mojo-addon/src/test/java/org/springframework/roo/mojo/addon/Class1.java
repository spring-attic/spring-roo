package org.springframework.roo.mojo.addon;

import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;

/**
 * Used by {@link AsmProcessingTests}.
 * 
 * @author Ben Alex
 *
 */
public class Class1 {
	@CliCommand( { "class", "dod", "test integration", "interface", "enum type", "enum constant" })
	void methodWithArrayAndManyElements() {}

	@CliCommand(value={ "singleElement" }, help="element help")
	void methodWithArrayAndSingleElement() {}

	@CliCommand(value="other", help="blah blah")
	void methodWithoutArray() {}

	@CliCommand(value = "class", help = "Creates a new Java class source file in any project path")
	public void createClass(
			@CliOption(key = "class", optionContext = "update,project", mandatory = true, help = "The name of the class to create") String name,
			@CliOption(key = "rooAnnotations", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should have common Roo annotations") boolean rooAnnotations, 
			@CliOption(key = "path", mandatory = false, unspecifiedDefaultValue = "SRC_MAIN_JAVA", specifiedDefaultValue = "SRC_MAIN_JAVA", help = "Source directory to create the class in") String path,
			@CliOption(key = "extends", mandatory = false, unspecifiedDefaultValue = "java.lang.Object", help = "The superclass (defaults to java.lang.Object)") String superclass, 
			@CliOption(key = "abstract", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Whether the generated class should be marked as abstract") boolean createAbstract, 
			@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {}
	
}
