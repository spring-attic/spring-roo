package org.springframework.roo.classpath.operations;

import java.util.SortedSet;

public interface HintOperations {

	public abstract String hint(String topic);

	public abstract SortedSet<String> getCurrentTopics();

}