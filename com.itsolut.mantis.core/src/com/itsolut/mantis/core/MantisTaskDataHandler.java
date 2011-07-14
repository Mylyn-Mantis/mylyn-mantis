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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;
import org.eclipse.osgi.util.NLS;

import biz.futureware.mantis.rpc.soap.client.AccountData;

import com.google.inject.Inject;
import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.exception.TicketNotFoundException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.model.MantisRelationship.RelationType;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.model.MantisTicketAttribute;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Dave Carver
 */
public class MantisTaskDataHandler extends AbstractTaskDataHandler {

    private final MantisClientManager clientManager;
    private final StatusFactory statusFactory;

    private static final String CONTEXT_ATTACHMENT_FILENAME = "mylyn-context.zip";

    private static final String CONTEXT_ATTACHMENT_DESCRIPTION = "mylyn/context/zip";

    private static final String CONTEXT_ATTACHMENT_DESCRIPTION_LEGACY = "mylar/context/zip";

    private static final String CONTEXT_ATTACHMENT_FILENAME_LEGACY = "mylar-context.zip";

    private static final Map<Attribute, RelationType> attributeToRelationType = new EnumMap<Attribute, RelationType>(Attribute.class);

    {
        attributeToRelationType.put(Attribute.RELATED_TO, RelationType.RELATED);
        attributeToRelationType.put(Attribute.DUPLICATE_OF, RelationType.DUPLICATE);
        attributeToRelationType.put(Attribute.HAS_DUPLICATE, RelationType.HAS_DUPLICATE);
        attributeToRelationType.put(Attribute.PARENT_OF, RelationType.PARENT);
        attributeToRelationType.put(Attribute.CHILD_OF, RelationType.CHILD);
    }
    
    private static final Map<RelationType, Attribute> relationTypeToAttribute = new EnumMap<RelationType, Attribute>(RelationType.class);
    {
        for ( Map.Entry<Attribute, RelationType> entry : attributeToRelationType.entrySet() )
            relationTypeToAttribute.put(entry.getValue(), entry.getKey());
    }

    
    private final static Map<MantisCustomFieldType, String> customFieldTypeToTaskType = new EnumMap<MantisCustomFieldType, String>(MantisCustomFieldType.class);
    {
        customFieldTypeToTaskType.put(MantisCustomFieldType.CHECKBOX, TaskAttribute.TYPE_BOOLEAN);
        customFieldTypeToTaskType.put(MantisCustomFieldType.DATE, TaskAttribute.TYPE_DATE);
        customFieldTypeToTaskType.put(MantisCustomFieldType.EMAIL, TaskAttribute.TYPE_SHORT_TEXT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.ENUM, TaskAttribute.TYPE_SINGLE_SELECT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.FLOAT, TaskAttribute.TYPE_SHORT_TEXT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.LIST , TaskAttribute.TYPE_SINGLE_SELECT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.MULTILIST, TaskAttribute.TYPE_MULTI_SELECT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.NUMERIC, TaskAttribute.TYPE_SHORT_TEXT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.RADIO, TaskAttribute.TYPE_SINGLE_SELECT);
        customFieldTypeToTaskType.put(MantisCustomFieldType.STRING, TaskAttribute.TYPE_SHORT_TEXT);
    }
    
    @Inject
    public MantisTaskDataHandler(MantisClientManager clientManager, StatusFactory statusFactory) {
        this.clientManager = clientManager;
        this.statusFactory = statusFactory;
    }

    @Override
    public TaskAttributeMapper getAttributeMapper(TaskRepository taskRepository) {
        return new MantisAttributeMapper(taskRepository);
    }

