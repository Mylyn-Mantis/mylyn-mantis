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

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;

import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisRelationship.RelationType;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Dave Carver
 */
public class MantisTaskDataHandler extends AbstractTaskDataHandler {

    private static enum OperationType {
        LEAVE("Leave as ", null, null) {

            @Override
            public void performPostOperation(TaskData taskData) {

                return;
            }
        },

        RESOLVE_AS("Resolve as ", TaskAttribute.TYPE_SINGLE_SELECT,
                MantisAttributeMapper.Attribute.RESOLUTION) {

            @Override
            public void performPostOperation(TaskData taskData) {

                taskData.getRoot().getAttribute(Attribute.STATUS.getKey()).setValue("resolved");
            }
        },

        ASSIGN_TO("Assign to ", TaskAttribute.TYPE_PERSON,
                MantisAttributeMapper.Attribute.ASSIGNED_TO) {

            @Override
            public void performPostOperation(TaskData taskData) {

                taskData.getRoot().getAttribute(Attribute.STATUS.getKey()).setValue("assigned");
            }
        };

        private final String                          label;

        private final String                          operationType;

        private final MantisAttributeMapper.Attribute attribute;

        private OperationType(String label, String operationType, MantisAttributeMapper.Attribute attribute) {

            this.label = label;
            this.operationType = operationType;
            this.attribute = attribute;
        }

        public String getLabel() {

            return label;
        }

        public String getOperationType() {

            return operationType;
        }

        public MantisAttributeMapper.Attribute getAttribute() {

            return attribute;
        }

        public abstract void performPostOperation(TaskData taskData) ;


    }

    private final MantisRepositoryConnector connector;

    private static final String CONTEXT_ATTACHMENT_FILENAME = "mylyn-context.zip";

    private static final String CONTEXT_ATTACHMENT_DESCRIPTION = "mylyn/context/zip";

    private static final String CONTEXT_ATTACHMENT_DESCRIPTION_LEGACY = "mylar/context/zip";

    private static final String CONTEXT_ATTACHMENT_FILENAME_LEGACY = "mylar-context.zip";

    private final Map<Attribute, RelationType> attributeToRelationType = new EnumMap<Attribute, RelationType>(Attribute.class);

    {
        attributeToRelationType.put(Attribute.RELATED_TO, RelationType.RELATED);
        attributeToRelationType.put(Attribute.DUPLICATE_OF, RelationType.DUPLICATE);
        attributeToRelationType.put(Attribute.HAS_DUPLICATE, RelationType.HAS_DUPLICATE);
        attributeToRelationType.put(Attribute.PARENT_OF, RelationType.PARENT);
        attributeToRelationType.put(Attribute.CHILD_OF, RelationType.CHILD);
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
        try {
            IMantisClient client = connector.getClientManager().getRepository(
                    repository);
            TaskAttribute projectAttribute = getAttribute(data, MantisAttributeMapper.Attribute.PROJECT.getKey().toString());
            String projectName = initializationData.getProduct();
			projectAttribute.setValue(projectName);
			
			createDefaultAttributes(data, client, projectName, monitor, false);
            createProjectSpecificAttributes(data, client, monitor);
            createCustomFieldAttributes(data, client, null, monitor);
            return true;
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(MantisCorePlugin.toStatus(e));
        }
    }

    @Override
    public RepositoryResponse postTaskData(TaskRepository repository,
            TaskData taskData, Set<TaskAttribute> oldAttributes,
            IProgressMonitor monitor) throws CoreException {
        try {
            IMantisClient client = connector.getClientManager().getRepository(
                    repository);

            processOperation(taskData);

            MantisTicket ticket = getMantisTicket(repository, taskData);


            if (taskData.isNew()) {
                int id = client.createTicket(ticket, monitor);
                return new RepositoryResponse(ResponseKind.TASK_UPDATED, id
                        + "");
            } else {



                String newComment = "";
                TaskAttribute newCommentAttribute = taskData.getRoot()
                .getMappedAttribute(TaskAttribute.COMMENT_NEW);
                if (newCommentAttribute != null)
                    newComment = newCommentAttribute.getValue();
                client.updateTicket(ticket, newComment, monitor);
                return new RepositoryResponse(ResponseKind.TASK_UPDATED, ticket
                        .getId()
                        + "");
            }
        } catch (Exception e) {
            MantisCorePlugin.log(e);
            throw new CoreException(MantisCorePlugin.toStatus(e));
        }
    }



