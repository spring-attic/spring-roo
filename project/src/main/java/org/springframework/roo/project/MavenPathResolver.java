package org.springframework.roo.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.MonitoringRequest;

/**
 * Resolves paths using the typical Maven directory conventions.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class MavenPathResolver extends AbstractPathResolver {
	private List<PathInformation> pathInformation = new ArrayList<PathInformation>();

	protected void activate(ComponentContext context) {
		// TODO CD move constant to proper location
		String workingDir = context.getBundleContext().getProperty("roo.working.directory");
		File root = MonitoringRequest.getInitialMonitoringRequest(workingDir).getFile();
		pathInformation.add(new PathInformation(Path.SRC_MAIN_JAVA, true, new File(root, "src/main/java")));
		pathInformation.add(new PathInformation(Path.SRC_MAIN_RESOURCES, true, new File(root, "src/main/resources")));
		pathInformation.add(new PathInformation(Path.SRC_TEST_JAVA, true, new File(root, "src/test/java")));
		pathInformation.add(new PathInformation(Path.SRC_TEST_RESOURCES, true, new File(root, "src/test/resources")));
		pathInformation.add(new PathInformation(Path.SRC_MAIN_WEBAPP, false, new File(root, "src/main/webapp")));
		pathInformation.add(new PathInformation(Path.ROOT, false, root));
		pathInformation.add(new PathInformation(Path.SPRING_CONFIG_ROOT,false, new File(root, "src/main/resources/META-INF/spring")));
		init();
	}

	@Override
	protected List<PathInformation> getPathInformation() {
		return pathInformation;
	}
}
