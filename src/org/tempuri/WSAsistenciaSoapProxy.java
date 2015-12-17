package org.tempuri;

public class WSAsistenciaSoapProxy implements org.tempuri.WSAsistenciaSoap {
  private String _endpoint = null;
  private org.tempuri.WSAsistenciaSoap wSAsistenciaSoap = null;
  
  public WSAsistenciaSoapProxy() {
    _initWSAsistenciaSoapProxy();
  }
  
  public WSAsistenciaSoapProxy(String endpoint) {
    _endpoint = endpoint;
    _initWSAsistenciaSoapProxy();
  }
  
  private void _initWSAsistenciaSoapProxy() {
    try {
      wSAsistenciaSoap = (new org.tempuri.WSAsistenciaLocator()).getWSAsistenciaSoap();
      if (wSAsistenciaSoap != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)wSAsistenciaSoap)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)wSAsistenciaSoap)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (wSAsistenciaSoap != null)
      ((javax.xml.rpc.Stub)wSAsistenciaSoap)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public org.tempuri.WSAsistenciaSoap getWSAsistenciaSoap() {
    if (wSAsistenciaSoap == null)
      _initWSAsistenciaSoapProxy();
    return wSAsistenciaSoap;
  }
  
  public org.tempuri.Response loginMoodle(java.lang.String username, java.lang.String semilla) throws java.rmi.RemoteException{
    if (wSAsistenciaSoap == null)
      _initWSAsistenciaSoapProxy();
    return wSAsistenciaSoap.loginMoodle(username, semilla);
  }
  
  public org.tempuri.Response loginMoodleXML(java.lang.String stringXml) throws java.rmi.RemoteException{
    if (wSAsistenciaSoap == null)
      _initWSAsistenciaSoapProxy();
    return wSAsistenciaSoap.loginMoodleXML(stringXml);
  }
  
  public org.tempuri.Response[] registrarAsistenciaXML(java.lang.String stringXml) throws java.rmi.RemoteException{
    if (wSAsistenciaSoap == null)
      _initWSAsistenciaSoapProxy();
    return wSAsistenciaSoap.registrarAsistenciaXML(stringXml);
  }
  
  public org.tempuri.Response registrarAsistencia(java.lang.String codigoModulo, java.lang.String rut, java.lang.String fechaAsistencia, java.lang.String token) throws java.rmi.RemoteException{
    if (wSAsistenciaSoap == null)
      _initWSAsistenciaSoapProxy();
    return wSAsistenciaSoap.registrarAsistencia(codigoModulo, rut, fechaAsistencia, token);
  }
  
  
}