/*******************************************************************************
 * Copyright (C) 2012 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.google.common.base.Joiner;
import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisUser;
import com.itsolut.mantis.core.util.MantisUtils;

public class MantisTaskDataMigrator {
	
	public enum TaskDataVersion {
		
		Version_Zero(0) {
			@Override
			public void migrateTaskData(TaskRepository repository, TaskData taskData, IMantisClient mantisClient) {
				
			}
		},
		
		/**
		 * Person attributes move from userId key to userName key -> displayName value
		 */
		Version_3_9(3.9) {
			@Override
			public void migrateTaskData(TaskRepository repository, TaskData taskData, IMantisClient mantisClient) {
				
				try {
					migratePersonAttribute(taskData, MantisAttributeMapper.Attribute.REPORTER, mantisClient);
					migratePersonAttribute(taskData, MantisAttributeMapper.Attribute.ASSIGNED_TO, mantisClient);
					migratePersonAttribute(taskData, MantisAttributeMapper.Attribute.MONITORS, mantisClient);
					refreshOldMonitors(taskData, mantisClient);
					
				} catch (MantisException e) {
					MantisCorePlugin.warn("Failed migrating TaskData " + taskData.getTaskId() + " for repository " + taskData.getRepositoryUrl(), e);
				}
			}

			private void refreshOldMonitors(TaskData taskData, IMantisClient mantisClient) throws MantisException {
				
				TaskAttribute attribute = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.MONITORS.getKey());
				if ( attribute == null )
					return;
				
                if (attribute.getValue().length() == 0)
					return;
				
				String oldValue = attribute.getMetaData().getValue(MantisAttributeMapper.TASK_ATTRIBUTE_ORIGINAL_MONITORS);
				
				List<String> monitorNames = userIdsToUserNames(mantisClient, new NullProgressMonitor(), MantisUtils.fromCsvString(oldValue));
				
				String originalValues = Joiner.on(',').join(monitorNames);
				
				attribute.getMetaData().putValue(MantisAttributeMapper.TASK_ATTRIBUTE_ORIGINAL_MONITORS, originalValues);
			}

			private void migratePersonAttribute(TaskData taskData, Attribute attributeKey, IMantisClient mantisClient) throws MantisException {
				
				TaskAttribute attribute = taskData.getRoot().getAttribute(attributeKey.getKey());
				if ( attribute == null )
					return;
				
				// recreate metadata
				attribute.getMetaData()
					.setKind(attributeKey.getKind()).setLabel(attributeKey.toString()).setType(attributeKey.getType());

				// at this point the configuration should not be stale so no I/O should be performed
				NullProgressMonitor progressMonitor = new NullProgressMonitor();

				List<String> newValues = getNewValues(mantisClient, attribute, progressMonitor);
				Map<String, String> newOptions = getNewOptions(mantisClient, attribute, progressMonitor);
				
				if ( newValues.size() > 0 )
					attribute.setValues(newValues);
				if ( newOptions.size() > 0 ) {
					attribute.clearOptions();
					for ( Map.Entry<String, String> optionEntry : newOptions.entrySet() )
						attribute.putOption(optionEntry.getKey(), optionEntry.getValue());
				}
			}

			private Map<String, String> getNewOptions( IMantisClient mantisClient, TaskAttribute attribute,
					NullProgressMonitor progressMonitor) throws MantisException {
				// migrate options
				Map<String, String> newOptions = new LinkedHashMap<String, String>(attribute.getOptions().size());
				
				for ( Map.Entry<String,String> optionEntry : attribute.getOptions().entrySet()) {

					// do not convert the empty option
					if ( optionEntry.getKey().length() == 0 )
						continue;

					Integer userId;
					try {
						userId = Integer.valueOf(optionEntry.getKey());
					} catch (NumberFormatException e) {
						// invalid format or already converted
						return Collections.emptyMap();
					}
					String userName = mantisClient.getCache(progressMonitor).getUserNameById(userId);
					if ( userName == null )
						continue;
					
					MantisUser user = mantisClient.getCache(progressMonitor).getUserByUsername(userName);
					
					newOptions.put(user.getKey(), user.getName());
				}
				
				return newOptions;
			}

			private List<String> getNewValues(IMantisClient mantisClient, TaskAttribute attribute, NullProgressMonitor progressMonitor) throws MantisException {
				
				return userIdsToUserNames(mantisClient, progressMonitor, attribute.getValues());
			}

			private List<String> userIdsToUserNames(IMantisClient mantisClient,
					NullProgressMonitor progressMonitor, List<String> userIds)
					throws MantisException {
				// no values
				if ( userIds == null || userIds.isEmpty() )
					return Collections.emptyList();
				
				if ( userIds.size() == 1 && userIds.get(0).length() == 0 )
					return Collections.emptyList();
				
				// migrate value
				List<String> newValues = new ArrayList<String>(userIds.size());
				for ( String oldValue : userIds ) {
					Integer userId;
					try {
						userId = Integer.valueOf(oldValue);
					} catch (NumberFormatException e) {
						// invalid format or already converted
						return Collections.emptyList();
					}
					String userName = mantisClient.getCache(progressMonitor).getUserNameById(userId);
					if ( userName == null )
						continue;
					newValues.add(userName);
					
				}
				return newValues;
			}
		};
		
		private final double value;

		private TaskDataVersion(double value) {
			
			this.value = value;
		}
		
		public double getValue() {
		
			return value;
		}
		
		public abstract void migrateTaskData(TaskRepository repository, TaskData taskData, IMantisClient mantisClient);
	}

	public TaskDataVersion getCurrent() {
		
		return TaskDataVersion.Version_3_9;
	}
	
	public void migrateTaskData(TaskRepository repository, TaskData taskData, IMantisClient mantisClient) {
		
		double taskDataVersion = 0;
		String taskDataVersionValue = taskData.getVersion();
		if (taskDataVersionValue != null) {
			try {
				taskDataVersion = Double.parseDouble(taskDataVersionValue);
			} catch (NumberFormatException e) {
				taskDataVersion = 0;
			}
		}

		for (TaskDataVersion version : TaskDataVersion.values()) 
			if (version.getValue() > taskDataVersion)
				version.migrateTaskData(repository, taskData, mantisClient);
		
		taskData.setVersion(String.valueOf(getCurrent().getValue()));
	}

}
