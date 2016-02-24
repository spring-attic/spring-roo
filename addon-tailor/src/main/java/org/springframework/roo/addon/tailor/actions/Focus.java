package org.springframework.roo.addon.tailor.actions;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.project.ProjectOperations;

/**
 * Focuses on a module with a given name. This action does not check for the
 * EXACT name, but looks for a module that contains that string. This makes
 * tailor configurations portable over projects with different names that have
 * naming conventions for their modules and can match for certain patterns in
 * modules.
 * <p/>
 * Advanced feature: <br/>
 * Imagine the case that there are 2 modules named "projectname-domain" and
 * "projectname-domain-test". <br/>
 * For these cases, the match string can also be a comma-separated list, e.g.
 * "domain,test". <br/>
 * Each of the members of that list can start with a "/" to indicate that this
 * member must NOT be present in the module to match, e.g. "domain,/test".
 * 
 * @author Vladimir Tihomirov
 * @author Birgitta Boeckeler
 */
@Component
@Service
public class Focus extends AbstractAction {

    @Reference protected ProjectOperations projectOperations;

    private final String baseCommand = "module focus --moduleName ";

    @Override
    public void executeImpl(final CommandTransformation trafo,
            final ActionConfig config) {
        if ("~".equals(config.getModule())) {
            trafo.addOutputCommand(baseCommand, "~");
            return;
        }

        // If a command is tailored right after the shell was started, sometimes
        // the module names are not yet loaded
        if (projectOperations.getModuleNames().isEmpty()) {
            throw new IllegalStateException(
                    "Module names not loaded, please try again.");
        }

        // If comma-separated list: Module name will be checked against both
        // those values
        final String[] matches = config.getModule().split(",");

        // If not root: Check if module name actually exists
        for (final String moduleName : projectOperations.getModuleNames()) {
            // if (StringUtils.isEmpty(moduleName)) {
            // continue;
            // }
            boolean matchesAll = true;
            for (final String matche : matches) {
                final String match = matche;
                if (match.startsWith("/")
                        && moduleName.contains(match.substring(1))) {
                    matchesAll = false;
                    break;
                }
                else if (!match.startsWith("/") && !moduleName.contains(match)) {
                    matchesAll = false;
                    break;
                }
            }
            if (matchesAll) {
                trafo.addOutputCommand(baseCommand, moduleName);
                return;
            }
        }
    }

    public String getDescription(final ActionConfig config) {
        return "Focusing: " + config.getModule();
    }

    public boolean isValid(final ActionConfig config) {
        return config != null && StringUtils.isNotBlank(config.getModule());
    }

}
