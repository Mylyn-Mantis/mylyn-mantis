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
 * Indicates an authentication error during login.
 * 
 * @author Steffen Pingel
 */
public class MantisLoginException extends MantisException {

	private static final long serialVersionUID = -6128773690643367414L;

	public MantisLoginException() {
	}

	public MantisLoginException(String message) {
		super(message);
	}

}
