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
		
			int startIndex = input.indexOf("<pre>", pos);
			int endIndex = input.indexOf("</pre>", pos) + "</pre>".length() + 1;
			
			
			if ( pos == 0 && startIndex > 0 ) { // pre exists, but is not the first

				ranges.add(new Range(pos, startIndex - 1, true));
			} else if ( pos == 0 && startIndex == -1 ) { // first iteration, pre does not exist at all
				
				ranges.add(new Range(pos, input.length(), true));
				break;
			} else if ( startIndex == -1 || endIndex == -1) { // last iteration, pre is no longer found
				
				ranges.add(new Range(pos, input.length(), true));
				break;
			} 
			
			// pre found
			ranges.add(new Range(startIndex, endIndex - 1, false));
			
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
}
