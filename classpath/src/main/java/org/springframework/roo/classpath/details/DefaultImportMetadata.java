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
public class DefaultImportMetadata extends
        AbstractIdentifiableJavaStructureProvider implements ImportMetadata {

    private final JavaPackage importPackage;
    private final JavaType importType;
    private boolean isAsterisk = false;
    private boolean isStatic = false;

    // Package protected to mandate the use of ImportMetadataBuilder
    DefaultImportMetadata(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final JavaPackage importPackage, final JavaType importType,
            final boolean isStatic, final boolean isAsterisk) {
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

    public boolean isAsterisk() {
        return isAsterisk;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
        tsc.append("typePackage", importPackage);
        tsc.append("type", importType);
        tsc.append("isStatic", isStatic);
        tsc.append("isAsterisk", isAsterisk);
        return tsc.toString();
    }
}
