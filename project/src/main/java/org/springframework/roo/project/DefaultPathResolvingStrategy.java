package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileUtils;

@Component(immediate = true)
@Service
@Reference(name = "pathResolvingStrategy", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = PathResolvingStrategy.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultPathResolvingStrategy extends AbstractPathResolvingStrategy {

	// Fields
	private final Collection<PathResolvingStrategy> otherPathResolvingStrategies = new ArrayList<PathResolvingStrategy>();
	private final Map<Path, PhysicalPath> rootModulePaths = new LinkedHashMap<Path, PhysicalPath>();
	
	// ------------ OSGi component methods ----------------
	
	protected void bindPathResolvingStrategy(final PathResolvingStrategy pathResolvingStrategy) {
		if (pathResolvingStrategy != this) {
			otherPathResolvingStrategies.add(pathResolvingStrategy);
		}
	}
	
	protected void unbindPathResolvingStrategy(final PathResolvingStrategy pathResolvingStrategy) {
		otherPathResolvingStrategies.remove(pathResolvingStrategy);
	}
	
	protected void activate(final ComponentContext context) {
		super.activate(context);
		populatePaths(getRoot());
	}
	
	private void populatePaths(final String projectDirectory) {
		for (final Path subPath : Path.values()) {
			rootModulePaths.put(subPath, subPath.getRootModulePath(projectDirectory));
		}
	}

	List<PhysicalPath> getPathInformation() {
		return new ArrayList<PhysicalPath>(rootModulePaths.values());
	}
	
	// ------------ PathResolvingStrategy methods ----------------

	public String getIdentifier(final LogicalPath path, final String relativePath) {
		return FileUtils.ensureTrailingSeparator(rootModulePaths.get(path.getPath()).getLocationPath()) + relativePath;
	}

	public String getRoot(final LogicalPath contextualPath) {
		Assert.notNull(contextualPath, "Path required");
		final PhysicalPath pathInfo = rootModulePaths.get(contextualPath.getPath());
		Assert.notNull(pathInfo, "Unable to determine information for path '" + contextualPath + "'");
		final File root = pathInfo.getLocation();
		return FileUtils.getCanonicalPath(root);
	}

	protected Collection<LogicalPath> getPaths(final boolean sourceOnly) {
		final List<LogicalPath> result = new ArrayList<LogicalPath>();
		for (final PhysicalPath modulePath : rootModulePaths.values()) {
			if (!sourceOnly || modulePath.isSource()) {
				result.add(modulePath.getContextualPath());
			}
		}
		return result;
	}

	/**
	 * Locates the first {@link PhysicalPath} which can be construed as a parent
	 * of the presented identifier.
	 *
	 * @param identifier to locate the parent of (required)
	 * @return the first matching parent, or null if not found
	 */
	protected PhysicalPath getApplicablePathInformation(final String identifier) {
		Assert.notNull(identifier, "Identifier required");
		for (final PhysicalPath pi : rootModulePaths.values()) {
			final FileDetails possibleParent = new FileDetails(pi.getLocation(), null);
			if (possibleParent.isParentOf(identifier)) {
				return pi;
			}
		}
		return null;
	}

	public String getCanonicalPath(final LogicalPath path, final JavaType javaType) {
		return null;
	}

	public String getFocusedIdentifier(final Path path, final String relativePath) {
		return null;
	}

	public String getFocusedRoot(final Path path) {
		return null;
	}

	public LogicalPath getFocusedPath(final Path path) {
		return null;
	}

	public String getFocusedCanonicalPath(final Path path, final JavaType javaType) {
		return null;
	}

	public boolean isActive() {
		for (final PathResolvingStrategy otherStrategy : otherPathResolvingStrategies) {
			if (otherStrategy.isActive()) {
				return false;
			}
		}
		return true;
	}
}
