/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.io.Serializable;

/**
 * @author Robert Munteanu
 */
public class User implements Serializable {

    private int id;
    private String username;
    private String realName;
    private String email;

    public User(int id, String username, String realName, String email) {

        this.id = id;
        this.username = username;
        this.realName = realName;
        this.email = email;
    }

    public String getUsername() {

        return username;
    }

    public String getEmail() {

        return email;
    }

    public String getRealName() {

        return realName;
    }
    
    public int getId() {

        return id;
    }
}
