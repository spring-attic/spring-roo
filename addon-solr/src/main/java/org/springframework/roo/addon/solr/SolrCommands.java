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
 *
 */
@Component
@Service
public class SolrCommands implements CommandMarker {
	
	@Reference private SolrOperations searchOperations;
	
	@CliAvailabilityIndicator({"solr setup","solr add","solr all"})
	public boolean isInstallJmsAvailable() {
		return searchOperations.isInstallSearchAvailable();
	}
	
	@CliCommand(value="solr setup", help="Install a support for Solr search integration")
	public void setup(@CliOption(key={"searchServerUrl"}, mandatory=false, unspecifiedDefaultValue="http://localhost:8983/solr", specifiedDefaultValue="http://localhost:8983/solr", help="The Url of the Solr search server") String searchServerUrl) {
		searchOperations.setupConfig(searchServerUrl);
	}
	
	@CliCommand(value="solr add", help="Make target type searchable")
	public void setup(@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The target type which is made searchable") JavaType javaType) {
		searchOperations.addSearch(javaType);
	}
	
	@CliCommand(value="solr all", help="Make all elegible project types searchable")
	public void setup() {
		searchOperations.addAll();
	}
}