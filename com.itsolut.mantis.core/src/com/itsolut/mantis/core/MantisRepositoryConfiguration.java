package com.itsolut.mantis.core;

import org.eclipse.mylyn.tasks.core.TaskRepository;

public class MantisRepositoryConfiguration {
    
    private static String DOWNLOAD_SUBTASKS_PROPERTY = "mantis.download.subtasks";
    
    public static boolean isDownloadSubTasks(TaskRepository repository) {

        return Boolean.parseBoolean(repository.getProperty(DOWNLOAD_SUBTASKS_PROPERTY));
    }
    
    public static void setDownloadSubTasks(TaskRepository repository, boolean downloadSubTasks) {

        repository.setProperty(DOWNLOAD_SUBTASKS_PROPERTY, String.valueOf(downloadSubTasks));
    }
    
}
