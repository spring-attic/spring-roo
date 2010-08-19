package org.springframework.roo.classpath.javaparser;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.TypeDeclaration;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.ActiveProcessManager;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata returned by {@link JavaParserMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaParserClassMetadata extends AbstractMetadataItem implements PhysicalTypeMetadata {
	private static final Logger logger = HandlerUtils.getLogger(JavaParserClassMetadata.class);
	private String fileIdentifier;
	private PhysicalTypeDetails physicalTypeDetails;

	/**
	 * Creates a new {@link JavaParserClassMetadata} that parses the specified file.
	 * 
	 * <p>
	 * The file must exist on disk when this constructor is invoked.
	 * 
	 * <p>
	 * If the file contains any errors, the invalid flag will be set.
	 * 
	 * @param fileManager that can be used for subsequently modifying the file (required)
	 * @param fileMetadata to parse (required)
	 * @param metadataIdentificationString to assign to this instance (required)
	 */
	public JavaParserClassMetadata(FileManager fileManager, String fileIdentifier, String metadataIdentificationString, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider) {
		super(metadataIdentificationString);
		Assert.isTrue(PhysicalTypeIdentifier.isValid(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid physical type identifier");
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(fileIdentifier, "File identifier required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");

		this.fileIdentifier = fileIdentifier;

		try {
			Assert.isTrue(fileManager.exists(fileIdentifier), "Path '" + fileIdentifier + "' must exist");
			CompilationUnit compilationUnit = JavaParser.parse(fileManager.getInputStream(fileIdentifier));

			for (TypeDeclaration candidate : compilationUnit.getTypes()) {
				// This implementation only supports the main type declared within a compilation unit
				if (PhysicalTypeIdentifier.getJavaType(metadataIdentificationString).getSimpleTypeName().equals(candidate.getName())) {
					// We have the required type declaration
					physicalTypeDetails = new JavaParserMutableClassOrInterfaceTypeDetails(compilationUnit, candidate, fileManager, metadataIdentificationString, fileIdentifier, PhysicalTypeIdentifier.getJavaType(metadataIdentificationString), metadataService, physicalTypeMetadataProvider);
					break;
				}
			}
			Assert.notNull(physicalTypeDetails, "Parsing empty, enum or annotation types is unsupported");

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Parsed '" + metadataIdentificationString + "'");
			}
		} catch (Throwable ex) {
			// Non-fatal, it just means the type could not be parsed

			if (ex.getMessage() != null && ex.getMessage().startsWith(JavaParserMutableClassOrInterfaceTypeDetails.UNSUPPORTED_MESSAGE_PREFIX)) {
				// We don't want the limitation of the metadata parsing subsystem to confuse the user into thinking there is a problem with their source code
				valid = false;
				return;
			}

			ProcessManager pm = ActiveProcessManager.getActiveProcessManager();
			if (pm != null && pm.isDevelopmentMode()) {
				logger.log(Level.INFO, "Parsing failure for '" + fileIdentifier + "' (development mode diagnostics)", ex);
			}
			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "Unable to parse '" + fileIdentifier + "'", ex);
			}
			valid = false;
		}
	}

	public PhysicalTypeDetails getPhysicalTypeDetails() {
		return physicalTypeDetails;
	}

	public String getPhysicalLocationCanonicalPath() {
		return fileIdentifier;
	}

	public String getItdCanoncialPath(ItdMetadataProvider metadataProvider) {
		Assert.notNull(metadataProvider, "Metadata provider required");
		String governorFileIdentifier = this.getPhysicalLocationCanonicalPath();
		Assert.notNull(governorFileIdentifier, "Unable to determine file identifier for governor");
		int dropFrom = governorFileIdentifier.lastIndexOf(".java");
		Assert.isTrue(dropFrom > -1, "Unexpected governor filename format '" + governorFileIdentifier + "'");
		return governorFileIdentifier.substring(0, dropFrom) + "_Roo_" + metadataProvider.getItdUniquenessFilenameSuffix() + ".aj";
	}

	public JavaType getItdJavaType(ItdMetadataProvider metadataProvider) {
		Assert.notNull(metadataProvider, "Metadata provider required");
		Assert.notNull(metadataProvider, "Metadata provider required");
		return new JavaType(PhysicalTypeIdentifier.getJavaType(getId()).getFullyQualifiedTypeName() + "_Roo_" + metadataProvider.getItdUniquenessFilenameSuffix());
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("fileIdentifier", fileIdentifier);
		tsc.append("physicalTypeDetails", physicalTypeDetails);
		return tsc.toString();
	}
}
