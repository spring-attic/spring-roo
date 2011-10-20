package org.springframework.roo.project;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

public class ContextualPath {

	private Path path;
	private String module;

	public static ContextualPath getInstance(Path path, String module) {
		return new ContextualPath(path, module);
	}

	public static ContextualPath getInstance(Path path) {
		return new ContextualPath(path);
	}

	public static ContextualPath getInstance(String contextualPath) {
		return new ContextualPath(contextualPath);
	}

	/**
	 * Creates a name with the specified string.
	 *
	 * <p>
	 * A name cannot contain a question mark character, due to it being a reserved character for metadata
	 * identification string tokenization.
	 *
	 * @param name the name (required and cannot contain a "?" character)
	 * @param basePath
	 */
	private ContextualPath(Path path, String module) {
		Assert.notNull(path, "Path required");
		if (module == null) {
			module = "";
		}
		this.path = path;
		this.module = module;
	}

	private ContextualPath(Path path) {
		this(path, null);
	}

	private ContextualPath(String contextualPath) {
		Assert.hasText(contextualPath, "Context path required");
		int index = contextualPath.indexOf('|');
		if (index != -1) {
			this.path = Path.valueOf(contextualPath.substring(index + 1, contextualPath.length()));
			this.module = contextualPath.substring(0, index);
		} else {
			path = Path.valueOf(contextualPath);
		}
	}

	public String getName() {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.hasText(module)) {
			sb.append(module).append("|");
		}
		sb.append(path);
		return sb.toString();
	}

	public Path getPath() {
		return path;
	}

	public String getModule() {
		if (module == null) {
			module = "";
		}
		return module;
	}

	public int hashCode() {
		return getName().hashCode();
	}

	public boolean equals(Object obj) {
		return obj instanceof ContextualPath && this.compareTo((ContextualPath) obj) == 0;
	}

	public int compareTo(ContextualPath o) {
		if (o == null) {
			throw new NullPointerException();
		}
		return getName().compareTo(o.getName());
	}

	public final String toString() {
		return getName();
	}
}
