package com.itsolut.mantis.ui.editor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

import com.itsolut.mantis.core.MantisCorePlugin;

public class MantisTaskEditorPage extends AbstractTaskEditorPage {


	public static final String ID_MANTIS_PART_STEPSTOREPRODUCE = "com.itsolut.mantis.tasks.ui.editors.parts.stepstoreproduce";
	public static final String ID_MANTIS_PART_ADDITIONALINFO = "com.itsolut.mantis.tasks.ui.editors.parts.additionalinfo";

	public MantisTaskEditorPage(TaskEditor editor) {
		super(editor, MantisCorePlugin.REPOSITORY_KIND);
		
		setNeedsPrivateSection(true);
		setNeedsSubmitButton(true);
		setNeedsSubmit(true);
	}

	@Override
	protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
		Set<TaskEditorPartDescriptor> descriptors = super
				.createPartDescriptors();
		
		// expand only on edited tasks
		final boolean taskIsSubmitted = getModel().getTaskData().getTaskId().length() != 0;
		
		if ( !taskIsSubmitted)
			removeNewCommentPart(descriptors);
		
		descriptors = insertPart(descriptors,
				new TaskEditorPartDescriptor(ID_MANTIS_PART_STEPSTOREPRODUCE) {
			@Override
			public AbstractTaskEditorPart createPart() {
				MantisStepsToReproducePart part = new MantisStepsToReproducePart(taskIsSubmitted);
				part.setExpandVertically(true);
				return part;
			}
		}.setPath(PATH_COMMENTS),
		     ID_PART_DESCRIPTION);
		
		descriptors = insertPart(descriptors,
				new TaskEditorPartDescriptor(ID_MANTIS_PART_ADDITIONALINFO) {
			@Override
			public AbstractTaskEditorPart createPart() {
				MantisAdditionalInformationPart part = new MantisAdditionalInformationPart(taskIsSubmitted);
				part.setExpandVertically(true);
				return part;
			}
		}.setPath(PATH_COMMENTS),
			ID_MANTIS_PART_STEPSTOREPRODUCE);
		
		return descriptors;
		
	}
	
	private void removeNewCommentPart(Set<TaskEditorPartDescriptor> descriptors) {
		
		for (Iterator<TaskEditorPartDescriptor> it = descriptors.iterator(); it.hasNext();) {
			TaskEditorPartDescriptor taskEditorPartDescriptor = it.next();
			if (taskEditorPartDescriptor.getId().equals(ID_PART_NEW_COMMENT))
				it.remove();
		}


	}

	protected Set<TaskEditorPartDescriptor> insertPart(Set<TaskEditorPartDescriptor> originalDescriptors, TaskEditorPartDescriptor newDescriptor, String insertAfterId ) {
		Set<TaskEditorPartDescriptor> newDescriptors = new LinkedHashSet<TaskEditorPartDescriptor>();
		for (TaskEditorPartDescriptor taskEditorPartDescriptor : originalDescriptors) {
			newDescriptors.add(taskEditorPartDescriptor);
			if (taskEditorPartDescriptor.getId().equals(insertAfterId)) {
				newDescriptors.add(newDescriptor);
			}
		}
		
		return newDescriptors;
	}
	
}
