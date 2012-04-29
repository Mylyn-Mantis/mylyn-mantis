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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

import com.google.common.base.Joiner;
import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.model.MantisSearch;

/**
 * Provides static helper methods.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisUtils {

    public static Date parseDate(long milliseconds) {

        return new Date(milliseconds);
    }

    public static long toMantisTime(Date date) {

        return date.getTime();
    }

    public static Date transform(Calendar cal) {
        
        return cal.getTime();
    }

    public static Calendar transform(Date date) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
    
    public static String convertFromCustomFieldDate(String customFieldValue) {
        
        if ( customFieldValue.length() == 0 )
            return customFieldValue;
        
        return customFieldValue + "000";
    }
    
    public static String convertToCustomFieldDate(String valueInMilliseconds) {
        
        if ( valueInMilliseconds.length() == 0 )
            return valueInMilliseconds;
        
        long dateValue = Long.valueOf(valueInMilliseconds) / 1000;

        return String.valueOf(dateValue);
    }
    
    public static boolean isEmpty(String value) {

        return (value == null || value.length() == 0);
    }
    
    public static boolean hasValue(TaskAttribute taskAttribute) {
        
        return (taskAttribute != null && taskAttribute.getValue() != null
                && taskAttribute.getValue().length() > 0);
    }
    
    public static boolean equal(Object o1, Object o2) {
        
        return o1 == o2 || (o1 != null && o1.equals(o2));
    }

    /**
     * Creates a <code>MantisSearch</code> object from this query.
     */
    public static MantisSearch getMantisSearch(IRepositoryQuery query) {

        String limitString = query.getAttribute(IMantisClient.SEARCH_LIMIT);
        int limit;
        if (limitString == null)
            limit = MantisSearch.DEFAULT_SEARCH_LIMIT; // default
        else
            limit = Integer.parseInt(limitString);

        MantisSearch search;

        String projectName = query.getAttribute(IMantisClient.PROJECT_NAME);

        // we still need to support the old format
        if (projectName == null)
            search = createMantisSearchFromUrl(query.getUrl());
        else {
            String filterName = query.getAttribute(IMantisClient.FILTER_NAME);
            search = new MantisSearch(projectName, filterName);
        }

        search.setLimit(limit);

        return search;
    }

    private static MantisSearch createMantisSearchFromUrl(String url) {

        return MantisSearchFromUrlParser.INSTANCE.fromUrl(url);

    }

    /**
     * Parser which supports the legacy query format, encoded in the url
     * 
     * @author Robert Munteanu
     * 
     */
    private static class MantisSearchFromUrlParser {

        static final MantisSearchFromUrlParser INSTANCE = new MantisSearchFromUrlParser();

        private MantisSearchFromUrlParser() {

        }

        public MantisSearch fromUrl(String url) {

            StringTokenizer t = new StringTokenizer(url, "&");
            String project = null;
            String filter = null;

            while (t.hasMoreTokens()) {
                String token = t.nextToken();
                int i = token.indexOf('=');
                if (i != -1)
                    try {
                        String key = URLDecoder.decode(token.substring(0, i), IMantisClient.CHARSET);
                        String value = URLDecoder.decode(token.substring(i + 1), IMantisClient.CHARSET);

                        if ("project".equals(key)) {
                            project = value;
                        } else if ("filter".equals(key)) {
                            filter = value;
                        }
                    } catch (UnsupportedEncodingException e) {
                        MantisCorePlugin.error("Unexpected encoding problem while parsing search string.", e);
                    }
            }

            return new MantisSearch(project, filter);
        }
    }

    /**
     * @deprecated Use {@link Joiner#join(Iterable)}
     */
    @Deprecated
    public static String toCsvString(List<String> values) {
        
        if ( values == null || values.isEmpty() )
            return "";
        
        StringBuilder builder = new StringBuilder();
        for ( String value : values )
            builder.append(value).append(',');
        builder.deleteCharAt(builder.length() - 1);

        return builder.toString();
    }

    public static List<String> fromCsvString(String value) {

        if ( isEmpty(value) )
            return new ArrayList<String>();
        
        return new ArrayList<String>(Arrays.asList(value.split(",")));
    }   
}
