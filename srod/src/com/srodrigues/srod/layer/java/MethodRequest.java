package com.srodrigues.srod.layer.java;

import java.io.Serializable;

public class MethodRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private String      service =   "" ;
	private String       method =   "" ;
	private Object[]       args = null ;
	private int 	         id =    0 ;

	// To allow subtypes of non-serializable classes to be serialized, the
	// subtype may assume responsibility for saving and restoring the state of
	// the supertype's public, protected, and (if accessible) package fields.
	// The subtype may assume this responsibility only if the class it extends
	// has an accessible no-arg constructor to initialize the class's state. It
	// is an error to declare a class Serializable if this is not the case. The
	// error will be detected at runtime.
	
	public MethodRequest(){ }
	
	public MethodRequest(final String service, final int id, final String method, final Object[] args) {
		this.service  =  service ;
		this.id       =       id ;
		this.method   =   method ;
		this.args     =     args ;
	}

	public String getService() {
		return service;
	}
	
	public int getId() {
		return id;
	}

	public String getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
	}

}
