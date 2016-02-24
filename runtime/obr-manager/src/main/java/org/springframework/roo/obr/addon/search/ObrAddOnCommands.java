package org.springframework.roo.obr.addon.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.api.AddOnSearch.SearchType;

/**
 * Commands to manage Addons on OBR repositories
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrAddOnCommands implements CommandMarker {

  @Reference
  private ObrAddOnSearchOperations operations;


  @CliCommand(value = "addon info bundle",
      help = "Provide information about a specific Spring Roo Add-on")
  public void infoBundle(
      @CliOption(key = "bundleSymbolicName", mandatory = true,
          help = "The bundle symbolic name for the add-on of interest") final ObrAddOnBundleSymbolicName bsn) {

    operations.addOnInfo(bsn);
  }

  @CliCommand(value = "addon install bundle", help = "Install Spring Roo Add-on")
  public void installBsn(
      @CliOption(key = "bundleSymbolicName", mandatory = true,
          help = "The bundle symbolic name for the add-on of interest") final ObrAddOnBundleSymbolicName bsn) {
    operations.installAddOn(bsn);
  }

  @CliCommand(value = "addon install url", help = "Install Spring Roo Add-on using url")
  public void installByUrl(@CliOption(key = "url", mandatory = true,
      help = "The url for the add-on of interest") final String url) {
    operations.installAddOnByUrl(url);
  }

  @CliCommand(value = "addon remove", help = "Remove Spring Roo Add-on")
  public void remove(@CliOption(key = "bundleSymbolicName", mandatory = true,
      help = "The bundle symbolic name for the add-on of interest") final BundleSymbolicName bsn) {

    operations.removeAddOn(bsn);
  }

  @CliCommand(value = "addon list", help = "List all installed addons")
  public void list() {

    operations.list();
  }

  @CliCommand(value = "addon search", help = "Search all known Spring Roo Add-ons")
  public void search(
      @CliOption(key = "requiresCommand", mandatory = true,
          help = "Only display add-ons in search results that offer this command") final String requiresCommand) {

    operations.searchAddOns(requiresCommand, SearchType.ADDON);
  }

}
