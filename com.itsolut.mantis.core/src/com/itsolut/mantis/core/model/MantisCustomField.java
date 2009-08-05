package com.itsolut.mantis.core.model;

import java.io.Serializable;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisCustomField implements Serializable {

    private static final long serialVersionUID = 6542524499720734253L;

    private int id;

    private String name;

    private MantisCustomFieldType type;

    private String defaultValue;

    private String[] possibleValues;

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public String getName() {

        return name;
    }

    public MantisCustomFieldType getType() {

        return type;
    }

    public void setName(String name) {

        this.name = name;
    }

    public void setType(MantisCustomFieldType type) {

        this.type = type;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }

    public void setPossibleValues(String[] possibleValues) {

        this.possibleValues = possibleValues;
    }

    public String[] getPossibleValues() {

        return possibleValues;
    }

}
