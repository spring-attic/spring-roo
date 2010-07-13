package org.springframework.roo.shell.osgi;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.SimpleParser;
import org.springframework.roo.shell.model.CommandInfo;
import org.springframework.roo.shell.model.ModelSerializer;

/**
 * OSGi component launcher for {@link SimpleParser}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate=true) // we want the background download to start ASAP
@Service
@References(value={
		@Reference(name="converter", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=Converter.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE),
		@Reference(name="command", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=CommandMarker.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
		})
public class SimpleParserComponent extends SimpleParser implements CommandMarker {

	@Reference private RepositoryAdmin repositoryAdmin;
	@Reference private ModelSerializer modelSerializer;
	private boolean obrRepositoriesDownloaded = false;
	
	private SortedMap<String,String> findBundlesOfferingCommand(String command) {
		SortedMap<String,String> result = new TreeMap<String,String>();
		for (Repository repo : repositoryAdmin.listRepositories()) {
			for (Resource resource : repo.getResources()) {
				outer:
				for (Capability capability : resource.getCapabilities()) {
					if ("shell-info-1".equals(capability.getName())) {
						Map<?,?> props = capability.getProperties();
						Object r = props.get("shell-info-1");
						if (r != null) {
							String rString = r.toString();
							List<CommandInfo> commandInfo = modelSerializer.deserializeList(rString);
							for (CommandInfo info : commandInfo) {
								for (String commandName : info.getCommandNames()) {
									if (commandName.startsWith(command) || command.startsWith(commandName)) {
										result.put(resource.getSymbolicName(), resource.getPresentationName());
										break outer;
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	protected void commandNotFound(Logger logger, String buffer) {
		logger.warning("Command '" + buffer + "' not found (for assistance press " + AbstractShell.completionKeys + " or type \"hint\" then hit ENTER)");
		if (!obrRepositoriesDownloaded) {
			logger.warning("No remote OBR repositories have been downloaded; no search of available commands performed");
			return;
		}
		SortedMap<String,String> result = findBundlesOfferingCommand(buffer);
		if (result.size() == 0) {
			if (repositoryAdmin.listRepositories().length == 0) {
				logger.warning("No remote OBR repositories registered; no search of available commands performed");
			} else {
				logger.warning("No Spring Roo add-ons were found that offer a similar command");
			}
		}
		else if (result.size() == 1) {
			logger.warning("The following Spring Roo add-on offers a similar command (to install use 'osgi obr start'):");
		} else if (result.size() > 1) {
			logger.warning("The following Spring Roo add-ons offer a similar command (to install use 'osgi obr start'):");
		}
		for (String bsn : result.keySet()) {
			logger.warning(bsn + ": " + result.get(bsn));
		}
	}

	protected void bindConverter(Converter c) {
		add(c);
	}
	
	protected void unbindConverter(Converter c) {
		remove(c);
	}
	
	protected void bindCommand(CommandMarker c) {
		add(c);
	}
	
	protected void unbindCommand(CommandMarker c) {
		remove(c);
	}
	
	protected void activate(ComponentContext context) {
		bindCommand(this);

		// Do a quick background query so we have the results cached
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					repositoryAdmin.listRepositories();
					obrRepositoriesDownloaded = true;
				} catch (RuntimeException ignore) {}
			}
		}, "OBR Eager Download");
		t.start();
	}
	
	protected void deactivate(ComponentContext context) {
		unbindCommand(this);
	}
	
	@CliCommand(value = "reference guide", help = "Writes the reference guide XML fragments (in DocBook format) into the current working directory")
	public void helpReferenceGuide() {
		super.helpReferenceGuide();
	}
	
	@CliCommand(value = "help", help = "Shows system help")
	public void obtainHelp(@CliOption(key = { "", "command" }, optionContext = "availableCommands", help = "Command name to provide help for") String buffer) {
		super.obtainHelp(buffer);
	}
	
}
