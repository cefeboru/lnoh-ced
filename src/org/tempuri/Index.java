package org.tempuri;

import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import blackboard.data.user.User;
import blackboard.data.user.User.SystemRole;
import blackboard.db.BbDatabase;
import blackboard.db.ConnectionManager;
import blackboard.db.ConnectionNotAvailableException;
import blackboard.persist.DatabaseContainer;
import blackboard.persist.user.UserDbLoader;
import blackboard.platform.context.Context;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Index {

	private WSAsistenciaSoapProxy cedProxy;
	private String id_curso;
	private String rut_estudiante;
	private String seccion;
	private String token;
	private String modulo;
	private String anio;
	private String semestre;
	private Context ctx;
	private String[][] Semanas;
	private int[] diasProgramados;

	public String flags = "";

	public Index(Context bbContext) throws Exception {
		DatabaseContainer.getDefaultInstance().getBbDatabase().getConnectionManager().setTraceConnectionOpeners(true);

		this.ctx = bbContext;

		// Instanciar el WeService
		cedProxy = new WSAsistenciaSoapProxy();

		// Obtener ID del curso
		this.setCourseId(this.ctx.getCourse().getCourseId());
		String[] courseIdComposers = getCourseId().split("-");
		setModulo(courseIdComposers[0]);
		setSeccion(courseIdComposers[1]);
		setAnio(courseIdComposers[2]);
		setSemestre(courseIdComposers[3]);

		this.calculateWeeks();
		
		Response r = cedProxy.loginMoodle("Moodle", "ET33OI8994FAQ351P");
		this.token = r.getMensaje();
		
		if (ctx.getRequest().getMethod() == "POST") {
			regulizarAsistencia(ctx.getRequest().getParameter("jsonData"));
		}
	}

	public int registrarAsistencia(Date date, User user)
			throws SQLException, ConnectionNotAvailableException, RemoteException {

		// Format Date
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fechaAsistencia = sdf.format(date);
		// Call the WebService method registrarAsistencia
		Response response = cedProxy.registrarAsistencia(getCourseId(), user.getStudentId(), fechaAsistencia,
				this.getToken());
		int codigo = response.getCodigo();
		
		System.out.print("WS response: " + response.getMensaje() + " WS code: " + response.getCodigo() + "Date: "
				+ fechaAsistencia);
		// Get the BlackBoard DATABASE CONNECTION
		ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
		Connection conn = cManager.getConnection();

		String queryString = "";
		if (BbDatabase.getDefaultInstance().isOracle()) {
			queryString = "insert into lnoh_ced_response VALUES " + "(LNOH_CED_RESPONSE_SEQ.nextVal, '"
					+ user.getStudentId() + "','" + this.getCourseId() + "'," + fechaAsistencia + ","
					+ (date.getTime() / 1000L) + "," + response.getCodigo() + ")";
		} else {
			queryString = "insert into lnoh_ced_response VALUES " + "(nextval('lnoh_ced_response_seq'), '"
					+ user.getStudentId() + "','" + this.getCourseId() + "'," + fechaAsistencia + ","
					+ (date.getTime() / 1000L) + "," + response.getCodigo() + ")";
		}

		PreparedStatement query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
		query.execute();

		query.close();
		query = null;
		conn.close();
		conn = null;
		cManager.close();
		cManager = null;
		return codigo;
	}
	
	public int registrarAsistencia(Date date, String rut)
			throws SQLException, ConnectionNotAvailableException, RemoteException {

		// Format Date
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fechaAsistencia = sdf.format(date);
		// Call the WebService method registrarAsistencia
		Response response = cedProxy.registrarAsistencia(getCourseId(), rut, fechaAsistencia,
				this.getToken());
		int codigo = response.getCodigo();
		System.out.print("WS response: " + response.getMensaje() + " WS code: " + response.getCodigo() + "Date: "
				+ fechaAsistencia);
		// Get the BlackBoard DATABASE CONNECTION
		ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
		Connection conn = cManager.getConnection();

		String queryString = "";
		if (BbDatabase.getDefaultInstance().isOracle()) {
			queryString = "insert into lnoh_ced_response VALUES " + "(LNOH_CED_RESPONSE_SEQ.nextVal, '"
					+ rut + "','" + this.getCourseId() + "'," + fechaAsistencia + ","
					+ (date.getTime() / 1000L) + "," + response.getCodigo() + ")";
		} else {
			queryString = "insert into lnoh_ced_response VALUES " + "(nextval('lnoh_ced_response_seq'), '"
					+ rut + "','" + this.getCourseId() + "'," + fechaAsistencia + ","
					+ (date.getTime() / 1000L) + "," + response.getCodigo() + ")";
		}

		PreparedStatement query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
		query.execute();

		query.close();
		query = null;
		conn.close();
		conn = null;
		cManager.close();
		cManager = null;
		return codigo;
	}

	public void regulizarAsistencia(String recievedData) throws ParseException, ConnectionNotAvailableException, RemoteException, SQLException {
		String[] data = recievedData.split(",");
		for(int i=0; i< data.length ; i++){
			String rut = data[i].split(":")[0];
			int semana = Integer.valueOf(data[i].split(":")[1]);
			Calendar cal = Calendar.getInstance();
			cal.setTime(new SimpleDateFormat("dd/MM/yyyy").parse(this.Semanas[semana-1][0]));
			for(int j=0; j < diasProgramados.length; j++ ){
				cal.add(Calendar.DAY_OF_WEEK, diasProgramados[j]-1);
				this.registrarAsistencia(cal.getTime(), rut );
			}	
		}
	}

	public void calculateWeeks() throws Exception {
		String queryString = "select * from lnoh_modulos_seccion WHERE cod_course_moodle='" + this.getCourseId() + "'";
		ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
		Connection conn = cManager.getConnection();

		// Pre-compile the Query
		PreparedStatement query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
		ResultSet resultSet = query.executeQuery();
		String fecha_inicio = "";
		String fecha_fin = "";
		if (resultSet.next()) {
			fecha_inicio = resultSet.getString("fecha_ini");
			fecha_fin = resultSet.getString("fecha_fin");
		} else {
			throw new Exception("No se encontro el modulo - @Calcular Semanas");
		}

		Date date_i = new SimpleDateFormat("yyyy-M-d").parse(fecha_inicio);
		Date date_f = new SimpleDateFormat("yyyy-M-d").parse(fecha_fin);

		Calendar cal = Calendar.getInstance();
		cal.setTime(date_i);
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		date_i = cal.getTime();
		cal.setTime(date_f);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		date_f = cal.getTime();
		long delta = date_f.getTime() - date_i.getTime();
		delta = (delta / 1000L) / (60 * 60 * 24) + 1;
		int weeks = (int) (Math.round((delta / 7)));

		Semanas = new String[weeks][2];

		cal.setTime(date_i);
		for (int i = 0; i < weeks; i++) {
			Semanas[i][0] = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
			cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
			Semanas[i][1] = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
			cal.add(Calendar.DATE, 1);
		}

		queryString = " SELECT DISTINCT(id_dia) FROM lnoh_horario_docente WHERE modulo='" + this.getModulo()
				+ "' AND seccion=" + this.getSeccion() + " AND ano_proceso=" + this.getAnio() + " AND semestre_proceso="
				+ this.getSemestre() + " AND (nom_edificio LIKE '%VIRTUAL%' OR nom_sala LIKE '%VIRTUAL%')";

		Statement sql = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		resultSet = sql.executeQuery(queryString);
		if (resultSet.last()) {
			this.diasProgramados = new int[resultSet.getRow()];
			resultSet.beforeFirst();
		}

		while (resultSet.next()) {
			diasProgramados[resultSet.getRow() - 1] = resultSet.getInt(1);
		}

		resultSet.close();
		query.close();
		conn.close();
		cManager.close();

		cManager = null;
		resultSet = null;
		query = null;
		conn = null;
	}

	public String renderWeeksStudent(User user) throws Exception {
		String rows = "";
		for (int i = 0; i < Semanas.length; i++) {
			Date inicio_semana = new SimpleDateFormat("dd/MM/yyyy").parse(Semanas[i][0]);
			Date fin_semana = new SimpleDateFormat("dd/MM/yyyy").parse(Semanas[i][1]);

			// OPEN DB CONNECTION
			ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			Connection conn = cManager.getConnection();

			String queryString = "SELECT Count(*) FROM lnoh_ced_response WHERE rut_estudiante='" + user.getStudentId()
					+ "' AND id_curso='" + this.getCourseId() + "' AND codigo IN(0,10) AND fecha_ws BETWEEN "
					+ (inicio_semana.getTime() / 1000L) + " AND " + (fin_semana.getTime() / 1000L);
			ResultSet rSet = conn.prepareStatement(queryString).executeQuery();

			String estado = "<td><font color=\"red\">Asistencia No Registrada</font></td>";
			if (rSet.next()) {
				int count = rSet.getInt(1);
				if (count > 0) {
					System.out.print("Test");
					estado = "<td><font color=\"green\">Asistencia Registrada</font></td>";
				} else {
					// Intentar Registrar Asistencia
					Date now = Calendar.getInstance().getTime();

					if (now.compareTo(inicio_semana) >= 0 && now.compareTo(fin_semana) <= 0) {
						System.out.print("Registrando asistencia para la semana " + (i+1));
						// Por cada clase programada
						Calendar tempCal = Calendar.getInstance();
						tempCal.setTime(inicio_semana);
						for (int j = 0; j < diasProgramados.length; j++) {
							int dia = Integer.valueOf(diasProgramados[j]);
							tempCal.add(Calendar.DATE, dia - 1);
							int code = registrarAsistencia(tempCal.getTime(), user);
							if (code == 0 || code == 10) {
								estado = "<td><font color=\"green\">Asistencia Registrada</font></td>";
							}
						}
					}
				}
			}
			// CLOSE DB CONNECTION
			rSet.close();
			conn.close();
			cManager.close();
			// HTML CODE
			rows += "<tr><th>Semana " + (i + 1) + "</th></tr>";
			rows += "<tr><td>" + Semanas[i][0] + " - " + Semanas[i][1] + "</td></tr>";
			rows += "<tr>" + estado + "</tr>";
		} // ENDFOR

		return rows;
	}

	public String renderTableHeaders() throws Exception {
		String row = "<th>Nombre</th>";
		for (int i = 0; i < Semanas.length; i++) {
			String temp = "<p>Semana " + (i + 1) + "</p>";
			temp += "<p>" + Semanas[i][0] + "</p>";
			temp += "<p>" + Semanas[i][1] + "</p>";
			temp = "<th>" + temp + "</th>";
			row += temp;
		}
		row = "<tr>" + row + "</tr>";
		return row;
	}

	public String renderTableBody() throws Exception {

		List<User> users = UserDbLoader.Default.getInstance().loadByCourseId(ctx.getCourseId());
		String content = "";
		for (int i = 0; i < users.size(); i++) {
			User currentUser = users.get(i);
			if (currentUser.getSystemRole().equals(SystemRole.DEFAULT)) {
				String cell = "<td>" + currentUser.getGivenName() + " " + currentUser.getFamilyName() + "</td>";

				// TODO Render Week Checkbox Cells
				//String log = "";
				String queryString = "";
				for (int j = 0; j < Semanas.length; j++) {

					Date inicio_semana = new SimpleDateFormat("dd/MM/yyyy").parse(Semanas[j][0]);
					Date fin_semana = new SimpleDateFormat("dd/MM/yyyy").parse(Semanas[j][1]);
					// Initialize connection
					ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
					Connection conn = cManager.getConnection();
					
					String queryStringTemp = "SELECT Count(*) FROM lnoh_ced_response WHERE rut_estudiante='"
							+ currentUser.getStudentId() + "' AND id_curso='" + this.getCourseId()
							+ "' AND codigo IN(0,10) AND fecha_ws BETWEEN " + (inicio_semana.getTime() / 1000L)
							+ " AND " + (fin_semana.getTime() / 1000L);
					
					queryString += "Semana " +(j+1) + " Query: " + queryStringTemp + "\n";

					PreparedStatement query = conn.prepareStatement(queryStringTemp, Statement.NO_GENERATED_KEYS);
					ResultSet rSet = query.executeQuery();
					if (rSet.next()) {
						String temp = rSet.getString(1);
						//log += "<p>Semana " + (j+1) + ": COUNT: " + temp + "</p>";
						int count = rSet.getInt(1);
						if (count > 0) {
							String weekCell = "<td><img width=\"15\" src=\"Resources/check.png\"></td>";
							cell += weekCell;
						} else {
							String weekCell = "<td><input type=\"checkbox\" week=\"" + (j+1) + "\" ></td>";
							cell += weekCell;
						}
					}
					// Close DB Connection
					query.close();
					rSet.close();
					conn.close();
					cManager.close();
				}
				
				content += "<tr rut=\"" + currentUser.getStudentId() + "\">" + cell + "</tr>";
				//throw new Exception(log);
				//throw new Exception(queryString);
			}
		}
		return content;
	}

	public String getCourseId() {
		return this.id_curso;
	}

	public void setCourseId(String courseId) {
		this.id_curso = courseId;
	}

	public String getToken() {
		return token;
	}

	public String getSeccion() {
		return seccion;
	}

	public void setSeccion(String seccion) {
		this.seccion = seccion;
	}

	public String getStudentId() {
		return rut_estudiante;
	}

	public void setStudentId(String rut_estudiante) {
		this.rut_estudiante = rut_estudiante;
	}

	public String getModulo() {
		return modulo;
	}

	public void setModulo(String modulo) {
		this.modulo = modulo;
	}

	public String getAnio() {
		return anio;
	}

	public void setAnio(String anio) {
		this.anio = anio;
	}

	public String getSemestre() {
		return semestre;
	}

	public void setSemestre(String semestre) {
		this.semestre = semestre;
	}

}
