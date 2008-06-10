/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.ui.tasklist;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.mylyn.tasks.core.AbstractTaskListMigrator;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.w3c.dom.Element;

import com.itsolut.mantis.core.MantisAttributeMapper;
import com.itsolut.mantis.core.MantisCorePlugin;

/**
 * @author Rob Elves
 * @since 3.0
 */
public class MantisTaskListMigrator extends AbstractTaskListMigrator {

	private static final String KEY_SEVERITY = "mantis.severity";

	private static final String KEY_MANTIS = "Mantis";

	private static final String KEY_MANTIS_TASK = KEY_MANTIS + KEY_TASK;

	private static final String KEY_MANTIS_QUERY = KEY_MANTIS + KEY_QUERY;

	@Override
	public String getConnectorKind() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public Set<String> getQueryElementNames() {
		Set<String> names = new HashSet<String>();
		names.add(KEY_MANTIS_QUERY);
		return names;
	}

	@Override
	public String getTaskElementName() {
		return KEY_MANTIS_TASK;
	}

	@Override
	public void migrateQuery(IRepositoryQuery query, Element element) {
		query.setAttribute(KEY_MANTIS_QUERY, Boolean.TRUE.toString());
//		if (element.getNodeName().equals(IBugzillaConstants.TAG_BUGZILLA_CUSTOM_QUERY)) {
//			query.setAttribute(IBugzillaConstants.ATTRIBUTE_BUGZILLA_QUERY_CUSTOM, Boolean.TRUE.toString());
//		}
	}

	@Override
	public void migrateTask(ITask task, Element element) {
		if (element.hasAttribute(KEY_SEVERITY)) {
			task.setAttribute(MantisAttributeMapper.Attribute.STATUS.getMantisKey(),
					element.getAttribute(MantisAttributeMapper.Attribute.STATUS.getMantisKey()));
		}
//		if (element.hasAttribute(KEY_PRODUCT)) {
//			task.setAttribute(BugzillaAttribute.PRODUCT.getKey(),
//					element.getAttribute(BugzillaAttribute.PRODUCT.getKey()));
//		}
//		if (element.hasAttribute(KEY_LAST_MOD_DATE)) {
//			task.setAttribute(BugzillaAttribute.DELTA_TS.getKey(), element.getAttribute(KEY_LAST_MOD_DATE));
//		}
	}

}
