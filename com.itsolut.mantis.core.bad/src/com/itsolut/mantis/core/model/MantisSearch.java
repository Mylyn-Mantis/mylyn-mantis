/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.core.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.mylyn.monitor.core.StatusHandler;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.model.MantisSearchFilter.CompareOperator;

/**
 * Represents a Mantis search. A search can have multiple {@link MantisSearchFilter}s
 * that all need to match.
 * 
 * @author Steffen Pingel
 */
public class MantisSearch {

	/** Stores search criteria in the order entered by the user. */
	private Map<String, MantisSearchFilter> filterByFieldName = new LinkedHashMap<String, MantisSearchFilter>();

	/** The field the result is ordered by. */
	private String orderBy;

	private boolean ascending = true;

	public MantisSearch() {
	}

	public void addFilter(String key, String value) {
		MantisSearchFilter filter = filterByFieldName.get(key);
		if (filter == null) {
			filter = new MantisSearchFilter(key);
			CompareOperator operator = CompareOperator.fromUrl(value);
			filter.setOperator(operator);
			filterByFieldName.put(key, filter);
		}

		filter.addValue(value.substring(filter.getOperator().getQueryValue().length()));
	}

	public void addFilter(MantisSearchFilter filter) {
		filterByFieldName.put(filter.getFieldName(), filter);
	}
	
	public List<MantisSearchFilter> getFilters() {
		return new ArrayList<MantisSearchFilter>(filterByFieldName.values());
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getOrderBy() {
		return orderBy;
	}

	/**
	 * Returns a Mantis query string that conforms to the format defined at
	 * {@link http://projects.edgewall.com/trac/wiki/MantisQuery#QueryLanguage}.
	 * 
	 * @return the empty string, if no search order and criteria are defined; a
	 *         string that starts with &amp;, otherwise
	 */
	public String toQuery() {
		StringBuilder sb = new StringBuilder();
		if (orderBy != null) {
			sb.append("&order=");
			sb.append(orderBy);
			if (!ascending) {
				sb.append("&desc=1");
			}
		}
		for (MantisSearchFilter filter : filterByFieldName.values()) {
			sb.append("&");
			sb.append(filter.getFieldName());
			sb.append(filter.getOperator().getQueryValue());
			sb.append("=");
			List<String> values = filter.getValues();
			for (Iterator<String> it = values.iterator(); it.hasNext();) {
				sb.append(it.next());
				if (it.hasNext()) {
					sb.append("|");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Returns a URL encoded string that can be passed as an argument to the
	 * Mantis query script.
	 * 
	 * @return the empty string, if no search order and criteria are defined; a
	 *         string that starts with &amp;, otherwise
	 */
	public String toUrl() {
		StringBuilder sb = new StringBuilder();
		if (orderBy != null) {
			sb.append("&order=");
			sb.append(orderBy);
			if (!ascending) {
				sb.append("&desc=1");
			}
		} else if (filterByFieldName.isEmpty()) {
			// TODO figure out why search must be ordered when logged in (otherwise
			// no results will be returned)
			sb.append("&order=id");
		}

		for (MantisSearchFilter filter : filterByFieldName.values()) {
			for (String value : filter.getValues()) {
				sb.append("&");
				sb.append(filter.getFieldName());
				sb.append("=");
				try {
					sb.append(URLEncoder.encode(filter.getOperator().getQueryValue(), IMantisClient.CHARSET));
					sb.append(URLEncoder.encode(value, IMantisClient.CHARSET));
				} catch (UnsupportedEncodingException e) {
					StatusHandler.log(e, "Unexpected exception while decoding URL");
				}
			}
		}
		return sb.toString();
	}

}
