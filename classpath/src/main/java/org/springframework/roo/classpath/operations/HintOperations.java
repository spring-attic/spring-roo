package org.springframework.roo.classpath.operations;

import java.util.SortedSet;

public interface HintOperations {

	String hint(String topic);

	SortedSet<String> getCurrentTopics();
}