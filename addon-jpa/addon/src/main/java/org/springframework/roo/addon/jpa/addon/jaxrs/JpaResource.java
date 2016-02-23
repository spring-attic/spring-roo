package org.springframework.roo.addon.jpa.addon.jaxrs;

import java.io.InputStream;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.JdbcDatabase;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.OrmProvider;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.rest.publisher.ResourceMarker;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * TODO
 * 
 * @author Juan Carlos García
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
@Component
@Service
@Path("persistence")
public class JpaResource implements ResourceMarker {

  private BundleContext context;
  private static final Logger LOGGER = HandlerUtils.getLogger(JpaResource.class);

  private Shell shell;
  private JpaOperations jpaOperations;
  private FileManager fileManager;
  private PathResolver pathResolver;

  protected void activate(final ComponentContext cContext) {
    this.context = cContext.getBundleContext();
  }

  /**
   * Method that get available OrmProviders
   * 
   * @param out
   */
  @GET
  @Path("providers")
  @Produces("application/json")
  public JsonArray getOrmProviders() {
    JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

    // Getting all available OrmProviders
    OrmProvider[] providers = OrmProvider.values();

    for (OrmProvider provider : providers) {
      JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
      jsonBuilder.add("providerName", provider.name());
      jsonArrayBuilder.add(jsonBuilder);
    }

    // Returning JSON
    JsonArray jsonArray = jsonArrayBuilder.build();
    return jsonArray;
  }

  /**
   * Method that get available Databases
   * 
   * @param out
   */
  @GET
  @Path("databases")
  @Produces("application/json")
  public JsonArray getJDBCDatabases() {
    JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

    // Getting all available JdbcDatabase
    JdbcDatabase[] databases = JdbcDatabase.values();

    for (JdbcDatabase database : databases) {
      JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
      jsonBuilder.add("databaseName", database.name());
      jsonArrayBuilder.add(jsonBuilder);
    }

    // Returning JSON
    JsonArray jsonArray = jsonArrayBuilder.build();
    return jsonArray;
  }

