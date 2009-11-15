/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Robert Munteanu
 *******************************************************************************/
package com.itsolut.mantis.core;

interface DefaultConstantValues {

    public enum Role {

        REPORTER(25), DEVELOPER(55);

        private final int value;

        private Role(int value) {

            this.value = value;
        }

        public int getValue() {

            return value;
        }
    }

    public enum Threshold {

        REPORT_BUG_THRESHOLD(Role.REPORTER.getValue()), UPDATE_BUG_ASSIGN_THRESHOLD(Role.DEVELOPER
                .getValue());

        private final int value;

        private Threshold(int value) {

            this.value = value;
        }

        public int getValue() {

            return value;
        }
    }

}
