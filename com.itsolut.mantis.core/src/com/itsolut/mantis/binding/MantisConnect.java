/**
 * MantisConnect.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.itsolut.mantis.binding;

public interface MantisConnect extends javax.xml.rpc.Service {
    public java.lang.String getMantisConnectPortAddress();

    public com.itsolut.mantis.binding.MantisConnectPortType getMantisConnectPort() throws javax.xml.rpc.ServiceException;

    public com.itsolut.mantis.binding.MantisConnectPortType getMantisConnectPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
