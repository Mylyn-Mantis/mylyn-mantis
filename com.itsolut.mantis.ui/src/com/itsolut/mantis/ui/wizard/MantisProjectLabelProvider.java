package com.itsolut.mantis.ui.wizard;

import org.eclipse.jface.viewers.LabelProvider;

import com.itsolut.mantis.core.model.MantisProject;

/**
 * @author Robert Munteanu
 *
 */
class MantisProjectLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
    	if (element instanceof MantisProject) {
    		MantisProject project = (MantisProject) element;
    		return project.getName() ; 
    	}
    	return "";
    }
}