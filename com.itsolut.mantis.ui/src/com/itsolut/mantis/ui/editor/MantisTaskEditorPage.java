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
		
		logDescriptors(descriptors);
		
		return descriptors;
		
	}
	

    private Set<TaskEditorPartDescriptor> replaceDescriptionPart(Set<TaskEditorPartDescriptor> descriptors) {
	    
	    String toInsertAfter = null;
        
	    for (Iterator<TaskEditorPartDescriptor> it = descriptors.iterator(); it.hasNext();) {
            TaskEditorPartDescriptor taskEditorPartDescriptor = it.next();
            if (taskEditorPartDescriptor.getId().equals(ID_PART_DESCRIPTION)) {
                it.remove();
                break;
            } else {
                toInsertAfter = taskEditorPartDescriptor.getId();
            }
        }
        
        return insertPart(descriptors, new TaskEditorPartDescriptor(ID_PART_DESCRIPTION) {
            @Override
            public AbstractTaskEditorPart createPart() {
                
                return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.DESCRIPTION.toString(), MantisAttributeMapper.Attribute.DESCRIPTION.getKey(), true);
            }
        }.setPath(PATH_COMMENTS), toInsertAfter);
    }
    
    private Set<TaskEditorPartDescriptor> replaceNewCommentPart(Set<TaskEditorPartDescriptor> descriptors) {
        
        String toInsertAfter = null;
        
        for (Iterator<TaskEditorPartDescriptor> it = descriptors.iterator(); it.hasNext();) {
            TaskEditorPartDescriptor taskEditorPartDescriptor = it.next();
            if (taskEditorPartDescriptor.getId().equals(ID_PART_NEW_COMMENT)) {
                it.remove();
                break;
            } else {
                toInsertAfter = taskEditorPartDescriptor.getId();
            }
        }
        
        return insertPart(descriptors, new TaskEditorPartDescriptor(ID_PART_NEW_COMMENT) {
            @Override
            public AbstractTaskEditorPart createPart() {
                
                return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.NEW_COMMENT.toString(), MantisAttributeMapper.Attribute.NEW_COMMENT.getKey(), true);
            }
        }.setPath(PATH_COMMENTS), toInsertAfter);
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

    private void logDescriptors(Set<TaskEditorPartDescriptor> descriptors) {
        
        StringBuilder output = new StringBuilder();
        
        for ( TaskEditorPartDescriptor descriptor : descriptors )
            output.append(descriptor.getId()).append(" - ").append(descriptor.getPath()).append('\n');
        
        output.deleteCharAt(output.length() - 1);
        
        MantisCorePlugin.debug("Generated descriptor list : " + output, null);
        
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
        
        HtmlTextTaskEditorPart editorPart = (HtmlTextTaskEditorPart) newCommentPart;
        editorPart.appendRawText("<p>" + text + "</p>");
    }
}
