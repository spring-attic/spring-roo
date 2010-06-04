package org.springframework.roo.addon.dbre;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.db.connection.ConnectionProvider;
import org.springframework.roo.addon.dbre.db.connection.ConnectionProviderImpl;
import org.springframework.roo.addon.dbre.db.metadata.DbMetadata;
import org.springframework.roo.addon.dbre.db.metadata.Table;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;

/**
 * Provides database reverse engineering configuration operations.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreOperationsImpl implements DbreOperations {
	private static final Logger logger = HandlerUtils.getLogger(DbreOperationsImpl.class);
	private BundleContext bundleContext;
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;

	// private Properties props;

	protected void activate(ComponentContext componentContext) {
		this.bundleContext = componentContext.getBundleContext();
	}

	public boolean isDbreAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"));
	}

	public void displayMetadata(String table) {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		DbMetadata dbMetadata = new DbMetadata(connection);
		if (StringUtils.hasLength(table)) {
			Table t = dbMetadata.getTable(null, null, table);
			logger.log(t != null ? Level.INFO : Level.WARNING, t != null ? t.toString() : "Table " + table + " does not exist");
		} else {
			String databaseMetaData = dbMetadata.toString();
			logger.log(StringUtils.hasText(databaseMetaData) ? Level.INFO : Level.WARNING, StringUtils.hasText(databaseMetaData) ? databaseMetaData : "Database metadata unavailable");
		}
		provider.closeConnection(connection);
	}
	
	public void reverseEngineer() {
		Map<String, String> map = propFileOperations.getProperties(Path.SPRING_CONFIG_ROOT, "database.properties");
		ConnectionProvider provider = new ConnectionProviderImpl(map);
		Connection connection = provider.getConnection();
		DbMetadata dbMetadata = new DbMetadata(connection);
		updateDbreXml(dbMetadata);
		provider.closeConnection(connection);
	}
		// System.out.println("driver class = " + map.get("database.driverClassName"));
		// ServiceReference[] refs = null;
		// try {
		// refs = bundleContext.getServiceReferences(DataSource.class.getName(), null);
		// } catch (InvalidSyntaxException ignore) {}
		// if (refs == null) {
		// throw new IllegalStateException("No Datasource-providing bundles found");
		// }
		// for (ServiceReference ref : refs) {
		// System.out.println("REference is " + ref);
		// Object o = bundleContext.getService(ref);
		// System.out.println("    > " + o);
		// }

		// DataSource ds = (DataSource)bundleContext.getService(reference);
		// DataSource dataSource =
		// ConnectionProvider provider = new ConnectionProviderImpl();
		// provider.configure(props);

		// Connection connection = provider.getConnection();
		// try {
		// Connection connection = ds.getConnection();

		// } catch (SQLException e) {
		// throw new IllegalStateException(e);
		// }

	
	private void updateDbreXml(DbMetadata dbMetadata) {
		String dbrePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/dbre.xml");
		MutableFile dbreMutableFile = null;

		Document dbre;
		try {
			if (fileManager.exists(dbrePath)) {
				dbreMutableFile = fileManager.updateFile(dbrePath);
				dbre = XmlUtils.getDocumentBuilder().parse(dbreMutableFile.getInputStream());
			} else {
				dbreMutableFile = fileManager.createFile(dbrePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dbre-template.xml");
				Assert.notNull(templateInputStream, "Could not acquire dbre.xml template");
				dbre = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		
		
	}
}