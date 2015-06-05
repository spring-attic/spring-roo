package org.springframework.roo.web.ui.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.addon.jpa.addon.JdbcDatabase;
import org.springframework.roo.addon.jpa.addon.JpaCommands;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.OrmProvider;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.web.ui.domain.Database;
import org.springframework.roo.web.ui.domain.PersistenceConfiguration;
import org.springframework.roo.web.ui.domain.PersistenceProvider;
import org.springframework.roo.web.ui.domain.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class PersistenceServlet extends HttpServlet {
	
	private BundleContext context;
	private static final Logger LOGGER = HandlerUtils
			.getLogger(PersistenceServlet.class);
	
	private Shell shell;
	private ProjectOperations projectOperations;
	private MavenOperations mavenOperations;
	private JpaOperations jpaOperations;
	private FileManager fileManager;
	private PathResolver pathResolver;
	
	
	public PersistenceServlet(BundleContext bContext){
		this.context = bContext;
	}

	
	public void init() throws ServletException {

	}

	/**
	 * Method to handle GET method request of /persistence service
	 * 
	 * @param request
	 * @param response
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();
		
		String method = request.getPathInfo();
		
		// Operation to get project Info
		if(method == null || method.equals("/")){
			getPersistenceInfo(out);
		}else if(method.equals("/providers")){
			getOrmProviders(out);
		}else if(method.equals("/databases")){
			getJDBCDatabases(out);
		}
	}


	/**
	 * Method to handle POST request of /persistence service
	 * 
	 * @param request
	 * @param response
	 * 
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();

		// Getting params
		String provider = request.getParameter("providerName").trim();
		String database = request.getParameter("database").trim();
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		
		// Execute project command 
		// TODO: Replace with JPA command
		String persistenceCommand = "jpa setup --provider \"" + provider + "\" --database " + database;
		
		if(username != null || !"".equals(username)){
			persistenceCommand = persistenceCommand.concat(" --userName ").concat(username);
		}
		
		if(password != null || !"".equals(password)){
			persistenceCommand = persistenceCommand.concat(" --password ").concat(password);
		}
		
		
		boolean status = getShell().executeCommand(persistenceCommand);
		
		String message = "Persistence was configured to use '"+provider+"' and '"+database+"'!";
		if(!status){
			message = "Error configuring persistence.";
		}
		
		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(new Status(status, message));
		out.println(json);
		
	}

	public void destroy() {
		// do nothing.
	}
	
	/**
	 * Method that obtains current JPA Persistence configuration
	 * 
	 * @param out
	 */
	private void getPersistenceInfo(PrintWriter out) throws ServletException, IOException {
		
		String persistenceProvider = "";
		String database = "";
		String databaseUrl = "";
		String username = "";
		String password = "";
		
		// Getting current persistence provider
		String persistenceFile = getPathResolver().getFocusedIdentifier(
		                Path.SRC_MAIN_RESOURCES,"META-INF/persistence.xml"); 
		if(getFileManager().exists(persistenceFile)){
			// Reading persistence.xml
			final InputStream inputStream = getFileManager().getInputStream(persistenceFile);
			final Document docXml = XmlUtils.readXml(inputStream);
	        final Element document = docXml.getDocumentElement();
	        
	        Element providerElement = XmlUtils.findFirstElement(
	                 "persistence-unit/provider", document);
	        
	        String adapter = providerElement.getTextContent();
	        
	        // Getting provider
	        for(OrmProvider provider : OrmProvider.values()){
	        	if(provider.getAdapter().equals(adapter)){
	        		persistenceProvider = provider.name();
	        	}
	        }
	        
			// With DATANUCLEUS provider, doesn't exists database.properties, 
			// so we need to get properties from persistence.xml 
	        if(persistenceProvider.equals("DATANUCLEUS")){
	        	Element propertiesElement = XmlUtils.findFirstElement(
		                 "persistence-unit/properties", document);
		        
		        NodeList properties = propertiesElement.getChildNodes();
		        for(int i = 0; i < properties.getLength(); i++){
		        	if(properties.item(i).hasAttributes()){
		        		Element property = (Element) properties.item(i);
			        	NamedNodeMap nodeAttrs = property.getAttributes();
			        	String nameAttr = property.getAttribute("name");
			        	String valueAttr = property.getAttribute("value");
			        	if(nameAttr.equals("datanucleus.ConnectionURL")){
			        		databaseUrl = valueAttr;
			        	}else if(nameAttr.equals("datanucleus.ConnectionUserName")){
			        		username = valueAttr;
			        	}else if(nameAttr.equals("datanucleus.ConnectionPassword")){
			        		password = valueAttr;
			        	}
		        	}
		        }
		        
		        for(int i = 0; i < properties.getLength(); i++){
		        	if(properties.item(i).hasAttributes()){
			        	Element property = (Element) properties.item(i);
			        	String nameAttr = property.getAttribute("name");
			        	String valueAttr = property.getAttribute("value");
			        	if(nameAttr.equals("datanucleus.ConnectionDriverName")){
			        		database = getDatabaseByDriverClassName(valueAttr, databaseUrl);
			        	}
		        	}
		        }
	        }
	        
	        if(!persistenceProvider.equals("DATANUCLEUS")){
				// Getting database properties
				SortedSet<String> databaseProperties = getJpaOperations().getDatabaseProperties();
				
				// Getting database URL
				for(String databaseProperty : databaseProperties){
					String[] keyValue = databaseProperty.split("=");
					String key = keyValue[0].trim();
					String value = keyValue[1].trim();
					if(key.equals("database.url")){
						databaseUrl = value;
					}
					
				}
				
				for(String databaseProperty : databaseProperties){
					String[] keyValue = databaseProperty.split("=");
					String key = keyValue[0].trim();
					String value = keyValue[1].trim();
					if(key.equals("database.driverClassName")){
						database = getDatabaseByDriverClassName(value, databaseUrl);
					}else if(key.equals("database.username")){
						username = value;
					}else if(key.equals("database.password")){
						password = value;
					}
				}
			}
		}
                        
		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(new PersistenceConfiguration(persistenceProvider, database, databaseUrl, username, password));
		out.println(json);
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
		
		for(JdbcDatabase database : availableDatabases){
			if(database.getDriverClassName().equals(driverClassName)){
				// If database type is HYPERSONIC_IN_MEMORY or HYPERSONIC_PERSISTENT
				// is necessary to check database URL to determine which is 
				// the valid one
				if(database.equals(JdbcDatabase.HYPERSONIC_IN_MEMORY) 
						|| database.equals(JdbcDatabase.HYPERSONIC_PERSISTENT)){
					if(databaseUrl.startsWith("jdbc:hsqldb:mem")){
						return JdbcDatabase.HYPERSONIC_IN_MEMORY.name();
					}else if(databaseUrl.startsWith("jdbc:hsqldb:file")){
						return JdbcDatabase.HYPERSONIC_PERSISTENT.name();
					}
				}else{
					return database.name();
				}
			}
		}

		return "";
	}


	/**
	 * 
	 * Method that get available OrmProviders
	 * 
	 * @param out
	 */
	private void getOrmProviders(PrintWriter out) throws ServletException, IOException {
		// Getting all available OrmProviders
		OrmProvider[] providers = OrmProvider.values();
		List<PersistenceProvider> providerList = new ArrayList<PersistenceProvider>();
		for(OrmProvider provider : providers){
			providerList.add(new PersistenceProvider(provider.name()));
		}
		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(providerList);
		out.println(json);
	}
	
	
	/**
	 * 
	 * Method that get available Databases
	 * 
	 * @param out
	 */
	private void getJDBCDatabases(PrintWriter out) throws ServletException, IOException {
		// Getting all available JdbcDatabase
		JdbcDatabase[] databases = JdbcDatabase.values();
		List<Database> databaseList = new ArrayList<Database>();
		for(JdbcDatabase database : databases){
			databaseList.add(new Database(database.name()));
		}
		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(databaseList);
		out.println(json);
	}
	
	/**
	 * Method to get JpaOperations Service implementation
	 * 
	 * @return
	 */
    public JpaOperations getJpaOperations(){
    	if(jpaOperations == null){
    		// Get all Services implement JpaOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(JpaOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				jpaOperations = (JpaOperations) this.context.getService(ref);
    				return jpaOperations;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load JpaOperations on ProjectConfigurationController.");
    			return null;
    		}
    	}else{
    		return jpaOperations;
    	}
    	
    }
    
	/**
	 * Method to get PathResolver Service implementation
	 * 
	 * @return
	 */
    public PathResolver getPathResolver(){
    	if(pathResolver == null){
    		// Get all Services implement PathResolver interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				pathResolver = (PathResolver) this.context.getService(ref);
    				return pathResolver;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PathResolver on ProjectConfigurationController.");
    			return null;
    		}
    	}else{
    		return pathResolver;
    	}
    	
    }
    
	/**
	 * Method to get FileManager Service implementation
	 * 
	 * @return
	 */
    public FileManager getFileManager(){
    	if(fileManager == null){
    		// Get all Services implement FileManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				fileManager = (FileManager) this.context.getService(ref);
    				return fileManager;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load FileManager on ProjectConfigurationController.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    	
    }
	
	/**
	 * Method to get MavenOperations Service implementation
	 * 
	 * @return
	 */
    public MavenOperations getMavenOperations(){
    	if(mavenOperations == null){
    		// Get all Services implement MavenOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MavenOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				mavenOperations = (MavenOperations) this.context.getService(ref);
    				return mavenOperations;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MavenOperations on ProjectConfigurationController.");
    			return null;
    		}
    	}else{
    		return mavenOperations;
    	}
    	
    }
	
	/**
	 * Method to get ProjectOperations Service implementation
	 * 
	 * @return
	 */
    public ProjectOperations getProjectOperations() {
        if (projectOperations == null) {
            // Get all Services implement ProjectOperations interface
            try {
                ServiceReference<?>[] references = this.context
                        .getAllServiceReferences(
                        		ProjectOperations.class.getName(), null);

                for (ServiceReference<?> ref : references) {
                    projectOperations = (ProjectOperations) this.context.getService(ref);
                    return projectOperations;
                }

                return null;

            }
            catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load ProjectOperations on ProjectConfigurationController.");
                return null;
            }
        }
        else {
            return projectOperations;
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
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								Shell.class.getName(), null);

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