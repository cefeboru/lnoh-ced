/**
 * WSAsistenciaSoap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.tempuri;

public interface WSAsistenciaSoap extends java.rmi.Remote {
    public org.tempuri.Response loginMoodle(java.lang.String username, java.lang.String semilla) throws java.rmi.RemoteException;
    public org.tempuri.Response loginMoodleXML(java.lang.String stringXml) throws java.rmi.RemoteException;
    public org.tempuri.Response[] registrarAsistenciaXML(java.lang.String stringXml) throws java.rmi.RemoteException;
    public org.tempuri.Response registrarAsistencia(java.lang.String codigoModulo, java.lang.String rut, java.lang.String fechaAsistencia, java.lang.String token) throws java.rmi.RemoteException;
}
