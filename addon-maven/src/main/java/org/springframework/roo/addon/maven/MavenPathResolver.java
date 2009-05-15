package org.springframework.roo.addon.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.process.manager.internal.InitialMonitoringRequest;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.internal.AbstractPathResolver;
import org.springframework.roo.project.internal.PathInformation;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;

@ScopeDevelopment
public class MavenPathResolver extends AbstractPathResolver {

	private List<PathInformation> pathInformation = new ArrayList<PathInformation>();

	public MavenPathResolver(InitialMonitoringRequest initialMonitoringRequest) {
		File root = initialMonitoringRequest.getMonitoringRequest().getFile();
		pathInformation.add(new PathInformation(Path.SRC_MAIN_JAVA, true, new File(root, "src/main/java")));
		pathInformation.add(new PathInformation(Path.SRC_MAIN_RESOURCES, true, new File(root, "src/main/resources")));
		pathInformation.add(new PathInformation(Path.SRC_TEST_JAVA, true, new File(root, "src/test/java")));
		pathInformation.add(new PathInformation(Path.SRC_TEST_RESOURCES, true, new File(root, "src/test/resources")));
		pathInformation.add(new PathInformation(Path.SRC_MAIN_WEBAPP, false, new File(root, "src/main/webapp")));
		pathInformation.add(new PathInformation(Path.ROOT, false, root));
		init();
	}

	@Override
	protected List<PathInformation> getPathInformation() {
		return pathInformation;
	}

	
}
