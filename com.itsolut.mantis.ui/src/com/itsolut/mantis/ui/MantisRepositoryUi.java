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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;

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

	@Override
	public AbstractRepositorySettingsPage getSettingsPage() {
		return new MantisRepositorySettingsPage(this);
	}

	@Override
	public WizardPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new MantisCustomQueryPage(repository);
	}

	@Override
	public List<AbstractTaskContainer> getLegendItems() {
		List<AbstractTaskContainer> legendItems = new ArrayList<AbstractTaskContainer>();

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
	public ImageDescriptor getTaskKindOverlay(AbstractTask task) {
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
				} else if ("trivial".equals(severity) || "minor".equals(severity)) {
					return MantisImages.OVERLAY_MINOR;
				} else {
					return null;
				}
			}
		}
		return super.getTaskKindOverlay(task);
	}
	
	@Override
	public boolean hasSearchPage() {
		return true;
	}
	
	@Override
	public IWizard getNewTaskWizard(TaskRepository repository) {
		return new NewMantisTaskWizard(repository);
	}
	
	@Override
	public IWizard getQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		return new EditMantisQueryWizard(repository, query);
	}

	@Override
	public String getConnectorKind() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}
}
