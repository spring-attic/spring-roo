package org.springframework.roo.model;

public interface Criteria<T> {

	boolean meets(T t);
}
