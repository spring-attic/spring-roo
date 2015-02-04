package org.springframework.roo.classpath.operations;

import java.util.SortedSet;

public interface HintOperations {

    SortedSet<String> getCurrentTopics();

    String hint(String topic);
}