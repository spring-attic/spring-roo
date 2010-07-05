package org.springframework.roo.shell.osgi;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.SimpleParser;

/**
 * OSGi component launcher for {@link SimpleParser}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
@References(value={
		@Reference(name="converter", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=Converter.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE),
		@Reference(name="command", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=CommandMarker.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
		})
public class SimpleParserComponent extends SimpleParser implements CommandMarker {

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
