/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.it;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.rpc.ServiceException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.junit.Test;

import com.itsolut.mantis.core.MantisAttachmentHandler;
import com.itsolut.mantis.core.MantisRepositoryConnector;

/**
 * @author Robert Munteanu
 */
public abstract class AbstractMantisAttachmentHandlerIntegrationTest extends AbstractIntegrationTest {

	@Test
	public void testAddAndGetAttachment() throws ServiceException, CoreException, IOException {

		int taskId = createTask("Upload task", "Description");

		MantisRepositoryConnector connector = new MantisRepositoryConnector();
		MantisAttachmentHandler attachmentHandler = (MantisAttachmentHandler) connector.getTaskAttachmentHandler();

		ITask task = getObjectsFactory().newTask(repositoryAccessor.getLocation().getUrl(), String.valueOf(taskId));

		attachmentHandler.postContent(repositoryAccessor.getRepository(), task,
				getObjectsFactory().newTaskAttachmentSource("Attachment contents"), "", null, new NullProgressMonitor());

		TaskData taskData = connector.getTaskData(repositoryAccessor.getRepository(), String.valueOf(taskId),
				new NullProgressMonitor());

		TaskAttribute attachmentAttribute = taskData.getRoot().getAttribute(TaskAttribute.PREFIX_ATTACHMENT + 1);

		InputStream content = attachmentHandler.getContent(repositoryAccessor.getRepository(), task,
				attachmentAttribute, new NullProgressMonitor());

		ByteArrayOutputStream output = readFrom(content);

		String contents = new String(output.toByteArray(), "UTF-8");

		assertEquals("Attachment contents", contents);
	}

	private ByteArrayOutputStream readFrom(InputStream content) throws IOException {

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			byte[] buffer = new byte[4096];
			while (true) {
				int count = content.read(buffer);
				if (count == -1) {
					break;
				}
				output.write(buffer, 0, count);
			}
		} finally {
			content.close();

		}
		return output;
	}
}
