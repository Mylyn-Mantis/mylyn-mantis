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
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.*;
import org.eclipse.mylyn.tasks.core.data.*;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.osgi.util.NLS;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Dave Carver - STAR - Standards for Technology in Automotive Retail
 * @author Chris Hane
 */
@SuppressWarnings("restriction")
public class MantisRepositoryConnector extends AbstractRepositoryConnector {

    private final static String CLIENT_LABEL = "Mantis (supports connector 0.0.5 or 1.1.0a4 or greater only)";
    public static final String TASK_KEY_SUPPORTS_SUBTASKS = "SupportsSubtasks";

    private MantisClientManager clientManager;

    private final MantisTaskDataHandler offlineTaskHandler = new MantisTaskDataHandler(this);

    private final MantisAttachmentHandler attachmentHandler = new MantisAttachmentHandler(this);

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

        // There is no way of knowing the proper URL for the repository
        // so we return at least a common prefix which should be good
        // enough for TaskRepositoryManager#getConnectorForRepositoryTaskUrl
        if (url == null) {
            return null;
        }
        int index = url.lastIndexOf(IMantisClient.URL_SHOW_BUG);
        return index == -1 ? null : url.substring(0, index);
    }

    @Override
    public String getTaskIdFromTaskUrl(String url) {

        if (url == null) {
            return null;
        }
        int index = url.lastIndexOf(IMantisClient.URL_SHOW_BUG);
        return index == -1 ? null : url.substring(index + IMantisClient.URL_SHOW_BUG.length());
    }

    @Override
    public String getTaskUrl(String repositoryUrl, String taskId) {

        return MantisUtils.getRepositoryBaseUrl(repositoryUrl) + IMantisClient.URL_SHOW_BUG + taskId.toString();
    }

    @Override
    public AbstractTaskAttachmentHandler getTaskAttachmentHandler() {

        return this.attachmentHandler;
    }

    @Override
    public AbstractTaskDataHandler getTaskDataHandler() {

        return offlineTaskHandler;
    }

    @Override
    public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
            ISynchronizationSession event, IProgressMonitor monitor) {

        final List<MantisTicket> tickets = new ArrayList<MantisTicket>();
        monitor.beginTask("Querying repository", IProgressMonitor.UNKNOWN);

        IMantisClient client;
        try {
            client = getClientManager().getRepository(repository);
            client.search(MantisUtils.getMantisSearch(query), tickets, monitor);
            for (MantisTicket ticket : tickets)
                resultCollector.accept(offlineTaskHandler.createTaskDataFromPartialTicket(client, repository, ticket, monitor));
            
        } catch (MantisException e) {
            return MantisCorePlugin.getDefault().getStatusFactory().toStatus(null, e, repository);
        } catch (CoreException e) {
            return e.getStatus();
        }

        return Status.OK_STATUS;

    }

    protected void updateAttributes(TaskRepository repository, IProgressMonitor monitor) throws CoreException, MantisException {

        IMantisClient client = getClientManager().getRepository(repository);
        client.updateAttributes(monitor);
    }

    public synchronized MantisClientManager getClientManager() {

        File cacheFile = MantisCorePlugin.getDefault().getRepositoryAttributeCachePath().toFile();
        if (clientManager == null)
            clientManager = new MantisClientManager(cacheFile);
        return clientManager;
    }

    public void stop() {

        if (clientManager != null)
            clientManager.persistCache();

    }

    @Override
    public String getTaskIdPrefix() {

        return "#";
    }

    public static int getTicketId(String taskId) throws CoreException {

        try {
            return Integer.parseInt(taskId);
        } catch (NumberFormatException e) {
            throw new CoreException(MantisCorePlugin.getDefault().getStatusFactory().toStatus("Invalid ticket id: " + taskId + ".", e, null));
        }
    }

    // For the repositories, perform the queries to get the latest information
    // about the
    // tasks. This allows the connector to get a limited list of items instead
    // of every
    // item in the repository. Next check to see if the tasks have changed since
    // the
    // last synchronization. If so, add their ids to a List.
    /**
     * Gets the changed tasks for a given query
     * 
     * <p>For the <tt>repository</tt>, run the <tt>query</tt> to get the latest information about the
     * tasks. This allows the connector to get a limited list of items instead of every item in the
     * repository. Next check to see if the tasks have changed since the last synchronization. If
     * so, add their ids to a List.</p>
     * 
     * @param monitor
     * @return the ids of the changed tasks
     * @throws CoreException 
     */
    private List<Integer> getChangedTasksByQuery(IRepositoryQuery query, TaskRepository repository, Date since,
            IProgressMonitor monitor) throws CoreException {

        MantisCorePlugin.debug(NLS.bind("Looking for tasks changed in query {0} since {1} .", query.getSummary(), since), null);
        
        final List<MantisTicket> tickets = new ArrayList<MantisTicket>();
        List<Integer> changedTickets = new ArrayList<Integer>();

        IMantisClient client;
        try {
            client = getClientManager().getRepository(repository);
            client.search(MantisUtils.getMantisSearch(query), tickets, monitor);

            for (MantisTicket ticket : tickets)
                if (ticket.getLastChanged() != null && ticket.getLastChanged().compareTo(since) > 0)
                    changedTickets.add(Integer.valueOf(ticket.getId()));
        } catch (MantisException e) {
           throw new CoreException(MantisCorePlugin.getDefault().getStatusFactory().toStatus("Failed getting changed tasks.", e, repository));
        }
        
        MantisCorePlugin.debug(NLS.bind("Found {0} changed tickets.", changedTickets.size()), null);
        
        return changedTickets;
    }

    @Override
    public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {

        try {
            updateAttributes(repository, monitor);
        } catch (MantisException e) {
            throw new CoreException(MantisCorePlugin.getDefault().getStatusFactory().toStatus("Could not update attributes", e, repository));
        }

    }

    @Override
    public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor)
            throws CoreException {

        return offlineTaskHandler.getTaskData(repository, taskId, monitor);
    }

    // Based off of Trac Implementation.
    @Override
    public boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData) {

        // always take into account the modification date since it
        // is returned by the search query
        TaskMapper mapper = getTaskMapper(taskData);

        Date repositoryDate = mapper.getModificationDate();
        Date taskModDate = task.getModificationDate();
        
        MantisCorePlugin.debug(NLS.bind("Checking if task with id {0} has changed: repositoryDate is {1} and task modification date is {2}", new Object[] { task.getTaskId(), repositoryDate, taskModDate }), new RuntimeException());

        return repositoryDate == null || !repositoryDate.equals(taskModDate) ? true : false;
    }

    @Override
    public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {

        TaskMapper scheme = getTaskMapper(taskData);
        scheme.applyTo(task);

        boolean completed = isCompleted(taskData);

        Date completionDate = completed ? scheme.getModificationDate() : null;

        task.setCompletionDate(completionDate);

        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager()
                .getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);

        task.setUrl(connector.getTaskUrl(repository.getRepositoryUrl(), taskData.getTaskId()));

        boolean supportsSubtasks = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey()) != null;

        task.setAttribute(TASK_KEY_SUPPORTS_SUBTASKS, Boolean.toString(supportsSubtasks));

    }

    private boolean isCompleted(TaskData taskData) {

        boolean completed = false;

        try {
            IMantisClient client = getClientManager().getRepository(taskData.getAttributeMapper().getTaskRepository());
            completed = client.isCompleted(taskData, new NullProgressMonitor());
        } catch (MantisException e) {
            MantisCorePlugin.error("Failed determining the completed status", e);
        }
        return completed;
    }

    public TaskMapper getTaskMapper(final TaskData taskData) {

        return new MantisTaskMapper(taskData);
    }

    @Override
    public ITaskMapping getTaskMapping(TaskData taskData) {

        return getTaskMapper(taskData);
    }

    @Override
    public void preSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {

        // No Tasks, don't contact the repository
        if (event.getTasks().isEmpty()) {
            return;
        }

        TaskRepository repository = event.getTaskRepository();

        if (repository.getSynchronizationTimeStamp() == null || repository.getSynchronizationTimeStamp().length() == 0) {
            for (ITask task : event.getTasks())
                event.markStale(task);
            return;
        }

        Date since = new Date(0);
        try {
            if (repository.getSynchronizationTimeStamp().length() > 0)
                since = MantisUtils.parseDate(Long.valueOf(repository.getSynchronizationTimeStamp()));
        } catch (NumberFormatException e) {
             MantisCorePlugin.warn("Failed parsing repository synchronisationTimestamp " + repository.getSynchronizationTimeStamp() + " .", e);
        }


        // Run the queries to get the list of tasks currently meeting the query
        // criteria. The result returned are only the ids that have changed.
        // Next checkt to see if any of these ids matches the ones in the
        // task list. If so, then set it to stale.
        // 
        // The prior implementation retireved each id individually, and
        // checked it's date, this caused unnecessary SOAP traffic during
        // synchronization.
        event.setNeedsPerformQueries(false);
        List<IRepositoryQuery> queries = getMantisQueriesFor(repository);

        monitor.beginTask("Retrieving queries for repository", queries.size());

        for (IRepositoryQuery query : queries) {

            for (Integer taskId : getChangedTasksByQuery(query, repository, since, monitor)) {
                for (ITask task : event.getTasks()) {
                    if (getTicketId(task.getTaskId()) == taskId.intValue()) {
                        event.setNeedsPerformQueries(true);
                        event.markStale(task);
                        
                        MantisCorePlugin.debug(NLS.bind("Marking task {0} as stale.", task), null);
                    }
                }
            }

            monitor.worked(1);
        }
    }

    private List<IRepositoryQuery> getMantisQueriesFor(TaskRepository taskRespository) {

        List<IRepositoryQuery> queries = new ArrayList<IRepositoryQuery>();

        for (IRepositoryQuery query : TasksUiInternal.getTaskList().getQueries()) {

            boolean isMantisQuery = MantisCorePlugin.REPOSITORY_KIND.equals(query.getConnectorKind());
            boolean belongsToThisRepository = query.getRepositoryUrl().equals(taskRespository.getUrl());

            if (isMantisQuery && belongsToThisRepository) {
                queries.add(query);
            }
        }

        return queries;
    }

    @Override
    public void postSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {

        try {
            monitor.beginTask("", 1);
            if (event.isFullSynchronization()) {
                Date date = getSynchronizationTimestamp(event);
                
                MantisCorePlugin.debug(NLS.bind("Synchronisation timestamp from event for {0} is {1} .", event.getTaskRepository(), date), null);
                
                if (date != null) {
                    event.getTaskRepository().setSynchronizationTimeStamp(MantisUtils.toMantisTime(date) + "");
                } else {
                    event.getTaskRepository().setSynchronizationTimeStamp(MantisUtils.toMantisTime(new Date()) + "");
                }
            }
        } catch (RuntimeException e) {
            event.getTaskRepository().setSynchronizationTimeStamp(MantisUtils.toMantisTime(new Date()) + "");
            throw new CoreException(MantisCorePlugin.getDefault().getStatusFactory().toStatus(null, e, event.getTaskRepository()));
        } finally {
            monitor.done();
        }
    }

    private Date getSynchronizationTimestamp(ISynchronizationSession event) {

        Date mostRecent = new Date(0);
        Date mostRecentTimeStamp = null;
        if (event.getTaskRepository().getSynchronizationTimeStamp() == null) {
            mostRecentTimeStamp = mostRecent;
        } else {
            mostRecentTimeStamp = MantisUtils.parseDate(Long.parseLong(event.getTaskRepository()
                    .getSynchronizationTimeStamp()));
        }
        for (ITask task : event.getChangedTasks()) {
            Date taskModifiedDate = task.getModificationDate();
            if (taskModifiedDate != null && taskModifiedDate.after(mostRecent)) {
                mostRecent = taskModifiedDate;
                mostRecentTimeStamp = task.getModificationDate();
            }
        }
        return mostRecentTimeStamp;
    }

    @Override
    public Collection<TaskRelation> getTaskRelations(TaskData taskData) {

        if (!MantisRepositoryConfiguration.isDownloadSubTasks(taskData.getAttributeMapper().getTaskRepository()))
            return null;

        TaskAttribute parentTasksAttribute = taskData.getRoot().getAttribute(
                MantisAttributeMapper.Attribute.PARENT_OF.getKey());

        TaskAttribute childTasksAttribute = taskData.getRoot().getAttribute(
                MantisAttributeMapper.Attribute.CHILD_OF.getKey());

        if (parentTasksAttribute == null && childTasksAttribute == null)
            return null;

        List<TaskRelation> relations = new ArrayList<TaskRelation>();

        if (parentTasksAttribute != null)
            for (String taskId : parentTasksAttribute.getValues())
                relations.add(TaskRelation.subtask(taskId));

        if (childTasksAttribute != null)
            for (String taskId : childTasksAttribute.getValues())
                relations.add(TaskRelation.parentTask(taskId));

        return relations;

    }

    @Override
    public boolean hasRepositoryDueDate(TaskRepository taskRepository, ITask task, TaskData taskData) {
        
        try {
            return getClientManager().getRepository(taskRepository).isDueDateEnabled(new NullProgressMonitor());
        } catch (MantisException e) {
            MantisCorePlugin.error("Failed determining if the due date is enabled", e);
            return false;
        }
    }
}