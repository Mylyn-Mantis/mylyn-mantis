/*******************************************************************************
 * Copyright (c) 2004, 2010 Robert Munteanu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Robert Munteanu - initial API and implementation
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Robert Munteanu
 *
 */
enum MantisOperation {
    
    
    LEAVE("Leave as ", null, null),

    RESOLVE_AS("Resolve as ", TaskAttribute.TYPE_SINGLE_SELECT, null) {
    	
    	@Override
		public void preOperation(TaskData taskData, TaskAttribute attribute, IMantisClient client, IProgressMonitor monitor) {
		
    		try {
    			
				TaskAttribute resolution = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.RESOLUTION.getKey());
				if ( !MantisUtils.hasValue(resolution) )
					return;
				
    			TaskAttribute resolveAs = createTaskAttributeFrom(taskData, resolution, getAttributeId(this));
    			
    			attribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, resolveAs.getId());

				for ( Map.Entry<String, String> option : resolution.getOptions().entrySet() )
					resolveAs.putOption(option.getKey(), option.getValue());
				
				resolveAs.setValue(String.valueOf(client.getCache(monitor).getBugResolutionFixedThreshold().getValue()));
			} catch (MantisException e) {
				MantisCorePlugin.warn("Unable to preselect bug fixed threshold.", e);
			}
		}
    	
        @Override
        public void performPostOperation(TaskData taskData, TaskAttribute attribute, IMantisClient client, IProgressMonitor monitor) {
       
            int resolvedStatus = DefaultConstantValues.Status.RESOLVED.getValue();
            try {
                resolvedStatus = client.getCache(monitor).getResolvedStatus();
            } catch (MantisException e) {
                MantisCorePlugin.warn("Failed retrieving customised resolved bug status. Using default.", e);
            }
            
            taskData.getRoot().getAttribute(Attribute.RESOLUTION.getKey()).setValue(taskData.getRoot().getAttribute(getAttributeId(this)).getValue());
            taskData.getRoot().getAttribute(Attribute.STATUS.getKey()).setValue(String.valueOf(resolvedStatus));
       }
    },
    
    TRACK_TIME("Track time ", TaskAttribute.TYPE_SHORT_TEXT, MantisAttributeMapper.Attribute.TIME_SPENT_NEW),

    ASSIGN_TO("Assign to ", TaskAttribute.TYPE_PERSON, MantisAttributeMapper.Attribute.ASSIGNED_TO) {

        @Override
        public void performPostOperation(TaskData taskData, TaskAttribute attribute, IMantisClient client, IProgressMonitor monitor) {

            int assignedStatus = DefaultConstantValues.Status.ASSIGNED.getValue();
            try {
                assignedStatus = client.getCache(monitor).getAssignedStatus().getValue();
            } catch (MantisException e) {
                MantisCorePlugin.warn("Failed retrieving customised assigned bug status. Using default.", e);
            }
            
            taskData.getRoot().getAttribute(Attribute.STATUS.getKey()).setValue(String.valueOf(assignedStatus));
        }
    };
    
    private static final String PREFIX_VIRTUAL_ATTRIBUTE = TaskAttribute.PREFIX_OPERATION + "virtual-";
    
    private static String getAttributeId(MantisOperation mantisOperation) {
		
		return PREFIX_VIRTUAL_ATTRIBUTE + mantisOperation.toString();
	}
    
    private static TaskAttribute createTaskAttributeFrom(TaskData taskData, TaskAttribute from, String attributeId) {
    	
		TaskAttribute attribute = taskData.getRoot().createAttribute(attributeId);
		attribute.getMetaData().setType(from.getMetaData().getType());
		
		return attribute;
    }
    
    public static boolean isOperationRelated(TaskAttribute attribute) {
        
        return attribute.getId().startsWith(TaskAttribute.PREFIX_OPERATION);
    }

	private final String label;

	private final String operationType;

    private final MantisAttributeMapper.Attribute attribute;

    private MantisOperation(String label, String operationType, MantisAttributeMapper.Attribute attribute) {

        this.label = label;
        this.operationType = operationType;
        this.attribute = attribute;
    }

    public String getLabel() {

        return label;
    }

    public String getOperationType() {

        return operationType;
    }

    public MantisAttributeMapper.Attribute getAttribute() {

        return attribute;
    }

    public void preOperation(TaskData taskData, TaskAttribute attribute, IMantisClient client, IProgressMonitor monitor) {
    	
    	if ( getOperationType() != null )
    		attribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, getAttribute().getKey());
    }
    
    public void performPostOperation(TaskData taskData,TaskAttribute attribute, IMantisClient client, IProgressMonitor monitor)  {
    	
    }
}