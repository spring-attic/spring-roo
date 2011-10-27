package org.springframework.roo.project;

import static org.springframework.roo.project.maven.Pom.DEFAULT_RESOURCES_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_SOURCE_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_SPRING_CONFIG_ROOT;
import static org.springframework.roo.project.maven.Pom.DEFAULT_TEST_RESOURCES_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_TEST_SOURCE_DIRECTORY;
import static org.springframework.roo.project.maven.Pom.DEFAULT_WAR_SOURCE_DIRECTORY;

import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.StringUtils;

/**
 * Common file paths used in Maven projects.
 *
 * <p>
 * {@link PathResolver}s can convert these paths to and from physical locations.
 *
 * @author Ben Alex
 * @since 1.0
 */
public enum Path {

	SRC_MAIN_JAVA {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			if (pom != null && StringUtils.hasText(pom.getSourceDirectory())) {
				return pom.getSourceDirectory();
			}
			return DEFAULT_SOURCE_DIRECTORY;
		}
	},
	
	SRC_MAIN_RESOURCES {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			return DEFAULT_RESOURCES_DIRECTORY;
		}
	},
	
	SRC_TEST_JAVA {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			if (pom != null && StringUtils.hasText(pom.getTestSourceDirectory())) {
				return pom.getTestSourceDirectory();
			}
			return DEFAULT_TEST_SOURCE_DIRECTORY;
		}
	},
	
	SRC_TEST_RESOURCES {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			return DEFAULT_TEST_RESOURCES_DIRECTORY;
		}
	},
	
	SRC_MAIN_WEBAPP {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			return DEFAULT_WAR_SOURCE_DIRECTORY;
		}
	},
	
	ROOT {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			return "";
		}
	},
	
	SPRING_CONFIG_ROOT {
		@Override
		public String getPathRelativeToPom(final Pom pom) {
			return DEFAULT_SPRING_CONFIG_ROOT;
		}
	};

	public ContextualPath contextualize() {
		return ContextualPath.getInstance(this);
	}

	public ContextualPath contextualize(final String context) {
		return ContextualPath.getInstance(this, context);
	}

	/**
	 * Returns the physical path of this logical {@link Path} relative to the
	 * given POM
	 * 
	 * @param pom can be <code>null</code>
	 * @return
	 */
	public abstract String getPathRelativeToPom(final Pom pom);
}
