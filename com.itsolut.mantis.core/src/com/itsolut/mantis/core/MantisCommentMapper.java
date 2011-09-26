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

    private final Map<Integer, CommentMapping> idToNumber = Maps.newHashMap();
    
    public void registerCommentNumber(int commentId, CommentMapping commentMapping) {
        
        synchronized(this) {
            idToNumber.put(commentId, commentMapping);
        }
    }
    
    public CommentMapping getCommentMapping(int commentId) {
        
        synchronized (this) {
            return idToNumber.get(commentId);
        }
    }
    
    public static class CommentMapping {
        
        private final int taskId;
        private final int commentNumber;
        
        public CommentMapping(int taskid, int commentNumber) {

            this.taskId = taskid;
            this.commentNumber = commentNumber;
        }
        
        public int getTaskId() {

            return taskId;
        }
        
        public int getCommentNumber() {

            return commentNumber;
        }
        
    }
}
