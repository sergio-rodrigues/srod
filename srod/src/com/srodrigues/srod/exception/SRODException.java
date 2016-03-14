package com.srodrigues.srod.exception;

public class SRODException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SRODException(final Exception e) {
		super(e);
	}

	public SRODException(final String string) {
		super(string);
	}

}
