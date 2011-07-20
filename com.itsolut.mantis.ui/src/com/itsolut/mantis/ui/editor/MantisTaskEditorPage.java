package com.itsolut.mantis.ui.editor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

import com.itsolut.mantis.core.MantisAttributeMapper;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConfiguration;
import com.itsolut.mantis.core.util.HtmlFormatter;

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
		Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();
		
		final boolean useRichTextEditor = MantisRepositoryConfiguration.isUseRichTextEditor(getModel().getTaskRepository());
		
		if ( useRichTextEditor ) {
		    descriptors = replaceDescriptionPart(descriptors);
		    descriptors = replaceNewCommentPart(descriptors);
		}
		
		descriptors = insertPart(descriptors,
				new TaskEditorPartDescriptor(ID_MANTIS_PART_STEPSTOREPRODUCE) {
			@Override
			public AbstractTaskEditorPart createPart() {
                if ( useRichTextEditor )
                    return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE.toString(), MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE.getKey(), false);
                    
                return new MantisStepsToReproducePart(false);

			}
		}.setPath(PATH_COMMENTS),
		     ID_PART_DESCRIPTION);
		
		descriptors = insertPart(descriptors,
				new TaskEditorPartDescriptor(ID_MANTIS_PART_ADDITIONALINFO) {
			@Override
			public AbstractTaskEditorPart createPart() {
			    
			    if ( useRichTextEditor )
                    return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.ADDITIONAL_INFO.toString(), MantisAttributeMapper.Attribute.ADDITIONAL_INFO.getKey(), false);

			    return new MantisAdditionalInformationPart(false);
			    
			}
		}.setPath(PATH_COMMENTS),
			ID_MANTIS_PART_STEPSTOREPRODUCE);
		
		return descriptors;
	}
	

    private Set<TaskEditorPartDescriptor> replaceDescriptionPart(Set<TaskEditorPartDescriptor> descriptors) {
	    
	    String toInsertAfter = removePartDescriptor(descriptors, ID_PART_DESCRIPTION);
        
        return insertPart(descriptors, new TaskEditorPartDescriptor(ID_PART_DESCRIPTION) {
            @Override
            public AbstractTaskEditorPart createPart() {
                
                return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.DESCRIPTION.toString(), MantisAttributeMapper.Attribute.DESCRIPTION.getKey(), true);
            }
        }.setPath(PATH_COMMENTS), toInsertAfter);
    }
    
    private Set<TaskEditorPartDescriptor> replaceNewCommentPart(Set<TaskEditorPartDescriptor> descriptors) {

        String toInsertAfter = removePartDescriptor(descriptors, ID_PART_NEW_COMMENT);
        
        // do not add a part if none matching by id was found
        if ( toInsertAfter == null )
            return descriptors;
        
        return insertPart(descriptors, new TaskEditorPartDescriptor(ID_PART_NEW_COMMENT) {
            @Override
            public AbstractTaskEditorPart createPart() {
                
                return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.NEW_COMMENT.toString(), MantisAttributeMapper.Attribute.NEW_COMMENT.getKey(), true);
            }
        }.setPath(PATH_COMMENTS), toInsertAfter);
    }

    /**
     * Removes a part descriptor by id
     * 
     * @param descriptors the descriptors to operate on
     * @param descriptorId the id of the descriptor to remove
     * @return the part descriptor id before the removed id, or null if the id was not found 
     */
    private String removePartDescriptor(Set<TaskEditorPartDescriptor> descriptors, String descriptorId) {

        String toInsertAfter = null;
        
        for (Iterator<TaskEditorPartDescriptor> it = descriptors.iterator(); it.hasNext();) {
            TaskEditorPartDescriptor taskEditorPartDescriptor = it.next();
            if (taskEditorPartDescriptor.getId().equals(descriptorId)) {
                it.remove();
                return toInsertAfter;
            } else {
                toInsertAfter = taskEditorPartDescriptor.getId();
            }
        }
        
        return null;
    }

    @Override
	protected AttributeEditorFactory createAttributeEditorFactory() {

	    final boolean useRichTextEditor = MantisRepositoryConfiguration.isUseRichTextEditor(getModel().getTaskRepository());
	    
	    return new HtmlAttributeEditorFactory(getModel(), getTaskRepository(), getEditorSite(), useRichTextEditor);
	}
    
	protected Set<TaskEditorPartDescriptor> insertPart(Set<TaskEditorPartDescriptor> originalDescriptors, TaskEditorPartDescriptor newDescriptor, String insertAfterId ) {
		
	    Set<TaskEditorPartDescriptor> newDescriptors = new LinkedHashSet<TaskEditorPartDescriptor>();
	    
	    boolean added = false;
	    
		for (TaskEditorPartDescriptor taskEditorPartDescriptor : originalDescriptors) {
			newDescriptors.add(taskEditorPartDescriptor);
			if (taskEditorPartDescriptor.getId().equals(insertAfterId)) {
				newDescriptors.add(newDescriptor);
				added = true;
			}
		}
		
		if ( !added )
		    throw new IllegalArgumentException("Did not find a part with id " + insertAfterId + " to insert the newDescriptor after");
		
		return newDescriptors;
	}

    @Override
    public void appendTextToNewComment(String text) {

        final boolean useRichTextEditor = MantisRepositoryConfiguration.isUseRichTextEditor(getModel().getTaskRepository());
        
        if ( !useRichTextEditor ) {
            super.appendTextToNewComment(text);
            return;
        }
        
        AbstractTaskEditorPart newCommentPart = getPart(ID_PART_NEW_COMMENT);
        if ( ! ( newCommentPart instanceof HtmlTextTaskEditorPart ))
            return;
        
        if ( text == null )
            text = "";
        
        HtmlTextTaskEditorPart editorPart = (HtmlTextTaskEditorPart) newCommentPart;
        editorPart.appendRawText(HtmlFormatter.convertToDisplayHtml(text));
    }
}
