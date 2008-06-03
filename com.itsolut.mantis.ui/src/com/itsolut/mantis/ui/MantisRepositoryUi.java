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

package com.itsolut.mantis.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.LegendElement;
import org.eclipse.mylyn.tasks.ui.TaskHyperlink;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskSearchPage;

import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisTask;
import com.itsolut.mantis.ui.wizard.EditMantisQueryWizard;
import com.itsolut.mantis.ui.wizard.MantisCustomQueryPage;
import com.itsolut.mantis.ui.wizard.MantisRepositorySettingsPage;
import com.itsolut.mantis.ui.wizard.NewMantisTaskWizard;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisRepositoryUi extends AbstractRepositoryConnectorUi {

	private static final Pattern HYPERLINK_PATTERN = Pattern.compile(
			"bug (\\d+)", Pattern.CASE_INSENSITIVE);


	@Override
	public ITaskSearchPage getSearchPage(TaskRepository repository,
			IStructuredSelection selection) {
		return new MantisCustomQueryPage(repository);
	}

	@Override
	public List<ITask> getLegendItems() {
		List<ITask> legendItems = new ArrayList<ITask>();

		MantisTask blocker = new MantisTask("", "block", "Block");
		blocker.setSeverity("block");
		legendItems.add(blocker);

		MantisTask major = new MantisTask("", "major", "Major, crash");
		major.setSeverity("major");
		legendItems.add(major);

		MantisTask enhancement = new MantisTask("", "feature", "Feature");
		enhancement.setSeverity("feature");
		legendItems.add(enhancement);

		MantisTask trivial = new MantisTask("", "trivial", "Trivial, Minor");
		trivial.setSeverity("trivial");
		legendItems.add(trivial);

		return legendItems;
	}

	@Override
	public ImageDescriptor getTaskKindOverlay(ITask task) {
		if (task instanceof MantisTask) {
			MantisTask mantisTask = (MantisTask) task;
			String severity = mantisTask.getSeverity();

			if (severity != null) {
				if ("block".equals(severity)) {
					return MantisImages.OVERLAY_CRITICAL;
				} else if ("major".equals(severity) || "crash".equals(severity)) {
					return MantisImages.OVERLAY_MAJOR;
				} else if ("feature".equals(severity)) {
					return MantisImages.OVERLAY_ENHANCEMENT;
				} else if ("trivial".equals(severity)
						|| "minor".equals(severity)) {
					return MantisImages.OVERLAY_MINOR;
				} else {
					return null;
				}
			}
		}
		return super.getTaskKindOverlay(task);
	}

	@Override
	public IHyperlink[] findHyperlinks(TaskRepository repository, String text,
			int lineOffset, int regionOffset) {

		Matcher matcher = HYPERLINK_PATTERN.matcher(text);

		List<IHyperlink> links = null;

		while (matcher.find()) {
			if (!isInRegion(lineOffset, matcher))
				continue;

			if (links == null)
				links = new ArrayList<IHyperlink>();

			String id = matcher.group(1);

			links.add(new TaskHyperlink(determineRegion(regionOffset, matcher),
					repository, id));
		}

		return links == null ? null : links
				.toArray(new IHyperlink[links.size()]);

	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}

	@Override
	public String getConnectorKind() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}

	private boolean isInRegion(int lineOffset, Matcher m) {
		return (lineOffset >= m.start() && lineOffset <= m.end());
	}

	private IRegion determineRegion(int regionOffset, Matcher m) {
		return new Region(regionOffset + m.start(), m.end() - m.start());
	}

	@Override
	public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping taskMapping) {
		// TODO Auto-generated method stub
		return new NewMantisTaskWizard(repository, taskMapping);
	}

	@Override
	public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery query) {
		return new EditMantisQueryWizard(repository, query);
	}

	@Override
	public ITaskRepositoryPage getSettingsPage(TaskRepository arg0) {
		return new MantisRepositorySettingsPage("Mantis", "Mantis", arg0);
	}

}
