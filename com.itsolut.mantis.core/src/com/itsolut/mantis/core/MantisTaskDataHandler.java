/*******************************************************************************
 * Copyright (c) 2008 - 2008 Standards for Technology in Automotive Retail
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     David Carver - STAR - adapted from Bugzilla mylyn 3.0 implementation.
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.TaskComment;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;
import org.eclipse.mylyn.tasks.core.TaskRepository;

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
 * @author Dave Carver
 */
public class MantisTaskDataHandler extends AbstractTaskDataHandler {


	private final MantisRepositoryConnector connector;

	public MantisTaskDataHandler(MantisRepositoryConnector connector) {
		this.connector = connector;
	}

	@Override
	public TaskAttributeMapper getAttributeMapper(TaskRepository taskRepository) {
		return new MantisAttributeMapper(taskRepository);
	}

	@Override
	public boolean initializeTaskData(TaskRepository repository, TaskData data,
			ITaskMapping initializationData, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RepositoryResponse postTaskData(TaskRepository repository,
			TaskData taskData, Set<TaskAttribute> oldAttributes,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
		try {
		    int id = Integer.parseInt(taskId);
		    return downloadTaskData(repository, id);
		} catch ( NumberFormatException e) {
		    throw new CoreException(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, "Task id must be numeric", e));
		}
	}
	
