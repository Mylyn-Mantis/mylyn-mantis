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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractAttributeFactory;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskAttribute;

import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * Provides a mapping from Mylar task keys to Mantis ticket keys. 
 *
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisAttributeFactory extends AbstractAttributeFactory {

	private static final long serialVersionUID = 5333211422546115138L;

	private static Map<String, Attribute> attributeByMantisKey = new HashMap<String, Attribute>();

	private static Map<String, String> mantisKeyByTaskKey = new HashMap<String, String>();
	
	public enum Attribute {
		ID(Key.ID, "<used by search engine>", null, true),
		ADDITIONAL_INFO(Key.ADDITIONAL_INFO, "Additional Information:", null, true, false),
		ASSIGNED_TO(Key.ASSIGNED_TO, "Assigned To:", RepositoryTaskAttribute.USER_ASSIGNED, true, false),
		CATEGORY(Key.CATEOGRY, "Category:", null, false, true),
		DATE_SUBMITTED(Key.DATE_SUBMITTED, "Submitted:", RepositoryTaskAttribute.DATE_CREATION, true, true),
		DESCRIPTION(Key.DESCRIPTION, "Description2:", RepositoryTaskAttribute.DESCRIPTION, true, false),
		ETA(Key.ETA, "ETA:", null), 
		LAST_UPDATED(Key.LAST_UPDATED, "Last Modification:", RepositoryTaskAttribute.DATE_MODIFIED, true, true),
		PRIORITY(Key.PRIORITY, "Priority:", RepositoryTaskAttribute.PRIORITY),
		PROJECT(Key.PROJECT, "Project:", null, true, false),
		PROJECTION(Key.PROJECTION, "Projection:", null),
		RELATIONSHIPS(Key.RELATIONSHIPS,  "Relationships:", null, true, false),
		REPORTER(Key.REPORTER, "Reporter:", RepositoryTaskAttribute.USER_REPORTER, true, false),
		REPRODUCIBILITY(Key.REPRODUCIBILITY, "Reproducibility:", null),
		RESOLUTION(Key.RESOLUTION, "Resolution:", RepositoryTaskAttribute.RESOLUTION, false, false),
		SEVERITY(Key.SEVERITY, "Severity:", null),
		STATUS(Key.STATUS, "Status:", RepositoryTaskAttribute.STATUS, false, false),
		STEPS_TO_REPRODUCE(Key.STEPS_TO_REPRODUCE, "Steps To Reproduce:", null, true, false),
		SUMMARY(Key.SUMMARY, "Summary:", RepositoryTaskAttribute.SUMMARY, true),
		VERSION(Key.VERSION, "Version:", null),
		FIXED_IN(Key.FIXED_IN, "Fixed In:", null),
		VIEW_STATE(Key.VIEW_STATE, "View State:", null), 
		
		
		
//		CC(Key.CC, "CC:", RepositoryTaskAttribute.USER_CC, true, false), 
//		COMPONENT(Key.COMPONENT, "Component:", null),
//		KEYWORDS(Key.KEYWORDS, "Keywords:", RepositoryTaskAttribute.KEYWORDS),
//		MILESTONE(Key.MILESTONE, "Milestone:", null),
////		OWNER(Key.OWNER, "Assigned to:", RepositoryTaskAttribute.USER_ASSIGNED, true, true),
//		TIME(Key.TIME, "Created:", RepositoryTaskAttribute.DATE_CREATION, true, true), 
//		TYPE(Key.TYPE, "Type:", null), 
		;		
		
		private final boolean isHidden;
		
		private final boolean isReadOnly;
		
		private final String mantisKey;
		
		private final String prettyName;

		private final String taskKey;

		Attribute(Key key, String prettyName, String taskKey, boolean hidden, boolean readonly) {		
			this.mantisKey = key.getKey();
			this.taskKey = taskKey;
			this.prettyName = prettyName;
			this.isHidden = hidden;
			this.isReadOnly = readonly;
			
			attributeByMantisKey.put(mantisKey, this);
			if (taskKey != null) {
				mantisKeyByTaskKey.put(taskKey, mantisKey);
			}
		}

		Attribute(Key key, String prettyName, String taskKey, boolean hidden) {		
			this(key, prettyName, taskKey, hidden, false);
		}
		
		Attribute(Key key, String prettyName, String taskKey) {		
			this(key, prettyName, taskKey, false, false);
		}

		public String getTaskKey() {
			return taskKey;
		}

		public String getMantisKey() {
			return mantisKey;
		}

		public boolean isHidden() {
			return isHidden;
		}	
		
		public boolean isReadOnly() {
			return isReadOnly;
		}
		
		@Override
		public String toString() {
			return prettyName;
		}
	}

	static {
		// make sure hash maps get initialized when class is loaded
		Attribute.values();
	}

	@Override
	public boolean isHidden(String key) {
		Attribute attribute = attributeByMantisKey.get(key);
		return (attribute != null) ? attribute.isHidden() : isInternalAttribute(key);
	}

	@Override
	public String getName(String key) {
		Attribute attribute = attributeByMantisKey.get(key);
		// TODO if attribute == null it is probably a custom field: need 
		// to query custom field information from repoository
		return (attribute != null) ? attribute.toString() : key;
	}

	@Override
	public boolean isReadOnly(String key) {
		Attribute attribute = attributeByMantisKey.get(key);
		return (attribute != null) ? attribute.isReadOnly() : false;
	}

	@Override
	public String mapCommonAttributeKey(String key) {
		String mantisKey = mantisKeyByTaskKey.get(key);
		return (mantisKey != null) ? mantisKey : key;
	}

	static boolean isInternalAttribute(String id) {
		return RepositoryTaskAttribute.REMOVE_CC.equals(id) || RepositoryTaskAttribute.NEW_CC.equals(id) || RepositoryTaskAttribute.ADD_SELF_CC.equals(id) || RepositoryTaskAttribute.COMMENT_NEW.equals(id);
	}

	@Override
	public Date getDateForAttributeType(String attributeKey, String dateString) {
		if (dateString == null || dateString.length() == 0) {
			return null;
		}

		try {
			String mappedKey = mapCommonAttributeKey(attributeKey);
			if (mappedKey.equals(Attribute.DATE_SUBMITTED.getMantisKey()) || mappedKey.equals(Attribute.LAST_UPDATED.getMantisKey())) {
				return MantisUtils.parseDate(Integer.valueOf(dateString));
			}
		} catch (Exception e) {
			MantisCorePlugin.log(e);
		}
		return null;
	}

}
