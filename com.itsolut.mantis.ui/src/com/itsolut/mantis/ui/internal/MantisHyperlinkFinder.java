/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.TaskHyperlink;

import com.google.inject.Inject;
import com.itsolut.mantis.core.MantisCommentMapper;


/**
 * @author Robert Munteanu
 *
 */
public class MantisHyperlinkFinder {
    
    private static final Pattern HYPERLINK_PATTERN = Pattern.compile("(bug|issue|task) #?(\\d+)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern COMMENT_PATTERN = Pattern.compile("~(\\d+)", Pattern.CASE_INSENSITIVE);

    private MantisCommentMapper commentMapper;
    

    @Inject
    public MantisHyperlinkFinder(MantisCommentMapper commentMapper) {
        
        this.commentMapper = commentMapper;
    }
    
    public IHyperlink[] findHyperlinks(TaskRepository repository, ITask task, String text, int lineOffset, int regionOffset) {

        
        Matcher matcher = HYPERLINK_PATTERN.matcher(text);

        List<IHyperlink> links = null;

        while (matcher.find()) {
            if (!isInRegion(lineOffset, matcher))
                continue;

            if (links == null)
                links = new ArrayList<IHyperlink>();

            String id = matcher.group(2);

            links.add(new TaskHyperlink(determineRegion(regionOffset, matcher), repository, id));
        }
        
        if ( task != null ) {
        
            matcher = COMMENT_PATTERN.matcher(text);
            
            while ( matcher.find() ) {
    
                if (!isInRegion(lineOffset, matcher))
                    continue;
    
                if (links == null)
                    links = new ArrayList<IHyperlink>();
    
                String commentId = matcher.group(1);
                
                Integer commentNumber = commentMapper.getCommentNumber(Integer.parseInt(commentId));
                
                if ( commentNumber == null )
                    continue;
                
                TaskHyperlink link = new TaskHyperlink(determineRegion(regionOffset, matcher), repository, task.getTaskId());
                link.setSelection(TaskAttribute.PREFIX_COMMENT + commentNumber);
                
                links.add(link);
            }
        }

        return links == null ? null : links.toArray(new IHyperlink[links.size()]);
    }
    
    private boolean isInRegion(int lineOffset, Matcher m) {
        
        if ( lineOffset == -1 )
            return true;

        return (lineOffset >= m.start() && lineOffset <= m.end());
    }

    private IRegion determineRegion(int regionOffset, Matcher m) {

        return new Region(regionOffset + m.start(), m.end() - m.start());
    }

}
