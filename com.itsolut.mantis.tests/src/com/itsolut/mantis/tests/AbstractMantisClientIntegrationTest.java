/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.MantisConnectPortType;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;

public abstract class AbstractMantisClientIntegrationTest extends TestCase {

	private static final String UTF8 = "UTF-8";

	private MantisRepositoryAccessor repositoryAccessor;

	protected abstract String getRepositoryUrl();
	
	private String getRepositoryUrlWithOverride() {
		
		return System.getProperty("mantis.test."+getOverrideKey()+".url", getRepositoryUrl());
	}
	
	protected String getOverrideKey () {
		
		return getClass().getSimpleName();
	}

	protected String getPassword() {
		return "root";
	}

	protected String getUsername() {

		return "administrator";
	}

	public void testValidate() throws MantisException {

		getClient().validate(new NullProgressMonitor());
	}

	public void testRefreshAttributes() throws MantisException {

		getClient().updateAttributes(new NullProgressMonitor());
	}

	public void testGetExistingTask() throws MantisException, MalformedURLException, RemoteException, ServiceException {

		String summary = "Summary";
		String description = "Description";
		
		int taskId = createTask(summary, description);

		MantisTicket ticket = getClient().getTicket(taskId, new NullProgressMonitor());

		assertEquals(summary, ticket.getValue(Key.SUMMARY));
		assertEquals(description, ticket.getValue(Key.DESCRIPTION));
	}
	
	public void testRetrieveIssuesUsingBuiltInFilter() throws MalformedURLException, RemoteException, ServiceException, MantisException {
		
		int firstTaskId = createTask("First task", "Description");
		int secondTaskId = createTask("Second task", "Description");
		
		MantisCache cache = getClient().getCache(new NullProgressMonitor());
		MantisProject project = cache.getProjectById(1);
		
		List<MantisProjectFilter> projectFilters = cache.getProjectFilters(1);
		
		assertEquals(projectFilters.toString(), 1, projectFilters.size());
		
		MantisProjectFilter filter = projectFilters.get(0);
		
		List<MantisTicket> results = new ArrayList<MantisTicket>(2);
		
		getClient().search(new MantisSearch(project.getName(), filter.getName()), results, new NullProgressMonitor());
		
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
	
	public void testUploadAndDownload() throws MalformedURLException, RemoteException, ServiceException, UnsupportedEncodingException, MantisException {

		String attachmentContents = "Sample attachment";
		
		int taskId = createTask("Attachment test task", "Description");
		
		getClient().putAttachmentData(taskId, "sample.txt", attachmentContents.getBytes(UTF8), new NullProgressMonitor());
		
		MantisTicket ticket = getClient().getTicket(taskId, new NullProgressMonitor());
		
		assertEquals(1, ticket.getAttachments().length);
		
		int attachmentId = ticket.getAttachments()[0].getId();
		
		byte[] attachmentData = getClient().getAttachmentData(attachmentId, new NullProgressMonitor());
		
		assertEquals(attachmentContents, new String(attachmentData, UTF8));
	}

	protected int createTask(String summary, String description) throws MalformedURLException, ServiceException,
			RemoteException {

		IssueData issue = new IssueData();
		issue.setSummary(summary);
		issue.setDescription(description);
		issue.setProject(new ObjectRef(BigInteger.ONE, ""));
		issue.setCategory("General");

		int newTaskId = getMantisConnectPort().mc_issue_add(getUsername(), getPassword(), issue).intValue();

		repositoryAccessor.registerIssueToDelete(newTaskId);

		return newTaskId;

	}

	protected AbstractWebLocation getLocation() {

		return repositoryAccessor.getLocation();
	}

	protected IMantisClient getClient() {

		return repositoryAccessor.getClient();
	}
	
	protected TaskRepository getRepository() {
		
		return repositoryAccessor.getRepository();
	}
	
	protected MantisConnectPortType getMantisConnectPort() {
		
		return repositoryAccessor.getMantisConnectPort();
	}

	protected void setUp() throws Exception {

		repositoryAccessor = new MantisRepositoryAccessor(getUsername(), getPassword(), getRepositoryUrlWithOverride());
		repositoryAccessor.init();

	}

	protected void tearDown() throws Exception {

		repositoryAccessor.deleteIssues();
	}

}
