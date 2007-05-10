/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylar.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.ITaskDataHandler;
import org.eclipse.mylar.tasks.core.RepositoryAttachment;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskComment;
import org.eclipse.mylar.tasks.core.TaskRepository;

import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisOfflineTaskHandler implements ITaskDataHandler {

	private AbstractAttributeFactory attributeFactory = new MantisAttributeFactory();

	private MantisRepositoryConnector connector;

	public MantisOfflineTaskHandler(MantisRepositoryConnector connector) {
		this.connector = connector;
	}

	public RepositoryTaskData getTaskData(TaskRepository repository, String taskId) throws CoreException {
		int id = Integer.parseInt(taskId);
		return downloadTaskData(repository, id);
	}

	public RepositoryTaskData downloadTaskData(TaskRepository repository, int id) throws CoreException {
		if (!MantisRepositoryConnector.hasRichEditor(repository)) {
			throw new CoreException(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Does not have a rich editor", null));
		}

		try {
			RepositoryTaskData data = new RepositoryTaskData(attributeFactory, MantisCorePlugin.REPOSITORY_KIND, repository.getUrl(), id + "");
			IMantisClient client = connector.getClientManager().getRepository(repository);
			client.updateAttributes(new NullProgressMonitor(), false);
			MantisTicket ticket = client.getTicket(id);
			createDefaultAttributes(attributeFactory, data, client, true);
			updateTaskData(repository, attributeFactory, data, client, ticket);
			createProjectSpecificAttributes(data, client);
			return data;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Ticket download from "
					+ repository.getUrl() + " for task " + id + " failed, please see details.", e));
		}
	}

	public AbstractAttributeFactory getAttributeFactory() {
		return attributeFactory;
	}

	public Date getDateForAttributeType(String attributeKey, String dateString) {
		if (dateString == null || dateString.length() == 0) {
			return null;
		}

		try {
			String mappedKey = attributeFactory.mapCommonAttributeKey(attributeKey);
			if (mappedKey.equals(Attribute.DATE_SUBMITTED.getMantisKey()) || mappedKey.equals(Attribute.LAST_UPDATED.getMantisKey())) {
				return MantisUtils.parseDate(Integer.valueOf(dateString));
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static void updateTaskData(TaskRepository repository, AbstractAttributeFactory factory, RepositoryTaskData data, 
			                          IMantisClient client, MantisTicket ticket) throws CoreException {
		
		if (ticket.getCreated() != null) {
			data.setAttributeValue(Attribute.DATE_SUBMITTED.getMantisKey(), MantisUtils.toMantisTime(ticket.getCreated()) + "");
		}
		if (ticket.getLastChanged() != null) {
			data.setAttributeValue(Attribute.LAST_UPDATED.getMantisKey(), MantisUtils.toMantisTime(ticket.getLastChanged()) + "");
		}
		Map<String, String> valueByKey = ticket.getValues();
		for (String key : valueByKey.keySet()) {
			data.setAttributeValue(key, valueByKey.get(key));
		}

		MantisComment[] comments = ticket.getComments();
		if (comments != null) {
			for (int i = 0; i < comments.length; i++) {
				TaskComment taskComment = new TaskComment(factory, data.getComments().size() + 1);
				taskComment.setAttributeValue(RepositoryTaskAttribute.USER_OWNER, comments[i].getReporter());
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_DATE, comments[i].getDateSubmitted().toString());
				taskComment.setAttributeValue(RepositoryTaskAttribute.DATE_MODIFIED, comments[i].getLastModified().toString());
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_TEXT, comments[i].getText());
//				taskComment.setAttributeValue("", comments[i].isSynched());
				data.addComment(taskComment);
			}
		}

		MantisAttachment[] attachments = ticket.getAttachments();
		if (attachments != null) {
			for (int i = 0; i < attachments.length; i++) {
				RepositoryAttachment taskAttachment = new RepositoryAttachment(factory);
//				taskAttachment.setCreator(attachments[i].getAuthor());
				taskAttachment.setRepositoryKind(MantisCorePlugin.REPOSITORY_KIND);
				taskAttachment.setRepositoryUrl(repository.getUrl());
				taskAttachment.setTaskId(""+ticket.getId());
				taskAttachment.setAttributeValue(Attribute.DESCRIPTION.getMantisKey(), attachments[i].getFilename());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_FILENAME, attachments[i]
						.getFilename());
//				taskAttachment.setAttributeValue(RepositoryTaskAttribute.USER_OWNER, attachments[i].getAuthor());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_DATE, attachments[i].getCreated()
						.toString());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_URL, repository.getUrl()
						+ IMantisClient.TICKET_ATTACHMENT_URL + ticket.getId() + "/" + attachments[i].getFilename());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_ID, attachments[i].getId()+"");
				data.addAttachment(taskAttachment);
			}
		}

