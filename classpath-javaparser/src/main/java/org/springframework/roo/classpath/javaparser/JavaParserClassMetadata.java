package org.springframework.roo.classpath.javaparser;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.TypeDeclaration;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
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
	private MemberHoldingTypeDetails memberHoldingTypeDetails;

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
	 * @param fileIdentifier the file identifier (required)
	 * @param metadataIdentificationString to assign to this instance (required)
	 * @param metadataService the metadata service (required)
	 * @param physicalTypeMetadataProvider the physical-type metadata provider (required)
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

			JavaType javaType = PhysicalTypeIdentifier.getJavaType(metadataIdentificationString);
			TypeDeclaration typeDeclaration = JavaParserUtils.locateTypeDeclaration(compilationUnit, javaType);
			Assert.notNull(typeDeclaration, "Could not locate '" + javaType.getSimpleTypeName() + "' in compilation unit");
			// Many callers rely on the metadata containing methods that provide mutation of the on-disk compilation unit
			memberHoldingTypeDetails = new JavaParserMutableClassOrInterfaceTypeDetails(compilationUnit, typeDeclaration, metadataIdentificationString, javaType, metadataService, physicalTypeMetadataProvider, fileManager, fileIdentifier);
			Assert.notNull(memberHoldingTypeDetails, "Unable to parse '" + javaType + "'");

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

	public MemberHoldingTypeDetails getMemberHoldingTypeDetails() {
		return memberHoldingTypeDetails;
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
		tsc.append("memberHoldingTypeDetails", memberHoldingTypeDetails);
		return tsc.toString();
	}
}
