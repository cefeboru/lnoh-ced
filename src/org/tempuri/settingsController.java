package org.tempuri;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;

import blackboard.db.BbDatabase;
import blackboard.db.ConnectionManager;
import blackboard.db.ConnectionNotAvailableException;
import blackboard.platform.context.Context;

public class settingsController {
	private Context bbContext;
	public String message = "";
	
	public settingsController(Context ctx) throws Exception {
		this.bbContext = ctx;
		HttpServletRequest request = bbContext.getRequest();
		if(request.getMethod().equals("POST")) {
			String newURL = request.getParameter("ws_url");
			if(newURL.isEmpty()) {
				throw new Exception("No se encontro el parametro ws_url en el POST request");
			} else {
				this.setURL(newURL);
			}
		}
	}
	
	public String getURL() {
		ConnectionManager cManager = null;
    	Connection conn = null;
    	ResultSet rs = null;
    	String WS_URL = "ERROR";
    	try {
    		cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			conn = cManager.getConnection();
			rs = conn.createStatement().executeQuery("SELECT * FROM LNOH_CED_SETTINGS WHERE NAME='WS_URL'");
			if(rs.next()) {
				String URL = rs.getString("VALUE");
				WS_URL = URL;
				System.out.println("Se encontro el WS_URL = " + URL);
			} else {
				WS_URL = "http://asistencia.agpc.cl/ws/WSAsistencia.asmx";
				System.out.println("No se encontro el WS_URL");
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
		return WS_URL;
	}
	
	public void setURL(String newURL){
		ConnectionManager cManager = null;
    	Connection conn = null;
    	ResultSet rs = null;
    	try {
    		cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			conn = cManager.getConnection();
			Statement Query = conn.createStatement();
			rs = Query.executeQuery("SELECT COUNT(*) FROM LNOH_CED_SETTINGS WHERE NAME='WS_URL'");
			if(rs.next()) {
				int count = rs.getInt(1);
				if(count > 0){
					Query.executeQuery("UPDATE LNOH_CED_SETTINGS SET VALUE='"+newURL+"' WHERE NAME='WS_URL'");
					message = "<div style='background:green;'><p>El nuevo WS se ha guardado</p></div>";
				} else {
					Query.executeQuery("INSERT INTO LNOH_CED_SETTINGS(NAME, VALUE) VALUES('WS_URL','"+newURL+"')");
					message = "<div style='background:green;'><p>El nuevo WS se ha guardado</p></div>";
				}
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
	}
}