//		String[] actions = ticket.getActions();
//		if (actions != null) {
//			// add operations in a defined order
//			List<String> actionList = new ArrayList<String>(Arrays.asList(actions));
//			addOperation(repository, data, ticket, actionList, "leave");
//			addOperation(repository, data, ticket, actionList, "accept");
//			addOperation(repository, data, ticket, actionList, "resolve");
//			addOperation(repository, data, ticket, actionList, "reassign");
//			addOperation(repository, data, ticket, actionList, "reopen");
//		}
	}

//	// TODO Reuse Labels from BugzillaServerFacade
//	private static void addOperation(TaskRepository repository, RepositoryTaskData data, MantisTicket ticket,
//			List<String> actions, String action) {
//		if (!actions.remove(action)) {
//			return;
//		}
//
//		RepositoryOperation operation = null;
//		if ("leave".equals(action)) {
//			operation = new RepositoryOperation(action, "Leave as " + data.getStatus() + " " + data.getResolution());
//			operation.setChecked(true);
//		} else if ("accept".equals(action)) {
//			operation = new RepositoryOperation(action, "Accept");
//		} else if ("resolve".equals(action)) {
//			operation = new RepositoryOperation(action, "Resolve as");
//			operation.setUpOptions("resolution");
//			for (String resolution : ticket.getResolutions()) {
//				operation.addOption(resolution, resolution);
//			}
//		} else if ("reassign".equals(action)) {
//			operation = new RepositoryOperation(action, "Reassign to");
//			operation.setInputName("owner");
//			operation.setInputValue(MantisRepositoryConnector.getDisplayUsername(repository));
//		} else if ("reopen".equals(action)) {
//			operation = new RepositoryOperation(action, "Reopen");
//		}
//
//		if (operation != null) {
//			data.addOperation(operation);
//		}
//	}

	public static void createDefaultAttributes(AbstractAttributeFactory factory, RepositoryTaskData data,
			IMantisClient client, boolean existingTask) throws CoreException {
		if (existingTask) {
		}

		createAttribute(factory, data, Attribute.RESOLUTION, client.getTicketResolutions(), client.getTicketResolutions()[0].getName());
		createAttribute(factory, data, Attribute.STATUS, client.getTicketStatus(), client.getTicketStatus()[0].getName());
//		createAttribute(factory, data, Attribute.VERSION, client.getVersions(), true);
		createAttribute(factory, data, Attribute.PRIORITY, client.getPriorities(), client.getPriorities()[0].getName());
		createAttribute(factory, data, Attribute.SEVERITY, client.getSeverities(), client.getSeverities()[0].getName());
		createAttribute(factory, data, Attribute.PROJECTION, client.getProjection(), client.getProjection()[0].getName());
		createAttribute(factory, data, Attribute.ETA, client.getETA(), client.getETA()[0].getName());
		createAttribute(factory, data, Attribute.REPRODUCIBILITY, client.getReproducibility(), client.getReproducibility()[0].getName());
		
		createAttributeHidden(factory, data, Attribute.PROJECT, null);
		createAttribute(factory, data, Attribute.CATEGORY, null);

		createAttributeHidden(factory, data, Attribute.STEPS_TO_REPRODUCE, null);
		createAttributeHidden(factory, data, Attribute.ADDITIONAL_INFO, null);
		
		createAttribute(factory, data, Attribute.VIEW_STATE, client.getViewState(), client.getViewState()[0].getName());

		createAttribute(factory, data, Attribute.ASSIGNED_TO);
		createAttribute(factory, data, Attribute.REPORTER);
		createAttribute(factory, data, Attribute.SUMMARY);
		createAttribute(factory, data, Attribute.DESCRIPTION);
//		if (existingTask) {
//		}

		if (!existingTask) {
		}
	}

	public static void createProjectSpecificAttributes(RepositoryTaskData data, IMantisClient client) {
		
		try {
//			RepositoryTaskAttribute attr = data.getAttribute(Attribute.PROJECT.getMantisKey());
//			attr.setReadOnly(false);
//			attr.clearOptions();
//			for(MantisProject mp : client.getProjects()){
//				attr.addOption(MantisUtils.stripProject(mp.toString()), MantisUtils.stripProject(mp.toString()));
//			}
			
			RepositoryTaskAttribute attr = data.getAttribute(Attribute.CATEGORY.getMantisKey());
			attr.setReadOnly(false);
			attr.clearOptions();
			boolean first = MantisUtils.isEmpty(attr.getValue());
			for(MantisProjectCategory mp : client.getProjectCategories(data.getAttributeValue(Attribute.PROJECT.getMantisKey()))){
				if(first) {
					attr.setValue(mp.toString());
					first=false;
				}
				attr.addOption(mp.toString(), mp.toString());
			}
			
		} catch (MantisException ex) {
			MantisCorePlugin.log(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, 0, ex.getMessage(), ex));
		}
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, RepositoryTaskData data, 
			                                               Attribute attribute, Object[] values, boolean allowEmtpy) {
		RepositoryTaskAttribute attr = factory.createAttribute(attribute.getMantisKey());
		if (values != null && values.length > 0) {
			if (allowEmtpy) {
				attr.addOption("", "");
			}
			for (int i = 0; i < values.length; i++) {
				attr.addOption(values[i].toString(), values[i].toString());
			}
		} else {
			// attr.setHidden(true);
			attr.setReadOnly(true);
		}
		data.addAttribute(attribute.getMantisKey(), attr);
		return attr;
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, RepositoryTaskData data, Attribute attribute) {
		RepositoryTaskAttribute attr = factory.createAttribute(attribute.getMantisKey());
		data.addAttribute(attribute.getMantisKey(), attr);
		return attr;
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, 
			                                               RepositoryTaskData data, Attribute attribute, Object[] values, String defaultValue) {
		RepositoryTaskAttribute rta = createAttribute(factory, data, attribute, values, false);
		rta.setValue(defaultValue);
		return rta;
	}

	private static RepositoryTaskAttribute createAttribute(AbstractAttributeFactory factory, 
			                                               RepositoryTaskData data, Attribute attribute, Object[] values) {
		return createAttribute(factory, data, attribute, values, false);
	}

	private static RepositoryTaskAttribute createAttributeHidden(AbstractAttributeFactory factory, 
			                                               RepositoryTaskData data, Attribute attribute, Object[] values) {
		RepositoryTaskAttribute attr = factory.createAttribute(attribute.getMantisKey());
		attr.setHidden(true);
		data.addAttribute(attribute.getMantisKey(), attr);
		return attr;
	}
	
	//TODO: not changed for Mantis yet...
	public Set<AbstractRepositoryTask> getChangedSinceLastSync(TaskRepository repository, Set<AbstractRepositoryTask> tasks) throws CoreException, UnsupportedEncodingException {
		if (repository.getSyncTimeStamp() == null) {
			return tasks;
		}

		if (!MantisRepositoryConnector.hasChangedSince(repository)) {
			// return an empty list to avoid causing all tasks to synchronized
			return Collections.emptySet();
		}

		Date since = new Date(0);
		try {
			since = MantisUtils.parseDate(Integer.parseInt(repository.getSyncTimeStamp()));
		} catch (NumberFormatException e) {
		}

		IMantisClient client;
		try {
			client = connector.getClientManager().getRepository(repository);
			Set<Integer> ids = client.getChangedTickets(since);

			Set<AbstractRepositoryTask> result = new HashSet<AbstractRepositoryTask>();
			if (!ids.isEmpty()) {
				for (AbstractRepositoryTask task : tasks) {
					Integer id = Integer.parseInt(AbstractRepositoryTask.getTaskId(task.getHandleIdentifier()));
					if (ids.contains(id)) {
						result.add(task);
					}
				}
			}
			return result;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "could not determine changed tasks", e));
		}
	}

	public String postTaskData(TaskRepository repository, RepositoryTaskData taskData) throws CoreException {
		return null;
	}
}
