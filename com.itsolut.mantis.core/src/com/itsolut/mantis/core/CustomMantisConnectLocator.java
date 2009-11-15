package com.itsolut.mantis.core;

import java.util.Hashtable;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.transport.http.HTTPConstants;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.internal.provisional.commons.soap.SoapHttpSender;

import com.itsolut.mantis.binding.MantisConnectLocator;

/**
 * 
 * Custom locator for preparing the connection
 * 
 * @author Robert Munteanu
 *
 */
@SuppressWarnings("restriction")
public class CustomMantisConnectLocator extends MantisConnectLocator {
    
    private static final long serialVersionUID = 1L;
    
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

        call.setProperty(SoapHttpSender.LOCATION, location);
        call.setProperty(SoapHttpSender.USER_AGENT, "Mylyn-Mantis Connector Apache Axis/1.4");
        
        // The Squid proxy server seems to choke unless this is set
        Map<String, Boolean> headers = new Hashtable<String, Boolean>();
        headers.put(HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED, Boolean.FALSE);
        call.setProperty(HTTPConstants.REQUEST_HEADERS, headers);
        
        return call;
    }
}
