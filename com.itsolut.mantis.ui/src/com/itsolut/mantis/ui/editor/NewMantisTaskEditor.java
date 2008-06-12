/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007, 2008 - 2008 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *     David Carver - STAR - migrated to Mylyn 3.0
 *******************************************************************************/

package com.itsolut.mantis.ui.editor;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.search.SearchHitCollector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisSearchFilter.CompareOperator;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 * @author David Carver
 * 
 * @since 2.0
 * 
 */
public class NewMantisTaskEditor extends AbstractTaskEditorPage {

	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	protected TextViewer stepsToReproduceViewer;

	protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
	protected TextViewer additionalViewer;
	

	public NewMantisTaskEditor(TaskEditor editor, String connectorKind) {
		super(editor, connectorKind);
	}

//	public SearchHitCollector getDuplicateSearchCollector(String searchString) {
//		MantisSearchFilter filter = new MantisSearchFilter("description");
//		filter.setOperator(CompareOperator.CONTAINS);
//		filter.addValue(searchString);
//
//		MantisSearch search = new MantisSearch();
//		search.addFilter(filter);
//
//		// TODO copied from MantisCustomQueryPage.getQueryUrl()
//		StringBuilder sb = new StringBuilder();
//		sb.append(getTaskRepository().getRepositoryUrl());
//		sb.append(IMantisClient.QUERY_URL);
//		sb.append(search.toUrl());
//
//		IRepositoryQuery query 
//		   = TasksUi.getRepositoryModel().createRepositoryQuery(getTaskRepository());
//		query.setUrl(sb.toString());
//		query.setSummary("<Duplicate Search>");
//
//		SearchHitCollector collector = new SearchHitCollector(TasksUiPlugin.getTaskList(), getTaskRepository(), query );
//		return collector;
//	}
}
