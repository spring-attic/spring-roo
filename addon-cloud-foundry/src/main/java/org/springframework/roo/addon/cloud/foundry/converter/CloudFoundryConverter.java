package org.springframework.roo.addon.cloud.foundry.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudApp;
import org.springframework.roo.addon.cloud.foundry.model.CloudAppMemoryOption;
import org.springframework.roo.addon.cloud.foundry.model.CloudControllerUrl;
import org.springframework.roo.addon.cloud.foundry.model.CloudDeployableFile;
import org.springframework.roo.addon.cloud.foundry.model.CloudFile;
import org.springframework.roo.addon.cloud.foundry.model.CloudLoginEmail;
import org.springframework.roo.addon.cloud.foundry.model.CloudUri;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperationsImpl;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.ServiceConfiguration;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class CloudFoundryConverter implements Converter {
	private static final Logger logger = Logger.getLogger(CloudFoundryConverter.class.getName());
	private static final String CREATE_OPTION = "CREATE";
	private static final String MEMORY_OPTION_SUFFIX = "MB";
	@Reference private CloudFoundrySession session;
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}

		if (CloudApp.class.equals(requiredType)) {
			return new CloudApp(value);
		} else if (CloudService.class.equals(requiredType)) {
			return session.getProvisionedService(value);
		} else if (ServiceConfiguration.class.equals(requiredType)) {
			return session.getService(value);
		} else if (CloudDeployableFile.class.equals(requiredType)) {
			if (CREATE_OPTION.equals(value)) {
				if (projectOperations instanceof MavenOperationsImpl) {
					try {
						if (projectOperations.getPathResolver() == null) {
							return null;
						}
						((MavenOperationsImpl) projectOperations).executeMvnCommand("clean package");
						String rootPath = projectOperations.getPathResolver().getRoot(Path.ROOT);
						Set<FileDetails> fileDetails = fileManager.findMatchingAntPath(rootPath + "/**/*.war");

						if (fileDetails.size() > 0) {
							FileDetails fileToDeploy = fileDetails.iterator().next();
							return new CloudDeployableFile(fileToDeploy);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				FileDetails fileToDeploy = fileManager.readFile(projectOperations.getPathResolver().getRoot(Path.ROOT) + value);
				return new CloudDeployableFile(fileToDeploy);
			}
		} else if (CloudUri.class.equals(requiredType)) {
			return new CloudUri(value);
		} else if (CloudFile.class.equals(requiredType)) {
			return new CloudFile(value);
		} else if (CloudAppMemoryOption.class.equals(requiredType)) {
			return new CloudAppMemoryOption(Integer.valueOf(value.replaceAll(MEMORY_OPTION_SUFFIX, "")));
		} else if (CloudControllerUrl.class.equals(requiredType)) {
			return new CloudControllerUrl(value);
		} else if (CloudLoginEmail.class.equals(requiredType)) {
			return new CloudLoginEmail(value);
		}
		return null;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return CloudApp.class.isAssignableFrom(requiredType) ||
				CloudService.class.isAssignableFrom(requiredType) ||
				ServiceConfiguration.class.isAssignableFrom(requiredType) ||
				CloudDeployableFile.class.isAssignableFrom(requiredType) ||
				CloudUri.class.isAssignableFrom(requiredType) ||
				CloudFile.class.isAssignableFrom(requiredType) ||
				CloudAppMemoryOption.class.isAssignableFrom(requiredType) ||
				CloudControllerUrl.class.isAssignableFrom(requiredType) ||
				CloudLoginEmail.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		if (CloudApp.class.equals(requiredType)) {
			completions.addAll(session.getApplicationNames());
		} else if (CloudService.class.equals(requiredType)) {
			completions.addAll(session.getProvisionedServices());
		} else if (ServiceConfiguration.class.equals(requiredType)) {
			completions.addAll(session.getServiceTypes());
		} else if (CloudDeployableFile.class.equals(requiredType)) {
			if (projectOperations.getPathResolver() == null) {
				logger.warning("A project has not been created please specify the full path of the file you wish to deploy");
				return false;
			}
			String rootPath = projectOperations.getPathResolver().getRoot(Path.ROOT);
			Set<FileDetails> fileDetails = fileManager.findMatchingAntPath(rootPath + "/**/*.war");

			if (fileDetails.size() == 0) {
				logger.warning("No deployable files found in the project directory. Please use the '" + CREATE_OPTION + "' option to build the war.");
				completions.add(CREATE_OPTION);
			}

			for (FileDetails fileDetail : fileDetails) {
				completions.add(fileDetail.getCanonicalPath().replaceAll(rootPath, ""));
			}
		} else if (CloudUri.class.equals(requiredType)) {
			List<String> uris = session.getBoundUrlMap().get(getOptionValue("appName", target.remainingBuffer));
			if (uris != null) {
				completions.addAll(uris);
			}
		} else if (CloudFile.class.equals(requiredType)) {
			String appName = getOptionValue("appName", target.remainingBuffer);
			String path = getOptionValue("path", target.remainingBuffer);
			if (path != null) {
				int index = path.lastIndexOf("/");
				if (index > 0) {
					path = path.substring(0, index + 1);
				} else {
					path = null;
				}
			}
			try {
				String file = session.getClient().getFile(appName, 1, path);
				List<String> options = getFileOptions(file);
				for (String option : options) {
					if (path == null) {
						path = "";
					}
					completions.add(path + option);
				}
			} catch (Exception ignored) {
			}
		} else if (CloudAppMemoryOption.class.equals(requiredType)) {
			for (Integer memoryOption : session.getApplicationMemoryOptions()) {
				completions.add(memoryOption + MEMORY_OPTION_SUFFIX);
			}
		} else if (CloudControllerUrl.class.equals(requiredType)) {
			completions.addAll(session.getStoredUrls());
		} else if (CloudLoginEmail.class.equals(requiredType)) {
			completions.addAll(session.getStoredEmails());
		}

		return false;
	}

	private List<String> getFileOptions(String files) {
		String[] lines = files.split("\n");
		List<String> options = new ArrayList<String>();
		for (String line : lines) {
			int index = line.indexOf(" ");
			if (index > 0) {
				options.add(line.substring(0, index));
			}
		}
		return options;
	}

	private String getOptionValue(String option, String buffer) {
		String[] words = buffer.split(" ");
		for (int i = 0; i < words.length; i++) {
			if (words[i].equals("--" + option)) {
				if (i + 1 < words.length) {
					return words[i + 1];
				}
			}
		}
		return null;
	}
}
