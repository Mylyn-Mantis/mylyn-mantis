package com.itsolut.mantis.ui.editor;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

import com.itsolut.mantis.core.MantisCorePlugin;

public class MantisTaskEditorPage extends AbstractTaskEditorPage {
	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	protected static final int STEPS_TO_REPRODUCE_HEIGHT = 150;
	protected TextViewer stepsToReproduceViewer;

	protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
	protected static final int ADDITIONAL_INFO_HEIGHT = 150;
	protected TextViewer additionalViewer;

	protected static final int DESCRIPTION_HEIGHT = 150;
	private static final int DESCRIPTION_WIDTH = 79 * 7; // 500;

	public static final String ID_MANTIS_PART_ATTRIBUTES = "com.itsolut.mantis.tasks.ui.editors.parts.attributes";

	private TaskData taskData;

	public MantisTaskEditorPage(TaskEditor editor, String connectorKind) {
		super(editor, connectorKind);
		// TODO Auto-generated constructor stub
	}

	public MantisTaskEditorPage(TaskEditor editor) {
		super(editor, MantisCorePlugin.REPOSITORY_KIND);
	}

	@Override
	protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
		Set<TaskEditorPartDescriptor> descriptors = super
				.createPartDescriptors();

		// remove unnecessary default editor parts
		for (TaskEditorPartDescriptor taskEditorPartDescriptor : descriptors) {
			if (taskEditorPartDescriptor.getId().equals(ID_PART_PEOPLE)) {
				descriptors.remove(taskEditorPartDescriptor);
				break;
			}
		}

//		// Add Mantis Attribute
//		TaskData data;
//		try {
//			data = TasksUi.getTaskDataManager().getTaskData(getTask());
//			if (data != null) {
//				descriptors.add(new TaskEditorPartDescriptor(
//						ID_MANTIS_PART_ATTRIBUTES) {
//					@Override
//					public AbstractTaskEditorPart createPart() {
//						return new MantisAttributeEditorPart();
//					}
//				}.setPath(PATH_ATTRIBUTES));
//			}
//		} catch (CoreException e) {
//			
//		}
		return descriptors;
	}

}
