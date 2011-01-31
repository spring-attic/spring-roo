package org.springframework.roo.addon.dod;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Holder for a field and its corresponding collaborating DataOnDemandMetadata item, which can be null.
 *  
 * @author Alan Stewart
 * @since 1.1.2
 */
public class CollaboratingDataOnDemandMetadataHolder {
	private FieldMetadata field;
	private DataOnDemandMetadata dataOnDemandMetadata;

	public CollaboratingDataOnDemandMetadataHolder(FieldMetadata field, DataOnDemandMetadata dataOnDemandMetadata) {
		Assert.notNull(field, "Field required");
		this.field = field;
		this.dataOnDemandMetadata = dataOnDemandMetadata;
	}

	public FieldMetadata getField() {
		return field;
	}

	public DataOnDemandMetadata getDataOnDemandMetadata() {
		return dataOnDemandMetadata;
	}
}
