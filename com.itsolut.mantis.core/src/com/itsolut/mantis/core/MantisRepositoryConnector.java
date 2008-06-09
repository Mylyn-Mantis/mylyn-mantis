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
 * Copyright (c) 2007, 2008 - 2007 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *     David Carver - STAR - fixed issue with background synchronization of repository.
 *     David Carver - STAR - Migrated to Mylyn 3.0
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

import com.itsolut.mantis.core.IMantisClient.Version;
import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Dave Carver - STAR - Standards for Technology in Automotive Retail
 * @author Chris Hane
 */
public class MantisRepositoryConnector extends AbstractRepositoryConnector {

	private final static String CLIENT_LABEL = "Mantis (supports connector 0.0.5 or 1.1.0a4 or greater only)";

	private MantisClientManager clientManager;

	private MantisTaskDataHandler offlineTaskHandler = new MantisTaskDataHandler(
			this);

	private MantisAttachmentHandler attachmentHandler = new MantisAttachmentHandler(
			this);

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
	public String getConnectorKind() {
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
		return index == -1 ? null : url.substring(index
				+ IMantisClient.TICKET_URL.length());
	}

	@Override
	public String getTaskUrl(String repositoryUrl, String taskId) {

		return new StringBuilder(MantisTask.getRepositoryBaseUrl(repositoryUrl))
				.append(IMantisClient.URL_SHOW_BUG).append(taskId).toString();
	}

	@Override
	public AbstractTaskAttachmentHandler getTaskAttachmentHandler() {
		return this.attachmentHandler;
	}

	@Override
	public AbstractTaskDataHandler getTaskDataHandler() {
		return offlineTaskHandler;
	}

	public IStatus performQuery(TaskRepository repository,
			IRepositoryQuery query, TaskDataCollector resultCollector,
			ISynchronizationSession event, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		final List<MantisTicket> tickets = new ArrayList<MantisTicket>();

		IMantisClient client;
		try {
			client = getClientManager().getRepository(repository);
			if (query instanceof MantisRepositoryQuery) {
				updateAttributes(repository, monitor);
				client.search(
						((MantisRepositoryQuery) query).getMantisSearch(),
						tickets);
			}

			for (MantisTicket ticket : tickets) {
				TaskData taskData = offlineTaskHandler
						.createTaskDataFromTicket(client, repository, ticket,
								monitor);
				resultCollector.accept(taskData);
			}
		} catch (Throwable e) {
			MantisCorePlugin.log(e);
			return MantisCorePlugin.toStatus(e);
		}

		return Status.OK_STATUS;

	}

	protected void updateAttributes(TaskRepository repository,
			IProgressMonitor monitor) throws CoreException {
		try {
			IMantisClient client = getClientManager().getRepository(repository);
			client.updateAttributes(monitor, true);
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(RepositoryStatus.createStatus(repository
					.getRepositoryUrl(), IStatus.WARNING,
					MantisCorePlugin.PLUGIN_ID, "Could not update attributes"));
		}
	}

	public synchronized MantisClientManager getClientManager() {
		if (clientManager == null) {
			File cacheFile = null;
			if (MantisCorePlugin.getDefault().getRepostioryAttributeCachePath() != null) {
				cacheFile = MantisCorePlugin.getDefault()
						.getRepostioryAttributeCachePath().toFile();
			}
			clientManager = new MantisClientManager(cacheFile);
		}
		return clientManager;
	}

	// public MantisTask createTask(String repositoryUrl, String taskId, String
	// summary) {
	// MantisTask task;
	// ITask existingTask = taskList.getTask(repositoryUrl, taskId);
	// if (existingTask instanceof MantisTask) {
	// task = (MantisTask) existingTask;
	// } else {
	// task = new MantisTask(repositoryUrl, taskId, summary);
	// taskList.addTask(task);
	// }
	// return task;
	// }

