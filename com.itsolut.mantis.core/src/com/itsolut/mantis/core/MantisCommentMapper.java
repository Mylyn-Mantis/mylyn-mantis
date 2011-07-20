/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;


/**
 * @author Robert Munteanu
 */
@Singleton
public class MantisCommentMapper {

    private final Map<Integer, Integer> idToNumber = Maps.newHashMap();
    
    public void registerCommentNumber(int commentId, int commentNumber) {
        
        synchronized(this) {
            idToNumber.put(commentId, commentNumber);
        }
    }
    
    public Integer getCommentNumber(int commentId) {
        
        synchronized (this) {
            return idToNumber.get(commentId);
        }
    }
}
