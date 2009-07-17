package org.springframework.roo.addon.bundlor;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.roo.process.manager.FileManager;

import com.springsource.util.osgi.manifest.BundleManifest;
import com.springsource.util.osgi.manifest.BundleManifestFactory;
import com.springsource.util.osgi.manifest.parse.ParserLogger;
import com.springsource.util.parser.manifest.ManifestContents;
import com.springsource.util.parser.manifest.ManifestParser;
import com.springsource.util.parser.manifest.ManifestProblem;
import com.springsource.util.parser.manifest.RecoveringManifestParser;

/**
 * Utility class for modifying template.mf files
 * Defensively coded to encapsulate and log all errors that may occur
 * while manipulating the manifest.
 * 
 * @author Adrian Colyer
 * @since 1.0
 *
 */
public class ManifestTemplate {

	final static Logger logger = Logger.getLogger(ManifestTemplate.class.getName());

	private static final String EXCLUDED_EXPORTS = "Excluded-Exports";
	private static final String IMPORT_TEMPLATE = "Import-Template";
	private static final String EXPORT_TEMPLATE = "Export-Template";
	private static final String IMPORT_PACKAGE = "Import-Package";

	private static final String IMPORT_BUNDLE = "Import-Bundle";

	private BundleManifest mf = null;
	private boolean manifestOK = true;
	private FileManager fileManager;
	private String templateMfPath;
	private JDKLoggerParserLogger parserLogger = new JDKLoggerParserLogger();

	
	public ManifestTemplate(FileManager fileManager, String pathToTemplateMfFile) {
		this.fileManager = fileManager;
		this.templateMfPath = pathToTemplateMfFile;
		ManifestParser parser = new RecoveringManifestParser();
		try {
			ManifestContents manifestContent = parser.parse(new FileReader(pathToTemplateMfFile));
			if (parser.foundProblems()) {
				manifestOK = false;
				logParsingErrors(parser.getProblems());
			}
			else {
				mf = BundleManifestFactory.createBundleManifest(manifestContent, parserLogger);
				if (parserLogger.hasReportedErrors()) {
					logger.log(Level.SEVERE,"Could not process template.mf file, bundlor command aborted");
					manifestOK = false;
					mf = null;
				}
			}
		}
		catch (Exception ex) {
			mf = null;
			manifestOK = false;
			logger.log(Level.SEVERE, "Could not process template.mf file, bundlor command aborted", ex);
		}		
	}


	public ManifestTemplate addExcludeExport(String packagePattern) {
		if (manifestOK) {			
			// current header value
			String excludeExportHeader = mf.getHeader(EXCLUDED_EXPORTS);
			if (null == excludeExportHeader) { excludeExportHeader = ""; }
			excludeExportHeader = excludeExportHeader.trim();
			
			// add new exclusion
			if (excludeExportHeader.length() != 0) {
				excludeExportHeader += ", ";
			}
			excludeExportHeader += packagePattern;
		
			// save it back
			mf.setHeader(EXCLUDED_EXPORTS, excludeExportHeader);

			if (parserLogger.hasReportedErrors()) {
				logger.log(Level.SEVERE, "bundlor command aborted due to ill-formed manifest header");
			}
		}
		
		return this;
	}

	public ManifestTemplate addExportTemplate(String packagePattern, String version) {
		if (manifestOK) {
			// current header value
			String exportTemplateHeader = mf.getHeader(EXPORT_TEMPLATE);
			if (null == exportTemplateHeader) { exportTemplateHeader = ""; }
			exportTemplateHeader = exportTemplateHeader.trim();
			
			// add new export
			if (exportTemplateHeader.length() != 0) {
				exportTemplateHeader += ", ";
			}
			exportTemplateHeader += (packagePattern + ";version=\"" + version + "\"");
		
			// save it back
			mf.setHeader(EXPORT_TEMPLATE, exportTemplateHeader);	
			
			if (parserLogger.hasReportedErrors()) {
				logger.log(Level.SEVERE, "bundlor command aborted due to ill-formed manifest header");
			}
		}

		return this;	
	}

