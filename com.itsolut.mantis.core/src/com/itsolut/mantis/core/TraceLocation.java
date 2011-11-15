package com.itsolut.mantis.core;

/**
 * @author Robert Munteanu
 *
 */
public enum TraceLocation {

    MAIN(""), CLIENT_MANAGER("/clientmanager"), CONVERTER("/converter"), CONFIG("/config"), SYNC("/sync");
    
    private final String _prefix;
    
    private TraceLocation(String prefix) {
        
        _prefix = prefix;
    }
    
    String getPrefix() {
        
        return _prefix;
    }
}
