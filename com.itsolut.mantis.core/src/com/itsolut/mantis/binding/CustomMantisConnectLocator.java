package com.itsolut.mantis.binding;

import java.util.Hashtable;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.transport.http.HTTPConstants;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;

/**
 * 
 * Custom locator for preparing the connection
 * 
 * @author Robert Munteanu
 *
 */
public class CustomMantisConnectLocator extends MantisConnectLocator {
    
    private AbstractWebLocation location;
    
    public CustomMantisConnectLocator() {

    }
    
    public CustomMantisConnectLocator(EngineConfiguration config) {

        super(config);
    }
    
    public CustomMantisConnectLocator(String wsdlLoc, QName name) throws ServiceException {

        super(wsdlLoc, name);
    }
    
    public void setLocation(AbstractWebLocation location) {

        this.location = location;
    }
    
    public AbstractWebLocation getLocation() {

        return location;
    }
    
    @Override public Call createCall() throws ServiceException {

        Call call = super.createCall();
        
        // for configuring the call
        call.setProperty(MantisHttpSender.LOCATION, location);
        
        // The Squid proxy server seems to choke unless this is et
        Map<String, Boolean> headers = new Hashtable<String, Boolean>();
        headers.put(HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED, Boolean.FALSE);
        call.setProperty(HTTPConstants.REQUEST_HEADERS, headers);
        
        return call;
    }
}
