package com.itsolut.mantis.core.model;

/**
 * @author Robert Munteanu
 */
public class MantisSearch {

    public static final int DEFAULT_SEARCH_LIMIT = 100;
    public static final String DEFAULT_SEARCH_LIMIT_STRING = String.valueOf(DEFAULT_SEARCH_LIMIT);

    private final String projectName;
    private final String filterName;

    private int limit = DEFAULT_SEARCH_LIMIT;

    public MantisSearch(String projectName, String filterName) {

        this.projectName = projectName;
        this.filterName = filterName;
    }

    public int getLimit() {

        return limit;
    }

    public void setLimit(int limit) {

        this.limit = limit;
    }

    public String getFilterName() {

        return filterName;
    }

    public String getProjectName() {

        return projectName;
    }

}
