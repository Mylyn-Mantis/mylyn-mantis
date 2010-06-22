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

import java.util.Set;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTaskEditor extends AbstractTaskEditorPage {

    protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
    protected static final int STEPS_TO_REPRODUCE_HEIGHT = 150;
    protected TextViewer stepsToReproduceViewer;

    protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
    protected static final int ADDITIONAL_INFO_HEIGHT = 150;
    protected TextViewer additionalViewer;

    protected static final int DESCRIPTION_HEIGHT = 150;

    public MantisTaskEditor(TaskEditor editor, String connectorKind) {

        super(editor, connectorKind);
    }

    @Override
    protected Set<TaskEditorPartDescriptor> createPartDescriptors() {

        Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();

        // remove unnecessary default editor parts
        for (TaskEditorPartDescriptor taskEditorPartDescriptor : descriptors) {
            if (taskEditorPartDescriptor.getId().equals(ID_PART_PEOPLE)) {
                descriptors.remove(taskEditorPartDescriptor);
                break;
            }
        }

        return descriptors;
    }

}
