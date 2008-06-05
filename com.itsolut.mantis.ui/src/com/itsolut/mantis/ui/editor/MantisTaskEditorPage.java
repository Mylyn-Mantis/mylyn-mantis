package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;

import com.itsolut.mantis.core.MantisCorePlugin;

public class MantisTaskEditorPage extends AbstractTaskEditorPage {

	public MantisTaskEditorPage(TaskEditor editor, String connectorKind) {
		super(editor, connectorKind);
		// TODO Auto-generated constructor stub
	}
	
	public MantisTaskEditorPage(TaskEditor editor) {
		super(editor, MantisCorePlugin.REPOSITORY_KIND);
	}

}
