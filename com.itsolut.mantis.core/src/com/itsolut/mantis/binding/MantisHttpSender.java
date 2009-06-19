package com.itsolut.mantis.binding;

import java.net.URL;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.CommonsHTTPSender;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.WebUtil;

/**
 * Custom HTTP sender used to configure the host
 * 
 * @author Robert Munteanu
 */
public class MantisHttpSender extends CommonsHTTPSender {
    
    static final String LOCATION = MantisHttpSender.class.getName() + ".location";
    
    private static final String USER_AGENT = "Mylyn-Mantis Connector";
    
    @Override protected HostConfiguration getHostConfiguration(HttpClient client, MessageContext context, URL url) {

        AbstractWebLocation location = (AbstractWebLocation) context.getProperty(LOCATION);
        
        WebUtil.configureHttpClient(client, USER_AGENT);
        return WebUtil.createHostConfiguration(client, location, getMonitor());
    }

    // XXX - Must implement a proper monitor
    // See JiraHttpSender for an example
    private IProgressMonitor getMonitor() {

        return new NullProgressMonitor();
    }
    
}
