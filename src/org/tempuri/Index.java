package org.tempuri;

import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import blackboard.data.ValidationException;
import blackboard.data.course.CourseMembership;
import blackboard.data.user.User;
import blackboard.db.BbDatabase;
import blackboard.db.ConnectionManager;
import blackboard.db.ConnectionNotAvailableException;
import blackboard.persist.DatabaseContainer;
import blackboard.persist.course.CourseMembershipDbLoader;
import blackboard.persist.user.UserDbLoader;
import blackboard.platform.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import blackboard.platform.email.BbMail;
import blackboard.platform.email.BbMailManagerFactory;

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
	private ArrayList<Integer> errorCodes = new ArrayList<Integer>(Arrays.asList(4, 5, 7, 8, 9, 11, 12, 16));

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
		int endIndex = this.getCourseId().lastIndexOf("-");
		Response response = cedProxy.registrarAsistencia(this.getCourseId().substring(0, endIndex), user.getStudentId(),
				fechaAsistencia, this.getToken());
		int codigo = response.getCodigo();

		System.out.println("WS response: " + response.getMensaje() + " WS code: " + response.getCodigo() + "Date: "
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

	public int registrarAsistencia(Date asistance_date, String rut) throws Exception {

		// Format Date
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fechaAsistencia = sdf.format(asistance_date);
		// Call the WebService method registrarAsistencia
		int endIndex = this.getCourseId().lastIndexOf("-");
		Response response = cedProxy.registrarAsistencia(getCourseId().substring(0, endIndex), rut, fechaAsistencia,
				this.getToken());
		int codigo = response.getCodigo();
		String responseMessage = response.getMensaje();
		System.out.println("WS response: " + response.getMensaje() + " WS code: " + response.getCodigo() + " Date: "
				+ fechaAsistencia);
		// Get the BlackBoard DATABASE CONNECTION
		ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
		Connection conn = cManager.getConnection();

		String queryString = "";
		Date currentTime = new Date(System.currentTimeMillis());
		if (BbDatabase.getDefaultInstance().isOracle()) {
			queryString = "insert into lnoh_ced_response VALUES (LNOH_CED_RESPONSE_SEQ.nextVal, '" + rut + "','"
					+ this.getCourseId() + "'," + fechaAsistencia + "," + (currentTime.getTime() / 1000L) + ","
					+ response.getCodigo() + ")";
		} else {
			queryString = "insert into lnoh_ced_response VALUES (nextval('lnoh_ced_response_seq'), '" + rut + "','"
					+ this.getCourseId() + "'," + fechaAsistencia + "," + (asistance_date.getTime() / 1000L) + ","
					+ response.getCodigo() + ")";
		}

		PreparedStatement query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
		query.execute();

		// WEB SERVICE ERROR HANDLING
		if (errorCodes.contains(codigo)) {
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			Date startOfDay = c.getTime();

			c.set(Calendar.HOUR_OF_DAY, 23);
			c.set(Calendar.MINUTE, 59);
			c.set(Calendar.SECOND, 59);
			Date endOfDay = c.getTime();
			
			String Query = "";
			if(codigo == 4){
				Query = "SELECT COUNT(*) FROM LNOH_CED_RESPONSE WHERE codigo="
						+ codigo + "AND ID_CURSO='"+this.getCourseId()+"' AND (fecha_ws BETWEEN " + startOfDay.getTime() / 1000L + " AND "
						+ endOfDay.getTime() / 1000L + ")";
			} else {
				Query = "SELECT COUNT(*) FROM LNOH_CED_RESPONSE WHERE rut_estudiante='" + rut + "' AND codigo="
						+ codigo + "AND ID_CURSO='"+this.getCourseId()+"' AND (fecha_ws BETWEEN " + startOfDay.getTime() / 1000L + " AND "
						+ endOfDay.getTime() / 1000L + ")";
			}
			
			ResultSet rs = conn.createStatement().executeQuery(Query);
			if (rs.next()) {
				int count = rs.getInt(1);
				System.out.println("Will try to Mail Exception - cId: " + this.getCourseId() + " - "
						+ " errors this day: " + count);
				if (count <= 1) {
					sendExceptionEmail(asistance_date, responseMessage, codigo, currentTime);
				}
			}
		}

		query.close();
		query = null;
		conn.close();
		conn = null;
		cManager.close();
		cManager = null;
		return codigo;
	}

	private void sendExceptionEmail(Date fechaAsistencia1, String responseMessage, int responseCode,
			Date currentTime) throws ParseException {
		String emailBody = "<html xmlns=\"http://www.w3.org/1999/xhtml\"> <head> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/> <title>Documento sin título</title> <style>body{background: #FFFFFF; color: #333333; font-family: Tahoma, Geneva, sans-serif; font-size: 14px; width: 800px; margin: 50px 40px 40px 60px;}p{text-align: justify;}.pie{text-align: center; font-size: 11px; color: #969696;}.tabla{border: thin solid #666666; border-collapse: collapse;}.tabla th{background-color: #A2A983;}</style> </head> <body> <p> El sistema de registro de Asistencia en la <b>Carpeta Electr&oacute;nica Docente</b> para cursos en la modalidad <b>Semipresencial</b> ha detectado un error : </p><table width=\"800\" border=\"1\" align=\"center\" class=\"tabla\" cellpadding=\"0\" cellspacing=\"0\"> <tr align=\"center\"> <th>COD.</th> <th>MENSAJE</th> <th>CATEGOR&Iacute;A</th> <th>FECHA ASIST.</th> <th>FECHA WS</th> </tr><tr align=\"center\"> <td>%dato1</td><td>%dato2</td><td>%dato3</td><td>%dato6</td><td>%dato7</td></tr></table> <p class=\"pie\"> Direcci&oacute;n de Tecnolog&iacute;as Educativas AIEP. </p></body> </html>";
		String Categoria = "";
		if (responseCode == 4)
			Categoria = "Usuario";
		else
			Categoria = "Curso";
		emailBody = emailBody.replace("%dato1", String.valueOf(responseCode));
		emailBody = emailBody.replace("%dato2", responseMessage);
		emailBody = emailBody.replace("%dato3", Categoria);
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fecha_asistencia = new SimpleDateFormat("dd/MM/yyyy").format(fechaAsistencia1);
		emailBody = emailBody.replace("%dato6", fecha_asistencia);
		emailBody = emailBody.replace("%dato7", new SimpleDateFormat("dd/MM/yyyy").format(currentTime));

		BbMail mail = BbMailManagerFactory.getInstance().createMessage();
		mail.setFrom("no-reply@blackboard.com");
		mail.setSubject("ERROR EN CARPETA ELECTRONICA DOCENTE");
		mail.setBody(emailBody);
		mail.addTo("cesar.bonilla@laureate.net");
		mail.addTo("Francisco.Vergara@aiep.cl");
		mail.addTo("Jose.Carcamo@aiep.cl");
		mail.doNotBccSender();

		try {
			mail.send();
		} catch (UnsupportedEncodingException e) {
			System.out.println("UnsupportedEncodingException : " + e.getMessage());
			e.printStackTrace();
		} catch (MessagingException e) {
			System.out.println("MessagingException : " + e.getMessage());
			e.printStackTrace();
		} catch (ValidationException e) { // TODO Auto-generated catch block
			System.out.println("ValidationException : " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void regulizarAsistencia(String recievedData) throws Exception {
		String[] data = recievedData.split(",");
		for (int i = 0; i < data.length; i++) {
			String rut = data[i].split(":")[0];
			int semana = Integer.valueOf(data[i].split(":")[1]);
			Calendar cal = Calendar.getInstance();
			cal.setTime(new SimpleDateFormat("dd/MM/yyyy").parse(this.Semanas[semana - 1][0]));

			for (int j = 0; j < diasProgramados.length; j++) {
				cal.add(Calendar.DAY_OF_WEEK, diasProgramados[j] - 1);
				System.out.println("Regulizando Asistencia para " + cal.getTime() + " con rut: " + rut);
				int codigo = this.registrarAsistencia(cal.getTime(), rut);
				cal.setTime(new SimpleDateFormat("dd/MM/yyyy").parse(this.Semanas[semana - 1][0]));
				if (codigo == 0 || codigo == 10) {
					continue;
				}
				// WebService Exception Handling
				if (codigo == 4) {
					throw new Exception("ERROR 4 - Alumno no econtrado");
				} else if (codigo == 5) {
					throw new Exception("ERROR 5 - Módulo no encontrado");
				} else if (codigo == 7) {
					throw new Exception("ERROR 7 - Fecha invalida");
				} else if (codigo == 8) {
					throw new Exception("ERROR 8 - Fecha festiva");
				} else if (codigo == 9) {
					throw new Exception(
							"ERROR 9 - Módulo-sección sin programación para la fecha indicada o regularizada con ausencia del docente");
				} else if (codigo == 11) {
					throw new Exception("ERROR 11 - Sección no encontrada");
				} else if (codigo == 12) {
					throw new Exception("ERROR 12 - Módulo se encuentra finalizado");
				} else {
					throw new Exception("ERROR Indefinido");
				}
			}
		}
	}

	public void calculateWeeks() throws Exception {
		String queryString = "select * from course_main WHERE course_id='" + this.getCourseId() + "'";
		ConnectionManager cManager = BbDatabase.getDefaultInstance().getConnectionManager();
		Connection conn = cManager.getConnection();

		// Pre-compile the Query
		PreparedStatement query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
		ResultSet resultSet = query.executeQuery();
		String fecha_inicio = "";
		String fecha_fin = "";
		if (resultSet.next()) {
			fecha_inicio = resultSet.getString("START_DATE");
			fecha_fin = resultSet.getString("END_DATE");
		} else {
			throw new Exception("No se encontro el modulo");
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
				+ this.getSemestre()
				+ " AND (nom_edificio LIKE '%VIRTUAL%' OR nom_sala LIKE '%VIRTUAL%' OR nom_edificio LIKE '%ONL%')";

		Statement sql = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		resultSet = sql.executeQuery(queryString);
		if (resultSet.last()) {
			this.diasProgramados = new int[resultSet.getRow()];
			resultSet.beforeFirst();
			System.out.println("Dias online programados para " + this.getCourseId() + ": " + diasProgramados.length);
			while (resultSet.next()) {
				diasProgramados[resultSet.getRow() - 1] = resultSet.getInt(1);
			}
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
					estado = "<td><font color=\"green\">Asistencia Registrada</font></td>";
				} else {
					// Intentar Registrar Asistencia
					Date now = Calendar.getInstance().getTime();

					if (now.compareTo(inicio_semana) >= 0 && now.compareTo(fin_semana) <= 0) {
						System.out.println("Registrando asistencia para la semana " + (i + 1));
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
			CourseMembership cm = CourseMembershipDbLoader.Default.getInstance()
					.loadByCourseAndUserId(this.ctx.getCourseId(), currentUser.getId());

			if (cm.getRole().getDbRole().getIdentifier().equalsIgnoreCase("Estudiante")) {
				String cell = "<td>" + currentUser.getGivenName() + " " + currentUser.getFamilyName() + "</td>";

				// TODO Render Week Checkbox Cells
				// String log = "";
				// String queryString = "";
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

					// queryString += "Semana " + (j + 1) + " Query: " +
					// queryStringTemp + "\n";

					PreparedStatement query = conn.prepareStatement(queryStringTemp, Statement.NO_GENERATED_KEYS);
					ResultSet rSet = query.executeQuery();
					if (rSet.next()) {
						// log += "<p>Semana " + (j+1) + ": COUNT: " + temp +
						// "</p>";
						int count = rSet.getInt(1);
						if (count > 0) {
							String weekCell = "<td><img width=\"15\" src=\"Resources/check.png\"></td>";
							cell += weekCell;
						} else {
							String weekCell = "<td><input type=\"checkbox\" week=\"" + (j + 1) + "\" ></td>";
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