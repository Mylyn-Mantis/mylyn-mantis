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

package com.itsolut.mantis.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskListFactory;
import org.w3c.dom.Element;

import com.itsolut.mantis.core.MantisRepositoryQuery;
import com.itsolut.mantis.core.MantisTask;

/**
 * @author Steffen Pingel
 */
public class MantisTaskListFactory extends AbstractTaskListFactory {

	private static final String KEY_SEVERITY = "severity";
	
	private static final String KEY_TRAC = "Mantis";

	private static final String KEY_TRAC_TASK = KEY_TRAC + KEY_TASK;

	private static final String KEY_TRAC_QUERY = KEY_TRAC + KEY_QUERY;

	// category related methods
	
	@Override
	public String getTaskElementName() {
		return KEY_TRAC_TASK;
	}

	@Override
	public Set<String> getQueryElementNames() {
		Set<String> names = new HashSet<String>();
		names.add(KEY_TRAC_QUERY);
		return names;
	}

	@Override
	public boolean canCreate(AbstractTask task) {
		return task instanceof MantisTask;
	}

	@Override
	public boolean canCreate(AbstractRepositoryQuery category) {
		return category instanceof MantisRepositoryQuery;
	}

	@Override
	public String getQueryElementName(AbstractRepositoryQuery query) {
		return query instanceof MantisRepositoryQuery ? KEY_TRAC_QUERY : "";
	}

	@Override
	public void setAdditionalAttributes(AbstractTask task, Element element) {
		element.setAttribute(KEY_SEVERITY, ((MantisTask)task).getSeverity());
	}
	
	@Override
	public AbstractTask createTask(String repositoryUrl, String taskId, String summary, Element element) {
		MantisTask task = new MantisTask(repositoryUrl, taskId, summary);
		if(element.hasAttribute(KEY_SEVERITY)) {
			task.setSeverity(element.getAttribute(KEY_SEVERITY));
		}
		return task;
	}

	@Override
	public AbstractRepositoryQuery createQuery(String repositoryUrl, String queryString, String label, Element element) {
		return new MantisRepositoryQuery(repositoryUrl, queryString, label);
	}

}
