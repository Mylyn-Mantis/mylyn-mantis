/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.TaskHyperlink;
import org.junit.Test;

import com.itsolut.mantis.core.MantisCommentMapper;
import com.itsolut.mantis.core.MantisCommentMapper.CommentMapping;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.tests.MantisTestConstants;
import com.itsolut.mantis.tests.MylynObjectsFactory;

/**
 * @author Robert Munteanu
 */
public class MantisHyperlinkFinderTest {
	
	private static final String TASK_ID = "12";
	private static final int COMMENT_TASK_ID = 13;
	private static final int COMMENT_ID = 25;
	private static final int COMMENT_NUMBER = 1;

	@Test
	public void noHyperlinksFound() {
		
		assertNull(findHyperlinks("Some text which contains\no references to bugs.", new MantisCommentMapper()));
	}
	
	private IHyperlink[] findHyperlinks(String text, MantisCommentMapper commentMapper) {

		MylynObjectsFactory factory = new MylynObjectsFactory();
		TaskRepository repo = new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, MantisTestConstants.TEST_MANTIS_HTTP_URL);
		
		ITask task = factory.newTask(repo.getRepositoryUrl(), TASK_ID);
		
		return new MantisHyperlinkFinder(commentMapper).findHyperlinks(repo, task, text, -1, text.length() - 3);
	}
	
	@Test
	public void bugHyperlinkFound() {
	
		IHyperlink[] hyperlinks = findHyperlinks("Some text which contains\reference to bug #" + TASK_ID +".", new MantisCommentMapper());
		
		assertNotNull(hyperlinks);
		assertEquals(1, hyperlinks.length);
		assertEquals(TASK_ID, ((TaskHyperlink) hyperlinks[0]).getTaskId());
	}
	
	@Test
	public void commentHyperlinkFound() {
	
		MantisCommentMapper commentMapper = new MantisCommentMapper();
		commentMapper.registerCommentNumber(COMMENT_ID, new CommentMapping(COMMENT_TASK_ID, COMMENT_NUMBER));

		IHyperlink[] hyperlinks = findHyperlinks("Some text which contains\reference to comment ~"+COMMENT_ID+".", commentMapper);
		
		assertNotNull(hyperlinks);
		assertEquals(1, hyperlinks.length);
		assertEquals(String.valueOf(COMMENT_TASK_ID), ((TaskHyperlink) hyperlinks[0]).getTaskId());
		assertEquals(TaskAttribute.PREFIX_COMMENT + COMMENT_NUMBER, ((TaskHyperlink) hyperlinks[0]).getSelection());
	}
}
