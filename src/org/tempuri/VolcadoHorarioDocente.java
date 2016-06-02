package org.tempuri;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import blackboard.db.BbDatabase;
import blackboard.db.ConnectionManager;
import blackboard.db.ConnectionNotAvailableException;

public class VolcadoHorarioDocente {

	public String insertRows(HttpServletRequest request){
		if (request.getMethod().equalsIgnoreCase("post")) {
			System.out.println("Iniciando Volcado de Horario Docente - " + (new Date()));
			long startTime = System.currentTimeMillis();
			BufferedReader reader;
			ConnectionManager cManager = null;
			Connection conn = null;
			String thisLine = null;
			int rowsReaded = 0;
			boolean isOracle = BbDatabase.getDefaultInstance().isOracle();
			boolean skipLine = true;
			PreparedStatement ps = null;
			
			try {
				reader = request.getReader();
				cManager = BbDatabase.getDefaultInstance().getConnectionManager();
				conn = cManager.getConnection();
				
				ps = conn.prepareStatement(
						"INSERT INTO LNOH_HORARIO_DOCENTE_TEMP VALUES(LNOH_HORARIO_DOCENTE_SEQ.nextval,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
				//int temp = 0;
				boolean leftElements = true;
				while ((thisLine = reader.readLine()) != null) {
					// SKIP THE HEADERS IN THE FLAT FILE
					if (skipLine) {
						skipLine = false;
						continue;
					} else {
						try {
							leftElements = true;
							String[] values = thisLine.split(";");
							if (isOracle) {
								this.setParameters(values, ps);
								if (rowsReaded % 500 == 0) {
									leftElements = false;
									ps.executeBatch();
									ps.clearBatch();
								}
								rowsReaded++;
								//temp++;
							}
						} catch (Exception ex) {
							System.out.println("CED - Error en volcado, linea " + rowsReaded + " Message:" + ex.getMessage());
							ex.printStackTrace();
						}
					}
				}

				if (leftElements) {
					if (isOracle) {
						ps.executeBatch();
						ps.clearBatch();
					}
				}
				
				if(rowsReaded == 0)
				{
					// DELETE ALL THE ROWS IN THE TEMPORAL TABLE
					conn.createStatement().execute("DELETE FROM lnoh_horario_docente_temp");
				} else {
					//DELETE THE CURRENT DATA
					conn.createStatement().executeQuery("DELETE FROM LNOH_HORARIO_DOCENTE");
					//INSERT THE NEW DATA
					conn.createStatement()
							.executeQuery("INSERT INTO LNOH_HORARIO_DOCENTE SELECT * FROM lnoh_horario_docente_temp");
					// DELETE THE TEMPORAL DATA
					conn.createStatement().execute("DELETE FROM lnoh_horario_docente_temp");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ConnectionNotAvailableException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					if(cManager != null) {
						cManager.releaseConnection(conn);
						cManager = null;
					}
					if(conn != null){
						conn.close();
						conn = null;
					}
					if(ps != null) {
						ps.close();
						ps = null;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			String data = "";
			Date currentDate = new Date();
			System.out.println(currentDate.toString() + " - " +rowsReaded + " rows have been successfully processed in " + totalTime / 1000 + " seconds.");
			data = "<p> " + rowsReaded + " rows have been successfully processed in " + totalTime / 1000
					+ " seconds .</p>";
			return data;
		} else
			return "<p>VOLCADO HORARIO DOCENTE</p>";
	}

	public String arrayToString(String[] array) throws Exception {
		String temp = "";
		return temp;
	}

	public void setParameters(String[] array, PreparedStatement ps) throws Exception {
		int i = 1;

		ps.setInt(i++, Integer.valueOf(((array[0].isEmpty())) ? "0" : array[0]));// ANO_PROCESO
		ps.setInt(i++, Integer.valueOf(((array[1].isEmpty())) ? "0" : array[1]));// SEMESTRE_PROCESO
		ps.setInt(i++, Integer.valueOf(((array[2].isEmpty())) ? "0" : array[2]));// RUT_DOCENTE
		ps.setString(i++, (array[3].isEmpty()) ? "-" : array[3]);// MODULO
		ps.setInt(i++, Integer.valueOf(((array[4].isEmpty())) ? "0" : array[4]));// SECCION
		ps.setString(i++, (array[5].isEmpty()) ? "-" : array[5]);// ID_DIA
		ps.setInt(i++, Integer.valueOf(((array[7].isEmpty())) ? "0" : array[7]));// ID_MODULO
		ps.setString(i++, (array[8].isEmpty()) ? "-" : array[8]);// HORARIO_MODULO
		ps.setInt(i++, Integer.valueOf(((array[9].isEmpty())) ? "0" : array[9]));// DURACION_MODULO
		ps.setString(i++, (array[10].isEmpty()) ? "-" : array[10]);// JORNADA_MODULO
		ps.setInt(i++, Integer.valueOf(((array[11]).isEmpty()) ? "0" : array[11]));// DSAL_CLASE
		ps.setString(i++, (array[12].isEmpty()) ? "-" : array[12]);// FECHA_OCUPADA
		ps.setString(i++, (array[13].isEmpty()) ? "-" : array[13]);// FECHA_LIBERADA
		ps.setString(i++, removeUnhandledCharacters((array[14].isEmpty()) ? " " : array[14]));// SALA
		ps.setString(i++, (array[15].isEmpty()) ? "-" : array[15]);// COD_EDIFICIO
		ps.setString(i++, removeUnhandledCharacters((array[16].isEmpty()) ? "-" : array[16]));// NOM_EDIFICIO
		ps.setString(i++, removeUnhandledCharacters((array[17].isEmpty()) ? "-" : array[17]));// SEDE_EDIFICIO
		ps.setString(i++, removeUnhandledCharacters((array[18].isEmpty()) ? "-" : array[18]));// NOM_SALA
		ps.setInt(i++, Integer.valueOf(((array[19]).isEmpty()) ? "0" : array[19]));// CARRERA_DEL_MODULO
		ps.setString(i++, (array[20].isEmpty()) ? "-" : array[20]);// MODALIDAD
		ps.setString(i++, (array[21].isEmpty()) ? "-" : array[21]);// COD_ESCUELA
		ps.addBatch();
	}

	public String removeUnhandledCharacters(String str) {
		char[] strArray = str.toCharArray();
		String temp = "";
		for (int i = 0; i < strArray.length; i++) {
			if (Character.isLetter(strArray[i]) || ' ' == strArray[i] || Character.isDigit(strArray[i])) {
				temp += strArray[i];
			}
		}
		return temp;
	}
}
