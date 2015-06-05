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
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.web.ui.domain.Status;

/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class FieldsServlet extends HttpServlet {

	private BundleContext context;
	private static final Logger LOGGER = HandlerUtils
			.getLogger(FieldsServlet.class);

	private Shell shell;
	private TypeLocationService typeLocationService;
	private ProjectOperations projectOperations;

	public FieldsServlet(BundleContext bContext) {
		this.context = bContext;
	}

	public void init() throws ServletException {

	}

	/**
	 * Method to handle GET method request of /fields service
	 * 
	 * @param request
	 * @param response
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Set response content type
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();
		out.println("<b>GET method on /fields context is not available</b>");
	}

	/**
	 * Method to handle POST request of /fields service
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
		String entityName = request.getParameter("entityName");
		String fieldName = request.getParameter("fieldName");
		String fieldGroup = request.getParameter("fieldGroup");
		String fieldType = request.getParameter("fieldType");
		String referencedClass = request.getParameter("referencedClass");
		// Execute fieldcommand
		// TODO: Replace with field command
		String fieldCommand = constructFieldCommand(entityName, fieldName, fieldGroup, fieldType, referencedClass);

		boolean status = getShell().executeCommand(fieldCommand);

		String message = "New field '"+ fieldName+"' created on '" + entityName + "'!";
		if (!status) {
			message = "Error creating field.";
		}

		// Returning JSON
		ObjectWriter ow = new ObjectMapper().writer()
				.withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(new Status(status, message));
		out.println(json);

	}

	/**
	 * Method to construct field command.
	 * 
	 * @param entityName
	 * @param fieldName
	 * @param fieldGroup
	 * @param fieldType
	 * @param referencedClass
	 * @return
	 */
	private String constructFieldCommand(String entityName, String fieldName,
			String fieldGroup, String fieldType, String referencedClass) {

		StringBuilder sb = new StringBuilder();
		
		sb.append("field ");
		
		// Depends of field group, command changes
		if("".equals(fieldGroup)){
			sb.append(fieldType.toLowerCase()).append(" ");
		}else{
			sb.append(fieldGroup.toLowerCase()).append(" --type ").append(fieldType.toLowerCase()).append(" ");
		}
		
		// Adding field name
		sb.append("--fieldName ").append(fieldName).append(" ");
		
		// Adding entity where field will be added
		sb.append("--class ").append(entityName);
		
		// Adding type param
		if(!"".equals(referencedClass)){
			sb.append(" --type ").append(referencedClass);
		}
		
		return sb.toString();
		
	}

	public void destroy() {
		// do nothing.
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
						.getAllServiceReferences(Shell.class.getName(), null);

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