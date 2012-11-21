package org.springframework.roo.addon.solr;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands for the 'solr search' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class SolrCommands implements CommandMarker {

    @Reference private SolrOperations solrOperations;

    @CliAvailabilityIndicator({ "solr setup" })
    public boolean setupCommandAvailable() {
        return solrOperations.isSolrInstallationPossible();
    }

    @CliCommand(value = "solr add", help = "Make target type searchable")
    public void solrAdd(
            @CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The type to be made searchable") final JavaType javaType) {

        solrOperations.addSearch(javaType);
    }

    @CliCommand(value = "solr all", help = "Make all eligible project types searchable")
    public void solrAll() {
        solrOperations.addAll();
    }

    @CliAvailabilityIndicator({ "solr add", "solr all" })
    public boolean solrCommandAvailable() {
        return solrOperations.isSearchAvailable();
    }

    @CliCommand(value = "solr setup", help = "Install support for Solr search integration")
    public void solrSetup(
            @CliOption(key = { "searchServerUrl" }, mandatory = false, unspecifiedDefaultValue = "http://localhost:8983/solr", specifiedDefaultValue = "http://localhost:8983/solr", help = "The URL of the Solr search server") final String searchServerUrl) {
        solrOperations.setupConfig(searchServerUrl);
    }
}