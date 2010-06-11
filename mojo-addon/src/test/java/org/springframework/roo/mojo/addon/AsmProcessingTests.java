package org.springframework.roo.mojo.addon;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.roo.shell.model.CommandInfo;


/**
 * Tests {@link AnnotationParser}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public class AsmProcessingTests {
	
	@Test
	public void processAll() throws IOException {
		String cp = System.getProperty("java.class.path");
		String[] entries = cp.split(File.pathSeparator);
		for (String entry : entries) {
			if (!entry.endsWith(".jar")) {
				List<CommandInfo> located = AnnotationParser.locateAllClassResources(entry);
				for (CommandInfo command : located) {
					System.out.println("scan: " + command);
				}
			}
		}
	}

}