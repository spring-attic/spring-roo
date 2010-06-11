package org.springframework.roo.mojo.addon;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.springframework.roo.shell.json.model.internal.ModelSerializerImpl;
import org.springframework.roo.shell.model.CommandInfo;
import org.springframework.roo.shell.model.ModelSerializer;

/**
 * Introspects all compiled .class files in a Roo add-on for Roo Shell annotations,
 * converting these into a JSON-serialized form and adding them to the JAR manifest.
 * 
 * @goal roo-addon
 * @phase process-classes
 * @description Obtains extra Roo-specific information for the OSGi manifest
 * @requiresDependencyResolution compile
 */
public class BuildMojo extends AbstractMojo {

	private ModelSerializer serializer = new ModelSerializerImpl();
	
	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {
		String outputDirectory = project.getBuild().getOutputDirectory();
		List<CommandInfo> commands = AnnotationParser.locateAllClassResources(outputDirectory);
		//System.out.println("Output directory '" + outputDirectory + "' found " + commands.size() + " commands");
		if (commands.size() > 0) {
			project.getProperties().setProperty("Shell-Info-1", serializer.serializeList(commands));
		}
	}

}
