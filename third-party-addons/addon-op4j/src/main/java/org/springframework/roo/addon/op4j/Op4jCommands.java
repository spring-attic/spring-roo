package org.springframework.roo.addon.op4j;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for addon-op4j.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class Op4jCommands implements CommandMarker {

    @Reference private Op4jOperations op4jOperations;

    @CliCommand(value = "op4j add", help = "Some helpful description")
    public void add(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The java type to apply the RooOp4j annotation to") final JavaType target) {

        op4jOperations.annotateType(target);
    }

    @CliAvailabilityIndicator({ "op4j setup", "Op4j add" })
    public boolean isOp4jAvailable() {
        return op4jOperations.isOp4jInstallationPossible();
    }

    @CliCommand(value = "op4j setup", help = "Setup Op4j addon")
    public void setup() {
        op4jOperations.setup();
    }
}