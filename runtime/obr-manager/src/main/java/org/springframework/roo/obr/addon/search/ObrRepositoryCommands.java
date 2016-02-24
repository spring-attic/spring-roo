package org.springframework.roo.obr.addon.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Commands to manage OBR repositories
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrRepositoryCommands implements CommandMarker {

  @Reference
  private ObrRepositoryOperations operations;


  @CliCommand(value = "addon repository add", help = "Adds a new OBR Repository to ROO Shell")
  public void addRepo(
      @CliOption(key = "url", mandatory = true,
          help = "URL file that defines repository. Ex: 'http://localhost/repo/index.xml'") final String url)
      throws Exception {
    operations.addRepository(url);
  }

  @CliCommand(value = "addon repository remove",
      help = "Removes an existing OBR Repository from ROO Shell")
  public void removeRepo(
      @CliOption(key = "url", mandatory = true,
          help = "URL file that defines repository. Ex: 'http://localhost/repo/index.xml'") final String url)
      throws Exception {
    operations.removeRepo(url);
  }

  @CliCommand(value = "addon repository list", help = "Lists existing OBR Repositories")
  public void listRepos() throws Exception {
    operations.listRepos();
  }

  @CliCommand(value = "addon repository introspect",
      help = "Introspects all installed OBR Repositories and list all their addons")
  public void introspectRepos() throws Exception {
    operations.introspectRepos();
  }
}
