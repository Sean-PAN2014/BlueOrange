package org.blueo.commons.jdbc.core;

import java.util.List;

public interface Search<T> {

	public T findByExample(T t, boolean nullableResult);
	
	public List<T> findByExample(T t);

	public List<T> findAll();

}