package org.springframework.roo.felix.help;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Enables a user to obtain Help
 * 
 * @author Juan Carlos García
 * @since 1.3
 */
@Service
@Component
public class HelpCommands implements CommandMarker {
	
	@Reference HelpService helpService;

    @CliCommand(value = "reference guide", help = "Writes the reference guide XML fragments (in DocBook format) into the current working directory")
    public void helpReferenceGuide() {
    	helpService.helpReferenceGuide();
    }

    @CliCommand(value = "help", help = "Shows system help")
    public void obtainHelp(
            @CliOption(key = { "", "command" }, optionContext = "availableCommands", help = "Command name to provide help for") final String buffer) {

    	helpService.obtainHelp(buffer);
    }
}