	/**
	 * Updates fields of <code>task</code> from <code>ticket</code>.
	 */
	// public void updateTaskDetails(MantisTask task, MantisTicket ticket,
	// boolean notify) {
	// if(ticket.getValue(Key.SUMMARY) != null) {
	// task.setSummary(getTicketDescription(ticket));
	// }
	//		
	// task.setCompleted(MantisTask.isCompleted(ticket.getValue(Key.STATUS)));
	//task.setPriority(MantisTask.getMylynPriority(ticket.getValue(Key.PRIORITY)
	// ).toString());
	// if (ticket.getValue(Key.TYPE) != null) {
	// Kind kind = MantisTask.Kind.fromType(ticket.getValue(Key.TYPE));
	// task.setKind((kind != null) ? kind.toString() :
	// ticket.getValue(Key.TYPE));
	// }
	// if (ticket.getCreated() != null) {
	// task.setCreationDate(ticket.getCreated());
	// }
	//
	// if (notify) {
	// // TODO: Check whatever to pass true or false
	// taskList.notify();
	// //taskList.notifyTaskChanged(task, true);
	// }
	// }
	public static String getTicketDescription(MantisTicket ticket) {
		return ticket.getValue(Key.SUMMARY);
	}

	public static String getTicketDescription(TaskData taskData) {
		return taskData.getRoot().getMappedAttribute(
				MantisAttributeFactory.Attribute.DESCRIPTION.toString())
				.toString();
	}

	public static boolean hasChangedSince(TaskRepository repository) {
		return Version.MC_1_0a5.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository) {
		return Version.MC_1_0a5.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository,
			AbstractTask task) {
		return hasRichEditor(repository);
	}

	public static boolean hasAttachmentSupport(TaskRepository repository,
			ITask task) {
		return true;
	}

	public void stop() {
		if (clientManager != null) {
			clientManager.writeCache();
		}
	}

	public static String getDisplayUsername(TaskRepository repository) {
		AuthenticationCredentials credentials = repository
				.getCredentials(AuthenticationType.REPOSITORY);
		if (credentials == null) {
			return IMantisClient.DEFAULT_USERNAME;
		}
		return credentials.getUserName();
	}

	@Override
	public String getTaskIdPrefix() {
		return "#";
	}

	// public static MantisTicket getMantisTicket(TaskRepository repository,
	// RepositoryTaskData data) throws InvalidTicketException {
	// MantisTicket ticket = new MantisTicket(Integer.parseInt(data.getId()));
	//	
	// List<TaskAttribute> attributes = data.getAttributes();
	// for (RepositoryTaskAttribute attribute : attributes) {
	// if (MantisAttributeFactory.isInternalAttribute(attribute.getId())) {
	// // ignore
	// } else if (!attribute.isReadOnly()) {
	// ticket.putValue(attribute.getId(), attribute.getValue());
	// }
	// }
	//		
	// // handle operations
	// RepositoryOperation operation = data.getSelectedOperation();
	// if (operation != null) {
	// String action = operation.getKnobName();
	// if ("assign_to".equals(action)) {
	// ticket.putBuiltinValue(Key.ASSIGNED_TO, operation.getOptionSelection());
	// ticket.putBuiltinValue(Key.STATUS, "assigned");
	// }else if( "resolve_as".equals(action)) {
	// ticket.putBuiltinValue(Key.RESOLUTION, operation.getOptionSelection());
	// ticket.putBuiltinValue(Key.STATUS, "resolved");
	// }
	// }
	//	
	// return ticket;
	// }

	// TODO: not changed for Mantis yet...

	// @Override
	// public boolean markStaleTasks(TaskRepository repository,
	// Set<AbstractTask> tasks, IProgressMonitor monitor)
	// throws CoreException {
	// // TODO Auto-generated method stub
	// try {
	// monitor.beginTask("Getting changed tasks", IProgressMonitor.UNKNOWN);
	//			
	// if (!MantisRepositoryConnector.hasChangedSince(repository)) {
	// // always run the queries for web mode
	// return true;
	// }
	//						
	// if (repository.getSynchronizationTimeStamp() == null) {
	// for (AbstractTask task : tasks) {
	// task.setStale(true);
	// }
	// return true;
	// }
	//
	// Date since = new Date(0);
	// try {
	// since = MantisUtils.parseDate(Integer.parseInt(repository.
	// getSynchronizationTimeStamp()));
	// } catch (NumberFormatException e) {
	// MantisCorePlugin.log(e);
	// }
	//
	// try {
	//				
	// // Run the queries to get the list of tasks currently meeting the query
	// // criteria. The result returned are only the ids that have changed.
	// // Next checkt to see if any of these ids matches the ones in the
	// // task list. If so, then set it to stale.
	// // The prior implementation retireved each id individually, and checked
	// it's
	// // date, this caused unnecessary SOAP traffic during synchronization.
	//	
	// for (IRepositoryQuery query : taskList.getQueries()) {
	// List<Integer> taskIds = this.getChangedTasksByQuery(query, repository,
	// since);
	// if (taskIds != null && taskIds.size() > 0) {
	// for (Integer taskId : taskIds) {
	// for (AbstractTask task : tasks) {
	// if (getTicketId(task.getTaskId()) == taskId.intValue()) {
	// task.setStale(true);
	// }
	// }
	// }
	// }
	// }
	//
	// return true;
	// } catch (Exception e) {
	// MantisCorePlugin.log(e);
	// throw new CoreException(new Status(IStatus.ERROR,
	// MantisCorePlugin.PLUGIN_ID, IStatus.OK,
	// "Could not determine changed tasks", e));
	// }
	// } finally {
	// monitor.done();
	// }
	// }

