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
 *     David Carver - fixed issue with background synchronization of repository.
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
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.tasks.core.AbstractAttachmentHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.ITaskCollector;
import org.eclipse.mylyn.tasks.core.RepositoryOperation;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import com.itsolut.mantis.core.IMantisClient.Version;
import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisRepositoryConnector extends AbstractRepositoryConnector {

	private final static String CLIENT_LABEL = "Mantis (supports connector 0.0.5 or 1.1.0a4 or greater only)";

	private MantisClientManager clientManager;

	private MantisTaskDataHandler offlineTaskHandler = new MantisTaskDataHandler(this);

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
		return index == -1 ? null : url.substring(index + IMantisClient.TICKET_URL.length());
	}

	@Override
	public String getTaskUrl(String repositoryUrl, String taskId) {
		
		return new StringBuilder(MantisTask.getRepositoryBaseUrl(repositoryUrl)).append(IMantisClient.URL_SHOW_BUG).append(taskId).toString();		
	}

	@Override
	public AbstractAttachmentHandler getAttachmentHandler() {
		return attachmentHandler;
	}

	@Override
	public AbstractTaskDataHandler getTaskDataHandler() {
		return offlineTaskHandler;
	}
	
	@Override
	public void updateTaskFromRepository(TaskRepository repository, AbstractTask repositoryTask, IProgressMonitor monitor) throws CoreException {
		if (repositoryTask instanceof MantisTask) {
			String id = RepositoryTaskHandleUtil.getTaskId(repositoryTask.getHandleIdentifier());
			try {
				IMantisClient connection = getClientManager().getRepository(repository);
				MantisTicket ticket = connection.getTicket(Integer.parseInt(id));
				updateTaskDetails((MantisTask) repositoryTask, ticket, false);
			} catch (Exception e) {
				MantisCorePlugin.log(e);
				throw new CoreException(MantisCorePlugin.toStatus(e));
			}
		}
	}

	@Override
	public IStatus performQuery(AbstractRepositoryQuery query, TaskRepository repository, IProgressMonitor monitor, ITaskCollector resultCollector) {

		final List<MantisTicket> tickets = new ArrayList<MantisTicket>();

		IMantisClient client;
		try {
			client = getClientManager().getRepository(repository);
			if (query instanceof MantisRepositoryQuery) {
				updateAttributes(repository, monitor);
				client.search(((MantisRepositoryQuery) query).getMantisSearch(), tickets);
			}

			for (MantisTicket ticket : tickets) {
				AbstractTask task = createTask(repository.getUrl(), ticket.getId() + "", "");
				updateTaskFromTicket((MantisTask) task, ticket, false);
				resultCollector.accept(task);

			}
		} catch (Throwable e) {
			MantisCorePlugin.log(e);
			return MantisCorePlugin.toStatus(e);			
		}

		return Status.OK_STATUS;
	}
	

	private void updateTaskFromTicket(MantisTask task, MantisTicket ticket, boolean notify) {
		if (ticket.getValue(Key.SUMMARY) != null) {
			task.setSummary(ticket.getValue(Key.SUMMARY));
		}
		task.setCompleted(MantisTask.isCompleted(ticket.getValue(Key.STATUS)));
		task.setPriority(MantisTask.getMylynPriority(ticket.getValue(Key.PRIORITY)));
		task.setSeverity(ticket.getValue(Key.SEVERITY));
		
		if (ticket.getCreated() != null) {
			task.setCreationDate(ticket.getCreated());
		}

		if (notify) {
			taskList.notifyTaskChanged(task, false);
		}

	}
	
	@Override
	public AbstractTask createTaskFromExistingId(
			TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
		
		AbstractTask task = super.createTaskFromExistingId(repository, taskId, monitor);
//		int bugId = -1;
//		try {
//			bugId = Integer.parseInt(taskId);
//		} catch (NumberFormatException e) {
//			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK,
//						"Invalid ticket id: " + taskId, e));
//		}
//		
//		MantisTask task;
//		ITask existingTask = taskList.getTask(repository.getUrl(), taskId);
//		if (existingTask instanceof MantisTask) {
//			task = (MantisTask) existingTask;
//		} else {
//			RepositoryTaskData taskData = offlineTaskHandler.downloadTaskData(repository, bugId);
//				task = new MantisTask(repository.getUrl(), taskId, getTicketDescription(taskData), true);
//				task.setTaskData(taskData);
//				taskList.addTask(task);
//		}
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

	public MantisTask createTask(String repositoryUrl, String taskId, String summary) {
		MantisTask task;
		AbstractTask existingTask = taskList.getTask(repositoryUrl, taskId);
		if (existingTask instanceof MantisTask) {
			task = (MantisTask) existingTask;
		} else {
			task = new MantisTask(repositoryUrl, taskId, summary);
			taskList.addTask(task);
		}
		return task;
	}

	/**
	 * Updates fields of <code>task</code> from <code>ticket</code>.
	 */
	public void updateTaskDetails(MantisTask task, MantisTicket ticket, boolean notify) {
		if(ticket.getValue(Key.SUMMARY) != null) {
			task.setSummary(getTicketDescription(ticket));			
		}
		
		task.setCompleted(MantisTask.isCompleted(ticket.getValue(Key.STATUS)));
		task.setPriority(MantisTask.getMylynPriority(ticket.getValue(Key.PRIORITY)));
		
//		if (ticket.getValue(Key.TYPE) != null) {
//			Kind kind = MantisTask.Kind.fromType(ticket.getValue(Key.TYPE));
//			task.setKind((kind != null) ? kind.toString() : ticket.getValue(Key.TYPE));
//		}
		if (ticket.getCreated() != null) {
			task.setCreationDate(ticket.getCreated());
		}

		if (notify) {
			// TODO: Check whatever to pass true or false
			taskList.notifyTaskChanged(task, true);
		}
	}

	public static String getTicketDescription(MantisTicket ticket) {
		return ticket.getValue(Key.SUMMARY);
	}

	public static String getTicketDescription(RepositoryTaskData taskData) {
		return taskData.getSummary();
	}

	public static boolean hasChangedSince(TaskRepository repository) {
		return Version.MC_1_0a5.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository) {
		return Version.MC_1_0a5.name().equals(repository.getVersion());
	}

	public static boolean hasRichEditor(TaskRepository repository, AbstractTask task) {
		return hasRichEditor(repository);
	}

	public static boolean hasAttachmentSupport(TaskRepository repository, AbstractTask task) {
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
			MantisCorePlugin.log(e);
			throw new CoreException(RepositoryStatus.createStatus(repository.getUrl(), IStatus.WARNING,
					MantisCorePlugin.PLUGIN_ID, "Could not update attributes"));
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
			if (MantisAttributeFactory.isInternalAttribute(attribute.getId())) {
				// ignore
			} else if (!attribute.isReadOnly()) {
				ticket.putValue(attribute.getId(), attribute.getValue());
			}
		}
		
		// handle operations
		RepositoryOperation operation = data.getSelectedOperation();
		if (operation != null) {
			String action = operation.getKnobName();
			if ("assign_to".equals(action)) {
				ticket.putBuiltinValue(Key.ASSIGNED_TO, operation.getOptionSelection());
				ticket.putBuiltinValue(Key.STATUS, "assigned");
			}else if( "resolve_as".equals(action)) {
				ticket.putBuiltinValue(Key.RESOLUTION, operation.getOptionSelection());
				ticket.putBuiltinValue(Key.STATUS, "resolved");
			}
		}
	
		return ticket;
	}
	
	//TODO: not changed for Mantis yet...
	

	@Override
	public void updateTaskFromTaskData(TaskRepository repository,
			AbstractTask repositoryTask, RepositoryTaskData taskData) {
		if(repositoryTask instanceof MantisTask) {
			MantisTask task = (MantisTask)repositoryTask;
			
			task.setSummary(getTicketDescription(taskData));
			task.setOwner(taskData.getAttributeValue(RepositoryTaskAttribute.USER_ASSIGNED));
			task.setCompleted(MantisTask.isCompleted(taskData.getStatus()));
			task.setUrl(MantisTask.getRepositoryBaseUrl(repository.getUrl()) + IMantisClient.URL_SHOW_BUG + taskData.getId());
			task.setPriority(MantisTask.getMylynPriority(taskData.getAttributeValue(Attribute.PRIORITY.getMantisKey())));
			task.setSeverity(taskData.getAttributeValue(Attribute.SEVERITY.getMantisKey()));
		}
		
		
		// TODO: Completion Date		
	}
	
	@Override
	public boolean markStaleTasks(TaskRepository repository,
			Set<AbstractTask> tasks, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("Getting changed tasks", IProgressMonitor.UNKNOWN);
			
			if (!MantisRepositoryConnector.hasChangedSince(repository)) {
				// always run the queries for web mode
				return true;
			}
						
			if (repository.getSynchronizationTimeStamp() == null) {
				for (AbstractTask task : tasks) {
					task.setStale(true);
				}
				return true;
			}

			Date since = new Date(0);
			try {
				since = MantisUtils.parseDate(Integer.parseInt(repository.getSynchronizationTimeStamp()));
			} catch (NumberFormatException e) {
				MantisCorePlugin.log(e);
			}

			try {
				
				// Run the queries to get the list of tasks currently meeting the query
				// criteria.  The result returned are only the ids that have changed.
				// Next checkt to see if any of these ids matches the ones in the
				// task list.  If so, then set it to stale.
				// The prior implementation retireved each id individually, and checked it's
				// date, this caused unnecessary SOAP traffic during synchronization.
	
                for (AbstractRepositoryQuery query : taskList.getQueries()) {
                    List<Integer> taskIds = this.getChangedTasksByQuery(query, repository, since);
                    if (taskIds != null && taskIds.size() > 0) {
            			for (Integer taskId : taskIds) {
            				for (AbstractTask task : tasks) {
            				   if (getTicketId(task.getTaskId()) == taskId.intValue()) {
            						task.setStale(true);
            					}
            				}
        				}
                    }
				}

				return true;
			} catch (Exception e) {
				MantisCorePlugin.log(e);
				throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK,
						"Could not determine changed tasks", e));
			}
		} finally {
			monitor.done();
		}
	}
	
	public static int getTicketId(String taskId) throws CoreException {
		try {
			return Integer.parseInt(taskId);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK,
					"Invalid ticket id: " + taskId, e));
		}
	}
	
	// For the repositories, perform the queries to get the latest information about the 
	// tasks.  This allows the connector to get a limited list of items instead of every
	// item in the repository.   Next check to see if the tasks have changed since the
	// last synchronization.  If so, add their ids to a List.
	private List<Integer> getChangedTasksByQuery(AbstractRepositoryQuery query, TaskRepository repository, Date since) {

		final List<MantisTicket> tickets = new ArrayList<MantisTicket>();
        List<Integer> changedTickets = new ArrayList<Integer>(); 
		
		IMantisClient client;
		try {
			client = getClientManager().getRepository(repository);
			if (query instanceof MantisRepositoryQuery) {
				client.search(((MantisRepositoryQuery) query).getMantisSearch(), tickets);
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
}