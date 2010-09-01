package org.springframework.roo.addon.git;

/**
 * Simple container for command result details.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class GitCommandResult {

	private String commitId;
	
	private String result;

	public GitCommandResult(String commitId, String result) {
		super();
		this.commitId = commitId;
		this.result = result;
	}

	public String getCommitId() {
		return commitId;
	}

	public String getResult() {
		return result;
	}
}
