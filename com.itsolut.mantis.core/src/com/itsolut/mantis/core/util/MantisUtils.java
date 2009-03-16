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
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

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

    public static String getRepositoryBaseUrl(String repositoryUrl) {
        String baseUrl = repositoryUrl;

        // get the base url of the installation (located in mc / as of version 1.1.0 its located in api/soap)
        if(repositoryUrl.toLowerCase().contains("mc/mantisconnect.php"))
            baseUrl = repositoryUrl.substring(0, repositoryUrl.toLowerCase().indexOf("mc/mantisconnect.php"));
        else if (repositoryUrl.toLowerCase().contains("api/soap/mantisconnect.php"))
            baseUrl = repositoryUrl.substring(0, repositoryUrl.toLowerCase().indexOf("api/soap/mantisconnect.php"));

        return baseUrl;
    }

    public static boolean isCompleted(String status) {
        return "closed".equals(status) || "resolved".equals(status);
    }


    public static String getQueryParameter(IRepositoryQuery query) {
        String url = query.getUrl();
        int i = url.indexOf(IMantisClient.QUERY_URL);
        if (i == -1)
            return null;
        return url.substring(i + IMantisClient.QUERY_URL.length());
    }

    /**
     * Creates a <code>MantisSearch</code> object from this query.
     */
    public static MantisSearch getMantisSearch(IRepositoryQuery query) {
        MantisSearch list = new MantisSearch();
        String url = getQueryParameter(query);
        String limitString = query.getAttribute(IMantisClient.SEARCH_LIMIT);
        int limit;
        if (limitString == null)
            limit = MantisSearch.DEFAULT_SEARCH_LIMIT; // default
        else
            limit = Integer.parseInt(limitString);
        list.setLimit(limit);
        if (url == null)
            return list;

        StringTokenizer t = new StringTokenizer(url, "&");
        while (t.hasMoreTokens()) {
            String token = t.nextToken();
            int i = token.indexOf("=");
            if (i != -1)
                try {
                    String key = URLDecoder.decode(token.substring(0, i), IMantisClient.CHARSET);
                    String value = URLDecoder.decode(token.substring(i + 1), IMantisClient.CHARSET);

                    if ("order".equals(key))
                        list.setOrderBy(value);
                    else if ("desc".equals(key))
                        list.setAscending(!"1".equals(value));
                    else if ("group".equals(key) || "groupdesc".equals(key) || "verbose".equals(key)) {
                        // ignore these parameters
                    } else
                        list.addFilter(key, value);
                } catch (UnsupportedEncodingException e) {
                    MantisCorePlugin.log(e);
                }
        }
        return list;
    }


}
