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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisCorePlugin;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class MantisUIPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.itsolut.mantis.ui";

	public static final String NEW_BUG_EDITOR_ID = PLUGIN_ID + ".newBugEditor";

	private static MantisUIPlugin plugin;

    private FormColors formColors;

	public MantisUIPlugin() {
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		MantisClientFactory.getDefault().setTaskRepositoryLocationFactory(new TaskRepositoryLocationUiFactory());
		TasksUi.getRepositoryManager().addListener(MantisCorePlugin.getDefault().getConnector().getClientManager());
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	    TasksUi.getRepositoryManager().removeListener(MantisCorePlugin.getDefault().getConnector().getClientManager());
		
		plugin = null;
		
		if ( formColors != null ) {
		    formColors.dispose();
		    formColors = null;
		}
		
		super.stop(context);
	}

	public static MantisUIPlugin getDefault() {
		return plugin;
	}
	
    public static void handleError(Throwable throwable, String message, boolean show) {

        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, throwable);

        int style = StatusManager.LOG;
        if (show)
            style |= StatusManager.SHOW;

        StatusManager.getManager().handle(status, style);
    }
    
    public FormColors getFormColors(Display display) {
        if (formColors == null) {
            formColors = new FormColors(display);
            formColors.markShared();
        }
        return formColors;
    }

}
