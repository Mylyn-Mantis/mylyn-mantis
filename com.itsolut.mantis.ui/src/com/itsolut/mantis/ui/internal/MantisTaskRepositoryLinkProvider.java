package com.itsolut.mantis.ui.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.mylyn.tasks.core.IRepositoryManager;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractTaskRepositoryLinkProvider;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;

public class MantisTaskRepositoryLinkProvider extends
		AbstractTaskRepositoryLinkProvider {

	private static final String PROPERTY_PREFIX = "project.repository";

	private static final String PROJECT_REPOSITORY_KIND = PROPERTY_PREFIX
			+ ".kind";

	private static final String PROJECT_REPOSITORY_URL = PROPERTY_PREFIX
			+ ".url";

	@Override
	public TaskRepository getTaskRepository(IResource resource,
			IRepositoryManager repositoryManager) {
		// TODO Auto-generated method stub
		IProject project = resource.getProject();
		
		if (project == null || !project.isAccessible())
			return null;

		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope
				.getNode(TasksUiPlugin.ID_PLUGIN);
		if (projectNode == null)
			return null;

		String kind = projectNode.get(PROJECT_REPOSITORY_KIND, "");
		String urlString = projectNode.get(PROJECT_REPOSITORY_URL, "");

		TaskRepository repository = repositoryManager.getRepository(kind,
				urlString);

		return repository;
	}
	

}
