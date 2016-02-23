package org.springframework.roo.project;

import static org.springframework.roo.file.monitor.event.FileOperation.CREATED;
import static org.springframework.roo.file.monitor.event.FileOperation.DELETED;
import static org.springframework.roo.file.monitor.event.FileOperation.MONITORING_FINISH;
import static org.springframework.roo.file.monitor.event.FileOperation.MONITORING_START;
import static org.springframework.roo.file.monitor.event.FileOperation.RENAMED;
import static org.springframework.roo.file.monitor.event.FileOperation.UPDATED;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.DirectoryMonitoringRequest;
import org.springframework.roo.file.monitor.MonitoringRequest;
import org.springframework.roo.file.monitor.NotifiableFileMonitorService;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.file.undo.UndoManager;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;

/**
 * 
 */
@Component
@Service
public class ProjectPathMonitoringInitializer implements MetadataNotificationListener {

  private static final FileOperation[] MONITORED_OPERATIONS = {MONITORING_START, MONITORING_FINISH,
      CREATED, RENAMED, UPDATED, DELETED};

  @Reference
  private NotifiableFileMonitorService fileMonitorService;
  @Reference
  private PathResolver pathResolver;
  private boolean pathsRegistered;
  @Reference
  private UndoManager undoManager;

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * </ul>
   */
  protected void activate(final ComponentContext context) {
    this.registryTracker = new MetadataDependencyRegistryTracker(context.getBundleContext(), this);
    this.registryTracker.open();
  }

  /**
   * This service is being deactivated so unregister upstream-downstream 
   * dependencies, triggers, matchers and listeners.
   * 
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    this.registryTracker.close();
  }

  private void monitorPathIfExists(final LogicalPath logicalPath) {
    final String canonicalPath = pathResolver.getRoot(logicalPath);
    // The path can be blank if a sub-folder contains a POM that doesn't
    // belong to a module
    if (StringUtils.isNotBlank(canonicalPath)) {
      final File directory = new File(canonicalPath);
      if (directory.isDirectory()) {
        final MonitoringRequest request =
            new DirectoryMonitoringRequest(directory, true, MONITORED_OPERATIONS);
        new UndoableMonitoringRequest(undoManager, fileMonitorService, request, true);
      }
    }
  }

  private void monitorProjectPaths() {
    for (final LogicalPath logicalPath : pathResolver.getPaths()) {
      if (requiresMonitoring(logicalPath)) {
        monitorPathIfExists(logicalPath);
      }
    }
  }

  public void notify(final String upstreamDependency, final String downstreamDependency) {
    if (pathsRegistered) {
      return;
    }
    monitorProjectPaths();
    pathsRegistered = true;
  }

  private boolean requiresMonitoring(final LogicalPath logicalPath) {
    if (logicalPath.isProjectRoot()) {
      return false; // already monitored by ProcessManager
    }
    if (StringUtils.isBlank(logicalPath.getModule())) {
      return true; // non-root path within root module
    }
    return logicalPath.isModuleRoot();
  }
}
