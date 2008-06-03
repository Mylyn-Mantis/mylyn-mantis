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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractAttributeFactory;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractTaskDataHandler;
import org.eclipse.mylyn.internal.tasks.core.deprecated.DefaultTaskSchema;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryOperation;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskAttribute;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskData;
import org.eclipse.mylyn.internal.tasks.core.deprecated.TaskComment;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;

import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTaskDataHandler extends AbstractTaskDataHandler {

	private AbstractAttributeFactory attributeFactory = new MantisAttributeFactory();

	private MantisRepositoryConnector connector;

	public MantisTaskDataHandler(MantisRepositoryConnector connector) {
		this.connector = connector;
	}

	public RepositoryTaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
		try {
		    int id = Integer.parseInt(taskId);
		    return downloadTaskData(repository, id);
		} catch ( NumberFormatException e) {
		    throw new CoreException(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, "Task id must be numeric", e));
		}
	}
	
	public RepositoryTaskData downloadTaskData(TaskRepository repository, int id) throws CoreException {
		if (!MantisRepositoryConnector.hasRichEditor(repository)) {
			throw new CoreException(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Does not have a rich editor", null));
		}

		try {
			RepositoryTaskData data = new RepositoryTaskData(attributeFactory, MantisCorePlugin.REPOSITORY_KIND, repository.getUrl(), id + "", AbstractTask.DEFAULT_TASK_KIND);
			IMantisClient client = connector.getClientManager().getRepository(repository);
			client.updateAttributes(new NullProgressMonitor(), false);
			MantisTicket ticket = client.getTicket(id);
			createDefaultAttributes(attributeFactory, data, client, true);
			updateTaskData(repository, attributeFactory, data, client, ticket);
			createProjectSpecificAttributes(data, client);
			return data;
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Ticket download from "
					+ repository.getUrl() + " for task " + id + " failed, please see details.", e));
		}
	}
	
	public AbstractAttributeFactory getAttributeFactory(
			RepositoryTaskData taskData) {
		return getAttributeFactory(taskData.getRepositoryUrl(), taskData.getRepositoryKind(), taskData.getTaskKind());
	}
	
	public AbstractAttributeFactory getAttributeFactory(String repositoryUrl, String repositoryKind, String taskKind) {
		return attributeFactory;
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
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_AUTHOR, comments[i].getReporter()); 
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_DATE, comments[i].getDateSubmitted().toString());
				taskComment.setAttributeValue(RepositoryTaskAttribute.DATE_MODIFIED, comments[i].getLastModified().toString());
				taskComment.setAttributeValue(RepositoryTaskAttribute.COMMENT_TEXT, comments[i].getText());
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
				
				// see javadoc for MantisAttachmentHandler for an explanation of this workaround
//				if ( attachments[i].getFilename().startsWith(MantisAttachmentHandler.MYLAR_CONTEXT_DESCRIPTION))
//					taskAttachment.setAttributeValue(Attribute.DESCRIPTION.getMantisKey(), MantisAttachmentHandler.MYLAR_CONTEXT_DESCRIPTION);
//				else
					taskAttachment.setAttributeValue(Attribute.DESCRIPTION.getMantisKey(), attachments[i].getFilename());
				
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_FILENAME, attachments[i]
						.getFilename());
