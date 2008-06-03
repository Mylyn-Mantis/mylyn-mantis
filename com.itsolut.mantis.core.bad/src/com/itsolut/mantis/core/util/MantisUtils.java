/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.core.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Provides static helper methods.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisUtils {

	public static Date parseDate(long seconds) {
		return new Date(seconds * 1000l);
	}

	public static long toMantisTime(Date date) {
		return date.getTime() / 1000l;
	}

	public static Date transform(Calendar cal) {
		return cal.getTime();
	}

	public static Calendar transform(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	public static boolean isEmpty(String value) {
		return (value==null || value.length()==0);
	}

//	public static String stripProject(String name) {
//		int index = name.indexOf(">");
//		return (index>=0) ? name.substring(index+2) : name;
//	}

}
