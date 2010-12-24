/*******************************************************************************
 * Copyright (c) 2010 Robert Munteanu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Robert Munteanu - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.EnumSet;
import java.util.Set;

import com.itsolut.mantis.core.exception.MantisException;

/**
 * Represents the version of the remote repository and its capabilities
 * 
 * @author Robert Munteanu
 *
 */
public enum RepositoryVersion {
    
    /**
     * Versions 0.0.5 or 1.1.x up to 1.1.6
     */
    VERSION_1_1_6_OR_LOWER("0.0.5 through 1.1.6", EnumSet.of(RepositoryCapability.CORRECT_BASE64_ENCODING)), 

    /**
     * Versions 1.1.7 or higher
     */
    VERSION_1_1_7_OR_HIGHER("1.1.7 or higher in the 1.1.x stream", EnumSet.noneOf(RepositoryCapability.class)), 

    /**
     * Version 1.2 alpha 3 or previous
     */
    VERSION_1_2_A3_OR_LOWER("1.2.0 alpha 3 or previous in the 1.2.x stream", EnumSet.of(RepositoryCapability.TARGET_VERSION, RepositoryCapability.TASK_RELATIONS, RepositoryCapability.CORRECT_BASE64_ENCODING)),
    
    /**
     * Versions 1.2 rc1 or newwe
     */
    VERSION_1_2_RC1_OR_HIGHER("1.2.0 rc1 to 1.2.0 (not inclusive)", EnumSet.of(RepositoryCapability.TARGET_VERSION, RepositoryCapability.TASK_RELATIONS)),
    
    /**
     * Versions 1.2.0 or newer.
     * 
     * <p>Supports target_version, task relations, requires
     * Base64-encoding of attachments and has due date support.</p>
     */
    VERSION_1_2_OR_HIGHER("1.2.0 to 1.2.1", EnumSet.complementOf(EnumSet.of(RepositoryCapability.CORRECT_BASE64_ENCODING))),
    
    /**
     * Versions 1.2.2 or newer.
     * 
     * <p>Supports target_version, task relations, does not require
     * Base64-encoding of attachments and has due date support.</p>
     */
    VERSION_1_2_2_OR_HIGHER("1.2.2 or higher in the 1.2.x stream", EnumSet.allOf(RepositoryCapability.class)),
    
    /**
     * Versions 1.3 or newer.
     * 
     * <p>Since this is a dev version, things might break.</p>
     * 
     * <p>Supports target_version, task relations, requires
     * Base64-encoding of attachments and has due date support.</p>
     */
    VERSION_1_3_DEV("1.3.x development version", EnumSet.allOf(RepositoryCapability.class));
    
    
    public static RepositoryVersion fromVersionString(String versionString) throws MantisException{
        
        if ( "0.0.5".equals(versionString))
            return VERSION_1_1_6_OR_LOWER;
        
        if ( versionString.startsWith("1.1"))
			return extractMantisMinorVersion(versionString) > 6 ? VERSION_1_1_7_OR_HIGHER : VERSION_1_1_6_OR_LOWER;
        
        if ( versionString.startsWith("1.2")) {
        	
        	String qualifier = extractQualifierFor12x(versionString);
        	
        	if ( qualifier.startsWith("rc"))
        		return VERSION_1_2_RC1_OR_HIGHER;
        	
        	if ( qualifier.length() == 0) {
        		
        		int minorVersion = extractMantisMinorVersion(versionString);
        		if ( minorVersion < 2)
        			return VERSION_1_2_OR_HIGHER;
        		else
        			return VERSION_1_2_2_OR_HIGHER;
        	}
        	
        	return VERSION_1_2_A3_OR_LOWER;
        }
        
        if ( versionString.startsWith("1.3"))
            return VERSION_1_3_DEV;
            
        
        throw new MantisException("Unknown version " + versionString + " .");
            
    }


	private static int extractMantisMinorVersion(String versionString) {

		int minorVersionIndex = versionString.lastIndexOf('.');
		
		String minorVersionString = versionString.substring(minorVersionIndex + 1);
		
		// Mantis has qualifiers appended, like -SVN or rc4, so we just look for all the digits we can find
		
		StringBuilder builder = new StringBuilder(4);
		
		for ( int i = 0; i < minorVersionString.length(); i++) {
			
			char maybeDigit = minorVersionString.charAt(i);
			
			if ( maybeDigit >= '0' && maybeDigit <= '9')
				builder.append(maybeDigit);
			else
				break;
			
			
		}
		
		return Integer.parseInt(builder.toString());
	}
	
	private static String extractQualifierFor12x(String versionString) {
		
		return versionString.substring("1.2.".length()+1);
	}
    

	private final EnumSet<RepositoryCapability> capabilities;
	private final String description;
    
    private RepositoryVersion(String description, EnumSet<RepositoryCapability> capabilities) {
    	
    	this.capabilities = capabilities;
    	this.description = description;
    }
    
    
    public boolean isHasProperTaskRelations() {

        return  capabilities.contains(RepositoryCapability.TASK_RELATIONS);
    }
    
    public boolean isHasTargetVersionSupport() {

        return capabilities.contains(RepositoryCapability.TARGET_VERSION);
    }
 
    public boolean hasCorrectBase64Encoding() {
    	
		return capabilities.contains(RepositoryCapability.CORRECT_BASE64_ENCODING);
	}
    
    public boolean isHasDueDateSupport() {

        return capabilities.contains(RepositoryCapability.DUE_DATE);
    }
    
    public boolean isHasTimeTrackingSupport() {

        return capabilities.contains(RepositoryCapability.TIME_TRACKING);
    }
    
    public Set<RepositoryCapability> getMissingCapabilities() {
    	
    	return EnumSet.complementOf(capabilities);
    }
    
    public String getDescription() {
    	
		return description;
	}
}