//				taskAttachment.setAttributeValue(RepositoryTaskAttribute.USER_OWNER, attachments[i].getAuthor());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_DATE, attachments[i].getCreated()
						.toString());
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_URL, ( MantisTask.getRepositoryBaseUrl(repository.getUrl()) + IMantisClient.TICKET_ATTACHMENT_URL
				+ attachments[i].getId()));
				taskAttachment.setAttributeValue(RepositoryTaskAttribute.ATTACHMENT_ID, attachments[i].getId()+"");
				data.addAttachment(taskAttachment);
			}
		}
		
		// relationships - support only child issues
		MantisRelationship[] relationsShips = ticket.getRelationships();
        for (MantisRelationship mantisRelationship : relationsShips)
            if (mantisRelationship.getType().equals(MantisRelationship.RelationType.PARENT))
                data.addAttributeValue(Attribute.RELATIONSHIPS.getMantisKey(), String.valueOf(mantisRelationship.getTargetId()));
		
		// add operations
		RepositoryOperation operation = new RepositoryOperation("leave", "Leave as " + data.getStatus());
		operation.setChecked(true);
		data.addOperation(operation);
		
		// Assign To Operation
		String[] users = client.getUsers(ticket.getValue(MantisTicket.Key.PROJECT));
		
		operation = new RepositoryOperation("assign_to", "Assign To");
		operation.setUpOptions("users");
		for (String user : users) {
			operation.addOption(user, user);
		}
		
		data.addOperation(operation);
		
		// Resolve As Operation
		MantisResolution [] options = client.getTicketResolutions();
		operation = new RepositoryOperation("resolve_as", "Resolve As");
		operation.setUpOptions("resolutions");
		for(MantisResolution op : options ) {
			operation.addOption(op.getName(), op.getName());
		}
		operation.setOptionSelection("fixed");
		data.addOperation(operation);
	}

	public static void createDefaultAttributes(AbstractAttributeFactory factory, RepositoryTaskData data,
			IMantisClient client, boolean existingTask) throws CoreException {
		if (existingTask) {
		}

		createAttribute(factory, data, Attribute.RESOLUTION, client.getTicketResolutions(), client.getTicketResolutions()[0].getName());
		createAttribute(factory, data, Attribute.STATUS, client.getTicketStatus(), client.getTicketStatus()[0].getName());
		createAttribute(factory, data, Attribute.PRIORITY, client.getPriorities(), client.getPriorities()[0].getName());
		createAttribute(factory, data, Attribute.SEVERITY, client.getSeverities(), client.getSeverities()[0].getName());
		createAttribute(factory, data, Attribute.PROJECTION, client.getProjection(), client.getProjection()[0].getName());
		createAttribute(factory, data, Attribute.ETA, client.getETA(), client.getETA()[0].getName());
		createAttribute(factory, data, Attribute.REPRODUCIBILITY, client.getReproducibility(), client.getReproducibility()[0].getName());
		
		createAttributeHidden(factory, data, Attribute.PROJECT, null);
		createAttribute(factory, data, Attribute.CATEGORY, null);
		createAttribute(factory, data, Attribute.VERSION, null);
		createAttribute(factory, data, Attribute.FIXED_IN, null);

		createAttributeHidden(factory, data, Attribute.STEPS_TO_REPRODUCE, null);
		createAttributeHidden(factory, data, Attribute.ADDITIONAL_INFO, null);
		
		createAttribute(factory, data, Attribute.VIEW_STATE, client.getViewState(), client.getViewState()[0].getName());
		
		createAttribute(factory, data, Attribute.ASSIGNED_TO);
		createAttribute(factory, data, Attribute.REPORTER);
		createAttribute(factory, data, Attribute.SUMMARY);
		createAttribute(factory, data, Attribute.DESCRIPTION);

		if (!existingTask) {
		}
	}

	public static void createProjectSpecificAttributes(RepositoryTaskData data, IMantisClient client) {
		
		try {
			// categories
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
			
			// versions
			RepositoryTaskAttribute repInVerAttr = data.getAttribute(Attribute.VERSION.getMantisKey());
			repInVerAttr.setReadOnly(false);
			repInVerAttr.clearOptions();
			repInVerAttr.addOption("none", ""); // empty option

			RepositoryTaskAttribute fixInVerAttr = data.getAttribute(Attribute.FIXED_IN.getMantisKey());
			fixInVerAttr.setReadOnly(false);
			fixInVerAttr.clearOptions();
			fixInVerAttr.addOption("none", "");// Add empty option

			for (MantisVersion v : client.getVersions(data.getAttributeValue(Attribute.PROJECT.getMantisKey()))) {

				/*
				 * Only display released versions for the reported in field,
				 * matches the behaviour of the mantis web interface.
				 */
				if (v.isReleased())
					repInVerAttr.addOption(v.getName(), v.getName());
				fixInVerAttr.addOption(v.getName(), v.getName());
			}
			
			/* If the value is empty then the issue has not yet been fixed */
			if (MantisUtils.isEmpty(fixInVerAttr.getValue())) 
				fixInVerAttr.setValue("none");
			
			if (MantisUtils.isEmpty(repInVerAttr.getValue())) 
				repInVerAttr.setValue("none");


			
			
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

	public String postTaskData(TaskRepository repository, RepositoryTaskData taskData, IProgressMonitor monitor) throws CoreException {		
		try {
			MantisTicket ticket = MantisRepositoryConnector.getMantisTicket(repository, taskData);
			IMantisClient server = ((MantisRepositoryConnector) connector).getClientManager().getRepository(repository);
			if (taskData.isNew()) {
				int id = server.createTicket(ticket);
				return Integer.toString(id);
			} else {
								
				String comment = taskData.getNewComment();
				// XXX: new comment is now an attribute
				taskData.removeAttribute(RepositoryTaskAttribute.COMMENT_NEW);
				server.updateTicket(ticket, comment);
				return null;
			}
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(MantisCorePlugin.toStatus(e));
		}
	}

	public boolean initializeTaskData(TaskRepository repository,
			RepositoryTaskData data, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
    public Set<String> getSubTaskIds(RepositoryTaskData taskData) {
	    
	    RepositoryTaskAttribute taskAttribute = taskData.getAttribute(Attribute.RELATIONSHIPS.getMantisKey());
	    if ( taskAttribute == null)
	        return Collections.<String>emptySet();
	        
	    return new HashSet<String>(taskAttribute.getValues());
	}
	
	public RepositoryTaskData createTaskDataFromTicket(IMantisClient client, TaskRepository repository,
			MantisTicket ticket, IProgressMonitor monitor) throws CoreException {
		RepositoryTaskData taskData = new RepositoryTaskData(attributeFactory, MantisCorePlugin.REPOSITORY_KIND,
				repository.getRepositoryUrl(), ticket.getId() + "");
		try {
			if (!MantisRepositoryConnector.hasRichEditor(repository)) {
				updateTaskDataFromTicket(taskData, ticket, client);
				taskData.setPartial(true);
			} else {
				createDefaultAttributes(attributeFactory, taskData, client, true);
				updateTaskData(repository, attributeFactory, taskData, client, ticket);
			}
			return taskData;
		} catch (OperationCanceledException e) {
			throw e;
		} catch (Exception e) {
			// TODO catch TracException
			throw new CoreException(MantisCorePlugin.toStatus(e));
		}
	}
	
	/**
	 * Updates attributes of <code>taskData</code> from <code>ticket</code>.
	 */
	public void updateTaskDataFromTicket(RepositoryTaskData taskData, MantisTicket ticket, IMantisClient client) {
		DefaultTaskSchema schema = new DefaultTaskSchema(taskData);
		if (ticket.getValue(Key.SUMMARY) != null) {
			schema.setSummary(ticket.getValue(Key.SUMMARY));
		}

		if (MantisTask.isCompleted(ticket.getValue(Key.STATUS))) {
			schema.setCompletionDate(ticket.getLastChanged());
		} else {
			schema.setCompletionDate(null);
		}

		String priority = ticket.getValue(Key.PRIORITY);
		MantisPriority[] mantisPriorities = client.getPriorities();
		PriorityLevel priorityLevel = MantisTask.getMylynPriority(priority);
		schema.setPriority(MantisTask.getMylynPriority(priority));

		if (ticket.getCreated() != null) {
			schema.setCreationDate(ticket.getCreated());
		}
	}
	
	
}
