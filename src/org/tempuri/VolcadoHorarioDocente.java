package org.tempuri;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import blackboard.db.BbDatabase;
import blackboard.db.ConnectionNotAvailableException;

public class VolcadoHorarioDocente {

	public String VolcadoHorarioDocente(HttpServletRequest request) throws Exception {
		if (request.getMethod().equalsIgnoreCase("post")) {
			long startTime = System.currentTimeMillis();
			BufferedReader reader = request.getReader();

			Connection conn = null;
			conn = BbDatabase.getDefaultInstance().getConnectionManager().getConnection();

			String thisLine = null;
			int rowsReaded = 0;
			boolean isOracle = BbDatabase.getDefaultInstance().isOracle();
			boolean skipLine = true;

			PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO LNOH_HORARIO_DOCENTE_TEMP VALUES(LNOH_HORARIO_DOCENTE_SEQ.nextval,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			while ((thisLine = reader.readLine()) != null) {
				// SKIP THE HEADERS IN THE FLAT FILE
				if (skipLine) {
					skipLine = false;
					continue;
				} else {
					// STARTS AT LINE 1
					String[] values = thisLine.split(";");
					if (isOracle) {
						this.setParameters(values, ps);
						rowsReaded++;
						if (rowsReaded % 500 == 0) {
							ps.executeBatch();
							ps.clearBatch();
						}
					}
				}
			}
			double temp = rowsReaded / 500.0;
			temp = temp - Math.ceil(temp);

			if (temp > 0) {
				if (isOracle) {
					ps.executeBatch();
				}
			}

			conn.createStatement().executeQuery("DELETE FROM LNOH_HORARIO_DOCENTE");
			conn.createStatement()
					.executeQuery("INSERT INTO LNOH_HORARIO_DOCENTE SELECT * FROM lnoh_horario_docente_temp");

			// DELETE ALL THE ROWS IN THE TEMPORAL TABLE
			conn.createStatement().execute("DELETE FROM lnoh_horario_docente_temp");

			conn.close();
			conn = null;
			ps.close();
			ps = null;

			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			String data = "";
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
		ps.setInt(i++, Integer.valueOf(array[0]));// ANO_PROCESO
		ps.setInt(i++, Integer.valueOf(array[1]));// SEMESTRE_PROCESO
		ps.setInt(i++, Integer.valueOf(array[2]));// RUT_DOCENTE
		ps.setString(i++, array[3]);// MODULO
		ps.setInt(i++, Integer.valueOf(array[4]));// SECCION
		ps.setString(i++, array[5]);// ID_DIA
		ps.setInt(i++, Integer.valueOf(array[7]));// ID_MODULO
		ps.setString(i++, array[8]);// HORARIO_MODULO
		ps.setInt(i++, Integer.valueOf(array[9]));// DURACION_MODULO
		ps.setString(i++, array[10]);// JORNADA_MODULO
		ps.setInt(i++, Integer.valueOf(array[11]));// DSAL_CLASE
		ps.setString(i++, array[12]);// FECHA_OCUPADA
		ps.setString(i++, array[13]);// FECHA_LIBERADA
		ps.setString(i++, removeUnhandledCharacters(array[14]));// SALA
		ps.setString(i++, array[15]);// COD_EDIFICIO
		ps.setString(i++, removeUnhandledCharacters(array[16]));// NOM_EDIFICIO
		ps.setString(i++, removeUnhandledCharacters(array[17]));// SEDE_EDIFICIO
		ps.setString(i++, removeUnhandledCharacters(array[18]));// NOM_SALA
		ps.setInt(i++, Integer.valueOf(array[19]));// CARRERA_DEL_MODULO
		ps.setString(i++, array[20]);// MODALIDAD
		ps.setString(i++, array[21]);// COD_ESCUELA
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
