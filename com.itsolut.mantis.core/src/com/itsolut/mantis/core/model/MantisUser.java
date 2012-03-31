/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core.model;


/**
 * @author Robert Munteanu
 */
public class MantisUser extends MantisTicketAttribute {

    private static final long serialVersionUID = 1L;
    
    private String realName;
    private String email;

    public MantisUser(int id, String username, String realName, String email) {

        super(username, id);
        this.realName = realName;
        this.email = email;
    }

    public String getEmail() {

        return email;
    }

    public String getRealName() {

        return realName;
    }
    
    @Override
    public String getKey() {
    	
    	return super.getName();
    }
    
    @Override
    public String getName() {
    	if ( realName != null && realName.trim().length() != 0 )
    		return realName;
    	
    	return super.getName();
    }
}
