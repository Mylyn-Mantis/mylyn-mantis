package com.itsolut.mantis.ui.editor;

import com.itsolut.mantis.core.MantisAttributeMapper;

public class MantisAdditionalInformationPart extends AbstractRichTextPart {
    protected static final String LABEL_SECTION_STEPS = "Additional Information";
    
    public MantisAdditionalInformationPart(boolean expandedByDefault) {

        super(LABEL_SECTION_STEPS, MantisAttributeMapper.Attribute.ADDITIONAL_INFO, expandedByDefault);
    }
    
}
