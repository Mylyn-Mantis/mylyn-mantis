package com.itsolut.mantis.core;

import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.tasks.core.TaskRepository;

@SuppressWarnings("restriction")
public class MantisRepositoryConfiguration {

    private static final String SUPPORTS_SUBTASKS = "supports_subtasks";
    private static final String USE_RICH_TEXT_EDITOR = "use_rich_text_editor";
    
    public static boolean isSupportsSubTasks(TaskRepository repository) {
        
        return getBooleanProperty(repository, SUPPORTS_SUBTASKS);
    }

    private static boolean getBooleanProperty(TaskRepository repository, String propertyName) {

        String property = repository.getProperty(propertyName);

        if (property == null) // default value
            return false;

        return Boolean.parseBoolean(property);
    }

    public static void setSupportsSubTasks(TaskRepository repository, boolean downloadSubTasks) {

        repository.setProperty(SUPPORTS_SUBTASKS, String.valueOf(downloadSubTasks));
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


	public static void setUseRichTextEditor(TaskRepository repository, boolean useRichTextEditor) {

	    repository.setProperty(USE_RICH_TEXT_EDITOR, String.valueOf(useRichTextEditor));
        
    }
    public static boolean isUseRichTextEditor(TaskRepository repository) {

        return getBooleanProperty(repository, USE_RICH_TEXT_EDITOR);
    }

}
