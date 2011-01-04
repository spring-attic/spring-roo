package org.springframework.roo.classpath.details;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link ImportMetadata}.
 * 
 * @author James Tyrrell
 * @since 1.1.1
 */
public final class ImportMetadataBuilder extends AbstractIdentifiableJavaStructureBuilder<ImportMetadata> {
	private JavaPackage importPackage;
    private JavaType importType;
    private boolean isStatic = false;
	private boolean isAsterisk = false;

	public ImportMetadataBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}
	
	public ImportMetadataBuilder(ImportMetadata existing) {
		super(existing);
		this.importPackage = existing.getImportPackage();
        this.importType = existing.getImportType();
        this.isStatic = existing.isStatic();
        this.isAsterisk = existing.isAsterisk();
	}

	public ImportMetadataBuilder(String declaredbyMetadataId, int modifier, JavaPackage importPackage, JavaType importType, boolean isStatic, boolean isAsterisk) {
		this(declaredbyMetadataId);
        setModifier(modifier);
		this.importPackage = importPackage;
        this.importType = importType;
        this.isStatic = isStatic;
        this.isAsterisk = isAsterisk;
	}
	
	public ImportMetadata build() {
		return new DefaultImportMetadata(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), importPackage, importType, isStatic, isAsterisk);
	}

    public JavaPackage getImportPackage() {
        return importPackage;
    }

    public void setImportPackage(JavaPackage importPackage) {
        this.importPackage = importPackage;
    }

    public JavaType getImportType() {
        return importType;
    }

    public void setImportType(JavaType importType) {
        this.importType = importType;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isAsterisk() {
        return isAsterisk;
    }

    public void setAsterisk(boolean asterisk) {
        isAsterisk = asterisk;
    }
}
