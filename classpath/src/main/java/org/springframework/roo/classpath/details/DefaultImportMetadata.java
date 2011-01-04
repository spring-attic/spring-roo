package org.springframework.roo.classpath.details;

import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;

/**
 * Default implementation of {@link ImportMetadata}.
 * 
 * @author James Tyrrell
 * @since 1.1.1
 */
public class DefaultImportMetadata extends AbstractIdentifiableJavaStructureProvider implements ImportMetadata {
	private boolean isStatic = false;
	private JavaPackage importPackage;
    private JavaType importType;
	private boolean isAsterisk = false;
	
	// Package protected to mandate the use of ImportMetadataBuilder
	DefaultImportMetadata(CustomData customData, String declaredByMetadataId, int modifier, JavaPackage importPackage, JavaType importType, boolean isStatic, boolean isAsterisk) {
		super(customData, declaredByMetadataId, modifier);
		this.importPackage = importPackage;
        this.importType = importType;
        this.isStatic = isStatic;
        this.isAsterisk = isAsterisk;
	}
	
	public JavaPackage getImportPackage() {
        return importPackage;
    }

    public JavaType getImportType() {
        return importType;
    }
	
	public boolean isStatic() {
		return isStatic;
	}
	
	public boolean isAsterisk() {
		return isAsterisk;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("typePackage", importPackage);
        tsc.append("type", importType);
		tsc.append("isStatic", isStatic);
		tsc.append("isAsterisk", isAsterisk);
		return tsc.toString();
	}
}
