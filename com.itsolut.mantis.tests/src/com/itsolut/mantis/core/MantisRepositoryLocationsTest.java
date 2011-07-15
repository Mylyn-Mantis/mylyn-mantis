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

package com.itsolut.mantis.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.itsolut.mantis.core.MantisRepositoryLocations;

/**
 * @author Robert Munteanu
 */
public class MantisRepositoryLocationsTest {

	@Test(expected=RuntimeException.class)
	public void testInvalidValue() {

		MantisRepositoryLocations.create(null);
	}
	
	@Test
	public void testParseFromBaseUrl() {
		
		String baseUrl = "http://mylyn-mantis.sourceforge.net/Mantis/";
		
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/", MantisRepositoryLocations.create(baseUrl).getBaseRepositoryLocation());
	}
	
	@Test
	public void testParseFromSoapUrl() {
		
		String baseUrl = "http://mylyn-mantis.sourceforge.net/Mantis/api/soap/mantisconnect.php";
		
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/", MantisRepositoryLocations.create(baseUrl).getBaseRepositoryLocation());
	}

	@Test
	public void testParseFromIssueUrl() {
		
		String baseUrl = "http://mylyn-mantis.sourceforge.net/Mantis/view.php?id=163";
		
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/", MantisRepositoryLocations.create(baseUrl).getBaseRepositoryLocation());
	}
	
	@Test
	public void testGetUrlWhenBaseHasTrailingSlash() {
		
		verifyLocationsAreCorrect("http://mylyn-mantis.sourceforge.net/Mantis/");
	}

	private void verifyLocationsAreCorrect(String baseUrl) {
		
		MantisRepositoryLocations locations = MantisRepositoryLocations.create(baseUrl);
		
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/api/soap/mantisconnect.php", locations.getSoapApiLocation());
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/view.php?id=1", locations.getTaskLocation(1));
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/file_download.php?type=bug&file_id=2", locations.getAttachmentDownloadLocation(2));
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/signup_page.php", locations.getSignupLocation());
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/account_page.php", locations.getAccountManagementLocation());
	}

	@Test
	public void testGetUrlWhenBaseDoesNotHaveTrailingSlash() {
		
		verifyLocationsAreCorrect("http://mylyn-mantis.sourceforge.net/Mantis");
	}
	
	@Test(expected = RuntimeException.class )
	public void testInvalidTaskUrl () {

		MantisRepositoryLocations.extractTaskId(null);
	}

	@Test
	public void testGetTaskIdFromUrl() {
		
		assertEquals(MantisRepositoryLocations.extractTaskId("http://mylyn-mantis.sourceforge.net/Mantis/view.php?id=1"), Integer.valueOf(1));
	}

	@Test
	public void getTaskIdFromUrlWithHash() {

		assertEquals(MantisRepositoryLocations.extractTaskId("http://mylyn-mantis.sourceforge.net/Mantis/view.php?id=56#comments"), Integer.valueOf(56));
	}

}
