package com.itsolut.mantis.core;

import com.itsolut.mantis.core.exception.MantisException;

/**
 * Represents the version of the remote repository and its capabilities
 * 
 * @author Robert Munteanu
 *
 */
public enum RepositoryVersion {
    
    /**
     * Versions 0.0.5 or 1.1.x
     * 
     * <p>Do not support target_version ( it is erased when using the SOAP API)
     * and do not have proper task relations support ( usually reversed )</p>
     */
    VERSION_1_1_OR_LOWER(false, false), 
    
    /**
     * Version 1.2
     * 
     * <p>Has target_version and proper task relation support, introduced 
     * after 1.2.0a2. For convenience, we treat all versions as having
     * this support</p>
     */
    VERSION_1_2(true, true);
    
    public static RepositoryVersion fromVersionString(String versionString) throws MantisException{
        
        if ( "0.0.5".equals(versionString) || versionString.startsWith("1.1"))
            return VERSION_1_1_OR_LOWER;
        
        if ( versionString.startsWith("1.2"))
            return VERSION_1_2;
        
        throw new MantisException("Unknown version " + versionString + " .");
            
    }
    

    private final boolean hasProperTaskRelations;
    private final boolean hasTargetVersionSupport;
    
    private RepositoryVersion(boolean hasProperTaskRelations, boolean hasTargetVersionSupport) {

        this.hasProperTaskRelations = hasProperTaskRelations;
        this.hasTargetVersionSupport = hasTargetVersionSupport;
    }
    
    
    public boolean isHasProperTaskRelations() {

        return hasProperTaskRelations;
    }
    
    public boolean isHasTargetVersionSupport() {

        return hasTargetVersionSupport;
    }
    
}