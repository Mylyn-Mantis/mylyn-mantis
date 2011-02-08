/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import com.itsolut.mantis.core.model.MantisRelationship;

/**
 * @author Robert Munteanu
 */
public class TaskRelationshipChange {
    
    public enum Direction {
        Added, Removed;
    }
    
    private final Direction _direction;
    private final MantisRelationship _relationship;
    
    public TaskRelationshipChange(Direction direction, MantisRelationship relationship) {

        _direction = direction;
        _relationship = relationship;
    }
    
    public Direction getDirection() {

        return _direction;
    }
    
    public MantisRelationship getRelationship() {

        return _relationship;
    }

}