	public ManifestTemplate addImportTemplate(String packagePattern, String versionRange,
			boolean optional) {
		if (manifestOK) {
			// current header value
			String importTemplateHeader = mf.getHeader(IMPORT_TEMPLATE);
			if (null == importTemplateHeader) { importTemplateHeader = ""; }
			importTemplateHeader = importTemplateHeader.trim();
			
			// add new import
			if (importTemplateHeader.length() != 0) {
				importTemplateHeader += ", ";
			}
			importTemplateHeader += (packagePattern + ";version=\"" + versionRange + "\"");
			if (optional) {
				importTemplateHeader += ";resolution:=optional";
			}
		
			// save it back
			mf.setHeader(IMPORT_TEMPLATE, importTemplateHeader);						

			if (parserLogger.hasReportedErrors()) {
				logger.log(Level.SEVERE, "bundlor command aborted due to ill-formed manifest header");
			}
		}
		
		return this;
		
	}

	public ManifestTemplate addImportPackage(String packageName, String versionRange,
			boolean optional) {
		if (manifestOK) {
			// current header value
			String importPackageHeader = mf.getHeader(IMPORT_PACKAGE);
			if (null == importPackageHeader) { importPackageHeader = ""; }
			importPackageHeader = importPackageHeader.trim();
			
			// add new import
			if (importPackageHeader.length() != 0) {
				importPackageHeader += ", ";
			}
			importPackageHeader += (packageName + ";version=\"" + versionRange + "\"");
			if (optional) {
				importPackageHeader += ";resolution:=optional";
			}
		
			// save it back
			mf.setHeader(IMPORT_PACKAGE, importPackageHeader);			

			if (parserLogger.hasReportedErrors()) {
				logger.log(Level.SEVERE, "bundlor command aborted due to ill-formed manifest header");
			}
		}
		return this;
		
	}
	
	public ManifestTemplate addImportBundle(String bundleDependency) {
		if (manifestOK) {
			// current header value
			String importBundleHeader = mf.getHeader(IMPORT_BUNDLE);
			if (null == importBundleHeader) { importBundleHeader = ""; }
			importBundleHeader = importBundleHeader.trim();
			
			// add new import
			if (importBundleHeader.length() != 0) {
				importBundleHeader += ", ";
			}
			importBundleHeader += bundleDependency;
		
			// save it back
			mf.setHeader(IMPORT_BUNDLE, importBundleHeader);			

			if (parserLogger.hasReportedErrors()) {
				logger.log(Level.SEVERE, "bundlor command aborted due to ill-formed manifest header");
			}
		}
		return this;
	}

	public ManifestTemplate removeImportBundle(String bundleDependency) {
		// TODO
		return this;
	}
	
	public void writeOut() {
		if (manifestOK) {			
			try {
				mf.write(new OutputStreamWriter(
						   fileManager.updateFile(templateMfPath).getOutputStream()));
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to write template.mf contents, bundlor command aborted", e);
			}
		}		
	}

	private void logParsingErrors(List<ManifestProblem> problems) {
		StringBuffer problemText = new StringBuffer();
		for (ManifestProblem problem : problems) {
			problemText.append(problem.toStringWithContext());
			problemText.append("\n");
		}
		logger.log(Level.SEVERE,"Problems found parsing template.mf, bundlor command aborted: " +
				problemText.toString());
	}
	
	/* helper class to route parsing messages through the regular
	 * ManifestTemplate logger
	 */
	private static class JDKLoggerParserLogger implements ParserLogger {

		private boolean hasReportedErrors = false;
		
		public boolean hasReportedErrors() {
			return hasReportedErrors;
		}
		
		public String[] errorReports() {
			return new String[0];
		}

		public void outputErrorMsg(Exception re, String item) {
			hasReportedErrors = true;
			ManifestTemplate.logger.log(
					Level.WARNING, 
					"Error parsing template.mf: " + item,
					re);
		}
		
	}

}
