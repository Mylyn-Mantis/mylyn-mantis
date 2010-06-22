package com.itsolut.mantis.core;

import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.tasks.core.TaskRepository;

@SuppressWarnings("restriction")
public class MantisRepositoryConfiguration {

    private static String DOWNLOAD_SUBTASKS_PROPERTY = "mantis.download.subtasks";

    public static boolean isDownloadSubTasks(TaskRepository repository) {

        String property = repository.getProperty(DOWNLOAD_SUBTASKS_PROPERTY);

        if (property == null) // default value
            return false;

        return Boolean.parseBoolean(property);
    }

    public static void setDownloadSubTasks(TaskRepository repository, boolean downloadSubTasks) {

        repository.setProperty(DOWNLOAD_SUBTASKS_PROPERTY, String.valueOf(downloadSubTasks));
    }
    
	/**
	 * Sets the category property on the repository if not already set
	 * 
	 * @param repository the task repository
	 * @return true if the property was set, false otherwise
	 */
	public static boolean setCategoryIfNotSet(TaskRepository repository) {
    	
    	String category = repository.getProperty(IRepositoryConstants.PROPERTY_CATEGORY);
    	if ( category != null)
    		return false;
    	
    	repository.setProperty(IRepositoryConstants.PROPERTY_CATEGORY, IRepositoryConstants.CATEGORY_BUGS);
    	
    	return true;
    }

}
