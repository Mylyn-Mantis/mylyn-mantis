/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;

public class MantisTicketTest extends TestCase {
	
	public void testTimeTrackingValueWithPadding() {
		
		MantisComment comment = new MantisComment();
		comment.setTimeTracking(120);
		
		MantisTicket ticket = new MantisTicket(-1);
		ticket.addComment(comment);
		
		assertEquals("2:00", ticket.getValue(Key.TIME_SPENT));		
	}

	public void testTimeTrackingValue() {
		
		MantisComment comment = new MantisComment();
		comment.setTimeTracking(12);
		
		MantisTicket ticket = new MantisTicket(-1);
		ticket.addComment(comment);
		
		assertEquals("0:12", ticket.getValue(Key.TIME_SPENT));		
	}
}