    /**
     * Apply the effects of the operation (if any) to an existing task data
     * 
     * @param taskData
     */
    private void processOperation(TaskData taskData) {

        TaskAttribute attributeOperation = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);

        if ( attributeOperation == null || "".equals(attributeOperation.getValue()))
            return; // i.e. no operation

        OperationType type = OperationType.valueOf(attributeOperation.getValue());

        type.performPostOperation(taskData);
    }

    public MantisTicket getMantisTicket(TaskRepository repository, TaskData data) throws InvalidTicketException {
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

            if (attribute.getId().equals(TaskAttribute.OPERATION) || attribute.getMetaData().isReadOnly())
                continue;

            ticket.putValue(attribute.getId(), attribute.getValue());
        }


        addRelations(data, ticket);

        return ticket;
    }

    private void addRelations(TaskData data, MantisTicket ticket) {

        for ( Map.Entry<Attribute, RelationType> entry : attributeToRelationType.entrySet()) {

            TaskAttribute attribute = data.getRoot().getAttribute(entry.getKey().getKey());

            if ( attribute == null || attribute.getValue().length() == 0)
                continue;

            // parse for each attribute
            for ( String value : attribute.getValue().split(",")) {
                MantisRelationship rel = new MantisRelationship();
                rel.setTargetId(Integer.parseInt(value));
                rel.setType(entry.getValue());
                ticket.addRelationship(rel);
            }
        }
    }

    public TaskData getTaskData(TaskRepository repository, String taskId,
            IProgressMonitor monitor) throws CoreException {
        try {
            int id = Integer.parseInt(taskId);
            if (!MantisRepositoryConnector.hasRichEditor(repository))
                throw new CoreException(new Status(Status.ERROR,
                        MantisCorePlugin.PLUGIN_ID, 0,
                        "Does not have a rich editor", null));

            try {
                IMantisClient client = connector.getClientManager().getRepository(
                        repository);
                MantisTicket ticket = client.getTicket(id, monitor);
                // createDefaultAttributes(data, client, true);
                // updateTaskData(repository, attributeMapper, data, client,
                // ticket);
                // createProjectSpecificAttributes(data, client);
                return createTaskDataFromTicket(client, repository, ticket,
                        monitor);
            } catch (Exception e) {
                MantisCorePlugin.log(e);
                throw new CoreException(new Status(IStatus.ERROR,
                        MantisCorePlugin.PLUGIN_ID, 0, "Ticket download from "
                        + repository.getRepositoryUrl() + " for task " + id
                        + " failed : " + e.getMessage() + " .", e));
            }
        } catch (NumberFormatException e) {
            throw new CoreException(new Status(Status.ERROR,
                    MantisCorePlugin.PLUGIN_ID, "Task id must be numeric", e));
        }
    }

    public static void updateTaskData(TaskRepository repository,
            TaskAttributeMapper attributeMapper, TaskData data,
            IMantisClient client, MantisTicket ticket) throws CoreException {

        if (ticket.getCreated() != null)
            data.getRoot().getAttribute(
                    MantisAttributeMapper.Attribute.DATE_SUBMITTED.getKey())
                    .setValue(
                            MantisUtils.toMantisTime(ticket.getCreated()) + "");

        Date lastChanged = ticket.getLastChanged();

        Map<String, String> valueByKey = ticket.getValues();
        for (String key : valueByKey.keySet())
            if (valueByKey.get(key) != null)
                getAttribute(data, key).setValue(valueByKey.get(key));

        addComments(data, ticket);
        addAttachments(repository, data, ticket);
        addRelationships(data, ticket);
        addOperation(data, ticket, OperationType.LEAVE);
        addOperation(data, ticket, OperationType.RESOLVE_AS);
        addOperation(data, ticket, OperationType.ASSIGN_TO);

        if (lastChanged != null)
            data.getRoot().getAttribute(
                    MantisAttributeMapper.Attribute.LAST_UPDATED.getKey())
                    .setValue(MantisUtils.toMantisTime(lastChanged) + "");

    }


    private static void addOperation(TaskData data, MantisTicket ticket, OperationType operation) {

        TaskAttribute operationAttribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + operation.toString());

        String label = operation != OperationType.LEAVE ? operation.getLabel() : operation.getLabel() + " " + data.getRoot().getAttribute(MantisAttributeMapper.Attribute.STATUS.getKey()).getValue();

        TaskOperation.applyTo(operationAttribute, operation.toString(), label);

        // simple operation ( e.g. Leave as )
        if ( operation.getOperationType() == null)
            return;

        operationAttribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID,
                operation.getAttribute().getKey());
    }

    // Resolve As Operation
    // MantisResolution[] options = client.getTicketResolutions();
    // TaskAttribute operationResolveAs = data.getRoot().createAttribute(
    // TaskAttribute.OPERATION);
    // for (MantisResolution op : options) {
    // operationResolveAs.putOption(op.getName(), op.getName());
    // }
    // operationResolveAs.setValue("fixed");
    // TaskOperation.applyTo(operationResolveAs, "resolve_as", "Resolve As");

    private static void addRelationships(TaskData data, MantisTicket ticket) {

        // relationships - support only child issues
        MantisRelationship[] relationsShips = ticket.getRelationships();
        for (MantisRelationship mantisRelationship : relationsShips) {

            int targetId = mantisRelationship.getTargetId();
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

            TaskAttribute taskAttribute = data.getRoot().getAttribute(attribute.getKey());
            if ( taskAttribute == null)
                taskAttribute = data.getRoot().createAttribute(attribute.getKey());

            taskAttribute.addValue(String.valueOf(targetId));


        }

    }


    private static void addAttachments(TaskRepository repository,
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
            taskAttachment.setUrl(MantisUtils.getRepositoryBaseUrl(repository
                    .getRepositoryUrl())
                    + IMantisClient.TICKET_ATTACHMENT_URL + attachment.getId());
            taskAttachment
            .setAttachmentId(Integer.toString(attachment.getId()));
            taskAttachment.applyTo(attribute);
            i++;
        }
    }

    private static void addComments(TaskData data, MantisTicket ticket) {
        int i = 1;
        if (ticket.getComments() == null)
            return;
        for (MantisComment comment : ticket.getComments()) {
            TaskAttribute attribute = data.getRoot().createAttribute(
                    TaskAttribute.PREFIX_COMMENT + i);
            TaskCommentMapper taskComment = TaskCommentMapper
            .createFrom(attribute);
            String report = comment.getReporter();
            if (report == null)
                taskComment.setAuthor(data.getAttributeMapper()
                        .getTaskRepository().createPerson("unknown"));
            else
                taskComment.setAuthor(data.getAttributeMapper()
                        .getTaskRepository()
                        .createPerson(comment.getReporter()));
            taskComment.setNumber(i);
            String commentText = comment.getText();
            taskComment.setText(commentText);
            taskComment.setCreationDate(comment.getDateSubmitted());
            taskComment.applyTo(attribute);
            i++;
        }
    }

    public static void createDefaultAttributes(TaskData data,
            IMantisClient client, String projectName, IProgressMonitor monitor, boolean existingTask) throws CoreException {

        // The order here is important as it controls how it appears in the
        // Editor.

        try {
            MantisCache cache = client.getCache(monitor);
            
            createAttribute(data, MantisAttributeMapper.Attribute.PROJECT, null).setValue(projectName);
            createAttribute(data, MantisAttributeMapper.Attribute.CATEGORY, null);

            createAttribute(data, MantisAttributeMapper.Attribute.RESOLUTION,
                    cache.getTicketResolutions(), cache.getTicketResolutions()[0].getName());
            createAttribute(data, MantisAttributeMapper.Attribute.STATUS, cache
                    .getTicketStatus(), cache.getTicketStatus()[0].getName());
            createAttribute(data, MantisAttributeMapper.Attribute.PRIORITY, cache
                    .getPriorities(), cache.getPriorities()[0].getName());
            createAttribute(data, MantisAttributeMapper.Attribute.SEVERITY, cache
                    .getSeverities(), cache.getSeverities()[0].getName());
            createAttribute(data, MantisAttributeMapper.Attribute.REPRODUCIBILITY,
                    cache.getReproducibility(), cache.getReproducibility()[0]
                                                                             .getName());
            createAttribute(data, MantisAttributeMapper.Attribute.VERSION, null);
            createAttribute(data, MantisAttributeMapper.Attribute.FIXED_IN, null);
            if ( client.getCache(monitor).getRepositoryVersion().isHasTargetVersionSupport())
                createAttribute(data, MantisAttributeMapper.Attribute.TARGET_VERSION, null);
            
            if ( client.getCache(monitor).getRepositoryVersion().isHasDueDateSupport())
                createAttribute(data, MantisAttributeMapper.Attribute.DUE_DATE, null);

            createAttribute(data, MantisAttributeMapper.Attribute.PROJECTION,
                    cache.getProjection(), cache.getProjection()[0].getName());
            createAttribute(data, MantisAttributeMapper.Attribute.ETA, cache
                    .getETA(), cache.getETA()[0].getName());
            if ( client.getCache(monitor).getRepositoryVersion().isHasProperTaskRelations())
                createTaskRelations(data, client);

            createAttribute(data, MantisAttributeMapper.Attribute.DESCRIPTION);
            createAttribute(data,
                    MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE);
            createAttribute(data, MantisAttributeMapper.Attribute.ADDITIONAL_INFO);
            createAttribute(data, MantisAttributeMapper.Attribute.NEW_COMMENT, null);

            createAttribute(data, MantisAttributeMapper.Attribute.VIEW_STATE,
                    cache.getViewState(), cache.getViewState()[0].getName());

            createAttribute(data, MantisAttributeMapper.Attribute.ASSIGNED_TO, cache.getDevelopersByProjectName(projectName, monitor), true);
            if (existingTask)
				createAttribute(data, MantisAttributeMapper.Attribute.REPORTER, cache.getUsersByProjectName(projectName, monitor));
            
            createAttribute(data, MantisAttributeMapper.Attribute.SUMMARY);
            createAttribute(data, MantisAttributeMapper.Attribute.DATE_SUBMITTED);
            createAttribute(data, MantisAttributeMapper.Attribute.LAST_UPDATED);

            // operations
            data.getRoot().createAttribute(TaskAttribute.OPERATION).getMetaData()
            .setType(TaskAttribute.TYPE_OPERATION);
        } catch (MantisException e) {
            throw new CoreException(MantisCorePlugin.toStatus(e));
        }

    }

    private static void createTaskRelations(TaskData data, IMantisClient client) {

        createAttribute(data, MantisAttributeMapper.Attribute.PARENT_OF, null);
        createAttribute(data, MantisAttributeMapper.Attribute.CHILD_OF, null);
        createAttribute(data, MantisAttributeMapper.Attribute.DUPLICATE_OF, null);
        createAttribute(data, MantisAttributeMapper.Attribute.HAS_DUPLICATE, null);
        createAttribute(data, MantisAttributeMapper.Attribute.RELATED_TO, null);
    }

    public static void createProjectSpecificAttributes(TaskData data, IMantisClient client, IProgressMonitor monitor) {

        try {
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
            
        } catch (MantisException ex) {
            MantisCorePlugin.log(new Status(Status.ERROR,
                    MantisCorePlugin.PLUGIN_ID, 0, ex.getMessage(), ex));
        }
    }

    private static TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute, Object[] values,
            boolean allowEmtpy) {

        boolean readOnly = data.isNew() ? attribute.isReadOnlyForNewTask() : attribute.isReadOnlyForExistingTask();

        TaskAttribute attr = data.getRoot().createAttribute(attribute.getKey());

        attr.getMetaData().setReadOnly(readOnly).setLabel(
                attribute.toString()).setKind(attribute.getKind()).setType(
                        attribute.getType());
        if (values != null && values.length > 0) {
            if (allowEmtpy)
                attr.putOption("", "");
            for (Object value : values)
                attr.putOption(value.toString(), value.toString());
        }
        return attr;
    }

    private static TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute) {

        boolean readOnly = data.isNew() ? attribute.isReadOnlyForNewTask() : attribute.isReadOnlyForExistingTask();

        TaskAttribute attr = data.getRoot().createAttribute(attribute.getKey());
        attr.getMetaData().setReadOnly(readOnly).setLabel(
                attribute.toString()).setKind(attribute.getKind()).setType(
                        attribute.getType());
        return attr;
    }

    private static TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute, Object[] values,
            String defaultValue) {
        TaskAttribute rta = createAttribute(data, attribute, values, false);
        rta.setValue(defaultValue);
        return rta;
    }

    private static TaskAttribute getAttribute(TaskData data, String key) {
        TaskAttribute attribute = data.getRoot().getAttribute(key);
        if (attribute == null)
            attribute = data.getRoot().createAttribute(key);
        return attribute;
    }

    private static TaskAttribute createAttribute(TaskData data,
            MantisAttributeMapper.Attribute attribute, Object[] values) {
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
        TaskData taskData = new TaskData(getAttributeMapper(repository),
                MantisCorePlugin.REPOSITORY_KIND,
                repository.getRepositoryUrl(), ticket.getId() + "");
        try {
        	String projectName = ticket.getValue(Key.PROJECT);
            createDefaultAttributes(taskData, client, projectName, monitor, true);
            updateTaskData(repository, getAttributeMapper(repository),
                    taskData, client, ticket);
            createProjectSpecificAttributes(taskData, client, monitor);
            createCustomFieldAttributes(taskData, client, ticket, monitor);

            if (!MantisRepositoryConnector.hasRichEditor(repository))
                // updateTaskDataFromTicket(taskData, ticket, client);
                taskData.setPartial(true);
            return taskData;
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(MantisCorePlugin.toStatus(e));
        }
    }

    /**
     * @param taskData
     * @param client
     * @param ticket the existing ticket, or null
     * @param monitor
     * @throws MantisException
     */
    public static void createCustomFieldAttributes(TaskData taskData, IMantisClient client,
            MantisTicket ticket, IProgressMonitor monitor) throws MantisException {
        
        TaskAttribute projectAttribute = taskData.getRoot().getAttribute( MantisAttributeMapper.Attribute.PROJECT.getKey());

        for ( MantisCustomField customField : client.getCache(monitor).getCustomFieldsByProjectName(projectAttribute.getValue()) ) {
            TaskAttribute customAttribute = taskData.getRoot().createAttribute(customField.getName());
            customAttribute.getMetaData().setReadOnly(false);
            customAttribute.getMetaData().setLabel(customField.getName());
            customAttribute.getMetaData().setKind(TaskAttribute.KIND_DEFAULT);
            customAttribute.getMetaData().setType(customFieldTypeToTaskType.get(customField.getType()));
            
            String valueToSet = ticket != null ? ticket.getCustomFieldValue(customField.getName()) : customField.getDefaultValue();
            if ( valueToSet == null)
                valueToSet = "";
            
            customAttribute.setValue(valueToSet);
            
            if ( customField.getPossibleValues() != null)
                for ( String possibleValue : customField.getPossibleValues())
                    customAttribute.putOption(possibleValue, possibleValue);
        }
        
    }

    /**
     * Updates attributes of <code>taskData</code> from <code>ticket</code>.
     * @throws MantisException 
     * 
     * @deprecated Possibly not used - double-check
     */
    @Deprecated
    public void updateTaskDataFromTicket(TaskData taskData,
            MantisTicket ticket, IMantisClient client) throws MantisException {

        TaskAttribute attributeSummary = taskData.getRoot().getAttribute(
                TaskAttribute.SUMMARY);
        if (ticket.getValue(Key.SUMMARY) != null)
            attributeSummary.setValue(ticket.getValue(Key.SUMMARY));

        TaskAttribute attributeCompletionDate = taskData.getRoot()
        .getAttribute(TaskAttribute.DATE_COMPLETION);

        if (client.isCompleted(taskData, new NullProgressMonitor()))
            attributeCompletionDate
            .setValue(ticket.getLastChanged().toString());
        else
            attributeCompletionDate.setValue(null);

        String priority = ticket.getValue(Key.PRIORITY);
        TaskAttribute attributePriority = taskData.getRoot().getAttribute(
                TaskAttribute.PRIORITY);
        attributePriority.setValue(MantisPriorityLevel.getMylynPriority(
                priority).toString());

        TaskAttribute attributeCreationDate = taskData.getRoot().getAttribute(
                TaskAttribute.DATE_CREATION);
        if (ticket.getCreated() != null)
            attributeCreationDate.setValue(ticket.getCreated().toString());
    }
    
    @Override
    public boolean canInitializeSubTaskData(TaskRepository taskRepository,
    		ITask task) {
    	
    	if ( task == null)
    		return false;
    	
    	return Boolean.parseBoolean(task.getAttribute(MantisRepositoryConnector.TASK_KEY_SUPPORTS_SUBTASKS));
    }
    
    @Override
    public boolean initializeSubTaskData(TaskRepository repository,
    		TaskData taskData, TaskData parentTaskData, IProgressMonitor monitor)
    		throws CoreException {
		
    	validateSupportsSubtasks(repository, parentTaskData);
    	
        try {
			createAttributesForTaskData(repository, taskData, parentTaskData, monitor);
			copyAttributesFromParent(taskData, parentTaskData); 
			clearTaskRelations(taskData);
			setChildAttribute(taskData, parentTaskData);

			return true;
		} catch (MalformedURLException e) {
			throw new CoreException(new RepositoryStatus(repository, IStatus.ERROR, MantisCorePlugin.PLUGIN_ID,
					RepositoryStatus.ERROR_REPOSITORY, "Invalid repository configuration."));
		} catch (MantisException e) {
			throw new CoreException(new RepositoryStatus(repository, IStatus.ERROR, MantisCorePlugin.PLUGIN_ID,
					RepositoryStatus.ERROR_REPOSITORY, "Failed updating attributes."));

		}
    }

    private void validateSupportsSubtasks(TaskRepository repository, TaskData parentTaskData) throws CoreException {

        if ( parentTaskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey()) == null)
			throw new CoreException(new RepositoryStatus(repository, IStatus.ERROR, MantisCorePlugin.PLUGIN_ID,
					RepositoryStatus.ERROR_REPOSITORY, "The repository does not support subtasks."));
    }
    
    private void createAttributesForTaskData(TaskRepository repository, TaskData taskData, TaskData parentTaskData,
            IProgressMonitor monitor) throws MalformedURLException, MantisException, CoreException {

        IMantisClient client = connector.getClientManager().getRepository(
                repository);
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
}