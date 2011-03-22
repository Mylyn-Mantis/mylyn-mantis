package com.itsolut.mantis.ui.editor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.htmltext.HtmlComposer;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

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
		
		if ( useRichTextEditor )
		    descriptors = replaceDescriptionPart(descriptors);
		
		descriptors = insertPart(descriptors,
				new TaskEditorPartDescriptor(ID_MANTIS_PART_STEPSTOREPRODUCE) {
			@Override
			public AbstractTaskEditorPart createPart() {
                if ( useRichTextEditor )
                    return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE.toString(), MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE.getKey());

                return new MantisStepsToReproducePart(false);

			}
		}.setPath(PATH_COMMENTS),
		     ID_PART_DESCRIPTION);
		
		descriptors = insertPart(descriptors,
				new TaskEditorPartDescriptor(ID_MANTIS_PART_ADDITIONALINFO) {
			@Override
			public AbstractTaskEditorPart createPart() {
			    
			    if ( useRichTextEditor )
			        return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.ADDITIONAL_INFO.toString(), MantisAttributeMapper.Attribute.ADDITIONAL_INFO.getKey());

			    return new MantisAdditionalInformationPart(false);
			    
			}
		}.setPath(PATH_COMMENTS),
			ID_MANTIS_PART_STEPSTOREPRODUCE);
		
		return descriptors;
		
	}
	
	private Set<TaskEditorPartDescriptor> replaceDescriptionPart(Set<TaskEditorPartDescriptor> descriptors) {
	    
        for (Iterator<TaskEditorPartDescriptor> it = descriptors.iterator(); it.hasNext();) {
            TaskEditorPartDescriptor taskEditorPartDescriptor = it.next();
            if (taskEditorPartDescriptor.getId().equals(ID_PART_DESCRIPTION))
                it.remove();
        }
        
        return insertPart(descriptors, new TaskEditorPartDescriptor(ID_PART_DESCRIPTION) {
            @Override
            public AbstractTaskEditorPart createPart() {
                
                return new HtmlTextTaskEditorPart(MantisAttributeMapper.Attribute.DESCRIPTION.toString(), MantisAttributeMapper.Attribute.DESCRIPTION.getKey());
            }
        }.setPath(PATH_COMMENTS), ID_PART_ATTACHMENTS);
    }

    @Override
	protected AttributeEditorFactory createAttributeEditorFactory() {

	    final boolean useRichTextEditor = MantisRepositoryConfiguration.isUseRichTextEditor(getModel().getTaskRepository());
	    
	    return new AttributeEditorFactory(getModel(), getTaskRepository(), getEditorSite()) {
	        
	        @Override
	        public AbstractAttributeEditor createEditor(String type, final TaskAttribute taskAttribute) {

	            if ( useRichTextEditor && TaskAttribute.TYPE_LONG_RICH_TEXT.equals(type) && 
	                    !taskAttribute.getId().equals(MantisAttributeMapper.Attribute.NEW_COMMENT.getKey()) &&
	                    !taskAttribute.getId().equals(TaskAttribute.COMMENT_TEXT))
	                return new AbstractAttributeEditor(getModel(), taskAttribute) {
                        
                        private HtmlComposer composer;

                        @Override
                        public void createControl(Composite parent, FormToolkit toolkit) {
                            
                            composer = new HtmlComposer(parent, SWT.None);
                            composer.setHtml(getTaskAttribute().getValue());
                            GridDataFactory.fillDefaults().applyTo(composer.getBrowser());

                            composer.addModifyListener(new ModifyListener() {
                                
                                public void modifyText(ModifyEvent e) {

                                    String oldValue = getAttributeMapper().getValue(getTaskAttribute());
                                    
                                    String newValue = composer.getHtml();
                                    
                                    getAttributeMapper().setValue(getTaskAttribute(), newValue);

                                    boolean attributeChanged = oldValue != "" && !(newValue.equals(oldValue)); 
                                    
                                    MantisCorePlugin.debug(NLS.bind("Attribute {0} changed from {1} to {2}. Change detected : {3}.", new Object[] { getTaskAttribute().getId(), oldValue, newValue , attributeChanged}), new RuntimeException());
                                    
                                    
                                    // we have no way of knowing when the initial value is set
                                    // so we consider that 'empty' means that no value is set
                                    if ( attributeChanged  )
                                        attributeChanged();
                                }
                            });
                            
                            setControl(composer.getBrowser());
                        }
                    };
	            
	            return super.createEditor(type, taskAttribute);
	        }
	    };
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
