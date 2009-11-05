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

package com.itsolut.mantis.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itsolut.mantis.core.exception.InvalidTicketException;

/**
 * Represents a Mantis ticket as it is retrieved from a Mantis repository.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTicket {

    /**
     * Represents the key of a string property of a ticket.
     * 
     * @author Steffen Pingel
     */
    public enum Key {
        ID("id"),
        ADDITIONAL_INFO("additional_information"),
        ASSIGNED_TO("assigned_to"),
        CATEOGRY("category"),
        DATE_SUBMITTED("date_submitted"),
        DESCRIPTION("description"),
        ETA("eta"),
        LAST_UPDATED("last_updated"),
        PRIORITY("priority"),
        PROJECT("project"),
        PROJECTION("projection"),
        REPORTER("reporter"),
        REPRODUCIBILITY("reproducibility"),
        RESOLUTION("resolution"),
        SEVERITY("severity"),
        STATUS("status"),
        STEPS_TO_REPRODUCE("steps_to_reproduce"),
        SUMMARY("summary"),
        VERSION("version"),
        VIEW_STATE("view_state"),
        FIXED_IN("fixed_in"),
        TARGET_VERSION("target_version"),
        DUE_DATE("due_date"),
        NEW_COMMENT("new_comment"),
        ATTACHID("attachid"),
        ATTACHMENT("attachment"),
        PARENT_OF("parent_of"),
        CHILD_OF("child_of"),
        HAS_DUPLICATE("has_duplicate"),
        DUPLICATE_OF("duplicate_of"),
        RELATED_TO("related_to");


        public static Key fromKey(String name) {
            for (Key key : Key.values()) {
                if (key.getKey().equals(name)) {
                    return key;
                }
            }
            return null;
        }

        private String key;

        Key(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }

        public String getKey() {
            return key;
        }
    }

    public static final int INVALID_ID = -1;

    private int id = INVALID_ID;

    private Date lastChanged;
    private Date created;

    //	/**
    //	 * User defined custom ticket fields.
    //	 *
    //	 * @see http://projects.edgewall.com/trac/wiki/TracTicketsCustomFields
    //	 */
    //	private Map<String, String> customValueByKey;
    //

    /** Mantis' built-in ticket properties. */
    private final Map<Key, String> valueByKey = new HashMap<Key, String>();

    private List<MantisComment> comments;

    private List<MantisAttachment> attachments;

    private List<MantisRelationship> relationships;
    
    private Map<String, String> customFieldValues = new HashMap<String, String>(); 

    //	private String[] actions;
    //
    //	private String[] resolutions;
    //
    public MantisTicket() {
    }
    //
    /**
     * Constructs a Trac ticket.
     * 
     * @param id
     *            the nummeric Trac ticket id
     */
    public MantisTicket(int id) {
        this.id = id;
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }


    public Date getCreated() {
        return created;
    }
    public Date getLastChanged() {
        return lastChanged;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
    public void setLastChanged(Date lastChanged) {
        this.lastChanged = lastChanged;
    }

    //	public String getCustomValue(String key) {
    //		if (customValueByKey == null) {
    //			return null;
    //		}
    //		return customValueByKey.get(key);
    //	}
    //
    public String getValue(Key key) {
        return valueByKey.get(key);
    }
    
    /**
     * Gets the value and filters the special 'none' value by turning into the empty string
     * 
     * <p>Used for Keys which refer to versions, which are displayed as 'none' to the user but
     * need to be sent as the empty string to the SOAP service</p>
     * 
     * @param key
     * @return the value, changed to '' if equal to 'none'
     */
    public String getValueAndFilterNone(Key key) {
        
        String value = valueByKey.get(key);
        
        if ( "none".equals(value))
            value = "";
        
        return value;
        
    }

    public Map<String, String> getValues() {
        Map<String, String> result = new HashMap<String, String>();
        for (Key key : valueByKey.keySet()) {
            result.put(key.getKey(), valueByKey.get(key));
        }
        //		if (customValueByKey != null) {
        //			result.putAll(customValueByKey);
        //		}
        return result;
    }

    public boolean isValid() {
        return getId() != MantisTicket.INVALID_ID;
    }

    public void putBuiltinValue(Key key, String value) throws InvalidTicketException {
        valueByKey.put(key, value);
    }

    //	public void putCustomValue(String key, String value) {
    //		if (customValueByKey == null) {
    //			customValueByKey = new HashMap<String, String>();
    //		}
    //		customValueByKey.put(key, value);
    //	}
    //
    /**
     * Stores a value as it is retrieved from the repository.
     * 
     * @throws InvalidTicketException
     *             thrown if the type of <code>value</code> is not valid
     */
    public boolean putValue(String keyName, String value) throws InvalidTicketException {
        Key key = Key.fromKey(keyName);
        if (key != null) {
            if (key == Key.ID || key == Key.LAST_UPDATED) {
                return false;
            }
            putBuiltinValue(key, value);
            //		} else if (value instanceof String) {
            //			putCustomValue(keyName, value);
        } else {
            putCustomFieldValue(keyName, value);
        }
        return true;
    }
    
    public void putCustomFieldValue(String customFieldName, String value) {
        
        customFieldValues.put(customFieldName, value);
    }
    
    public Map<String,String> getCustomFieldValues() {
        
        return Collections.unmodifiableMap(customFieldValues);
    }
    
    public String getCustomFieldValue(String name) {

        return customFieldValues.get(name);
    }
    

    public void addComment(MantisComment comment) {
        if (comments == null) {
            comments = new ArrayList<MantisComment>();
        }
        comments.add(comment);
    }
    public MantisComment[] getComments() {
        return (comments != null) ? comments.toArray(new MantisComment[0]) : null;
    }

    public void addAttachment(MantisAttachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<MantisAttachment>();
        }
        attachments.add(attachment);
    }

    public void addRelationship(MantisRelationship relationship) {
        if(relationships == null) {
            relationships = new ArrayList<MantisRelationship>();
        }
        relationships.add(relationship);
    }

    public MantisRelationship[] getRelationships() {
        return relationships != null ? relationships.toArray(new MantisRelationship[relationships.size()]) : new MantisRelationship[0];
    }

    public MantisAttachment[] getAttachments() {
        return (attachments != null) ? attachments.toArray(new MantisAttachment[0]) : null;
    }

    //	public void setActions(String[] actions) {
    //		this.actions = actions;
    //	}
    //
    //	public String[] getActions() {
    //		return actions;
    //	}
    //
    //	public void setResolutions(String[] resolutions) {
    //		this.resolutions = resolutions;
    //	}
    //
    //	public String[] getResolutions() {
    //		return resolutions;
    //	}


}
