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
	 */
	void commitAllChanges(String message);
	
	/**
	 * Triggers Git reset (hard).
	 * 
	 * @param noOfCommitsToReset number of commits to reset (HEAD - n)
	 * @param message Commit message
	 */
	void reset(int noOfCommitsToReset, String message);
	
	/**
	 * Triggers revert of last commit.
	 * @param message Commit message
	 */
	void revertLastCommit(String message);
	
	/**
	 * Present git log.
	 * @param maxHistory
	 */
	void log(int maxHistory);
	
	/**
	 * Trigger revert of commit with given rev string.
	 * 
	 * @param revstr
	 * @param message
	 */
	void revertCommit(String revstr, String message);
	
	/**
	 * Trigger git push.
	 */
	void push();
	
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
	
	/**
	 * Check if automatic commit is enabled for successful Roo commands.
	 * 
	 * @return automaticCommit
	 */
	boolean isAutomaticCommit();
}
