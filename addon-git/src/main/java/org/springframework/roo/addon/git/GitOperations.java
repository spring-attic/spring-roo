package org.springframework.roo.addon.git;

/**
 * Operations offered by Git addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface GitOperations {

	/**
	 * Triggers commit for all changes in the Git tree. 
	 * (works like 'git commmit -a -m {message}')
	 * 
	 * @param message Commit message
	 * @return Container for Git commit Id and message
	 */
	GitCommandResult commitAllChanges(String message);
	
	/**
	 * Triggers Git revert.
	 * 
	 * @param noOfCommitsToRevert number of commits to revert (HEAD - n)
	 * @param message Commit message
	 * @return Container for Git commit Id and message
	 */
	GitCommandResult revertCommit(int noOfCommitsToRevert, String message);
	
	/**
	 * Convenience access to the Git config (allows setting config options)
	 * 
	 * @param category The Git config category.
	 * @param key The config key.
	 * @param value The config value.
	 */
	void setConfig(String category, String key, String value);
	
	/**
	 * Initial setup of git repository in target project.
	 */
	void setup();
	
	/**
	 * Check if Git commands are available in Shell. Depends on presence of 
	 * .git repository.
	 * 
	 * @return availability
	 */
	boolean isGitCommandAvailable();
	
	/**
	 * Check if Git setup command is available in Shell.
	 * 
	 * @return availability
	 */
	boolean isSetupCommandAvailable();
}
