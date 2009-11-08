
package com.itsolut.mantis.ui.wizard;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.itsolut.mantis.core.model.MantisProject;

/**
 * @author Robert Munteanu
 *
 */
class MantisProjectITreeContentProvider implements ITreeContentProvider {
    public Object[] getChildren(Object parentElement) {

        if (parentElement instanceof MantisProject[]) {
            return (MantisProject[]) parentElement;
        }
        return null;
    }

    public Object getParent(Object element) {

        return null;
    }

    public boolean hasChildren(Object element) {

        return false;
    }

    public Object[] getElements(Object inputElement) {

        return getChildren(inputElement);
    }

    public void dispose() {

    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

    }
}