package org.springframework.roo.classpath.scanner;

import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MemberDetails}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public class MemberDetailsImpl implements MemberDetails {
	private  List<MemberHoldingTypeDetails> details;

	/**
	 * Constructors a new instance.
	 * 
	 * @param details the member holders that should be stored in this instance (cannot be null or empty)
	 */
	MemberDetailsImpl(List<MemberHoldingTypeDetails> details) {
		Assert.notEmpty(details, "Member holding details required");
		this.details = details;
	}

	public List<MemberHoldingTypeDetails> getDetails() {
		return Collections.unmodifiableList(details);
	}
	
}
