/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.itsolut.mantis.core.util.HtmlFormatter;

/**
 * @author Robert Munteanu
 */
public class HtmlFormatterTest {

	private static final String OUTPUT_BR = "first<br/>second<br/>third";

	private static final String INPUT_BR = "first\nsecond\nthird";

	private static final String INPUT_PRE = "<pre>first\nsecond\nthird</pre>";

	private static final String OUTPUT_PRE = INPUT_PRE;

	private static final String INPUT_MIXED = "first\nsecond\n<pre>pre\nformatted</pre>third\nfourth";

	private static final String OUTPUT_MIXED = "first<br/>second<br/><pre>pre\nformatted</pre>third<br/>fourth";
	
	private static final String OUTPUT_MIXED_SPACES = "first<br/>second<br/><pre>pre\nformatted</pre>third<br />fourth";

	private static final String INPUT_UL_AND_PRE = "<pre>formatted\n</pre>third\n<ol>\n<li>First thing</li><li>Second thing</li></ol>";
	
	private static final String OUTPUT_UL_AND_PRE = "<pre>formatted\n</pre>third<br/><ol>\n<li>First thing</li><li>Second thing</li></ol>";

	@Test
	public void linesHaveBrAppended() {

		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_BR), is(OUTPUT_BR));
	}

	@Test
	public void preBlockDoesNotHaveBrAppended() {

		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_PRE), is(OUTPUT_PRE));
	}

	@Test
	public void linesBeforeAndAfterPreBlockHaveBrAppended() {

		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_MIXED), is(OUTPUT_MIXED));
	}
	
	@Test
	public void linesWithBrAreConvertedToNewlines() {
		
		assertThat(HtmlFormatter.convertFromDisplayHtml(OUTPUT_BR), is(INPUT_BR));
	}
	
	@Test
	public void linesWithPreAreNotConvertedToNewlines() {

		assertThat(HtmlFormatter.convertFromDisplayHtml(OUTPUT_PRE), is(INPUT_PRE));
	}
	
	@Test
	public void linesWithBrBeforeAndAfterPreAreConvertedToNewlines() {

		assertThat(HtmlFormatter.convertFromDisplayHtml(OUTPUT_MIXED_SPACES), is(INPUT_MIXED));
	}
	
	@Test
	public void emptyTextIsConvertedToEmptyHtml() {
		
		assertThat(HtmlFormatter.convertToDisplayHtml(""), is(""));
	}
	
	@Test
	public void emptyHtmlIsConvertedToEmptyText() {
		
		assertThat(HtmlFormatter.convertFromDisplayHtml(""), is(""));
	}
	
	@Test
	public void brWithNewLineIsConvertedToPlainNewline() {
		
		assertThat(HtmlFormatter.convertFromDisplayHtml("<br/>\n"), is("\n"));
	}
	
	@Test
	public void inputWithMultipleTagsPreservesFormatting() {
		
		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_UL_AND_PRE), is(OUTPUT_UL_AND_PRE));
	}
}
