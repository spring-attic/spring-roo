package org.springframework.roo.project;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;

/**
 * Automatically upgrades a Spring Roo annotation JAR to the current version of Roo.
 * If the annotation JAR is equal to or newer than the version of Roo running, the
 * upgrade service makes no changes.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
public class AutomaticProjectUpgradeService implements MetadataNotificationListener {
	private static final String MY_BUNDLE_SYMBOLIC_NAME = AutomaticProjectUpgradeService.class.getPackage().getName();
	@Reference private ProjectOperations projectOperations;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	private VersionInfo bundleVersionInfo = null;
	
	private static final String PROJECT_METADATA_IDENTIFIER = ProjectMetadata.getProjectIdentifier();
	
	protected void activate(ComponentContext componentContext) {
		metadataDependencyRegistry.addNotificationListener(this);
		for (Bundle b : componentContext.getBundleContext().getBundles()) {
			if (!MY_BUNDLE_SYMBOLIC_NAME.equals(b.getSymbolicName())) {
				continue;
			}
			Object v = b.getHeaders().get("Bundle-Version");
			if (v != null) {
				String version = v.toString();
				this.bundleVersionInfo = extractVersionInfoFromString(version);
			}
			break;
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		metadataDependencyRegistry.removeNotificationListener(this);
	}
	
	/**
	 * Extracts the version information from the string. Never throws an exception.
	 * 
	 * @param version to extract from (can be null or empty)
	 * @return the version information or null if it was not in a normal form
	 */
	private VersionInfo extractVersionInfoFromString(String version) {
		if (version == null || version.length() == 0) {
			return null;
		}
		
		String[] ver = version.split("\\.");
		try {
			if (ver.length == 4) {
				VersionInfo result = new VersionInfo();
				result.major = new Integer(ver[0]);
				result.minor = new Integer(ver[1]);
				result.patch = new Integer(ver[2]);
				result.qualifier = ver[3];
				return result;
			}
		} catch (RuntimeException e) {
			return null;
		}
		return null;
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (bundleVersionInfo != null && upstreamDependency.equals(PROJECT_METADATA_IDENTIFIER)) {
			// Project Metadata changed.
			ProjectMetadata md = (ProjectMetadata) metadataService.get(PROJECT_METADATA_IDENTIFIER);
			if (md == null) {
				return;
			}
			
			Set<Property> results = md.getPropertiesExcludingValue(new Property("roo.version"));
			for (Property existingProperty : results) {
				VersionInfo rooVersion = extractVersionInfoFromString(existingProperty.getValue());
				if (rooVersion != null) {
					if (rooVersion.compareTo(bundleVersionInfo) < 0) {
						Property newProperty = new Property(existingProperty.getName(), bundleVersionInfo.toString());
						projectOperations.addProperty(newProperty);
						break;
					}
				}
			}
			
		}
	}
	
	private class VersionInfo implements Comparable<VersionInfo> {
		private Integer major = 0;
		private Integer minor = 0;
		private Integer patch = 0;
		private String qualifier = "";
		
		public int compareTo(VersionInfo v) {
			if (v == null) {
				throw new NullPointerException();
			}
			int result = major.compareTo(v.major);
			if (result != 0) {
				return result;
			}
			result = minor.compareTo(v.minor);
			if (result != 0) {
				return result;
			}
			result = patch.compareTo(v.patch);
			if (result != 0) {
				return result;
			}
			result = qualifier.compareTo(v.qualifier);
			if (result != 0) {
				return result;
			}
			return 0;
		}

		public String toString() {
			return major + "." + minor+ "." + patch + "." + qualifier;
		}
		
	}
}
