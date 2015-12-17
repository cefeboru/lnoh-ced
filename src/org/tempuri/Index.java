package org.tempuri;

import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import blackboard.base.BbList;
import blackboard.data.course.CourseMembership;
import blackboard.data.user.User;
import blackboard.db.BbDatabase;
import blackboard.db.ConnectionManager;
import blackboard.db.ConnectionNotAvailableException;
import blackboard.persist.DatabaseContainer;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.persist.course.CourseMembershipDbLoader;
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
	private Context ctx;
	private String [][] WeeksDates;
	
	
	
	public String flags = "";
	

	public Index(Context bbContext) {
		this.ctx = bbContext;
		// Establecer el contexto de Blackboard
		// Instanciar el WeService
		cedProxy = new WSAsistenciaSoapProxy();
		// Obtener ID del curso
		modulo = this.ctx.getCourse().getCourseId();
		setRut_estudiante(this.ctx.getUser().getStudentId());
		DatabaseContainer.getDefaultInstance().getBbDatabase().getConnectionManager().setTraceConnectionOpeners( true );
	}

	public String registrarAsistencia(Date date) throws SQLException, ConnectionNotAvailableException, RemoteException {
		String respuesta = "";

		// Call the WS method loginMoodle to get the Token
		token = getToken();

		// Date Format Recieved by the WebService
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fechaAsistencia = sdf.format(date);
		// Call the WebService method registrarAsistencia
		Response response = cedProxy.registrarAsistencia(getModulo(), rut_estudiante, fechaAsistencia, token);
		respuesta = response.getMensaje();

		// Get the BlackBoard DATABASE CONNECTION
		ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
		Connection conn = cManager.getConnection();

		String queryString = "";
		if (BbDatabase.getDefaultInstance().isOracle()) {
			queryString = "insert into lnoh_ced_response VALUES " + "(LNOH_CED_RESPONSE_SEQ.nextVal, '"
					+ this.getRut_estudiante() + "','" + this.getModulo() + "'," + fechaAsistencia + ","
					+ (date.getTime() / 1000L) + "," + response.getCodigo() + ")";
		} else {
			queryString = "insert into lnoh_ced_response VALUES " + "(nextval('lnoh_ced_response_seq'), '"
					+ this.getRut_estudiante() + "','" + this.getModulo() + "'," + fechaAsistencia + ","
					+ (date.getTime() / 1000L) + "," + response.getCodigo() + ")";
		}

		PreparedStatement query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
		query.executeQuery();

		query.close();
		query = null;
		conn.close();
		conn = null;
		cManager.close();
		cManager = null;
		return respuesta;
	}

	public String calcularSemanas() throws Exception {
		String queryString = "select * from lnoh_modulos_seccion WHERE cod_course_moodle='" + this.getModulo() + "'";
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
			flags += "<p>Course ID found in lnoh_modulos_seccion</p><br>";
		} else {
			throw new Exception("No se encontro el modulo - @Calcular Semanas");
		}

		String[][] Semanas;
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

		String html = "<table width=100% border=0> {content} <table>";
		String content = "";

		resultSet.close();
		resultSet = null;
		query.close();
		query = null;
		conn.close();
		conn = null;
		cManager.close();
		cManager = null;

		cal.setTime(date_i);
		for (int i = 0; i < weeks; i++) {
			// Initialize connection
			cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			conn = cManager.getConnection();
			// GET WEEK INITIAL AND FINAL DATE
			Date inicio_semana = cal.getTime();
			cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
			Date fin_semana = cal.getTime();
			// GET THE DATE LABELS TO SHOW
			Semanas[i][0] = new SimpleDateFormat("dd/MM/yyyy").format(inicio_semana);
			Semanas[i][1] = new SimpleDateFormat("dd/MM/yyyy").format(fin_semana);

			queryString = "SELECT Count(*) as COUNT FROM lnoh_ced_response WHERE rut_estudiante='"
					+ this.getRut_estudiante() + "' AND id_curso='" + this.getModulo()
					+ "' AND codigo IN(0,10) AND fecha_ws BETWEEN " + (inicio_semana.getTime() / 1000L) + " AND "
					+ (fin_semana.getTime() / 1000L);

			query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
			resultSet = query.executeQuery();

			String estado = "<font color=\"red\">Asistencia No Registrada</font>";
			if (resultSet.next()) {
				int count = resultSet.getInt(1);
				// content += "Week "+(i+1)+" count: " + count + "<br>";
				if (count > 0) {
					flags += "<p>Semana " + (i + 1) + "</p><br>";
					flags += "<p>Se encontro asistencia registrada</p><br>";
					estado = "<font color=\"green\">Asistencia Registrada</font>";
					;
				} else {
					Date now = Calendar.getInstance().getTime();
					// Evaluar si estamos en la semana i
					if ((now.compareTo(inicio_semana) >= 0) && (now.compareTo(fin_semana) <= 0)) {
						flags += "<p>now > fecha inicio</p><br>";
						// decompose the module on modulo, seccion , a�o and
						// semestre
						String[] mod_composers = this.getModulo().split("-");
						// GET the scheduled classes
						String tempQueryString = " SELECT DISTINCT(id_dia) FROM lnoh_horario_docente ORDER BY id_dia ASC WHERE modulo='"
								+ mod_composers[0] + "' AND seccion=" + mod_composers[1] + " AND ano_proceso="
								+ mod_composers[2] + " AND semestre_proceso=" + mod_composers[3]
								+ " AND (nom_edificio LIKE '%VIRTUAL%' OR nom_sala LIKE '%VIRTUAL%')";
						ResultSet temp = conn.createStatement().executeQuery(tempQueryString);

						while (temp.next()) {
							int dia = temp.getInt("1");
							if (dia == 1) {
								Calendar tempCal = Calendar.getInstance();
								tempCal.setTime(inicio_semana);
								this.registrarAsistencia(tempCal.getTime());
							} else if (dia == 7) {
								Calendar tempCal = Calendar.getInstance();
								tempCal.setTime(fin_semana);
								this.registrarAsistencia(tempCal.getTime());
							} else {
								Calendar tempCal = Calendar.getInstance();
								tempCal.setTime(inicio_semana);
								tempCal.add(Calendar.DAY_OF_WEEK, dia);
								this.registrarAsistencia(tempCal.getTime());
							}
						}
						temp.close();
						temp = null;
					}
				}
			}

			resultSet.close();
			resultSet = null;
			query.close();
			query = null;
			conn.close();
			conn = null;
			cManager.close();
			cManager = null;

			// Generate the HTML
			content += "<tr><td align=center><b>Semana " + (i + 1) + "</b></td></tr>";
			content += "<tr><td align=center>" + Semanas[i][0] + " - " + Semanas[i][1] + "</td></tr>";
			content += "<tr><td align=center>" + estado + "</td></tr>";

			// Set time to next week
			cal.add(Calendar.DATE, 1);
		} // END_FOR WEEKS

		html = html.replace("{content}", content);
		return html;
	}

	public String renderTableHeaders() throws Exception {
		String headers = "<th rowspan=\"3\"><b>Nombre</b></th>";
		// Open DB Conection
		String queryString = "select * from lnoh_modulos_seccion WHERE cod_course_moodle='" + this.getModulo() + "'";
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
			flags += "<p>Course ID found in lnoh_modulos_seccion</p><br>";
		} else {
			throw new Exception("No se encontro el modulo - @Calcular Semanas");
		}

		// Close DB conection
		resultSet.close();
		resultSet = null;
		query.close();
		query = null;
		conn.close();
		conn = null;
		cManager.close();
		cManager = null;

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
		
		cal.setTime(date_i);

		for (int i = 0; i < weeks; i++) {
			String tableHeader = "";
			tableHeader += "<p>Semana "+(i+1)+"</p>";
			String inicio_semana = new SimpleDateFormat("dd-MM-yyyy").format(cal.getTime());
			tableHeader += "<p>"+inicio_semana+"</p>";
			cal.set(Calendar.DAY_OF_MONTH, Calendar.SUNDAY);
			String fin_semana = new SimpleDateFormat("dd-MM-yyyy").format(cal.getTime());
			tableHeader += "<p>" + fin_semana + "</p>";
			cal.add(Calendar.DAY_OF_MONTH, 1);
			tableHeader = "<th>" + tableHeader + "</th>";
			headers += tableHeader;
		}
		headers = "<tr>" + headers + "<tr>";
		return headers;
	}
	
	public String renderTableBody() throws KeyNotFoundException, PersistenceException {
		
		List<User> users = UserDbLoader.Default.getInstance().loadByCourseId(ctx.getCourseId());
		String content = "";
		for(int i=0; i < enrolledUsers.size(); i++){
			String cell = "<td>" + enrolledUsers.get(i).toString() + "</td>";
			content += "<tr>" + cell + "</tr>";
		}
		return content;
	}

	public String getModulo() {
		return modulo;
	}

	public String getToken() {
		String temp;
		try {
			Response r = cedProxy.loginMoodle("Moodle", "ET33OI8994FAQ351P");
			temp = r.getMensaje();
		} catch (RemoteException e) {
			e.printStackTrace();
			temp = "Error Generating Token";
		}
		return temp;
	}

	public String getSeccion() {
		return seccion;
	}

	public void setSeccion(String seccion) {
		this.seccion = seccion;
	}

	public void setId_curso(String id_curso) {
		this.id_curso = id_curso;
	}

	public String getRut_estudiante() {
		return rut_estudiante;
	}

	public void setRut_estudiante(String rut_estudiante) {
		this.rut_estudiante = rut_estudiante;
	}

	public String getCourseID() {
		return id_curso;
	}
}