	protected TaskData downloadTaskData(TaskRepository repository, int id) throws CoreException {
		if (!MantisRepositoryConnector.hasRichEditor(repository)) {
			throw new CoreException(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Does not have a rich editor", null));
		}
		
		TaskAttributeMapper attributeMapper = getAttributeMapper(repository);

		try {
			TaskData data = new TaskData(attributeMapper, connector.getConnectorKind(), repository.getRepositoryUrl(), Integer.toString(id));
			IMantisClient client = connector.getClientManager().getRepository(repository);
			client.updateAttributes(new NullProgressMonitor(), false);
			MantisTicket ticket = client.getTicket(id);
			createDefaultAttributes(attributeMapper, data, client, true);
			updateTaskData(repository, attributeMapper, data, client, ticket);
			createProjectSpecificAttributes(data, client);
			return data;
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Ticket download from "
					+ repository.getRepositoryUrl() + " for task " + id + " failed, please see details.", e));
		}
	}
	
	public static void updateTaskData(TaskRepository repository, TaskAttributeMapper attributeMapper, TaskData data, 
			                          IMantisClient client, MantisTicket ticket) throws CoreException {
		
		if (ticket.getCreated() != null) {
			data.getRoot().getAttribute(MantisAttributeMapper.Attribute.DATE_SUBMITTED.getMantisKey()).setValue(MantisUtils.toMantisTime(ticket.getCreated()) + "");
		}
		
		if (ticket.getLastChanged() != null) {
			data.getRoot().getAttribute(MantisAttributeMapper.Attribute.LAST_UPDATED.getMantisKey()).setValue(MantisUtils.toMantisTime(ticket.getLastChanged()) + "");
		}

		Map<String, String> valueByKey = ticket.getValues();
		for (String key : valueByKey.keySet()) {
			data.getRoot().getAttribute(key).setValue(valueByKey.get(key));
			
		}

		MantisComment[] comments = ticket.getComments();
		if (comments != null) {
			for (int i = 0; i < comments.length; i++) {
			    TaskAttribute attributeComment = data.getRoot().createAttribute(TaskAttribute.PREFIX_COMMENT);
				TaskCommentMapper taskComment = TaskCommentMapper.createFrom(attributeComment);
				taskComment.setCommentId(Integer.toString(i + 1));
				IRepositoryPerson author = data.getAttributeMapper().getTaskRepository().createPerson(comments[i].getReporter());
				taskComment.setAuthor(author);
				if (comments[i].getLastModified() != null) {
					taskComment.setCreationDate(comments[i].getLastModified());
				} else {
					taskComment.setCreationDate(comments[i].getDateSubmitted());
				}
				taskComment.setText(comments[i].getText());
				taskComment.applyTo(attributeComment);
			}
		}

		MantisAttachment[] attachments = ticket.getAttachments();
		if (attachments != null) {
			for (int i = 0; i < attachments.length; i++) {
			    TaskAttribute attachmentAttribute = attributeMapper.createTaskAttachment(data);
				TaskAttachmentMapper taskAttachment = TaskAttachmentMapper.createFrom(attributeMapper.createTaskAttachment(data));
				taskAttachment.setUrl(MantisTask.getRepositoryBaseUrl(repository.getRepositoryUrl()) + IMantisClient.TICKET_ATTACHMENT_URL + attachments[i].getId());
				taskAttachment.setDescription("");
				taskAttachment.setFileName(attachments[i].getFilename());
				taskAttachment.setCreationDate(attachments[i].getCreated());
				taskAttachment.setAttachmentId(Integer.toString(attachments[i].getId()));
				taskAttachment.applyTo(attachmentAttribute);
			}
		}
		
		// relationships - support only child issues
		MantisRelationship[] relationsShips = ticket.getRelationships();
        for (MantisRelationship mantisRelationship : relationsShips)
            if (mantisRelationship.getType().equals(MantisRelationship.RelationType.PARENT))
            	data.getRoot().createAttribute(MantisAttributeMapper.Attribute.RELATIONSHIPS.getMantisKey()).addValue(String.valueOf(mantisRelationship.getTargetId()));
		
		// add operations
        TaskAttribute operationAttribute = data.getRoot().createAttribute(TaskAttribute.OPERATION);
        TaskOperation.applyTo(operationAttribute, "leave", "Leave as " + data.getRoot().getAttribute(TaskAttribute.STATUS).getValue());
		
		// Assign To Operation
		String[] users = client.getUsers(ticket.getValue(MantisTicket.Key.PROJECT));
		
		TaskAttribute operationAssignTo = data.getRoot().createAttribute(TaskAttribute.OPERATION);
		for (String user : users) {
			operationAssignTo.putOption(user, user);
		}
		TaskOperation.applyTo(operationAssignTo, "assign_to", "Assign To");
		
		// Resolve As Operation
		MantisResolution [] options = client.getTicketResolutions();
		TaskAttribute operationResolveAs = data.getRoot().createAttribute(TaskAttribute.OPERATION);
		for(MantisResolution op : options ) {
			operationResolveAs.putOption(op.getName(), op.getName());
		}
		operationResolveAs.setValue("fixed");
		TaskOperation.applyTo(operationResolveAs, "resolve_as", "Resolve As");
	}

	public static void createDefaultAttributes(TaskAttributeMapper attributeMapper, TaskData data,
			IMantisClient client, boolean existingTask) throws CoreException {
		if (existingTask) {
		}

		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.RESOLUTION, client.getTicketResolutions(), client.getTicketResolutions()[0].getName());
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.STATUS, client.getTicketStatus(), client.getTicketStatus()[0].getName());
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.PRIORITY, client.getPriorities(), client.getPriorities()[0].getName());
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.SEVERITY, client.getSeverities(), client.getSeverities()[0].getName());
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.PROJECTION, client.getProjection(), client.getProjection()[0].getName());
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.ETA, client.getETA(), client.getETA()[0].getName());
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.REPRODUCIBILITY, client.getReproducibility(), client.getReproducibility()[0].getName());
		
		createAttributeHidden(attributeMapper, data, MantisAttributeMapper.Attribute.PROJECT, null);
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.CATEGORY, null);
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.VERSION, null);
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.FIXED_IN, null);

		//createAttributeHidden(attributeMapper, data, MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE, null);
		//createAttributeHidden(attributeMapper, data, MantisAttributeMapper.Attribute.ADDITIONAL_INFO, null);
		
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.VIEW_STATE, client.getViewState(), client.getViewState()[0].getName());
		
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.ASSIGNED_TO);
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.REPORTER);
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.SUMMARY);
		createAttribute(attributeMapper, data, MantisAttributeMapper.Attribute.DESCRIPTION);
	}

	public static void createProjectSpecificAttributes(TaskData data, IMantisClient client) {
		
		try {
			// categories
			TaskAttribute attr = data.getRoot().getAttribute(MantisAttributeMapper.Attribute.CATEGORY.getMantisKey());
			attr.clearOptions();
			boolean first = MantisUtils.isEmpty(attr.getValue());
			for(MantisProjectCategory mp : client.getProjectCategories(data.getRoot().getAttribute(MantisAttributeMapper.Attribute.PROJECT.getMantisKey()).getValue())){
				if(first) {
					attr.setValue(mp.toString());
					first=false;
				}
				attr.putOption(mp.toString(), mp.toString());
			}
			
			
			// versions
			TaskAttribute repInVerAttr = data.getRoot().getAttribute(MantisAttributeMapper.Attribute.VERSION.getMantisKey());
			repInVerAttr.clearOptions();
			repInVerAttr.putOption("none", ""); // empty option

			TaskAttribute fixInVerAttr = data.getRoot().getAttribute(MantisAttributeMapper.Attribute.FIXED_IN.getMantisKey());
			fixInVerAttr.clearOptions();
			fixInVerAttr.putOption("none", "");// Add empty option

			for (MantisVersion v : client.getVersions(data.getRoot().getAttribute(MantisAttributeMapper.Attribute.PROJECT.getMantisKey()).getValue())) {

				/*
				 * Only display released versions for the reported in field,
				 * matches the behaviour of the mantis web interface.
				 */
				if (v.isReleased())
					repInVerAttr.putOption(v.getName(), v.getName());
				fixInVerAttr.putOption(v.getName(), v.getName());
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
	
	private static TaskAttribute createAttribute(TaskAttributeMapper attributeMapper, TaskData data, 
			                                               MantisAttributeMapper.Attribute attribute, Object[] values, boolean allowEmtpy) {
		TaskAttribute attr = data.getRoot().getMappedAttribute(attribute.getTaskKey());
		if (values != null && values.length > 0) {
			if (allowEmtpy) {
				attr.putOption("", "");
			}
			for (int i = 0; i < values.length; i++) {
				attr.putOption(values[i].toString(), values[i].toString());
			}
		}
		data.getRoot().deepAddCopy(attr);
		return attr;
	}
	
	private static TaskAttribute createAttribute(TaskAttributeMapper attributeMapper, TaskData data, MantisAttributeMapper.Attribute attribute) {
		TaskAttribute attr =  data.getRoot().createAttribute(attribute.getMantisKey());
		data.getRoot().deepAddCopy(attr);
		return attr;
	}

	private static TaskAttribute createAttribute(TaskAttributeMapper attributeMapper, 
			                                     TaskData data, MantisAttributeMapper.Attribute attribute, Object[] values, String defaultValue) {
		TaskAttribute rta = createAttribute(attributeMapper, data, attribute, values, false);
		rta.setValue(defaultValue);
		return rta;
	}

	private static TaskAttribute createAttribute(TaskAttributeMapper attributeMapper, 
			                                               TaskData data, MantisAttributeMapper.Attribute attribute, Object[] values) {
		return createAttribute(attributeMapper, data, attribute, values, false);
	}

	/**
	 * 
	 * @deprecated
	 */
	private static TaskAttribute createAttributeHidden(TaskAttributeMapper attributeMapper, 
			                                               TaskData data, MantisAttributeMapper.Attribute attribute, Object[] values) {
		TaskAttribute attr = createAttribute(attributeMapper, data, attribute, values);
		data.getRoot().deepAddCopy(attr);
		return attr;
	}
	

	//
//	public String postTaskData(TaskRepository repository, RepositoryTaskData taskData, IProgressMonitor monitor) throws CoreException {		
//		try {
//			MantisTicket ticket = MantisRepositoryConnector.getMantisTicket(repository, taskData);
//			IMantisClient server = ((MantisRepositoryConnector) connector).getClientManager().getRepository(repository);
//			if (taskData.isNew()) {
//				int id = server.createTicket(ticket);
//				return Integer.toString(id);
//			} else {
//								
//				String comment = taskData.getNewComment();
//				// XXX: new comment is now an attribute
//				taskData.removeAttribute(RepositoryTaskAttribute.COMMENT_NEW);
//				server.updateTicket(ticket, comment);
//				return null;
//			}
//		} catch (Exception e) {
//			MantisCorePlugin.log(e);
//			throw new CoreException(MantisCorePlugin.toStatus(e));
//		}
//	}
//
//
	
	
	
//	@Override
//    public Set<String> getSubTaskIds(RepositoryTaskData taskData) {
//	    
//	    RepositoryTaskAttribute taskAttribute = taskData.getAttribute(MantisAttribute.RELATIONSHIPS.getMantisKey());
//	    if ( taskAttribute == null)
//	        return Collections.<String>emptySet();
//	        
//	    return new HashSet<String>(taskAttribute.getValues());
//	}
//	

	/**
	 * Given a Mantis Ticket create the necessary TaskData object
	 * @param IMantisClient client
	 * @param TaskRepository repository
	 * @param MantisTicket ticket
	 * @param IProgressMontiro monitor
	 * 
	 * @since 3.0
	 */
	public TaskData createTaskDataFromTicket(IMantisClient client, TaskRepository repository,
			MantisTicket ticket, IProgressMonitor monitor) throws CoreException {
		TaskData taskData = new TaskData(getAttributeMapper(repository), MantisCorePlugin.REPOSITORY_KIND,
				repository.getRepositoryUrl(), ticket.getId() + "");
		try {
			if (!MantisRepositoryConnector.hasRichEditor(repository)) {
				updateTaskDataFromTicket(taskData, ticket, client);
				taskData.setPartial(true);
			} else {
				createDefaultAttributes(getAttributeMapper(repository), taskData, client, true);
				updateTaskData(repository, getAttributeMapper(repository), taskData, client, ticket);
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
	public void updateTaskDataFromTicket(TaskData taskData, MantisTicket ticket, IMantisClient client) {
	
		TaskAttribute attributeSummary = taskData.getRoot().getAttribute(TaskAttribute.SUMMARY);
		if (ticket.getValue(Key.SUMMARY) != null) {
			attributeSummary.setValue(ticket.getValue(Key.SUMMARY));
		}

		TaskAttribute attributeCompletionDate = taskData.getRoot().getAttribute(TaskAttribute.DATE_COMPLETION);

		if (MantisTask.isCompleted(ticket.getValue(Key.STATUS))) {
			attributeCompletionDate.setValue(ticket.getLastChanged().toString());
		} else {
			attributeCompletionDate.setValue(null);
		}

		String priority = ticket.getValue(Key.PRIORITY);
		TaskAttribute attributePriority = taskData.getRoot().getAttribute(TaskAttribute.PRIORITY);
		attributePriority.setValue(MantisTask.getMylynPriority(priority).toString());

		TaskAttribute attributeCreationDate = taskData.getRoot().getAttribute(TaskAttribute.DATE_CREATION);
		if (ticket.getCreated() != null) {
			attributeCreationDate.setValue(ticket.getCreated().toString());
		}
	}
}