  /**
   * Method that obtains current JPA Persistence configuration
   * 
   * @param out
   */
  @GET
  @Produces("application/json")
  public JsonObject getPersistenceInfo() {

    String persistenceProvider = "";
    String database = "";
    String databaseUrl = "";
    String username = "";
    String password = "";

    // Getting current persistence provider
    String persistenceFile =
        getPathResolver().getFocusedIdentifier(
            org.springframework.roo.project.Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");

    if (getFileManager().exists(persistenceFile)) {

      // Reading persistence.xml
      final InputStream inputStream = getFileManager().getInputStream(persistenceFile);
      final Document docXml = XmlUtils.readXml(inputStream);
      final Element document = docXml.getDocumentElement();

      Element providerElement = XmlUtils.findFirstElement("persistence-unit/provider", document);

      String adapter = providerElement.getTextContent();

      // Getting provider
      for (OrmProvider provider : OrmProvider.values()) {
        if (provider.getAdapter().equals(adapter)) {
          persistenceProvider = provider.name();
        }
      }

      // With DATANUCLEUS provider, doesn't exists database.properties,
      // so we need to get properties from persistence.xml
      if (persistenceProvider.equals("DATANUCLEUS")) {
        Element propertiesElement =
            XmlUtils.findFirstElement("persistence-unit/properties", document);

        NodeList properties = propertiesElement.getChildNodes();
        for (int i = 0; i < properties.getLength(); i++) {
          if (properties.item(i).hasAttributes()) {
            Element property = (Element) properties.item(i);
            String nameAttr = property.getAttribute("name");
            String valueAttr = property.getAttribute("value");
            if (nameAttr.equals("datanucleus.ConnectionURL")) {
              databaseUrl = valueAttr;
            } else if (nameAttr.equals("datanucleus.ConnectionUserName")) {
              username = valueAttr;
            } else if (nameAttr.equals("datanucleus.ConnectionPassword")) {
              password = valueAttr;
            }
          }
        }

        for (int i = 0; i < properties.getLength(); i++) {
          if (properties.item(i).hasAttributes()) {
            Element property = (Element) properties.item(i);
            String nameAttr = property.getAttribute("name");
            String valueAttr = property.getAttribute("value");
            if (nameAttr.equals("datanucleus.ConnectionDriverName")) {
              database = getDatabaseByDriverClassName(valueAttr, databaseUrl);
            }
          }
        }
      }

      if (!persistenceProvider.equals("DATANUCLEUS")) {
        // Getting database properties
        SortedSet<String> databaseProperties = getJpaOperations().getDatabaseProperties(null);

        // Getting database URL
        for (String databaseProperty : databaseProperties) {
          String[] keyValue = databaseProperty.split("=");
          String key = keyValue[0].trim();
          String value = keyValue[1].trim();
          if (key.equals("database.url")) {
            databaseUrl = value;
          }

        }

        for (String databaseProperty : databaseProperties) {
          String[] keyValue = databaseProperty.split("=");
          String key = keyValue[0].trim();
          String value = keyValue[1].trim();
          if (key.equals("database.driverClassName")) {
            database = getDatabaseByDriverClassName(value, databaseUrl);
          } else if (key.equals("database.username")) {
            username = value;
          } else if (key.equals("database.password")) {
            password = value;
          }
        }
      }
    }

    // Returning JSON
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    jsonBuilder.add("persistenceProvider", persistenceProvider).add("database", database)
        .add("databaseUrl", databaseUrl).add("username", username).add("password", password);

    JsonObject jsonObject = jsonBuilder.build();
    return jsonObject;
  }

  /**
   * Method to handle POST request of /persistence service
   * 
   */
  @POST
  @Consumes("application/x-www-form-urlencoded")
  @Produces("application/json")
  public JsonObject setupJpa(@FormParam("providerName") String provider,
      @FormParam("database") String database, @FormParam("username") String username,
      @FormParam("password") String password) {

    // Execute project command
    // TODO: Replace with JPA command
    String persistenceCommand = "jpa setup --provider \"" + provider + "\" --database " + database;

    if (username != null || !"".equals(username)) {
      persistenceCommand = persistenceCommand.concat(" --userName ").concat(username);
    }

    if (password != null || !"".equals(password)) {
      persistenceCommand = persistenceCommand.concat(" --password ").concat(password);
    }

    boolean status = getShell().executeCommand(persistenceCommand);

    String message = "Persistence was configured to use '" + provider + "' and '" + database + "'!";
    if (!status) {
      message = "Error configuring persistence.";
    }

    // Returning JSON
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    jsonBuilder.add("success", status).add("message", message);

    JsonObject jsonObject = jsonBuilder.build();
    return jsonObject;
  }

  /**
   * Method that obtains Database by DriverClassName
   * 
   * @param driverClassName
   * @param databaseUrl
   * @return
   */
  private String getDatabaseByDriverClassName(String driverClassName, String databaseUrl) {

    // Getting all available databases
    JdbcDatabase[] availableDatabases = JdbcDatabase.values();

    for (JdbcDatabase database : availableDatabases) {
      if (database.getDriverClassName().equals(driverClassName)) {
        // If database type is HYPERSONIC_IN_MEMORY or
        // HYPERSONIC_PERSISTENT
        // is necessary to check database URL to determine which is
        // the valid one
        if (database.equals(JdbcDatabase.HYPERSONIC_IN_MEMORY)
            || database.equals(JdbcDatabase.HYPERSONIC_PERSISTENT)) {
          if (databaseUrl.startsWith("jdbc:hsqldb:mem")) {
            return JdbcDatabase.HYPERSONIC_IN_MEMORY.name();
          } else if (databaseUrl.startsWith("jdbc:hsqldb:file")) {
            return JdbcDatabase.HYPERSONIC_PERSISTENT.name();
          }
        } else {
          return database.name();
        }
      }
    }

    return "";
  }

  /**
   * Method to get JpaOperations Service implementation
   * 
   * @return
   */
  public JpaOperations getJpaOperations() {
    if (jpaOperations == null) {
      // Get all Services implement JpaOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(JpaOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          jpaOperations = (JpaOperations) this.context.getService(ref);
          return jpaOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load JpaOperations on ProjectConfigurationController.");
        return null;
      }
    } else {
      return jpaOperations;
    }

  }

  /**
   * Method to get PathResolver Service implementation
   * 
   * @return
   */
  public PathResolver getPathResolver() {
    if (pathResolver == null) {
      // Get all Services implement PathResolver interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PathResolver.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          pathResolver = (PathResolver) this.context.getService(ref);
          return pathResolver;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load PathResolver on ProjectConfigurationController.");
        return null;
      }
    } else {
      return pathResolver;
    }

  }

  /**
   * Method to get FileManager Service implementation
   * 
   * @return
   */
  public FileManager getFileManager() {
    if (fileManager == null) {
      // Get all Services implement FileManager interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(FileManager.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          fileManager = (FileManager) this.context.getService(ref);
          return fileManager;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load FileManager on ProjectConfigurationController.");
        return null;
      }
    } else {
      return fileManager;
    }

  }

  /**
   * Method to get Shell Service implementation
   * 
   * @return
   */
  public Shell getShell() {
    if (shell == null) {
      // Get all Services implement Shell interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(Shell.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          shell = (Shell) context.getService(ref);
          return shell;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load Shell on ProjectConfigurationController.");
        return null;
      }
    } else {
      return shell;
    }
  }
}
