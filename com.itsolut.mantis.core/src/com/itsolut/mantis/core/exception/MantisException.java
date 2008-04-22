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
 * Indicates an error during repository access.
 * 
 * @author Steffen Pingel
 */
public class MantisException extends Exception {

	private static final long serialVersionUID = 1929614326467463462L;

	public MantisException() {
	}

	public MantisException(String message) {
		super(message);
	}

	public MantisException(Throwable cause) {
		super(cause);
	}

	public MantisException(String message, Throwable cause) {
		super(message, cause);
	}

}
