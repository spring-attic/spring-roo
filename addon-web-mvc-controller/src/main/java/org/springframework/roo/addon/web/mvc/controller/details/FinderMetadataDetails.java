package org.springframework.roo.addon.web.mvc.controller.details;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Aggregates metadata for a given Roo finder which is scaffolded by Web add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public class FinderMetadataDetails implements Comparable<FinderMetadataDetails>, Cloneable {
	
	private String finderName;
	private MethodMetadata finderMethodMetadata;
	private List<FieldMetadata> finderMethodParamFields;
	
	public FinderMetadataDetails(String finderName, MethodMetadata finderMethodMetadata, List<FieldMetadata> finderMethodParamFields) {
		Assert.hasText(finderName, "Finder name required");
		Assert.notNull(finderMethodMetadata, "Finder method metadata required");
		this.finderName = finderName;
		this.finderMethodMetadata = finderMethodMetadata;
		this.finderMethodParamFields = finderMethodParamFields;
	}

	public String getFinderName() {
		return finderName;
	}

	public MethodMetadata getFinderMethodMetadata() {
		return finderMethodMetadata;
	}

	public List<FieldMetadata> getFinderMethodParamFields() {
		return finderMethodParamFields;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((finderName == null) ? 0 : finderName.hashCode());
		return result;
	}

	public final boolean equals(Object obj) {
		// NB: Not using the normal convention of delegating to compareTo (for efficiency reasons)
		return obj != null && obj instanceof FinderMetadataDetails && finderName.equals(((FinderMetadataDetails) obj).finderName);
	}

	public final int compareTo(FinderMetadataDetails o) {
		// NB: If adding more fields to this class ensure the equals(Object) method is updated accordingly
		if (o == null) return -1;
		int cmp = finderName.compareTo(o.finderName);
		return cmp;
	}
}