	public static int getTicketId(String taskId) throws CoreException {
		try {
			return Integer.parseInt(taskId);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					MantisCorePlugin.PLUGIN_ID, IStatus.OK,
					"Invalid ticket id: " + taskId, e));
		}
	}

	// For the repositories, perform the queries to get the latest information
	// about the
	// tasks. This allows the connector to get a limited list of items instead
	// of every
	// item in the repository. Next check to see if the tasks have changed since
	// the
	// last synchronization. If so, add their ids to a List.
	private List<Integer> getChangedTasksByQuery(IRepositoryQuery query,
			TaskRepository repository, Date since) {

		final List<MantisTicket> tickets = new ArrayList<MantisTicket>();
		List<Integer> changedTickets = new ArrayList<Integer>();

		IMantisClient client;
		try {
			client = getClientManager().getRepository(repository);
			if (query instanceof MantisRepositoryQuery) {
				client.search(
						((MantisRepositoryQuery) query).getMantisSearch(),
						tickets);
			}

			for (MantisTicket ticket : tickets) {
				if (ticket.getLastChanged() != null) {
					if (ticket.getLastChanged().compareTo(since) > 0)
						changedTickets.add(new Integer(ticket.getId()));
				}
			}
		} catch (Throwable e) {
			MantisCorePlugin.log(e);
			return null;
		}
		return changedTickets;
	}

	@Override
	public void updateRepositoryConfiguration(TaskRepository repository,
			IProgressMonitor monitor) throws CoreException {
		try {
			updateAttributes(repository, monitor);
		} catch (Exception e) {
			throw new CoreException(RepositoryStatus.createStatus(repository
					.getRepositoryUrl(), IStatus.WARNING,
					MantisCorePlugin.PLUGIN_ID, "Could not update attributes"));
		}

	}

	@Override
	public TaskData getTaskData(TaskRepository repository, String taskId,
			IProgressMonitor monitor) throws CoreException {
		return offlineTaskHandler.getTaskData(repository, taskId, monitor);
	}

	@Override
	public boolean hasTaskChanged(TaskRepository taskRepository, ITask task,
			TaskData taskData) {
		if (taskData.isPartial()) {
			return false;
		}
		String lastKnownMod = task.getAttribute(MantisAttributeMapper.Attribute.LAST_UPDATED.getTaskKey());
		if (lastKnownMod != null) {
			TaskAttribute attrModification = taskData.getRoot()
					.getMappedAttribute(TaskAttribute.DATE_MODIFICATION);
			if (attrModification != null) {
				return !lastKnownMod.equals(attrModification.getValue());
			}

		}
		return true;
	}

	@Override
	public void updateTaskFromTaskData(TaskRepository repository, ITask task,
			TaskData taskData) {
		TaskMapper scheme = new TaskMapper(taskData);
		scheme.applyTo(task);

		if (task instanceof MantisTask) {
			MantisTask mtask = (MantisTask) task;

			mtask.setSummary(getTicketDescription(taskData));
			mtask.setOwner(taskData.getRoot().getMappedAttribute(
					MantisAttributeFactory.Attribute.ASSIGNED_TO.toString())
					.toString());
			mtask.setCompletionDate(new Date(taskData.getRoot().getAttribute(
					TaskAttribute.STATUS).getValue()));
			mtask.setUrl(MantisTask.getRepositoryBaseUrl(repository
					.getRepositoryUrl())
					+ IMantisClient.URL_SHOW_BUG + taskData.getTaskId());
			mtask.setPriority(MantisTask.getMylynPriority(
					taskData.getRoot().getAttribute(
							Attribute.PRIORITY.getMantisKey()).getValue())
					.toString());
			mtask.setSeverity(taskData.getRoot().getAttribute(
					Attribute.SEVERITY.getMantisKey()).getValue());
		}
	}
}