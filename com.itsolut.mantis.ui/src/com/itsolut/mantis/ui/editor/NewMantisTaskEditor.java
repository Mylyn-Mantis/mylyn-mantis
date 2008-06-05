/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.internal.tasks.ui.deprecated.TaskFactory;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.deprecated.AbstractNewRepositoryTaskEditor;
import org.eclipse.mylyn.internal.tasks.ui.search.SearchHitCollector;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.ui.forms.editor.FormEditor;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisRepositoryQuery;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisSearchFilter.CompareOperator;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class NewMantisTaskEditor extends AbstractTaskEditorPage {

	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	protected TextViewer stepsToReproduceViewer;

	protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
	protected TextViewer additionalViewer;

	public NewMantisTaskEditor(FormEditor editor) {
		super(editor);
	}
	

	public SearchHitCollector getDuplicateSearchCollector(String searchString) {
		MantisSearchFilter filter = new MantisSearchFilter("description");
		filter.setOperator(CompareOperator.CONTAINS);
		filter.addValue(searchString);

		MantisSearch search = new MantisSearch();
		search.addFilter(filter);

		// TODO copied from MantisCustomQueryPage.getQueryUrl()
		StringBuilder sb = new StringBuilder();
		sb.append(repository.getUrl());
		sb.append(IMantisClient.QUERY_URL);
		sb.append(search.toUrl());

		MantisRepositoryQuery query = new MantisRepositoryQuery(repository.getUrl(), sb.toString(), "<Duplicate Search>");

		SearchHitCollector collector = new SearchHitCollector(TasksUiPlugin.getTaskListManager().getTaskList(), repository, query );
		return collector;
	}
}
