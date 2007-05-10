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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.IAttachmentHandler;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.ITaskDataHandler;
import org.eclipse.mylar.tasks.core.QueryHitCollector;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskRepository;

import com.itsolut.mantis.core.IMantisClient.Version;
import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisRepositoryConnector extends AbstractRepositoryConnector {

	private final static String CLIENT_LABEL = "Mantis (supports connector 0.0.5 only)";

	private List<String> supportedVersions;

	private MantisClientManager clientManager;

	private MantisOfflineTaskHandler offlineTaskHandler = new MantisOfflineTaskHandler(this);

	private MantisAttachmentHandler attachmentHandler = new MantisAttachmentHandler(this);

	public MantisRepositoryConnector() {
		MantisCorePlugin.getDefault().setConnector(this);
	}

	@Override
	public boolean canCreateNewTask(TaskRepository repository) {
		return true;
	}

	@Override
	public boolean canCreateTaskFromKey(TaskRepository repository) {
		return true;
	}

	@Override
	public String getLabel() {
		return CLIENT_LABEL;
	}

	@Override
	public String getRepositoryType() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public String getRepositoryUrlFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.lastIndexOf(IMantisClient.TICKET_URL);
		return index == -1 ? null : url.substring(0, index);
	}

	public String getTaskIdFromTaskUrl(String url) {
		if (url == null) {
			return null;
		}
		int index = url.lastIndexOf(IMantisClient.TICKET_URL);
		return index == -1 ? null : url.substring(index + IMantisClient.TICKET_URL.length());
	}

	@Override
	public String getTaskWebUrl(String repositoryUrl, String taskId) {
		return repositoryUrl + IMantisClient.TICKET_URL + taskId;
	}

	
	@Override
	public List<String> getSupportedVersions() {
		if (supportedVersions == null) {
			supportedVersions = new ArrayList<String>();
			for (Version version : Version.values()) {
				supportedVersions.add(version.toString());
			}
		}
		return supportedVersions;
	}

	@Override
	public IAttachmentHandler getAttachmentHandler() {
		return attachmentHandler;
	}

	@Override
	public ITaskDataHandler getTaskDataHandler() {
		return offlineTaskHandler;
	}

	@Override
	public void updateTask(TaskRepository repository, AbstractRepositoryTask repositoryTask) throws CoreException {
		if (repositoryTask instanceof MantisTask) {
			String id = AbstractRepositoryTask.getTaskId(repositoryTask.getHandleIdentifier());
			try {
				IMantisClient connection = getClientManager().getRepository(repository);
				MantisTicket ticket = connection.getTicket(Integer.parseInt(id));
				updateTaskDetails((MantisTask) repositoryTask, ticket, false);
			} catch (Exception e) {
				throw new CoreException(MantisCorePlugin.toStatus(e));
			}
		}
	}

	@Override
	public IStatus performQuery(AbstractRepositoryQuery query, TaskRepository repository, IProgressMonitor monitor, QueryHitCollector resultCollector) {

		final List<MantisTicket> tickets = new ArrayList<MantisTicket>();

		IMantisClient client;
		try {
			client = getClientManager().getRepository(repository);
			if (query instanceof MantisRepositoryQuery) {
				client.search(((MantisRepositoryQuery) query).getMantisSearch(), tickets);
			}

			for (MantisTicket ticket : tickets) {
				MantisQueryHit hit = new MantisQueryHit(taskList, query.getRepositoryUrl(), getTicketDescription(ticket), ticket.getId() + "");
				hit.setPriority(ticket.getValue(Key.PRIORITY));
				hit.setCompleted(false);
//				hit.setPriority(MantisTask.getMylarPriority(ticket.getValue(Key.PRIORITY)));
//				hit.setCompleted(MantisTask.isCompleted(ticket.getValue(Key.STATUS)));
				resultCollector.accept(hit);
			}
		} catch (Throwable e) {			
			return MantisCorePlugin.toStatus(e);			
		}

		return Status.OK_STATUS;
	}

	@Override
	public AbstractRepositoryTask createTaskFromExistingKey(TaskRepository repository, String id) throws CoreException {
		int bugId = -1;
		try {
			bugId = Integer.parseInt(id);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK,
						"Invalid ticket id: " + id, e));
		}
		
		String handle = AbstractRepositoryTask.getHandle(repository.getUrl(), bugId);
		
		MantisTask task;
		ITask existingTask = taskList.getTask(handle);
		if (existingTask instanceof MantisTask) {
			task = (MantisTask) existingTask;
		} else {
			RepositoryTaskData taskData = offlineTaskHandler.downloadTaskData(repository, bugId);
//			if (taskData != null) {
				task = new MantisTask(handle, getTicketDescription(taskData), true);
				task.setTaskData(taskData);
				taskList.addTask(task);
//			} else {
//				// repository does not support XML-RPC, fall back to web access
//				try {
//					IMantisClient connection = getClientManager().getRepository(repository);
//					MantisTicket ticket = connection.getTicket(Integer.parseInt(id));
//
//					task = new MantisTask(handle, getTicketDescription(ticket), true);
//					updateTaskDetails(task, ticket, false);
//					taskList.addTask(task);
//				} catch (Exception e) {
//					throw new CoreException(MantisCorePlugin.toStatus(e));
//				}
//			}
		}
		return task;
	}

	public synchronized MantisClientManager getClientManager() {
		if (clientManager == null) {
			File cacheFile = null;
			if (MantisCorePlugin.getDefault().getRepostioryAttributeCachePath() != null) {
				cacheFile = MantisCorePlugin.getDefault().getRepostioryAttributeCachePath().toFile();
			}
			clientManager = new MantisClientManager(cacheFile);
		}
		return clientManager;
	}

	public MantisTask createTask(MantisTicket ticket, String handleIdentifier) {
		MantisTask task;
		ITask existingTask = taskList.getTask(handleIdentifier);
		if (existingTask instanceof MantisTask) {
			task = (MantisTask) existingTask;
		} else {
			task = new MantisTask(handleIdentifier, getTicketDescription(ticket), true);
			taskList.addTask(task);
		}
		return task;
	}

	/**
	 * Updates fields of <code>task</code> from <code>ticket</code>.
	 */
	public void updateTaskDetails(MantisTask task, MantisTicket ticket, boolean notify) {
		
		task.setDescription(ticket.getValue(Key.DESCRIPTION));
		RepositoryTaskData data = task.getTaskData();
		
		data.setAttributeValue(Attribute.DESCRIPTION.toString(), ticket.getValue(Key.DESCRIPTION));
		
//		if (ticket.getValue(Key.SUMMARY) != null) {
//			task.setDescription(getTicketDescription(ticket));
//		}
//		task.setCompleted(MantisTask.isCompleted(ticket.getValue(Key.STATUS)));
//		task.setPriority(MantisTask.getMylarPriority(ticket.getValue(Key.PRIORITY)));
//		if (ticket.getValue(Key.TYPE) != null) {
//			Kind kind = MantisTask.Kind.fromType(ticket.getValue(Key.TYPE));
//			task.setKind((kind != null) ? kind.toString() : ticket.getValue(Key.TYPE));
//		}
		if (ticket.getCreated() != null) {
			task.setCreationDate(ticket.getCreated());
		}

		if (notify) {
			taskList.notifyLocalInfoChanged(task);
		}
	}

	public static String getTicketDescription(MantisTicket ticket) {
		return /* ticket.getId() + ": " + */ ticket.getValue(Key.SUMMARY);
	}

	public static String getTicketDescription(RepositoryTaskData taskData) {
		return taskData.getId() + ":" + taskData.getSummary();
	}

	public static boolean hasChangedSince(TaskRepository repository) {
		return Version.MC_1_0a5.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository) {
		return Version.MC_1_0a5.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository, AbstractRepositoryTask task) {
		return hasRichEditor(repository);
	}

	public static boolean hasAttachmentSupport(TaskRepository repository, AbstractRepositoryTask task) {
		return true;
	}

	public void stop() {
		if (clientManager != null) {
			clientManager.writeCache();
		}
	}

	@Override
	public void updateAttributes(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
		try {
			IMantisClient client = getClientManager().getRepository(repository);
			client.updateAttributes(monitor, true);
		} catch (Exception e) {
			MylarStatusHandler.fail(e, "Could not update attributes", false);
		}
	}
	
	public static String getDisplayUsername(TaskRepository repository) {
		if (!repository.hasCredentials()) {
			return IMantisClient.DEFAULT_USERNAME;
		}
		return repository.getUserName();
	}

	@Override
	public String getTaskIdPrefix() {
		return "#";
	}

	public static MantisTicket getMantisTicket(TaskRepository repository, RepositoryTaskData data) throws InvalidTicketException {
		MantisTicket ticket = new MantisTicket(Integer.parseInt(data.getId()));
	
		List<RepositoryTaskAttribute> attributes = data.getAttributes();
		for (RepositoryTaskAttribute attribute : attributes) {
			if (MantisAttributeFactory.isInternalAttribute(attribute.getID())) {
				// ignore
			} else if (!attribute.isReadOnly()) {
				ticket.putValue(attribute.getID(), attribute.getValue());
			}
		}

//		// set cc value
//		StringBuilder sb = new StringBuilder();
//		List<String> removeValues = data.getAttributeValues(RepositoryTaskAttribute.REMOVE_CC);
//		List<String> values = data.getAttributeValues(RepositoryTaskAttribute.USER_CC);
//		for (String user : values) {
//			if (!removeValues.contains(user)) {
//				if (sb.length() > 0) {
//					sb.append(",");
//				}
//				sb.append(user);
//			}
//		}
//		if (data.getAttributeValue(RepositoryTaskAttribute.NEW_CC).length() > 0) {
//			if (sb.length() > 0) {
//				sb.append(",");
//			}
//			sb.append(data.getAttributeValue(RepositoryTaskAttribute.NEW_CC));
//		}
//		if (RepositoryTaskAttribute.TRUE.equals(data.getAttributeValue(RepositoryTaskAttribute.ADD_SELF_CC))) {
//			if (sb.length() > 0) {
//				sb.append(",");
//			}
//			sb.append(repository.getUserName());
//		}
//		ticket.putBuiltinValue(Key.CC, sb.toString());
		
//		RepositoryOperation operation = data.getSelectedOperation();
//		if (operation != null) {
//			String action = operation.getKnobName();
//			if (!"leave".equals(action)) {
//				if ("accept".equals(action)) {
//					ticket.putValue("status", "assigned");
//					ticket.putValue("owner", getDisplayUsername(repository));
//				} else if ("resolve".equals(action)) {
//					ticket.putValue("status", "closed");
//					ticket.putValue("resolution", operation.getOptionSelection());
//				} else if ("reopen".equals(action)) {
//					ticket.putValue("status", "reopened");
//					ticket.putValue("resolution", "");
//				} else if ("reassign".equals(operation.getKnobName())) {
//					ticket.putValue("status", "new");
//					ticket.putValue("owner", operation.getInputValue());
//				}
//			}
//		}
	
		return ticket;
	}
	
}