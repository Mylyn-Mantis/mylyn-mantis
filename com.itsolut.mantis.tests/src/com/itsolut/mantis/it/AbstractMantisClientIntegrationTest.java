/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.exception.TicketNotFoundException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;

public abstract class AbstractMantisClientIntegrationTest extends AbstractIntegrationTest {

	private static final String UTF8 = "UTF-8";

	@Test
	public void testValidate() throws MantisException {

		repositoryAccessor.getClient().validate(new NullProgressMonitor());
	}

	@Test
	public void testRefreshAttributes() throws MantisException {

		repositoryAccessor.getClient().updateAttributes(new NullProgressMonitor());
	}

	@Test
	public void testGetExistingTask() throws MantisException, MalformedURLException, RemoteException, ServiceException {

		String summary = "Summary";
		String description = "Description";
		
		int taskId = createTask(summary, description);

		MantisTicket ticket = repositoryAccessor.getClient().getTicket(taskId, new NullProgressMonitor());

		assertEquals(summary, ticket.getValue(Key.SUMMARY));
		assertEquals(description, ticket.getValue(Key.DESCRIPTION));
	}
	
	@Test(expected = TicketNotFoundException.class)
	public void testGetInexistentTask() throws MantisException, MalformedURLException, RemoteException, ServiceException {

		repositoryAccessor.getClient().getTicket(-500, new NullProgressMonitor());
	}
	
	@Test
	public void testRetrieveIssuesUsingBuiltInFilter() throws MalformedURLException, RemoteException, ServiceException, MantisException {
		
		int firstTaskId = createTask("First task", "Description");
		int secondTaskId = createTask("Second task", "Description");
		
		MantisCache cache = repositoryAccessor.getClient().getCache(new NullProgressMonitor());
		MantisProject project = cache.getProjectById(DEFAULT_PROJECT_ID.intValue());
		
		List<MantisProjectFilter> projectFilters = cache.getProjectFilters(1);
		
		assertEquals(projectFilters.toString(), 1, projectFilters.size());
		
		MantisProjectFilter filter = projectFilters.get(0);
		
		List<MantisTicket> results = new ArrayList<MantisTicket>(2);
		
		repositoryAccessor.getClient().search(new MantisSearch(project.getName(), filter.getName()), results, new NullProgressMonitor());
		
		assertEquals(2, results.size());
		assertTicketIsFoundById(results, firstTaskId);
		assertTicketIsFoundById(results, secondTaskId);

	}
	
	private void assertTicketIsFoundById(List<MantisTicket> results, int firstTaskId) {
		
		for ( MantisTicket ticket : results )
			if ( ticket.getId() == firstTaskId)
				return;
		
		fail("Ticket with id " + firstTaskId + " not found in result " + results + " .");
		
	}
	
	@Test
	public void testUploadAndDownload() throws MalformedURLException, RemoteException, ServiceException, UnsupportedEncodingException, MantisException {

		String attachmentContents = "Sample attachment";
		
		int taskId = createTask("Attachment test task", "Description");
		
		repositoryAccessor.getClient().putAttachmentData(taskId, "sample.txt", attachmentContents.getBytes(UTF8), new NullProgressMonitor());
		
		MantisTicket ticket = repositoryAccessor.getClient().getTicket(taskId, new NullProgressMonitor());
		
		assertEquals(1, ticket.getAttachments().length);
		
		int attachmentId = ticket.getAttachments()[0].getId();
		
		byte[] attachmentData = repositoryAccessor.getClient().getAttachmentData(attachmentId, new NullProgressMonitor());
		
		assertEquals(attachmentContents, new String(attachmentData, UTF8));
	}


}
