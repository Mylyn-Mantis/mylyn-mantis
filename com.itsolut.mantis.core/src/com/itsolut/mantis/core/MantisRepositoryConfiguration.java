package com.itsolut.mantis.core;

import org.eclipse.mylyn.tasks.core.TaskRepository;

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

}