    @Override
    public boolean initializeTaskData(TaskRepository repository, TaskData data,
            ITaskMapping initializationData, IProgressMonitor monitor)
    throws CoreException {
        try {
            IMantisClient client = clientManager.getRepository(repository);
            
            // project name
            TaskAttribute projectAttribute = getAttribute(data, MantisAttributeMapper.Attribute.PROJECT.getKey().toString());
            projectAttribute.setValue(initializationData.getProduct());
            
			createDefaultAttributes(data, client, initializationData.getProduct(), monitor, false);
            createProjectSpecificAttributes(data, client, monitor);
            createCustomFieldAttributes(data, client, new DefaultCustomFieldValueSource(), monitor);
            return true;
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus(null, e, repository));
        }
    }

    @Override
    public RepositoryResponse postTaskData(TaskRepository repository,
            TaskData taskData, Set<TaskAttribute> oldAttributes,
            IProgressMonitor monitor) throws CoreException {
        try {
            IMantisClient client = clientManager.getRepository( repository);

            processOperation(taskData, client, monitor);

            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=337251
            if ( taskData.isNew() ) 
                oldAttributes = Collections.emptySet();
            
            TaskRelationshipChangeFinder changeFinder = new TaskRelationshipChangeFinder(this);
            List<TaskRelationshipChange> changes = changeFinder.findChanges(taskData, oldAttributes);
            
            MantisTicket ticket = getMantisTicket(repository, taskData);
            
            if (taskData.isNew()) {
                int id = client.createTicket(ticket, monitor, changes);
                return new RepositoryResponse(ResponseKind.TASK_UPDATED, id + "");
            } else {
                
                String newComment = "";
                TaskAttribute newCommentAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
                if (newCommentAttribute != null)
                    newComment = newCommentAttribute.getValue();
                TaskAttribute timeTrackingAttribute = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.TIME_SPENT_NEW.getKey());
                
                int timeTracking = 0;
                if( timeTrackingAttribute  != null && timeTrackingAttribute.getValue() != null && timeTrackingAttribute.getValue().length() != 0 ) {
                    timeTracking = Integer.parseInt(timeTrackingAttribute.getValue());
                    timeTrackingAttribute.clearValues();
                }
                
                client.updateTicket(ticket, newComment, timeTracking, changes, monitor);
                
                return new RepositoryResponse(ResponseKind.TASK_UPDATED, ticket.getId()+ "");
            }
        } catch ( NumberFormatException e) {
            throw new CoreException(statusFactory.toStatus("Invalid time tracking value, must be an integer.", new MantisException(e), repository));
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus(null, e, repository));
        }
    }

    /**
     * Apply the effects of the operation (if any) to an existing task data
     * 
     * @param taskData
     * @param client 
     * @param monitor 
     */
    private void processOperation(TaskData taskData, IMantisClient client, IProgressMonitor monitor) {

        TaskAttribute attributeOperation = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);

        if ( attributeOperation == null || "".equals(attributeOperation.getValue()))
            return; // i.e. no operation

        MantisOperation type = MantisOperation.valueOf(attributeOperation.getValue());

        type.performPostOperation(taskData, attributeOperation, client, monitor);
    }

    private MantisTicket getMantisTicket(TaskRepository repository, TaskData data) throws MantisException {
        MantisTicket ticket;
        if (data.getTaskId() == null || data.getTaskId().length() == 0)
            ticket = new MantisTicket();
        else
            ticket = new MantisTicket(Integer.parseInt(data.getTaskId()));


        Collection<TaskAttribute> attributes = data.getRoot().getAttributes().values();

        for (TaskAttribute attribute : attributes) {
            
            if ( attribute.getId().equals("project")) {
                ticket.putValue(attribute.getId(), attribute.getValue());
                continue;
            }
            
            if ( attribute.getId().equals(Key.MONITORS.toString())) {
                
                List<String> monitorsToRemove = new ArrayList<String>(attribute.getValues());
                List<String> originalMonitors = MantisUtils.fromCsvString(attribute.getMetaData().getValue(MantisAttributeMapper.TASK_ATTRIBUTE_ORIGINAL_MONITORS));
                
                originalMonitors.removeAll(monitorsToRemove);
                
                System.out.println("Final value of monitors is " + originalMonitors);
                
                ticket.putValue(attribute.getId(), MantisUtils.toCsvString(originalMonitors));
                continue;
            }

            if (attribute.getId().equals(TaskAttribute.OPERATION) || attribute.getMetaData().isReadOnly() || MantisOperation.isOperationRelated(attribute))
                continue;

            ticket.putValue(attribute.getId(), attribute.getValue());
        }

        return ticket;
    }

    public TaskData getTaskData(TaskRepository repository, String taskId,
            IProgressMonitor monitor) throws CoreException {

        int id = Integer.parseInt(taskId);
        try {
            IMantisClient client = clientManager.getRepository(repository);
            MantisTicket ticket = client.getTicket(id, monitor);
            return createTaskDataFromTicket(client, repository, ticket, monitor);
        } catch ( TicketNotFoundException e) {
        	throw new CoreException(statusFactory.toStatus(e.getMessage(), e, repository));
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus("Ticket download from "
                    + repository.getRepositoryUrl() + " for task " + id + " failed : " + e.getMessage() + " .", e, repository));
        }
    }

    private void updateTaskData(TaskRepository repository,
            TaskAttributeMapper attributeMapper, TaskData data,
            IMantisClient client, MantisTicket ticket, IProgressMonitor monitor) throws CoreException, MantisException {

        if (ticket.getCreated() != null)
            data.getRoot().getAttribute(
                    MantisAttributeMapper.Attribute.DATE_SUBMITTED.getKey())
                    .setValue(
                            MantisUtils.toMantisTime(ticket.getCreated()) + "");

        Date lastChanged = ticket.getLastChanged();

        copyValuesFromTicket(data, ticket);

        addComments(data, ticket, client, monitor);
        addAttachments(repository, data, ticket);
        addRelationships(data, ticket);
        addMonitors(data, ticket, client, repository, monitor);
        addOperation(data, ticket, MantisOperation.LEAVE, client, monitor);
        if ( client.isTimeTrackingEnabled(monitor))
            addOperation(data, ticket, MantisOperation.TRACK_TIME, client, monitor);
        addOperation(data, ticket, MantisOperation.RESOLVE_AS, client, monitor);
        addOperation(data, ticket, MantisOperation.ASSIGN_TO, client, monitor);

        if (lastChanged != null)
            data.getRoot().getAttribute(
                    MantisAttributeMapper.Attribute.LAST_UPDATED.getKey())
                    .setValue(MantisUtils.toMantisTime(lastChanged) + "");

    }
    
    private void copyValuesFromTicket(TaskData data, MantisTicket ticket) {

		boolean warningLogged = false;
		
		Map<String, String> valueByKey = ticket.getValues();
        for (String key : valueByKey.keySet()) {

            String value = valueByKey.get(key);
			if (value == null)
				value = "";
			
            TaskAttribute attribute = getAttribute(data, key);
       
            // no options, just copy the value
            if ( attribute.getOptions().isEmpty() ) {
                attribute.setValue(value);
            } else {

                // map string to ids
                String keyFromOption = null;
                
                for ( Map.Entry<String, String> optionEntry : attribute.getOptions().entrySet() ) {
                    if ( optionEntry.getValue().equals(value) ) {
                        keyFromOption = optionEntry.getKey();
                        attribute.setValue(keyFromOption);
                        break;
                    }
                }
                
                if ( keyFromOption == null && !warningLogged ) {
                    MantisCorePlugin.warn(NLS.bind("Task {0} : Unable to find match for {1} value {2} in repository-supplied options {3}. Further similar warnings will be suppressed for this task, but errors may occur when submitting.", new Object[] { ticket.getId(),  key, value, attribute.getOptions() } ));
                    warningLogged = true;
                }
            }
        }
	}

    private void addOperation(TaskData data, MantisTicket ticket, MantisOperation operation, IMantisClient client, IProgressMonitor monitor) {

        TaskAttribute operationAttribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + operation.toString());

        String label;
        if ( operation == MantisOperation.LEAVE) {
            TaskAttribute statusAttribute = data.getRoot().getAttribute(MantisAttributeMapper.Attribute.STATUS.getKey());
            label = operation.getLabel() + " " + statusAttribute.getOption(statusAttribute.getValue());
        } else {
            label = operation.getLabel();
        }

        TaskOperation.applyTo(operationAttribute, operation.toString(), label);

        operation.preOperation(data, operationAttribute, client, monitor);
    }

    private void addRelationships(TaskData data, MantisTicket ticket) {

        // relationships - support only child issues
        MantisRelationship[] relationsShips = ticket.getRelationships();
        for (MantisRelationship mantisRelationship : relationsShips) {

            Attribute attribute = null;

            switch (mantisRelationship.getType()) {
            case PARENT:
                attribute = Attribute.PARENT_OF;
                break;

            case CHILD:
                attribute = Attribute.CHILD_OF;
                break;

            case DUPLICATE:
                attribute = Attribute.DUPLICATE_OF;
                break;

            case HAS_DUPLICATE:
                attribute = Attribute.HAS_DUPLICATE;
                break;

            case RELATED:
                attribute = Attribute.RELATED_TO;
                break;

            case UNKNOWN:
            default:
                break;
            }

            if (attribute == null)
                continue;

            int targetId = mantisRelationship.getTargetId();
            TaskAttribute taskAttribute = data.getRoot().getAttribute(attribute.getKey());
            String storedRelationshipIds;
            if ( taskAttribute == null)
                taskAttribute = data.getRoot().createAttribute(attribute.getKey());
            
            if ( MantisUtils.isEmpty(taskAttribute.getValue()) )
                taskAttribute.getMetaData().putValue(MantisAttributeMapper.TASK_ATTRIBUTE_RELATIONSHIP_IDS, "");
            
            storedRelationshipIds = taskAttribute.getMetaData().getValue(MantisAttributeMapper.TASK_ATTRIBUTE_RELATIONSHIP_IDS);
            if ( MantisUtils.isEmpty(storedRelationshipIds) )
                storedRelationshipIds = String.valueOf(mantisRelationship.getId());
            else 
                storedRelationshipIds += "," + mantisRelationship.getId();                    
            
            taskAttribute.getMetaData().putValue(MantisAttributeMapper.TASK_ATTRIBUTE_RELATIONSHIP_IDS, storedRelationshipIds);
            
            taskAttribute.addValue(String.valueOf(targetId));
        }

    }

    private void addMonitors(TaskData data, MantisTicket ticket, IMantisClient client, TaskRepository repository, IProgressMonitor monitor) throws MantisException {

        if ( ticket.getMonitors() == null )
            return;
        
        createAttribute(data, MantisAttributeMapper.Attribute.ADD_SELF_TO_MONITORS);
        
        if ( ticket.getMonitors().length == 0 )
            return;
        
        TaskAttribute attribute = createAttribute(data, Attribute.MONITORS);
        List<String> originalValues = new ArrayList<String>();
        
        for ( AccountData issueMonitor: ticket.getMonitors() ) {
            IRepositoryPerson person = newPerson(repository, issueMonitor.getName() , client, monitor);
            attribute.putOption(person.getPersonId(), person.toString());
            originalValues.add(person.getPersonId());
        }
        
        attribute.getMetaData().putValue(MantisAttributeMapper.TASK_ATTRIBUTE_ORIGINAL_MONITORS, MantisUtils.toCsvString(originalValues));
    }
    
    private void addAttachments(TaskRepository repository,
            TaskData data, MantisTicket ticket) {

        int i = 1;
        if (ticket.getAttachments() == null)
            return;

        for (MantisAttachment attachment : ticket.getAttachments()) {
            TaskAttribute attribute = data.getRoot().createAttribute(
                    TaskAttribute.PREFIX_ATTACHMENT + i);
            TaskAttachmentMapper taskAttachment = TaskAttachmentMapper
            .createFrom(attribute);
            taskAttachment.setFileName(attachment.getFilename());
            if (CONTEXT_ATTACHMENT_FILENAME.equals(attachment.getFilename()))
                taskAttachment.setDescription(CONTEXT_ATTACHMENT_DESCRIPTION);
            else if (CONTEXT_ATTACHMENT_FILENAME_LEGACY.equals(attachment
                    .getFilename()))
                taskAttachment
                .setDescription(CONTEXT_ATTACHMENT_DESCRIPTION_LEGACY);
            else
                taskAttachment.setDescription(attachment.getFilename());
            taskAttachment.setLength(Long.parseLong(Integer.toString(attachment
                    .getSize())));
            taskAttachment.setCreationDate(attachment.getCreated());
            taskAttachment.setUrl(MantisRepositoryLocations.create(repository
                    .getRepositoryUrl()).getAttachmentDownloadLocation(attachment.getId()));
            taskAttachment
            .setAttachmentId(Integer.toString(attachment.getId()));
            taskAttachment.applyTo(attribute);
            i++;
        }
    }

    private void addComments(TaskData data, MantisTicket ticket, IMantisClient client, IProgressMonitor monitor) throws MantisException {
        int i = 1;
        if (ticket.getComments() == null)
            return;
        for (MantisComment comment : ticket.getComments()) {
            TaskAttribute attribute = data.getRoot().createAttribute(
                    TaskAttribute.PREFIX_COMMENT + i);
            TaskCommentMapper taskComment = TaskCommentMapper .createFrom(attribute);
            taskComment.setAuthor(newPerson(data.getAttributeMapper().getTaskRepository(), comment.getReporter(), client, monitor));
            taskComment.setNumber(i);
            
            String commentText = comment.getText();
            taskComment.setText(commentText);
            taskComment.setCreationDate(comment.getDateSubmitted());
            taskComment.applyTo(attribute);
            i++;
        }
    }

    private IRepositoryPerson newPerson(TaskRepository repository, String personId, IMantisClient client, IProgressMonitor monitor) throws MantisException {

        if ( personId == null )
            return repository.createPerson("unknown");
        
        IRepositoryPerson person = repository.createPerson(personId);
        User user = client.getCache(monitor).getUserByUsername(personId);
        if ( user != null )
            person.setName(user.getRealName());
        return person;
    }

    private void createDefaultAttributes(TaskData data,
            IMantisClient client, String projectName, IProgressMonitor monitor, boolean existingTask) throws CoreException {

        // The order here is important as it controls how it appears in the Editor

        try {
            MantisCache cache = client.getCache(monitor);
            
            createAttribute(data, MantisAttributeMapper.Attribute.PROJECT, null).setValue(projectName);
            createAttribute(data, MantisAttributeMapper.Attribute.CATEGORY, null);

            createAttribute(data, MantisAttributeMapper.Attribute.RESOLUTION,
                    cache.getTicketResolutions(), cache.getDefaultResolutionName());
            createAttribute(data, MantisAttributeMapper.Attribute.STATUS, cache
                    .getTicketStatus(), cache.getSubmitStatus());
            createAttribute(data, MantisAttributeMapper.Attribute.PRIORITY, cache
                    .getPriorities(), cache.getDefaultPriorityName());
            createAttribute(data, MantisAttributeMapper.Attribute.SEVERITY, cache
                    .getSeverities(), cache.getDefaultSeverityName());
            createAttribute(data, MantisAttributeMapper.Attribute.REPRODUCIBILITY,
                    cache.getReproducibility(), cache.getDefaultReproducibilityName());
            createAttribute(data, MantisAttributeMapper.Attribute.VERSION, null);
            createAttribute(data, MantisAttributeMapper.Attribute.FIXED_IN, null);
            if ( client.getCache(monitor).getRepositoryVersion().isHasTargetVersionSupport())
                createAttribute(data, MantisAttributeMapper.Attribute.TARGET_VERSION, null);
            
            if ( cache.isProjectionEnabled())
            	createAttribute(data, MantisAttributeMapper.Attribute.PROJECTION, cache.getProjection(), cache.getDefaultProjectionName());
            if ( cache.isEtaEnabled() )
            	createAttribute(data, MantisAttributeMapper.Attribute.ETA, cache.getETA(), cache.getDefaultEtaName());
            
            if ( client.isDueDateEnabled(monitor))
                createAttribute(data, MantisAttributeMapper.Attribute.DUE_DATE, null);
            if (client.isTimeTrackingEnabled(monitor)) {
                createAttribute(data, MantisAttributeMapper.Attribute.TIME_SPENT, null);
                createAttribute(data, MantisAttributeMapper.Attribute.TIME_SPENT_NEW, null);
            }
            
            if ( client.getCache(monitor).getRepositoryVersion().isHasProperTaskRelations())
                createTaskRelations(data, client);

            createAttribute(data, MantisAttributeMapper.Attribute.DESCRIPTION);
            createAttribute(data,
                    MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE).setValue(cache.getDefaultStepsToReproduce());
            createAttribute(data, MantisAttributeMapper.Attribute.ADDITIONAL_INFO).setValue(cache.getDefaultAdditionalInfo());
            createAttribute(data, MantisAttributeMapper.Attribute.NEW_COMMENT, null);

            createAttribute(data, MantisAttributeMapper.Attribute.VIEW_STATE,
                    cache.getViewState(), cache.getDefaultViewStateName());

            createAttribute(data, MantisAttributeMapper.Attribute.ASSIGNED_TO, cache.getDevelopersByProjectName(projectName, monitor), true);
            if (existingTask)
				createAttribute(data, MantisAttributeMapper.Attribute.REPORTER, cache.getUsersByProjectName(projectName, monitor), false);
            
            createAttribute(data, MantisAttributeMapper.Attribute.SUMMARY);
            createAttribute(data, MantisAttributeMapper.Attribute.DATE_SUBMITTED);
            createAttribute(data, MantisAttributeMapper.Attribute.LAST_UPDATED);
            createAttribute(data, MantisAttributeMapper.Attribute.COMPLETION_DATE);

            // operations
            data.getRoot().createAttribute(TaskAttribute.OPERATION).getMetaData()
                .setType(TaskAttribute.TYPE_OPERATION);
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus(null, e, null));
        }

    }

    private void createTaskRelations(TaskData data, IMantisClient client) {

        createAttribute(data, MantisAttributeMapper.Attribute.PARENT_OF, null);
        createAttribute(data, MantisAttributeMapper.Attribute.CHILD_OF, null);
        createAttribute(data, MantisAttributeMapper.Attribute.DUPLICATE_OF, null);
        createAttribute(data, MantisAttributeMapper.Attribute.HAS_DUPLICATE, null);
        createAttribute(data, MantisAttributeMapper.Attribute.RELATED_TO, null);
    }

    private void createProjectSpecificAttributes(TaskData data, IMantisClient client, IProgressMonitor monitor) throws MantisException {

            // categories
            TaskAttribute attr = getAttribute(data,
                    MantisAttributeMapper.Attribute.CATEGORY.getKey());
            attr.clearOptions();
            boolean first = MantisUtils.isEmpty(attr.getValue());
            TaskAttribute projectAttribute = data.getRoot().getAttribute( MantisAttributeMapper.Attribute.PROJECT.getKey());
            
            for (MantisProjectCategory mp : client.getCache(monitor).getProjectCategories(projectAttribute.getValue())) {
                if (first) {
                    attr.setValue(mp.toString());
                    first = false;
                }
                attr.putOption(mp.toString(), mp.toString());
            }

            // versions
            TaskAttribute repInVerAttr = getAttribute(data,
                    MantisAttributeMapper.Attribute.VERSION.getKey());
            repInVerAttr.clearOptions();
            repInVerAttr.putOption("none", ""); // empty option

            TaskAttribute fixInVerAttr = getAttribute(data,
                    MantisAttributeMapper.Attribute.FIXED_IN.getKey());
            fixInVerAttr.clearOptions();
            fixInVerAttr.putOption("none", "");// Add empty option

            TaskAttribute targetVersionAttr = null;
            if ( client.getCache(monitor).getRepositoryVersion().isHasTargetVersionSupport()) {
                targetVersionAttr = getAttribute(data,
                        MantisAttributeMapper.Attribute.TARGET_VERSION.getKey());
                targetVersionAttr.clearOptions();
                targetVersionAttr.putOption("none", "");// Add empty option
            }

            for (MantisVersion v : client.getCache(monitor).getVersionsByProjectName(getAttribute(data,
                    MantisAttributeMapper.Attribute.PROJECT.getKey())
                    .getValue())) {

                /*
                 * Only display released versions for the reported in field,
                 * matches the behaviour of the mantis web interface.
                 */
                if (v.isReleased())
                    repInVerAttr.putOption(v.getName(), v.getName());
                fixInVerAttr.putOption(v.getName(), v.getName());
                if ( client.getCache(monitor).getRepositoryVersion().isHasTargetVersionSupport())
                    targetVersionAttr.putOption(v.getName(), v.getName());

            }

            /* If the value is empty then the issue has not yet been fixed */
            if (MantisUtils.isEmpty(fixInVerAttr.getValue()))
                fixInVerAttr.setValue("none");

            if (MantisUtils.isEmpty(repInVerAttr.getValue()))
                repInVerAttr.setValue("none");

            if ( client.getCache(monitor).getRepositoryVersion().isHasTargetVersionSupport() && MantisUtils.isEmpty(targetVersionAttr.getValue()))
                targetVersionAttr.setValue("none");
    }

    private TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute, MantisTicketAttribute[] values,
            boolean allowEmtpy) {

        boolean readOnly = data.isNew() ? attribute.isReadOnlyForNewTask() : attribute.isReadOnlyForExistingTask();

        TaskAttribute attr = data.getRoot().createAttribute(attribute.getKey());

        attr.getMetaData().setReadOnly(readOnly).setLabel(
                attribute.toString()).setKind(attribute.getKind()).setType(
                        attribute.getType());
        if (values != null && values.length > 0) {
            if (allowEmtpy)
                attr.putOption("", "");
            for (MantisTicketAttribute value : values)
                attr.putOption(String.valueOf(value.getValue()), value.getName());
        }
        return attr;
    }

    private TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute) {

        boolean readOnly = data.isNew() ? attribute.isReadOnlyForNewTask() : attribute.isReadOnlyForExistingTask();

        TaskAttribute attr = data.getRoot().createAttribute(attribute.getKey());
        attr.getMetaData().setReadOnly(readOnly).setLabel(
                attribute.toString()).setKind(attribute.getKind()).setType(
                        attribute.getType());
        return attr;
    }

    private TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute, MantisTicketAttribute[] values,
            String defaultValue) {
        TaskAttribute rta = createAttribute(data, attribute, values, false);
        rta.setValue(defaultValue);
        return rta;
    }

    private TaskAttribute getAttribute(TaskData data, String key) {
        TaskAttribute attribute = data.getRoot().getAttribute(key);
        if (attribute == null)
            attribute = data.getRoot().createAttribute(key);
        return attribute;
    }

    private TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute, MantisTicketAttribute[] values) {
        return createAttribute(data, attribute, values, false);
    }

    /**
     * Given a Mantis Ticket create the necessary TaskData object
     * 
     * @param IMantisClient
     *            client
     * @param TaskRepository
     *            repository
     * @param MantisTicket
     *            ticket
     * @param IProgressMontiro
     *            monitor
     * 
     * @since 3.0
     */
    public TaskData createTaskDataFromTicket(IMantisClient client,
            TaskRepository repository, MantisTicket ticket,
            IProgressMonitor monitor) throws CoreException {
        TaskData taskData = newTaskData(repository, ticket);
        try {
        	String projectName = ticket.getValue(Key.PROJECT);
            createDefaultAttributes(taskData, client, projectName, monitor, true);
            updateTaskData(repository, getAttributeMapper(repository),
                    taskData, client, ticket, monitor);
            createProjectSpecificAttributes(taskData, client, monitor);
            createCustomFieldAttributes(taskData, client, new MantisTicketCustomFieldValueSource(ticket), monitor);

            return taskData;
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus(null, e, repository));
        }
    }
    
    public TaskData createTaskDataFromPartialTicket(IMantisClient client,
            TaskRepository repository, MantisTicket ticket,
            IProgressMonitor monitor) throws CoreException, MantisException {
     
        TaskData taskData = newTaskData(repository, ticket);
        taskData.setPartial(true);
        
        createAttribute(taskData, MantisAttributeMapper.Attribute.PROJECT).setValue(ticket.getValue(Key.PROJECT));
        createAttribute(taskData, MantisAttributeMapper.Attribute.SUMMARY).setValue(ticket.getValue(Key.SUMMARY));
        createAttribute(taskData, MantisAttributeMapper.Attribute.STATUS).setValue(ticket.getValue(Key.STATUS));
        createAttribute(taskData, MantisAttributeMapper.Attribute.RESOLUTION).setValue(ticket.getValue(Key.RESOLUTION));
        createAttribute(taskData, MantisAttributeMapper.Attribute.PRIORITY).setValue(ticket.getValue(Key.PRIORITY));
        createAttribute(taskData, MantisAttributeMapper.Attribute.SEVERITY).setValue(ticket.getValue(Key.SEVERITY));
        if ( ticket.getLastChanged() != null ) // XXX Remove once we have a fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=331733
            createAttribute(taskData, MantisAttributeMapper.Attribute.LAST_UPDATED).setValue(String.valueOf(MantisUtils.toMantisTime(ticket.getLastChanged())));
        if ( ticket.getValue(Key.COMPLETION_DATE) != null )
        	createAttribute(taskData, MantisAttributeMapper.Attribute.COMPLETION_DATE).setValue(ticket.getValue(Key.COMPLETION_DATE));
        
        return taskData;
    }

    private TaskData newTaskData(TaskRepository repository, MantisTicket ticket) {

        return new TaskData(getAttributeMapper(repository), MantisCorePlugin.REPOSITORY_KIND, repository.getRepositoryUrl(), String.valueOf(ticket.getId()));
    }

    /**
     * @param taskData
     * @param client
     * @param ticket the existing ticket, or null
     * @param monitor
     * @throws MantisException
     */
    private void createCustomFieldAttributes(TaskData taskData, IMantisClient client,
            CustomFieldValueSource customFieldValueSource, IProgressMonitor monitor) throws MantisException {
        
        TaskAttribute projectAttribute = taskData.getRoot().getAttribute( MantisAttributeMapper.Attribute.PROJECT.getKey());

        for ( MantisCustomField customField : client.getCache(monitor).getCustomFieldsByProjectName(projectAttribute.getValue()) ) {
            TaskAttribute customAttribute = taskData.getRoot().createAttribute(customField.getName());
            customAttribute.getMetaData().setReadOnly(false);
            customAttribute.getMetaData().setLabel(customField.getName());
            customAttribute.getMetaData().setKind(TaskAttribute.KIND_DEFAULT);
            customAttribute.getMetaData().setType(customFieldTypeToTaskType.get(customField.getType()));
            
            customAttribute.setValue(customFieldValueSource.getCustomFieldValue(customField));
            
            if ( customField.getPossibleValues() != null)
                for ( String possibleValue : customField.getPossibleValues())
                    customAttribute.putOption(possibleValue, possibleValue);
        }
        
    }

    @Override
    public boolean canInitializeSubTaskData(TaskRepository taskRepository,
    		ITask task) {
        
        return MantisRepositoryConfiguration.isSupportsSubTasks(taskRepository);
    }
    
    @Override
    public boolean initializeSubTaskData(TaskRepository repository,
    		TaskData taskData, TaskData parentTaskData, IProgressMonitor monitor)
    		throws CoreException {
		
    	validateSupportsSubtasks(repository, parentTaskData);
    	
        try {
            IMantisClient client = clientManager.getRepository( repository);
			createAttributesForTaskData(repository, taskData, parentTaskData, monitor);
			copyAttributesFromParent(taskData, parentTaskData); 
			clearTaskRelations(taskData);
			setChildAttribute(taskData, parentTaskData);
			createCustomFieldAttributes(taskData, client, new TaskDataCustomFieldValueSource(parentTaskData), monitor);

			return true;
		} catch (MantisException e) {
			throw new CoreException(statusFactory.toStatus("Failed updating attributes.", e, repository));

		}
    }

    private void validateSupportsSubtasks(TaskRepository repository, TaskData parentTaskData) throws CoreException {

        if ( parentTaskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey()) == null)
			throw new CoreException(statusFactory.toStatus("The repository does not support subtasks.", null, repository));
    }
    
    private void createAttributesForTaskData(TaskRepository repository, TaskData taskData, TaskData parentTaskData,
            IProgressMonitor monitor) throws MantisException, CoreException {

        IMantisClient client = clientManager.getRepository( repository);
        TaskAttribute projectAttribute = parentTaskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.PROJECT.getKey());
        createDefaultAttributes(taskData, client, projectAttribute.getValue(), monitor, false);
        getAttribute(taskData, MantisAttributeMapper.Attribute.PROJECT.getKey()).setValue(projectAttribute.getValue());
        
        createProjectSpecificAttributes(taskData, client, monitor);
    }
    
    private void copyAttributesFromParent(TaskData taskData, TaskData parentTaskData) {

        TaskMapper mapper = new TaskMapper(taskData);
        mapper.merge(new TaskMapper(parentTaskData));
        mapper.setDescription("");
        mapper.setSummary("");
    }

    private void clearTaskRelations(TaskData taskData) {

        for (Key taskRelationKey : MantisAttributeMapper.taskRelationKeys())
            taskData.getRoot().getAttribute(taskRelationKey.getKey()).clearValues();
    }
    
    private void setChildAttribute(TaskData taskData, TaskData parentTaskData) {

        TaskAttribute attribute = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.CHILD_OF.getKey());
        attribute.setValue(parentTaskData.getTaskId());
    }

    public RelationType getRelationTypeForAttribute(Attribute attribute) {
        
        RelationType relationType = attributeToRelationType.get(attribute);
        
        if ( relationType == null )
            throw new RuntimeException("No relationType for attribute " + attribute);
        
        return relationType;
    }
    
    private static interface CustomFieldValueSource {
        
        String getCustomFieldValue(MantisCustomField customField);
    }
    
    private static class MantisTicketCustomFieldValueSource implements CustomFieldValueSource {

        private final MantisTicket ticket;
        
        public MantisTicketCustomFieldValueSource(MantisTicket ticket) {

            this.ticket = ticket;
        }

        public String getCustomFieldValue(MantisCustomField customField) {

            String valueToSet = ticket != null ? ticket.getCustomFieldValue(customField.getName()) : customField.getDefaultValue();
            if ( valueToSet == null)
                valueToSet = "";

            return valueToSet;
        }
    }
    
    private static class TaskDataCustomFieldValueSource implements CustomFieldValueSource {
        
        private final TaskData taskData;

        public TaskDataCustomFieldValueSource(TaskData taskData) {

            Assert.isNotNull(taskData);
            
            this.taskData = taskData;
        }
        
        public String getCustomFieldValue(MantisCustomField customField) {
        
            return taskData.getRoot().getAttribute(customField.getName()).getValue();
        }
    }
    
    private static class DefaultCustomFieldValueSource implements CustomFieldValueSource {
        
        public String getCustomFieldValue(MantisCustomField customField) {

            return customField.getDefaultValue();
        }
        
    }
}
