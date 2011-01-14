/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/


package com.itsolut.mantis.tests;

/**
 * A <tt>RepositoryConfiguration</tt> names the types of configuration we test in our integration tests.
 * 
 * <p>Good examples include:
 * 
 * <ol>
 *   <li>A basic installation, with no customisation applied.</li>
 *   <li>An installation with a different language setting</li>
 *   <li>An installation with customised statuses/priorities</li>
 * </ol>
 * 
 * @author Robert Munteanu
 */
public enum RepositoryConfiguration {
	
	/**
	 * A basic Mantis 1.2.x installation of the most recently released version.
	 * 
	 * <p>Must not contain any customisation.</p>
	 */
	MANTIS_12_BASIC("http://localhost/mantis-1.2/api/soap/mantisconnect.php"), 
	/**
	 * A basic Mantis 1.1.x installation of the most recently released version.
	 * 
	 * <p>Must not contain any customisation.</p>
	 */
	MANTIS_11_BASIC("http://localhost/mantis-1.2/api/soap/mantisconnect.php");
	
	private String defaultUrl;

	RepositoryConfiguration(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}
	
	public String getDefaultUrl() {
		return defaultUrl;
	}

}
