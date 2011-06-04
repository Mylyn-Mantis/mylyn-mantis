/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Robert Munteanu
 */
public abstract class HtmlFormatter {

	/**
	 * This function mimics the behaviour of MantisBT's <tt>string_nl2br</tt>
	 * method
	 * 
	 * <p>
	 * All newlines have a <tt>{@literal <br/>}</tt> tag appended, except for
	 * those contained in {@literal <pre></pre>} tags.
	 * </p>
	 * 
	 * @param input
	 * @return
	 */
	public static String convertToDisplayHtml(String input) {

		List<Range> ranges = new ArrayList<Range>();
		
		int pos = 0;
		while ( pos < input.length() ) {
		    
		    TagRange range = TagRange.find(input, pos);
		
			int startIndex = range != null ? range.getStart() : -1;
			int endIndex = range != null ? range.getEnd() : -1;
			boolean outsideTag = false;
			
			// the found tag range is after the current start
			// this denotes a text gap
			if ( startIndex > pos ) {
			    endIndex = startIndex + 1;
			    startIndex = pos;
			    outsideTag = true;
			}
			
			if ( pos == 0 && startIndex > 0 ) { // tag exists, but is not the first

				ranges.add(new Range(pos, startIndex - 1, true));
			} else if ( pos == 0 && startIndex == -1 ) { // first iteration, tag does not exist at all
				
				ranges.add(new Range(pos, input.length(), true));
				break;
			} else if ( startIndex == -1 || endIndex == -1) { // last iteration, tag is no longer found
				
				ranges.add(new Range(pos, input.length(), true));
				break;
			} 
			
			// tag found
			ranges.add(new Range(startIndex, endIndex - 1, outsideTag));
			
			pos = endIndex -1;
		}

		StringBuilder output = new StringBuilder(input.length());
		
		for ( Range range : ranges ) {
			
			String rangeString = input.substring(range.from, range.to);
			if ( range.applyNl2Br ) {

				boolean applyFinalBr = range.to != input.length();

				output.append(nl2br(rangeString, applyFinalBr));
			} else
				output.append(rangeString);
		}

		
		return output.toString();
	}
	
	public static String convertFromDisplayHtml(String input) {
		
		return input.replaceAll("<br\\s?/>\\n?", "\n");
	}

	private static String nl2br(String input, boolean applyFinalBr) {
		
		String[] lines = input.split("\n");

		StringBuilder output = new StringBuilder(input.length());
		for ( int i = 0; i < lines.length ; i++) {
			output.append(lines[i]);
			if ( i + 1 < lines.length || applyFinalBr )
				output.append("<br/>");
		}

		return output.toString();
	}

	private HtmlFormatter() {

	}
	
	private static class Range {
		
		public int from;
		public int to;
		public boolean applyNl2Br;
		
		public Range(int from, int to, boolean applyNl2Br) {
			
			this.from = from;
			this.to = to;
			this.applyNl2Br = applyNl2Br;
		}
		
	}

	private static enum Tag {
	    pre, ol, ul;
	}
	
	private static class TagRange implements Comparable<TagRange> {
	    
	    private final int start;
        private final int end;
	    
	    public TagRange(String input, Tag tag, int offset) {

            String openTag = "<" + tag.toString() + ">";
            String closeTag = "</" + tag.toString() + ">";
            
            start = input.indexOf(openTag, offset);
            int tagEnd = input.indexOf(closeTag, offset) + closeTag.length() + 1;
            int allLength = input.length();
            if ( tagEnd < allLength ) {
                char nextChar = input.charAt(tagEnd - 1);
                if ( nextChar == '\n')
                    tagEnd++;
            }
            
            end = tagEnd;
        }

        public static TagRange find(String input, int start ) {
	        
	        Set<TagRange> allRanges = new TreeSet<TagRange>();

	        for ( Tag tag : Tag.values()) {
	            TagRange tagRange = new TagRange(input, tag, start);

	            if ( tagRange.exists() )
	                allRanges.add(tagRange);
	        }
	        
	        if ( allRanges.isEmpty() )
	            return null;
	        
            return allRanges.iterator().next();
	    }
        
        public int getStart() {

            return start;
        }
        
        public int getEnd() {

            return end;
        }
        
        public boolean exists() {
            
            return start != -1;
        }

        public int compareTo(TagRange other) {

            return Integer.valueOf(start).compareTo(other.start);
        }
	}
}
