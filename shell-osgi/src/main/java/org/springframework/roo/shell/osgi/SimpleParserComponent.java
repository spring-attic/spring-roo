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
import org.osgi.service.obr.Resource;
import org.springframework.roo.obr.AddOnFinder;
import org.springframework.roo.obr.AddOnSearchManager;
import org.springframework.roo.obr.ObrResourceFinder;
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
@Component
@Service
@References(value={
	@Reference(name = "converter", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = Converter.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE), 
	@Reference(name = "command", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = CommandMarker.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
})
public class SimpleParserComponent extends SimpleParser implements CommandMarker, AddOnFinder {
	@Reference private ObrResourceFinder obrResourceFinder;
	@Reference private AddOnSearchManager addOnSearchManager;
	@Reference private ModelSerializer modelSerializer;
	
	public SortedMap<String, String> findAddOnsOffering(String command) {
		SortedMap<String, String> result = new TreeMap<String, String>();
		for (Resource resource : obrResourceFinder.getKnownResources()) {
			outer: for (Capability capability : resource.getCapabilities()) {
				if ("shell-info-1".equals(capability.getName())) {
					Map<?, ?> props = capability.getProperties();
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
		return result;
	}

	public String getFinderTargetSingular() {
		return "command";
	}

	public String getFinderTargetPlural() {
		return "commands";
	}

	@Override
	protected void commandNotFound(Logger logger, String buffer) {
		logger.warning("Command '" + buffer + "' not found (for assistance press " + AbstractShell.completionKeys + " or type \"hint\" then hit ENTER)");
		addOnSearchManager.completeAddOnSearch(buffer, this);
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
