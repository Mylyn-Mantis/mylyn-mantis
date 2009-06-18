package com.itsolut.mantis.ui.editor;

import com.itsolut.mantis.core.MantisAttributeMapper;

public class MantisStepsToReproducePart extends AbstractRichTextPart {
    protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
    
    public MantisStepsToReproducePart(boolean expandedByDefault) {

        super(LABEL_SECTION_STEPS, MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE, expandedByDefault);
    }
    
}
