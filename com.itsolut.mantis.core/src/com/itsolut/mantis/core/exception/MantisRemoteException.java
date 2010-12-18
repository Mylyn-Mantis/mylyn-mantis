/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.core.exception;


/**
 * Indicates that an exception on the repository side has been encountered while
 * processing the request.
 * 
 * @author Steffen Pingel
 */
public class MantisRemoteException extends MantisException {

	private static final long serialVersionUID = -6761365344287289624L;
	
	private boolean unexpected = false;

	public MantisRemoteException() {
	}

	public MantisRemoteException(String message) {
		super(message);
	}
	
	public MantisRemoteException(String message, boolean unexpected) {
	    
	    super(message);
	    this.unexpected = unexpected;
	}

	public MantisRemoteException(Throwable cause) {
		super(cause);
	}

	public MantisRemoteException(String message, Throwable cause) {
		super(message, cause);
	}
	

	public MantisRemoteException(String message, Throwable cause, boolean unexpected) {
	    super(message, cause);
	    
	    this.unexpected = unexpected;
	}
	
	public boolean isUnexpected() {

        return unexpected;
    }

}
