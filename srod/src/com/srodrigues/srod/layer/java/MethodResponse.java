package com.srodrigues.srod.layer.java;

import java.io.Serializable;

public class MethodResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private Object value = null;
	private int       id = 0;

	// To allow subtypes of non-serializable classes to be serialized, the
	// subtype may assume responsibility for saving and restoring the state of
	// the supertype's public, protected, and (if accessible) package fields.
	// The subtype may assume this responsibility only if the class it extends
	// has an accessible no-arg constructor to initialize the class's state. It
	// is an error to declare a class Serializable if this is not the case. The
	// error will be detected at runtime.
	public MethodResponse(){}
	
	public MethodResponse(final int id, final Object value) {
		this.value = value ;
		this.id    =    id ;
	}

	public Object getValue() {
		return value;
	}

	public int getId() {
		return id;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}

	public void setId(int id) {
		this.id = id;
	}
}
