package com.itsolut.mantis.ui.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.itsolut.mantis.core.model.MantisProject;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisProjectITreeContentProvider implements ITreeContentProvider {

    private MantisProject[] projects;

    public Object[] getChildren(Object parentElement) {

        if (parentElement instanceof MantisProject[])
            return topLevelOnly((MantisProject[]) parentElement);

        if (parentElement instanceof MantisProject) {
            Integer parentProjectId = ((MantisProject) parentElement).getValue();
            List<MantisProject> childProjects = getChildProjects(parentProjectId);
            return childProjects.toArray(new MantisProject[childProjects.size()]);
        }

        return null;
    }

    private MantisProject[] topLevelOnly(MantisProject[] parentElement) {
        
        List<MantisProject> topLevel = new ArrayList<MantisProject>();
        
        for ( MantisProject project : parentElement )
            if ( project.getParentProjectId() == null )
                topLevel.add(project);

        return topLevel.toArray(new MantisProject[topLevel.size()]);
    }

    private List<MantisProject> getChildProjects(Integer parentProjectId) {

        if ( parentProjectId == null )
            return Collections.emptyList();
        
        List<MantisProject> childProjects = new ArrayList<MantisProject>();
        for (MantisProject childCandidate : projects)
            if (parentProjectId.equals(childCandidate.getParentProjectId()))
                childProjects.add(childCandidate);

        return childProjects;
    }

    public Object getParent(Object element) {

        if (element instanceof MantisProject) {

            Integer parentId = ((MantisProject) element).getParentProjectId();
            if (parentId == null)
                return null;

            for (MantisProject project : projects)
                if (parentId.equals(project.getValue()))
                    return project;
        }
        
        return null;
    }

    public boolean hasChildren(Object element) {

        if (element instanceof MantisProject)
            return getChildProjects(((MantisProject) element).getValue()).size() > 0;

        return false;
    }

    public Object[] getElements(Object inputElement) {

        return getChildren(inputElement);
    }

    public void dispose() {

    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        setInput(newInput);
    }

    public void setInput(Object newInput) {

        if (newInput instanceof MantisProject[])
            projects = (MantisProject[]) newInput;
        else
            projects = new MantisProject[0];
    }
}