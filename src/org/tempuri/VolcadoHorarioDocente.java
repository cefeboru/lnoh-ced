package org.tempuri;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import blackboard.db.BbDatabase;
import blackboard.db.ConnectionNotAvailableException;

public class VolcadoHorarioDocente {

	public String VolcadoHorarioDocente(HttpServletRequest request) throws Exception {
		String debug = "";
		if (request.getMethod().equalsIgnoreCase("post")) {
			String data = "";
			data += "<p>THIS IS A POST REQUEST, Will try to read the data:</p>\n";
			BufferedReader reader = request.getReader();
			String thisLine = null;
			int lineNumber = 0;

			Connection conn = null;
			conn = BbDatabase.getDefaultInstance().getConnectionManager().getConnection();
			String queryString = "DELETE FROM lnoh_horario_docente_temp";
			conn.createStatement().execute(queryString);
			boolean isOracle = BbDatabase.getDefaultInstance().isOracle();
			int rowsReaded = 0;
			String multipleInsertQuery = "";
			StringBuilder stringBuilder = new StringBuilder();

			while ((thisLine = reader.readLine()) != null) {
				if (lineNumber == 0) {
					lineNumber++;
					continue;
				} else {
					String[] values = thisLine.split(";");
					if (isOracle) {
						queryString = "INTO lnoh_horario_docente_temp VALUES(LNOH_HORARIO_DOCENTE_SEQ.nextVal,"
								+ arrayToString(values) + ")";
						// debug += queryString + "\n";
						// data += "Line " + lineNumber + ": " + thisLine +
						// "\n";
						// TODO Make ORACLE MULTIPLE INSERT
						rowsReaded++;
					} else {
						// debug += queryString + "\n";
						// queryString = "INSERT INTO lnoh_horario_docente_temp
						// VALUES(nextval('lnoh_horario_docente_seq'),"+arrayToString(values)+")";
						stringBuilder.append("(nextval('lnoh_horario_docente_seq')," + arrayToString(values) + "),");
						rowsReaded++;
					}
					// throw new Exception(debug);
					if (rowsReaded >= 500) {
						rowsReaded = 0;
						String multipleValues = stringBuilder.toString();
						multipleValues = multipleValues.substring(0, multipleValues.length() - 1);
						conn.createStatement().execute("INSERT INTO lnoh_horario_docente_temp VALUES" + multipleValues);
						stringBuilder = new StringBuilder();
						// lnoh_horario_docente_temp VALUES" + multipleValues);
						//throw new Exception("INSERT INTO lnoh_horario_docente_temp VALUES" + multipleValues);
					}

					lineNumber++;

				}
			}
			conn.close();
			conn = null;
			return data;
		} else
			return "<p>VOLCADO HORARIO DOCENTE</p>";
	}

	public String arrayToString(String[] array) throws Exception {
		String temp = "";
		temp += array[0] + ",";// ANO_PROCESO
		temp += array[1] + ",";// SEMESTRE_PROCESO
		temp += array[2] + ",";// RUT_DOCENTE
		temp += "'" + array[3] + "',";// MODULO
		temp += array[4] + ",";// SECCION
		temp += "'" + array[5] + "',";// ID_DIA
		temp += "'" + array[6] + "',";// DIA_LETRAS
		temp += array[7] + ",";// ID_MODULO
		temp += "'" + array[8] + "',";// HORARIO_MODULO
		temp += array[9] + ",";// DURACION_MODULO
		temp += "'" + array[10] + "',";// JORNADA_MODULO
		temp += array[11] + ",";// DSAL_CLASE
		temp += "'" + array[12] + "',";// FECHA_OCUPADA
		temp += "'" + array[13] + "',";// FECHA_LIBERADA
		temp += "'" + array[14] + "',";// SALA
		temp += "'" + array[15] + "',";// COD_EDIFICIO
		temp += "'" + array[16] + "',";// NOM_EDIFICIO
		temp += array[17] + ",";// SEDE_EDIFICIO
		temp += "'" + array[18] + "',";// NOM_SALA
		temp += array[19] + ",";// CARRERA_DEL_MODULO
		temp += "'" + array[20] + "',";// MODALIDAD
		temp += "'" + array[21] + "',";// COD_ESCUELA
		temp += "'" + array[21] + "'";// NOM_ESCUELA
		return temp;
	}
}
