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
     * Versions 0.0.5 or 1.1.x up to 1.1.6
     * 
     * <p>Do not support target_version ( it is erased when using the SOAP API)
     * , do not have proper task relations support ( usually reversed ) and
     * does not require attachments to be Base64-encoded</p>
     */
    VERSION_1_1_6_OR_LOWER(false, false, false, false), 

    /**
     * Versions 1.1.7 or higher
     * 
     * <p>Do not support target_version ( it is erased when using the SOAP API)
     * , do not have proper task relations support ( usually reversed ) and
     * requires attachments to be Base64-encoded</p>
     */
    VERSION_1_1_7_OR_HIGHER(false, false, true, false), 

    /**
     * Version 1.2 alpha 3 or previous
     * 
     * <p>Has target_version and proper task relation support, introduced 
     * after 1.2.0a2. Does not require attachments to be Base64-Encoded.</p>
     */
    VERSION_1_2_A3_OR_LOWER(true, true, false, false),
    
    /**
     * Versions 1.2 rc1 or newwer
     * 
     * <p>Do not support target_version ( it is erased when using the SOAP API)
     * , do not have proper task relations support ( usually reversed ) and
     * requires attachments to be Base64-encoded</p>
     */
    VERSION_1_2_RC1_OR_HIGHER(true, true, true, false),
    
    /**
     * Versions 1.3 or newer.
     * 
     * <p>Since this is a dev version, things might break.</p>
     * 
     * <p>Supports target_version, task relations, requires
     * Base64-encoding of attachments and has due date support.</p>
     */
    VERSION_1_3_DEV(true, true, true, true);
    

    
    public static RepositoryVersion fromVersionString(String versionString) throws MantisException{
        
        if ( "0.0.5".equals(versionString))
            return VERSION_1_1_6_OR_LOWER;
        
        if ( versionString.startsWith("1.1"))
			return extractMantisMinorVersion(versionString) > 6 ? VERSION_1_1_7_OR_HIGHER : VERSION_1_1_6_OR_LOWER;
        
        if ( versionString.startsWith("1.2")) {
        	
        	int minorVersion = extractMantisMinorVersion(versionString);
        	
        	if ( minorVersion > 0)
        		return VERSION_1_2_RC1_OR_HIGHER;
        	
        	String qualifier = extractQualifierFor120(versionString);
        	
        	if ( qualifier.length() == 0 || qualifier.startsWith("rc"))
        		return VERSION_1_2_RC1_OR_HIGHER;
        	
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
	
	private static String extractQualifierFor120(String versionString) {
		
		return versionString.substring("1.2.0".length());
	}
    

    private final boolean hasProperTaskRelations;
    private final boolean hasTargetVersionSupport;
    private final boolean requiresBase64EncodedAttachment;
    private final boolean hasDueDateSupport;
    
    private RepositoryVersion(boolean hasProperTaskRelations, boolean hasTargetVersionSupport, boolean requiresBase64EncodedAttachment, boolean hasDueDateSupport) {

        this.hasProperTaskRelations = hasProperTaskRelations;
        this.hasTargetVersionSupport = hasTargetVersionSupport;
        this.requiresBase64EncodedAttachment = requiresBase64EncodedAttachment;
        this.hasDueDateSupport = hasDueDateSupport;
    }
    
    
    public boolean isHasProperTaskRelations() {

        return hasProperTaskRelations;
    }
    
    public boolean isHasTargetVersionSupport() {

        return hasTargetVersionSupport;
    }
 
    public boolean isRequiresBase64EncodedAttachment() {
    	
		return requiresBase64EncodedAttachment;
	}
    
    public boolean isHasDueDateSupport() {

        return hasDueDateSupport;
    }
}