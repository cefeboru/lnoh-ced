/**
 * WSAsistenciaLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.tempuri;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import blackboard.db.BbDatabase;
import blackboard.db.ConnectionManager;
import blackboard.db.ConnectionNotAvailableException;

public class WSAsistenciaLocator extends org.apache.axis.client.Service implements org.tempuri.WSAsistencia {

    public WSAsistenciaLocator() {
    }


    public WSAsistenciaLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public WSAsistenciaLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for WSAsistenciaSoap
    private java.lang.String WSAsistenciaSoap_address = "http://cedtest3.intercapit.com/ws/WSAsistencia.asmx";

    public java.lang.String getWSAsistenciaSoapAddress() {
    	ConnectionManager cManager = null;
    	Connection conn = null;
    	ResultSet rs = null;
    	try {
    		cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			conn = cManager.getConnection();
			rs = conn.createStatement().executeQuery("SELECT * FROM LNOH_CED_SETTINGS WHERE NAME='WS_URL'");
			if(rs.next()) {
				String URL = rs.getString("WS_URL");
				return URL;
			} else {
				return "http://asistencia.agpc.cl/ws/WSAsistencia.asmx";
			}
			
		} catch (ConnectionNotAvailableException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(cManager != null && conn != null) {
				try {
					rs.close();
					conn.close();
					cManager.releaseConnection(conn);
					rs = null;
					conn = null;
					cManager = null;
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
			}
		}
        return WSAsistenciaSoap_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String WSAsistenciaSoapWSDDServiceName = "WSAsistenciaSoap";

    public java.lang.String getWSAsistenciaSoapWSDDServiceName() {
        return WSAsistenciaSoapWSDDServiceName;
    }

    public void setWSAsistenciaSoapWSDDServiceName(java.lang.String name) {
        WSAsistenciaSoapWSDDServiceName = name;
    }

    public org.tempuri.WSAsistenciaSoap getWSAsistenciaSoap() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(WSAsistenciaSoap_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getWSAsistenciaSoap(endpoint);
    }

    public org.tempuri.WSAsistenciaSoap getWSAsistenciaSoap(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.tempuri.WSAsistenciaSoapStub _stub = new org.tempuri.WSAsistenciaSoapStub(portAddress, this);
            _stub.setPortName(getWSAsistenciaSoapWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setWSAsistenciaSoapEndpointAddress(java.lang.String address) {
        WSAsistenciaSoap_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.tempuri.WSAsistenciaSoap.class.isAssignableFrom(serviceEndpointInterface)) {
                org.tempuri.WSAsistenciaSoapStub _stub = new org.tempuri.WSAsistenciaSoapStub(new java.net.URL(WSAsistenciaSoap_address), this);
                _stub.setPortName(getWSAsistenciaSoapWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("WSAsistenciaSoap".equals(inputPortName)) {
            return getWSAsistenciaSoap();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://tempuri.org/", "WSAsistencia");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://tempuri.org/", "WSAsistenciaSoap"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("WSAsistenciaSoap".equals(portName)) {
            setWSAsistenciaSoapEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
