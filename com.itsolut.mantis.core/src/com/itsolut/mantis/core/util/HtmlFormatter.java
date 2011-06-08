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

    private static final String HTML_PRE ="" +
    "<html>" +
    "  <head><style type='text/css'>" +
    "    body { " +
    "       margin: 0; padding: 0;" +
    "       font-size:   %spt;" +
    "       font-family: %s;" +
    "       font-weight: %s;" +
    "       font-style:  %s;"+
    "    } " +
    "  </style></head>" +
    "  <body>" +
    "";

    private static final String HTML_POST = "" +
    "  </body>" +
    "</html>";

    
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

		List<Range> ranges = parseIntoRanges(input);

		StringBuilder output = new StringBuilder(input.length());
		
		for ( Range range : ranges ) {
			
			String rangeString = input.substring(range.from, range.to);
			if ( range.rangeKind == RangeKind.TEXT ) {

				boolean applyFinalBr = range.to != input.length();

				output.append(nl2br(rangeString, applyFinalBr));
			} else
				output.append(rangeString);
		}

		
		return output.toString();
	}

    private static List<Range> parseIntoRanges(String input) {

        List<Range> ranges = new ArrayList<Range>();
		
		int pos = 0;
		while ( pos < input.length() ) {
		    
		    TagRange range = TagRange.find(input, pos);
		
			int startIndex = range != null ? range.getStart() : -1;
			int endIndex = range != null ? range.getEnd() : -1;
			RangeKind rangeKind;
			if ( range == null )
			    rangeKind = RangeKind.TEXT;
			else if ( range.getTag() == Tag.pre )
			    rangeKind = RangeKind.TAG_PRESERVE_NL;
			else 
			    rangeKind = RangeKind.TAG_CLEAN_NL;
			
			// the found tag range is after the current start
			// this denotes a text gap
			if ( startIndex > pos ) {
			    endIndex = startIndex + 1;
			    startIndex = pos;
			    rangeKind = RangeKind.TEXT;
			}
			
			if ( pos == 0 && startIndex > 0 ) { // tag exists, but is not the first

				ranges.add(new Range(pos, startIndex - 1, rangeKind));
			} else if ( pos == 0 && startIndex == -1 ) { // first iteration, tag does not exist at all
				
				ranges.add(new Range(pos, input.length(), RangeKind.TEXT));
				break;
			} else if ( startIndex == -1 || endIndex == -1) { // last iteration, tag is no longer found
				
				ranges.add(new Range(pos, input.length(), RangeKind.TEXT));
				break;
			} 
			
			// tag found
			ranges.add(new Range(startIndex, endIndex - 1, rangeKind));
			
			pos = endIndex -1;
		}
        return ranges;
    }
	
	public static String convertFromDisplayHtml(String input) {
		
        List<Range> ranges = parseIntoRanges(input);

        StringBuilder output = new StringBuilder(input.length());

        for (Range range : ranges) {

            String rangeString = input.substring(range.from, range.to);
            if (range.rangeKind == RangeKind.TEXT)
                output.append(rangeString.replaceAll("<br\\s?/>\\n?", "\n"));
            else if ( range.rangeKind == RangeKind.TAG_CLEAN_NL )
                output.append(rangeString.replaceAll("\n", ""));
            else
                output.append(rangeString);
        }

        return output.toString();
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

    /**
     * Wraps the specified <tt>value</tt> in a complete HTML declaration, ensuring that is optimised for display
     * 
     * @param htmlSnippet the value to wrap
     * @param fontFamily the font-family of the font
     * @param fontSizePt the size of the font in points
     * @param isBold true if the text should be bold 
     * @param isItalic true if the text should be italic
     * @return the wrapped value
     */
    public static String wrapForBrowserDisplay(String htmlSnippet, String fontFamily, int fontSizePt, boolean isBold, boolean isItalic) {
        
        String boldStyle = isBold ? "bold" : "normal";
        String italicStyle = isItalic ? "italic" : "normal";

        return String.format(HTML_PRE, fontSizePt, fontFamily, boldStyle, italicStyle) + htmlSnippet + HTML_POST;
    }
	
	private HtmlFormatter() {

	}
	
	private enum RangeKind {
	    
	    TAG_PRESERVE_NL, TAG_CLEAN_NL, TEXT,
	}
	
	private static class Range {
		
		public int from;
		public int to;
		public RangeKind rangeKind;
		
		public Range(int from, int to, RangeKind rangeKind) {
			
			this.from = from;
			this.to = to;
            this.rangeKind = rangeKind;
		}
		
	}

	private static enum Tag {
	    pre, ol, ul;
	}
	
	private static class TagRange implements Comparable<TagRange> {
	    
	    private final int start;
        private final int end;
        private final Tag tag;
	    
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
            this.tag = tag;
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
        
        public Tag getTag() {

            return tag;
        }
        
        public boolean exists() {
            
            return start != -1;
        }

        public int compareTo(TagRange other) {

            return Integer.valueOf(start).compareTo(other.start);
        }
	}
}
