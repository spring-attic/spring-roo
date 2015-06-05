package org.springframework.roo.web.ui.controllers;

import java.io.IOException;
import java.io.PrintWriter;
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
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.web.ui.domain.Project;
import org.springframework.roo.web.ui.domain.Status;

/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ProjectServlet extends HttpServlet {
	
	private BundleContext context;
	private static final Logger LOGGER = HandlerUtils
			.getLogger(ProjectServlet.class);
	
	private Shell shell;
	private ProjectOperations projectOperations;
	private MavenOperations mavenOperations;
	
	
	public ProjectServlet(BundleContext bContext){
		this.context = bContext;
	}

	
	public void init() throws ServletException {

	}

	// Method to handle GET method request.
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();
		// Operation to get project Info
		String projectName = "";
		String topLevelPackage = "";
		boolean exists = false;
		
		// Checking if project exists
		if(!getMavenOperations().isCreateProjectAvailable()){
			// Getting project info
			projectName = getProjectOperations().getFocusedProjectName();
			topLevelPackage = getProjectOperations().getFocusedTopLevelPackage().toString();
			exists = true;
		}
		
		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(new Project(projectName, topLevelPackage, exists));
		out.println(json);
	}


	// Method to handle POST method request.
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();

		// Getting params
		String projectName = request.getParameter("projectName").replaceAll(" ", "");
		String topLevelPackage = request.getParameter("topLevelPackage");
		
		// Execute project command 
		// TODO: Replace with Project command
		boolean status = getShell().executeCommand("project setup --projectName " + projectName + " --topLevelPackage " + topLevelPackage);
		
		String message = "Spring Roo project has been generated correctly!";
		if(!status){
			message = "Error configuring Spring Roo project";
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
    				return (MavenOperations) this.context.getService(ref);
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
                    return (ProjectOperations) this.context.getService(ref);
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