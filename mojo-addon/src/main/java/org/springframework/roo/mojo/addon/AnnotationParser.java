package org.springframework.roo.mojo.addon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.springframework.roo.shell.model.CommandInfo;
import org.springframework.roo.shell.model.CommandOption;
import org.springframework.roo.shell.model.CommandInfo.CommandInfoBuilder;

/**
 * Locates <code>@CliCommand</code> and <code>@CliOption</code> annotations in .class files,
 * converting these into {@link CommandInfo} objects.
 * 
 * @author Ben Alex
 */
public class AnnotationParser {

	/**
	 * Scans a particular directory, including all sub-directories, for .class files.
	 * For each class file, checks for shell annotations and builds a consolidated
	 * list of all located {@link CommandInfo}.
	 * 
	 * @param directory the top-level directory to scan (required)
	 * @return a list of located annotation metadata (never null, but may be empty)
	 */
	public static List<CommandInfo> locateAllClassResources(String directory) {
		List<CommandInfo> soFar = new ArrayList<CommandInfo>();
		locateAllClassResources(soFar, new File(directory));
		return soFar;
	}

	private static void locateAllClassResources(List<CommandInfo> soFar, File file) {
		if (!file.exists() || !file.isDirectory()) return;
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				locateAllClassResources(soFar, f);
			} else if (f.getName().endsWith(".class")) {
				FileInputStream fis;
				try {
					fis = new FileInputStream(f);
					List<CommandInfo> data = parseShellAnnotationsInClassFile(fis);
					soFar.addAll(data);
				} catch (FileNotFoundException skipAndIgnore) {
				}
			}
		}
	}

	/**
	 * Parses the presented bytecode and locates all Roo @CliCommand and @CliOption annotations. These are then converted
	 * into a list of {@link CommandInfo} instances for easier handling.
	 * 
	 * @param input for a valid class file (required)
	 * @return a list of zero or more elements (never returns null)
	 */
	@SuppressWarnings("unchecked") 
	public static List<CommandInfo> parseShellAnnotationsInClassFile(InputStream input) {
		if (input == null) throw new IllegalArgumentException("Input stream must be bytecode for a valid class file");
		List<CommandInfo> result = new ArrayList<CommandInfo>();

		ClassReader cr;
		try {
			cr = new ClassReader(input);
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		List<MethodNode> methods = cn.methods;
		if (methods != null) {
			for (MethodNode mn : methods) {
				List<AnnotationNode> annotations = mn.visibleAnnotations;
				if (annotations != null) {
					for (AnnotationNode annotation : annotations) {
						String annotationType = Type.getType(annotation.desc).getInternalName();
						if ("org/springframework/roo/shell/CliCommand".equals(annotationType)) {
							// We have a @CliCommand
							CommandInfoBuilder builder = CommandInfo.builder();

							// Start by producing the CommandInfo details first (ie command names, help text)
							List<Object> values = annotation.values;
							if (values != null) {
								String key = null;
								for (Object value : values) {
									if (key == null) {
										// This is a key
										key = value.toString();
									} else {
										// This is a @CliCommand value
										if ("value".equals(key)) {
											if (value instanceof Collection) {
												for (Object element : (Collection<?>) value) {
													if (element instanceof String) {
														builder.addCommandName(element.toString());
													}
												}
											}
										} else if ("help".equals(key)) {
											if (value instanceof String) {
												builder.setHelp(value.toString());
											}
										}
										key = null;
									}
								}
							}

							// Next let's check for @CliOption values on the individual parameters
							List<AnnotationNode>[] parameterAnnotations = mn.visibleParameterAnnotations;
							if (parameterAnnotations != null) {
								for (List<AnnotationNode> list : parameterAnnotations) {
									for (AnnotationNode element : list) {
										String newType = Type.getType(element.desc).getInternalName();
										if ("org/springframework/roo/shell/CliOption".equals(newType)) {
											List<String> keys = new ArrayList<String>();
											boolean mandatory = false;
											String unspecifiedDefaultValue = "__NULL__";
											String specifiedDefaultValue = "__NULL__";
											String help = "";

											List<Object> optionValues = element.values;
											if (optionValues != null) {
												String key = null;
												for (Object value : optionValues) {
													if (key == null) {
														// This is a key
														key = value.toString();
													} else {
														// This is a @CliOption value
														if ("key".equals(key)) {
															if (value instanceof Collection) {
																for (Object e : (Collection<?>) value) {
																	if (e instanceof String) {
																		keys.add(e.toString());
																	}
																}
															}
														} else if ("help".equals(key)) {
															if (value instanceof String) {
																help = value.toString();
															}
														} else if ("mandatory".equals(key)) {
															if (value instanceof Boolean) {
																mandatory = ((Boolean) value).booleanValue();
															}
														} else if ("unspecifiedDefaultValue".equals(key)) {
															if (value instanceof String) {
																unspecifiedDefaultValue = value.toString();
															}
														} else if ("specifiedDefaultValue".equals(key)) {
															if (value instanceof String) {
																specifiedDefaultValue = value.toString();
															}
														}
														key = null;
													}
												}
											}
											String[] optionNames = keys.toArray(new String[] {});
											builder.addCommandOption(new CommandOption(mandatory, specifiedDefaultValue, unspecifiedDefaultValue, help, optionNames));
										}
									}
								}
							}

							result.add(builder.build());
						}
					}
				}
			}
		}

		return result;
	}
}
