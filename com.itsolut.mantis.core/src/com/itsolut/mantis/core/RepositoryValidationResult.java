/*******************************************************************************
 * Copyright (c) 2004, 2010 Robert Munteanu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Robert Munteanu - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.core;

/**
 * @author Robert Munteanu
 * 
 */
public class RepositoryValidationResult {

	private final RepositoryVersion version;

	public RepositoryValidationResult(RepositoryVersion version) {

		this.version = version;
	}

	public RepositoryVersion getVersion() {

		return version;
	}
}
