/*
 * Copyright (c) 2011, Evernote Corporation
 * All rights reserved. 
 */
package com.evernote.android.edam;

import com.evernote.thrift.TException;

public class FileDataException extends TException {
	private static final long serialVersionUID = 1L;

	FileDataException(String message) {
		super(message);
	}

	FileDataException(Exception e) {
		super(e);
	}
}