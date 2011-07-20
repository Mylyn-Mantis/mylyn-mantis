/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.axis.configuration.FileProvider;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

import biz.futureware.mantis.rpc.soap.client.MantisConnectLocator;
import biz.futureware.mantis.rpc.soap.client.MantisConnectPortType;

import com.google.common.collect.Maps;
import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.MantisAttachmentHandler;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisCommentMapper;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisTaskDataHandler;
import com.itsolut.mantis.core.StatusFactory;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * The <tt>MantisRepositoryAccessor</tt> provides test-specific methods for easy
 * access to a Mantis Repository.
 * 
 * @author Robert Munteanu
 */
public class MantisRepositoryAccessor {
	
	private static final class InMemoryMantisClientManager implements IMantisClientManager {

		private final Map<String, IMantisClient> urlToClient = Maps.newHashMap();

		public synchronized IMantisClient getRepository(TaskRepository taskRepository) throws MantisException {
			
			IMantisClient client = urlToClient.get(taskRepository.getUrl());
			if ( client == null ) {
				client = new MantisClientFactory(new TaskRepositoryLocationFactory()).createClient(taskRepository);
				urlToClient.put(taskRepository.getUrl(), client);
			}

			return client;
		}
		
		public void onShutdown() {
			// ignore
		}
	}

	public static final IMantisClientManager clientManager = new InMemoryMantisClientManager();
	
	public static final MantisRepositoryConnector connector;
	
	static {
		
		StatusFactory statusFactory = new StatusFactory();
		MantisTaskDataHandler dataHandler = new MantisTaskDataHandler(clientManager, statusFactory, new MantisCommentMapper());
		MantisAttachmentHandler attachmentHandler = new MantisAttachmentHandler(clientManager, statusFactory);

		connector = new MantisRepositoryConnector(clientManager, dataHandler, attachmentHandler, statusFactory);
		
	}
	
	private final String username;
	private final String password;
	private final String repositoryUrl;
	
	public MantisRepositoryAccessor(String username, String password, String repositoryUrl) {
		
		this.username = username;
		this.password = password;
		this.repositoryUrl = repositoryUrl;
	}

	private List<Integer> tasksToDelete = new ArrayList<Integer>();

	private IMantisClient client;

	private AbstractWebLocation location;

	private MantisConnectPortType mantisConnectPort;

	private TaskRepository repository;
	
	public AbstractWebLocation getLocation() {

		return location;
	}

	public IMantisClient getClient() {

		return client;
	}
	
	public TaskRepository getRepository() {
		
		return repository;
	}
	
	public MantisConnectPortType getMantisConnectPort() {
	
		return mantisConnectPort;
	}

	public void init() throws Exception {

		repository = new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, repositoryUrl);
		repository.setCredentials(AuthenticationType.REPOSITORY, new AuthenticationCredentials(username,
				password), false);
		location = new TaskRepositoryLocationFactory().createWebLocation(repository);

		client = clientManager.getRepository(repository);

		FileProvider provider = new FileProvider(this.getClass().getClassLoader().getResourceAsStream(
				"test-client-config.wsdd"));

		MantisConnectLocator locator = new MantisConnectLocator(provider);
		mantisConnectPort = locator.getMantisConnectPort(new URL(repositoryUrl));

	}
	
	public void registerIssueToDelete(int issueId) {
		
		tasksToDelete.add(issueId);
	}
	
	public void unregisterIssueToDelete(int issueId ) {
		
		tasksToDelete.remove(Integer.valueOf(issueId));
	}

	public void deleteIssues() throws Exception {

		for (Integer taskToDelete : tasksToDelete)
			mantisConnectPort.mc_issue_delete(username, password, BigInteger.valueOf(taskToDelete.intValue()));
	}
	
}
