/*******************************************************************************
 * Copyright (c) 2008 - Standards for Technology in Automotive Retail and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     David Carver - Initial implementation based on old MantisAttributeFactory
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;

import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * Provides a mapping from Mylyn task keys to Mantis ticket keys.
 * 
 * @author Chris Hane 2.0
 * @author David Carver
 * 
 * @since 3.0
 */
public class MantisAttributeMapper extends TaskAttributeMapper {
    
    private final static Set<Key> _taskRelationKeys = EnumSet.of(Key.PARENT_OF, Key.CHILD_OF, Key.RELATED_TO, Key.HAS_DUPLICATE, Key.DUPLICATE_OF);

    private static final long serialVersionUID = 5333211422546115138L;
    
    /**
     * @return an unmodifiable set containing all the {@link Key keys} which are related to task relations.
     */
    public static final Set<Key> taskRelationKeys() {
        
        return Collections.unmodifiableSet(_taskRelationKeys);
    }
    

    /**
     * Attribute controls how information is displayed in the Attributes column and in the Text editor.
     * 
     * Items that are marked hidden will not be shown in the Attributes drop down.  The format of the enum is
     * 
     * mantis key, Display Name, Attribute Type, Hidden, Read Only.
     * 
     * In Mylyn 3.0, the Type controls what type of ui control is used for that attribute.
     * @author dcarver
     *
     */
    public enum Attribute {
        ID(Key.ID, "<used by search engine>", IMantisConstants.METADATA_SEARCH_ID, true),
        ADDITIONAL_INFO(Key.ADDITIONAL_INFO, "Additional Information:",	TaskAttribute.TYPE_LONG_RICH_TEXT, true, false),
        ASSIGNED_TO(Key.ASSIGNED_TO, "Assigned To:", TaskAttribute.TYPE_SINGLE_SELECT, true, false),
        CATEGORY(Key.CATEOGRY, "Category:",	TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        DATE_SUBMITTED(Key.DATE_SUBMITTED, "Submitted:", TaskAttribute.TYPE_DATE, true, true),
        DESCRIPTION(Key.DESCRIPTION, "Description:", TaskAttribute.TYPE_LONG_RICH_TEXT, true, false),
        ETA(Key.ETA, "ETA:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        LAST_UPDATED(Key.LAST_UPDATED, "Last Modification:", TaskAttribute.TYPE_DATE, true, true),
        PRIORITY(Key.PRIORITY, "Priority:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        PROJECT(Key.PROJECT, "Project:", TaskAttribute.TYPE_SINGLE_SELECT, false, true),
        PROJECTION(Key.PROJECTION, "Projection:", TaskAttribute.TYPE_SHORT_TEXT, true, false),
        REPORTER(Key.REPORTER, "Reporter:", TaskAttribute.TYPE_SINGLE_SELECT, true, false),
        REPRODUCIBILITY(Key.REPRODUCIBILITY, "Reproducibility:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        RESOLUTION(Key.RESOLUTION, "Resolution:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        SEVERITY(Key.SEVERITY, "Severity:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        STATUS(Key.STATUS, "Status:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        STEPS_TO_REPRODUCE(Key.STEPS_TO_REPRODUCE, "Steps To Reproduce:", TaskAttribute.TYPE_LONG_RICH_TEXT, true, false),
        SUMMARY(Key.SUMMARY, "Summary:", TaskAttribute.TYPE_SHORT_RICH_TEXT, true, false),
        VERSION(Key.VERSION, "Version:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        FIXED_IN(Key.FIXED_IN, "Fixed In:",	TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        TARGET_VERSION(Key.TARGET_VERSION, "Target version:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        VIEW_STATE(Key.VIEW_STATE, "View State:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
        NEW_COMMENT(Key.NEW_COMMENT, "new_comment",	TaskAttribute.TYPE_LONG_RICH_TEXT, true, false),
        ATTACHID(Key.ATTACHID, "attachid", TaskAttribute.TYPE_SHORT_TEXT, false, false),
        ATTACHMENT(Key.ATTACHMENT, "attachment", TaskAttribute.TYPE_ATTACHMENT, false, false),
        DUE_DATE(Key.DUE_DATE, "due_date", TaskAttribute.TYPE_DATETIME, false, false),
        // task relations
        // read-only for existing tasks. see https://bugs.eclipse.org/bugs/show_bug.cgi?id=269407
        PARENT_OF(Key.PARENT_OF, "Parent of", TaskAttribute.TYPE_TASK_DEPENDENCY, false, ReadOnly.READ_ONLY_FOR_EXISTING),
        CHILD_OF(Key.CHILD_OF, "Child of", TaskAttribute.TYPE_TASK_DEPENDENCY, false, ReadOnly.READ_ONLY_FOR_EXISTING),
        DUPLICATE_OF(Key.DUPLICATE_OF, "Duplicate of", TaskAttribute.TYPE_TASK_DEPENDENCY, false, ReadOnly.READ_ONLY_FOR_EXISTING),
        HAS_DUPLICATE(Key.HAS_DUPLICATE, "Has duplicate", TaskAttribute.TYPE_TASK_DEPENDENCY, false, ReadOnly.READ_ONLY_FOR_EXISTING), //
        RELATED_TO(Key.RELATED_TO, "Related to", TaskAttribute.TYPE_TASK_DEPENDENCY, false, ReadOnly.READ_ONLY_FOR_EXISTING);

        private final boolean isHidden;

        private final ReadOnly isReadOnly;

        private final String mantisKey;

        private final String prettyName;

        private final String type;

        Attribute(Key key, String prettyName, String type, boolean hidden, ReadOnly readonly) {

            this.mantisKey = key.getKey();
            this.type = type;
            this.prettyName = prettyName;
            this.isHidden = hidden;
            this.isReadOnly = readonly;
        }

        Attribute(Key key, String prettyName, String type, boolean hidden,
                boolean readonly) {
            this.mantisKey = key.getKey();
            this.type = type;
            this.prettyName = prettyName;
            this.isHidden = hidden;
            this.isReadOnly = readonly ? ReadOnly.ALWAYS_READ_ONLY : ReadOnly.NEVER_READ_ONLY;
        }

        Attribute(Key key, String prettyName, String taskKey, boolean hidden) {
            this(key, prettyName, taskKey, hidden, false);
        }

        Attribute(Key key, String prettyName, String taskKey) {
            this(key, prettyName, taskKey, false, false);
        }

        public String getKey() {
            return mantisKey;
        }

        public boolean isHidden() {
            return isHidden;
        }


        public boolean isReadOnlyForExistingTask() {

            return isReadOnly == ReadOnly.ALWAYS_READ_ONLY || isReadOnly == ReadOnly.READ_ONLY_FOR_EXISTING;
        }

        public boolean isReadOnlyForNewTask() {

            return isReadOnly == ReadOnly.ALWAYS_READ_ONLY || isReadOnly == ReadOnly.READ_ONLY_FOR_NEW;
        }

        public String getKind() {
            return isHidden() ? null : TaskAttribute.KIND_DEFAULT;
        }

        public String getType() {
            return this.type;
        }

        @Override
        public String toString() {
            return prettyName;
        }
    }

    public enum ReadOnly {

        /**
         * The attribute is always read only
         */
        ALWAYS_READ_ONLY,
        /**
         * The attribute is read only for new tasks, but writable for existing ones
         */
        READ_ONLY_FOR_NEW,
        /**
         * The attribute is read only for existing tasks, but writable for new ones
         */
        READ_ONLY_FOR_EXISTING,
        /**
         * The attribute is always readonly
         */
        NEVER_READ_ONLY;
    }

    public MantisAttributeMapper(TaskRepository taskRepository) {
        super(taskRepository);
    }
    
    private Map<String, Attribute> taskAttributeToMantisAttributes = new HashMap<String, Attribute>();
    {
    	taskAttributeToMantisAttributes.put(TaskAttribute.COMMENT_NEW, Attribute.NEW_COMMENT);
    	taskAttributeToMantisAttributes.put(TaskAttribute.DESCRIPTION, Attribute.DESCRIPTION);
    	taskAttributeToMantisAttributes.put(TaskAttribute.DATE_MODIFICATION, Attribute.LAST_UPDATED);
    	taskAttributeToMantisAttributes.put(TaskAttribute.SUMMARY, Attribute.SUMMARY);
    	taskAttributeToMantisAttributes.put(TaskAttribute.DATE_CREATION, Attribute.DATE_SUBMITTED);
    	taskAttributeToMantisAttributes.put(TaskAttribute.ATTACHMENT_ID, Attribute.ATTACHID);
    	taskAttributeToMantisAttributes.put(TaskAttribute.USER_ASSIGNED, Attribute.ASSIGNED_TO);
    	taskAttributeToMantisAttributes.put(TaskAttribute.TASK_KEY, Attribute.ID);
    	taskAttributeToMantisAttributes.put(TaskAttribute.USER_REPORTER, Attribute.REPORTER);
    	taskAttributeToMantisAttributes.put(TaskAttribute.STATUS, Attribute.STATUS);
    	taskAttributeToMantisAttributes.put(TaskAttribute.RESOLUTION, Attribute.RESOLUTION);
    	taskAttributeToMantisAttributes.put(TaskAttribute.PRIORITY, Attribute.PRIORITY);
    	taskAttributeToMantisAttributes.put(TaskAttribute.TASK_KIND, Attribute.SEVERITY);
    	taskAttributeToMantisAttributes.put(TaskAttribute.VERSION, Attribute.VERSION);
    	taskAttributeToMantisAttributes.put(TaskAttribute.SEVERITY, Attribute.SEVERITY);
    }

    @Override
    public String mapToRepositoryKey(TaskAttribute parent, String key) {
    	
    	Attribute mapped = taskAttributeToMantisAttributes.get(key);
    	if ( mapped != null)
    		return mapped.getKey().toString();
    	
        return super.mapToRepositoryKey(parent, key).toString();
    }

    @Override
    public Date getDateValue(TaskAttribute attribute) {
        //		try {
        //			String mappedKey = mapToRepositoryKey(attribute, attribute.getId());
        //			if (mappedKey.equals(Attribute.DATE_SUBMITTED.getKey())
        //					|| mappedKey.equals(Attribute.LAST_UPDATED.getKey())) {
        //				return MantisUtils.parseDate(Long.valueOf(attribute
        //						.getValue()));
        //			}
        //		} catch (Exception e) {
        //			MantisCorePlugin.log(e);
        //		}
        if (attribute.getValue().length() > 0)
            return MantisUtils.parseDate(Long.valueOf(attribute.getValue()));
        return null;
    }

    @Override
    public void setDateValue(TaskAttribute attribute, Date date) {
        attribute.setValue(MantisUtils.toMantisTime(date) + "");
    }

    @Override
    public void updateTaskAttachment(ITaskAttachment taskAttachment, TaskAttribute taskAttribute) {

        super.updateTaskAttachment(taskAttachment, taskAttribute);

        if (taskAttachment.getFileName().startsWith(MantisAttachmentHandler.CONTEXT_DESCRIPTION))
            taskAttachment.setDescription(MantisAttachmentHandler.CONTEXT_DESCRIPTION);

    }
    
}
