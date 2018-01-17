package com.rdnsn.b2intgr.processor;

public class UploadException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6121599403776306084L;

	public UploadException() {
		super();
	}
	
	public UploadException(String message) {
		super(message);
	}

	
	public UploadException(Exception e) {
		super(e);
	}

}
