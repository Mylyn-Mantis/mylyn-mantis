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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

	private static final long serialVersionUID = 5333211422546115138L;

	private static Map<String, Attribute> attributeByMantisKey = new HashMap<String, Attribute>();

	private static Map<String, String> mantisKeyByTaskKey = new HashMap<String, String>();

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
		ASSIGNED_TO(Key.ASSIGNED_TO, "Assigned To:", TaskAttribute.USER_ASSIGNED, true, false),
		CATEGORY(Key.CATEOGRY, "Category:",	TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		DATE_SUBMITTED(Key.DATE_SUBMITTED, "Submitted:", TaskAttribute.TYPE_DATE, false, true),
		DESCRIPTION(Key.DESCRIPTION, "Description:", TaskAttribute.TYPE_LONG_RICH_TEXT, true, false),
		ETA(Key.ETA, "ETA:", TaskAttribute.TYPE_SHORT_TEXT, false, false),
		LAST_UPDATED(Key.LAST_UPDATED, "Last Modification:", TaskAttribute.TYPE_DATE, false, true),
		PRIORITY(Key.PRIORITY, "Priority:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		PROJECT(Key.PROJECT, "Project:", TaskAttribute.TYPE_SHORT_TEXT, false, true),
		PROJECTION(Key.PROJECTION, "Projection:", IMantisConstants.METADATA_PROJECTION, true, false),
		RELATIONSHIPS(Key.RELATIONSHIPS, "Relationships:", TaskAttribute.TYPE_SHORT_TEXT, false, false),
		REPORTER(Key.REPORTER, "Reporter:", TaskAttribute.USER_REPORTER, true, false),
		REPRODUCIBILITY(Key.REPRODUCIBILITY, "Reproducibility:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		RESOLUTION(Key.RESOLUTION, "Resolution:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		SEVERITY(Key.SEVERITY, "Severity:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		STATUS(Key.STATUS, "Status:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		STEPS_TO_REPRODUCE(Key.STEPS_TO_REPRODUCE, "Steps To Reproduce:", TaskAttribute.TYPE_LONG_RICH_TEXT, true, false),
		SUMMARY(Key.SUMMARY, "Summary:", TaskAttribute.TYPE_SHORT_TEXT, true, false),
		VERSION(Key.VERSION, "Version:", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		FIXED_IN(Key.FIXED_IN, "Fixed In:",	TaskAttribute.TYPE_SINGLE_SELECT, false, false),
		VIEW_STATE(Key.VIEW_STATE, "View State:", IMantisConstants.METADATA_VIEW_STATE, true, true),
		NEW_COMMENT(Key.NEW_COMMENT, "new_comment",	TaskAttribute.TYPE_LONG_RICH_TEXT, true, false);

		private final boolean isHidden;

		private final boolean isReadOnly;

		private final String mantisKey;

		private final String prettyName;

		private final String type;

		Attribute(Key key, String prettyName, String type, boolean hidden,
				boolean readonly) {
			this.mantisKey = key.getKey();
			this.type = type;
			this.prettyName = prettyName;
			this.isHidden = hidden;
			this.isReadOnly = readonly;
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

		public boolean isReadOnly() {
			return isReadOnly;
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

	public MantisAttributeMapper(TaskRepository taskRepository) {
		super(taskRepository);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String mapToRepositoryKey(TaskAttribute parent, String key) {
		if (key.equals(TaskAttribute.COMMENT_NEW)) {
			return Attribute.NEW_COMMENT.getKey().toString();
		}
		
		if (key.equals(TaskAttribute.DESCRIPTION)) {
			return Attribute.DESCRIPTION.getKey().toString();
		}
		
		if (key.equals(TaskAttribute.DATE_MODIFICATION)) {
			return Attribute.LAST_UPDATED.getKey().toString();
		}
		
		if (key.equals(TaskAttribute.SUMMARY)) {
			return Attribute.SUMMARY.getKey().toString();
		}
		
		if (key.equals(TaskAttribute.DATE_CREATION)) {
			return Attribute.DATE_SUBMITTED.getKey().toString();
		}
		
		return super.mapToRepositoryKey(parent, key).toString();
	}

	public Date getDateValue(TaskAttribute attribute) {
		try {
			String mappedKey = mapToRepositoryKey(attribute, attribute.getId());
			if (mappedKey.equals(Attribute.DATE_SUBMITTED.getKey())
					|| mappedKey.equals(Attribute.LAST_UPDATED.getKey())) {
				return MantisUtils.parseDate(Integer.valueOf(attribute
						.getValue()));
			}
		} catch (Exception e) {
			MantisCorePlugin.log(e);
		}
		return null;
	}

}
