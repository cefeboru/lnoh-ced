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
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.persist.course.CourseMembershipDbLoader;
import blackboard.persist.user.UserDbLoader;
import blackboard.platform.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;


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
	private Date endOfCourse;
	private ArrayList<String[]> semanasList = new ArrayList<String[]>();
	private ArrayList<int[]> diasProgramadosSemana = new ArrayList<int[]>();
	private ArrayList<Integer> errorCodes = new ArrayList<Integer>(Arrays.asList(4, 5, 7, 8, 9, 11, 12, 16));

	public String flags = "";

	public Index(Context bbContext){

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
		Response r;
		try {
			r = cedProxy.loginMoodle("moodle", "ET33OI8994FAQ351P");
			this.token = r.getMensaje();
			if (ctx.getRequest().getMethod() == "POST") {
				regulizarAsistencia(ctx.getRequest().getParameter("jsonData"));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public int registrarAsistencia(Date asistance_date, String rut,Connection conn) {

		// Format Date
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fechaAsistencia = sdf.format(asistance_date);
		// Call the WebService method registrarAsistencia
		int endIndex = this.getCourseId().lastIndexOf("-");
		Response response;
		PreparedStatement preparedQuery = null;
		PreparedStatement query = null;
		ResultSet rs = null;
		
		int codigo = -1;
		try {
			String courseId = getCourseId().substring(0, endIndex);
			//CALL TO WebService
			response = cedProxy.registrarAsistencia(courseId, rut, fechaAsistencia,this.getToken());
			
			//GET RESPONSE CODE FORM WS
			codigo = response.getCodigo();

			String queryString = "";
			//INSERT QUERY
			queryString = "INSERT INTO lnoh_ced_response VALUES (LNOH_CED_RESPONSE_SEQ.nextVal,?,?,?,?,?)";

			//SET QUERY PARAMETERS
			query = conn.prepareStatement(queryString, Statement.NO_GENERATED_KEYS);
			query.setString(1, rut);
			query.setString(2, getCourseId());
			query.setInt(3, (int)(asistance_date.getTime() / 1000L));
			query.setInt(4, (int)(System.currentTimeMillis() / 1000L));
			query.setInt(5, codigo);
			
			//INSERT VALUES
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
				// Manejar errores de CED
				if (codigo == 4) {
					// Error de tipo Usuario
					Query = "SELECT COUNT(*) FROM LNOH_CED_RESPONSE WHERE codigo=? AND ID_CURSO=? AND (fecha_ws BETWEEN ? AND ?)";
					preparedQuery = conn.prepareStatement(Query);
					preparedQuery.setInt(1, codigo);
					preparedQuery.setString(2, getCourseId());
					preparedQuery.setInt(3, (int)(startOfDay.getTime() / 1000L));
					preparedQuery.setInt(4, (int)(endOfDay.getTime() / 1000L));
					
					rs = preparedQuery.executeQuery();
					if (rs.next()) {
						// Si se registro un error, se enviara un correo
						int count = rs.getInt(1);
						if (count <= 1) {
							try {
								sendExceptionEmail(asistance_date, response.getMensaje(), codigo,new Date(System.currentTimeMillis()) );
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
				}

				
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}  catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if(preparedQuery != null)
					preparedQuery.close();
				if(query != null)
					query.close();
				if(rs != null)
					rs.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return codigo;
	}

	private void sendExceptionEmail(Date fechaAsistencia1, String responseMessage, int responseCode, Date currentTime)
			throws ParseException {
		String emailBody = "<html xmlns=\"http://www.w3.org/1999/xhtml\"> <head> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/> <title>Documento sin t�tulo</title> <style>body{background: #FFFFFF; color: #333333; font-family: Tahoma, Geneva, sans-serif; font-size: 14px; width: 800px; margin: 50px 40px 40px 60px;}p{text-align: justify;}.pie{text-align: center; font-size: 11px; color: #969696;}.tabla{border: thin solid #666666; border-collapse: collapse;}.tabla th{background-color: #A2A983;}</style> </head> <body> <p> El sistema de registro de Asistencia en la <b>Carpeta Electr&oacute;nica Docente</b> para cursos en la modalidad <b>Semipresencial</b> ha detectado un error : </p><table width=\"800\" border=\"1\" align=\"center\" class=\"tabla\" cellpadding=\"0\" cellspacing=\"0\"> <tr align=\"center\"> <th>COD.</th> <th>MENSAJE</th> <th>CATEGOR&Iacute;A</th> <th>FECHA ASIST.</th> <th>FECHA WS</th> </tr><tr align=\"center\"> <td>%dato1</td><td>%dato2</td><td>%dato3</td><td>%dato6</td><td>%dato7</td></tr></table> <p class=\"pie\"> Direcci&oacute;n de Tecnolog&iacute;as Educativas AIEP. </p></body> </html>";
		String Categoria = "";
		if (responseCode == 4)
			Categoria = "Usuario";
		else
			Categoria = "Curso";
		emailBody = emailBody.replace("%dato1", String.valueOf(responseCode));
		emailBody = emailBody.replace("%dato2", responseMessage);
		emailBody = emailBody.replace("%dato3", Categoria);
		//SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String fecha_asistencia = new SimpleDateFormat("dd/MM/yyyy").format(fechaAsistencia1);
		emailBody = emailBody.replace("%dato6", fecha_asistencia);
		emailBody = emailBody.replace("%dato7", new SimpleDateFormat("dd/MM/yyyy").format(currentTime));

		BbMail mail = BbMailManagerFactory.getInstance().createMessage();
		mail.setFrom("no-reply@blackboard.com");
		mail.setSubject("ERROR EN CARPETA ELECTRONICA DOCENTE");
		mail.setBody(emailBody);
		mail.addTo("cesar.bonilla@laureate.net");
		mail.addTo("francisco.vergara@aiep.cl");
		mail.addTo("dte@aiep.cl");
		mail.addTo("soporte@soporte.aiep.cl");
		mail.addTo("jose.Carcamo@aiep.cl");
		mail.doNotBccSender();

		try {
			mail.send();
		} catch (UnsupportedEncodingException e) {
			System.out.println("UnsupportedEncodingException : " + e.getMessage());
			e.printStackTrace();
		} catch (MessagingException e) {
			System.out.println("MessagingException : " + e.getMessage());
			e.printStackTrace();
		} catch (ValidationException e) {
			System.out.println("ValidationException : " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void regulizarAsistencia(String recievedData) throws ParseException {
		String[] data = recievedData.split(",");
		
		ConnectionManager cManager = null;
		Connection conn = null;
		
		cManager = BbDatabase.getDefaultInstance().getConnectionManager();

		try{
			conn = cManager.getConnection();
			for (int i = 0; i < data.length; i++) {
				String rut = data[i].split(":")[0];
				int semana = Integer.valueOf(data[i].split(":")[1]);
				Calendar cal = Calendar.getInstance();

				if (diasProgramadosSemana.size() == 0) {
					System.out.println("No se encontraron clases programadas para el modulo " + this.getCourseId());
				}
				int[] diasProgramados = diasProgramadosSemana.get(semana - 1);
				for (int j = 0; j < diasProgramados.length; j++) {
					cal.setTime(new SimpleDateFormat("dd/MM/yyyy").parse(semanasList.get(semana - 1)[0]));
					cal.add(Calendar.DAY_OF_WEEK, diasProgramados[j] - 1);
					int codigo = this.registrarAsistencia(cal.getTime(), rut,conn);

					if (codigo == 0 || codigo == 10) {
						continue;
					}

					// WebService Exception Handling
					if (codigo == 4) {
						throw new Exception("ERROR 4 - Alumno no econtrado");
					} else if (codigo == 5) {
						throw new Exception("ERROR 5 - M�dulo no encontrado");
					} else if (codigo == 7) {
						throw new Exception("ERROR 7 - Fecha invalida");
					} else if (codigo == 8) {
						throw new Exception("ERROR 8 - Fecha festiva");
					} else if (codigo == 9) {
						throw new Exception(
								"ERROR 9 - M�dulo-secci�n sin programaci�n para la fecha indicada o regularizada con ausencia del docente");
					} else if (codigo == 11) {
						throw new Exception("ERROR 11 - Secci�n no encontrada");
					} else if (codigo == 12) {
						throw new Exception("ERROR 12 - M�dulo se encuentra finalizado");
					} else {
						throw new Exception("ERROR Indefinido");
					}
				}
			}
		} catch(ParseException ex) {
			ex.printStackTrace();
		} catch (ConnectionNotAvailableException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				cManager.releaseConnection(conn);
				conn.close();
				conn = null;
				cManager = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}

	public void calculateWeeks() {
		StringBuilder queryBuilder = new StringBuilder();
		
		queryBuilder.append("SELECT MIN(TO_DATE(REGEXP_SUBSTR(FECHA_OCUPADA, '^.{10}'),'yyyy-MM-dd')) \"START_DATE\", ");
		queryBuilder.append("MAX(TO_DATE(REGEXP_SUBSTR(FECHA_LIBERADA, '^.{10}'),'yyyy-MM-dd')) \"END_DATE\"");
		queryBuilder.append("FROM lnoh_horario_docente WHERE modulo=? AND SECCION = ? ");
		queryBuilder.append("AND ano_proceso=? AND semestre_proceso=?");
		
		/*
		SELECT MIN(TO_DATE(REGEXP_SUBSTR(FECHA_OCUPADA, '^.{10}'),'yyyy-MM-dd')) "START_DATE",
		MAX(TO_DATE(REGEXP_SUBSTR(FECHA_LIBERADA, '^.{10}'),'yyyy-MM-dd')) "END_DATE"
		FROM lnoh_horario_docente WHERE modulo=? AND SECCION = ?
		AND ano_proceso=? AND semestre_proceso=?
		*/

		ConnectionManager cManager = null;
		Connection conn = null;
		PreparedStatement preparedQuery = null;
		ResultSet resultSet = null;
		try {
			cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			conn = cManager.getConnection();
			preparedQuery = conn.prepareStatement(queryBuilder.toString());
			preparedQuery.setString(1, getModulo());
			preparedQuery.setInt(2, Integer.valueOf(getSeccion()));
			preparedQuery.setInt(3, Integer.valueOf(getAnio()));
			preparedQuery.setInt(4, Integer.valueOf(getSemestre()));
			
			resultSet = preparedQuery.executeQuery();
			String fecha_inicio = "";
			String fecha_fin = "";
			if (resultSet.next()) {
				fecha_inicio = resultSet.getString("START_DATE");
				fecha_fin = resultSet.getString("END_DATE");
			} else {
				System.out.println("No se encontro el modulo - " + this.getCourseId());
			}

			Date date_i = new SimpleDateFormat("yyyy-M-d").parse(fecha_inicio);
			Date date_f = new SimpleDateFormat("yyyy-M-d").parse(fecha_fin);
			this.endOfCourse = date_f;

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
				String startOfWeek = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
				cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				String endOfWeek = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
				cal.add(Calendar.DAY_OF_WEEK, 1);
				semanasList.add(new String[] { startOfWeek, endOfWeek });

			}
			
			String queryString = " SELECT DISTINCT(id_dia) FROM lnoh_horario_docente WHERE modulo= ? AND seccion= ? AND ano_proceso= ? "
					+ "AND semestre_proceso= ? AND (nom_edificio LIKE '%VIRTUAL%' "
					+ "OR nom_sala LIKE '%VIRTUAL%' OR nom_edificio LIKE '%ONL%')";

			//System.out.println("calculateWeeks: ID DIAS Query: " + queryString);

			preparedQuery = conn.prepareStatement(queryString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			preparedQuery.setString(1, getModulo());
			preparedQuery.setInt(2, Integer.valueOf(getSeccion()));
			preparedQuery.setInt(3, Integer.valueOf(getAnio()));
			preparedQuery.setInt(4, Integer.valueOf(getSemestre()));
			
			resultSet = preparedQuery.executeQuery();
			int[] diasProgramados = null;

			if (resultSet.last())  {
				diasProgramados = new int[resultSet.getRow()];
				resultSet.beforeFirst();
				while (resultSet.next()) {
					diasProgramados[resultSet.getRow() - 1] = resultSet.getInt(1);
				}
			} else {
				diasProgramados = new int[0];
			}

			String[] lastWeekArray = semanasList.get(semanasList.size() - 1);
			Date lastWeekStart = new SimpleDateFormat("dd/MM/yyyy").parse(lastWeekArray[0]);

			for (int i = 0; i < this.semanasList.size(); i++) {
				diasProgramadosSemana.add(diasProgramados);
			}

			ArrayList<Integer> clasesUltimaSemana = new ArrayList<Integer>();
			for (int j = 0; j < diasProgramados.length; j++) {
				Calendar tempCalendar = Calendar.getInstance();
				tempCalendar.setTime(lastWeekStart);
				tempCalendar.add(Calendar.DAY_OF_WEEK, diasProgramados[j] - 1);
				if (tempCalendar.getTime().compareTo(this.endOfCourse) <= 0) {
					clasesUltimaSemana.add(diasProgramados[j]);
				}
			}
			
			Object tempArray[] = clasesUltimaSemana.toArray();
			int diasUltimasemana[] = new int[tempArray.length];
			
			for(int i=0;i < tempArray.length ; i++) {
				diasUltimasemana[i] = Integer.valueOf(String.valueOf(tempArray[i]));
			}
			
			this.diasProgramadosSemana.set(diasProgramadosSemana.size()-1,diasUltimasemana);
			
		} catch (ConnectionNotAvailableException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception ex){
			ex.printStackTrace();
		} finally {
			try {
				if(conn != null) {
					cManager.releaseConnection(conn);
					conn.close();
					conn = null;
					cManager = null;
				}
				if(preparedQuery != null) {
					preparedQuery.close();
					preparedQuery = null;
				}
				if(resultSet != null) {
					resultSet.close();
					resultSet = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public String renderWeeksStudent(User user) {
		ArrayList<String> weeksItems = new ArrayList<String>();
		ConnectionManager cManager = null;
		Connection conn = null;
		String columns = "";
		ResultSet rSet = null;
		PreparedStatement preparedQuery = null;
		
		try {
			cManager = BbDatabase.getDefaultInstance().getConnectionManager();
			conn = cManager.getConnection();
			
			for (int i = 0; i < semanasList.size(); i++) {
				Date inicio_semana = new SimpleDateFormat("dd/MM/yyyy").parse(semanasList.get(i)[0]);
				Date fin_semana = new SimpleDateFormat("dd/MM/yyyy").parse(semanasList.get(i)[1]);
				
				StringBuilder queryString = new StringBuilder();
				queryString.append("SELECT Count(*) FROM lnoh_ced_response ");
				queryString.append("WHERE rut_estudiante=? and id_curso=? AND codigo IN(0,10) ");
				queryString.append("AND fecha_asistencia BETWEEN ? AND ? ");
				
				preparedQuery = conn.prepareStatement(queryString.toString());
				preparedQuery.setString(1, user.getStudentId());
				preparedQuery.setString(2, getCourseId());
				preparedQuery.setInt(3, (int)(inicio_semana.getTime() / 1000L));
				preparedQuery.setInt(4, (int)(fin_semana.getTime() / 1000L));
				
				rSet = preparedQuery.executeQuery();

				String estado = "<img width=\"20\" src=\"Resources/cross.png\">";
				if (rSet.next()) {
					int count = rSet.getInt(1);
					if (count > 0) {
						estado = "<img width=\"20\" src=\"Resources/check.png\">";
					} else {
						// Intentar Registrar Asistencia
						Date now = Calendar.getInstance().getTime();

						if (now.compareTo(inicio_semana) >= 0 && now.compareTo(fin_semana) <= 0) {
							Calendar tempCal = Calendar.getInstance();

							for (int j = 0; j < diasProgramadosSemana.get(i).length; j++) {
								tempCal.setTime(inicio_semana);
								int dia = diasProgramadosSemana.get(i)[j];
								tempCal.add(Calendar.DATE, dia - 1);
								
								System.out.println("Registrando asistencia para el dia:  "+tempCal.getTime());
								int code = registrarAsistencia(tempCal.getTime(), user.getStudentId(),conn);
								if (code == 0 || code == 10) {
									estado = "<img width=\"20\" src=\"Resources/check.png\">";
								}
							}
						}
					}
				} else {
					System.out.println("No se encontro registros para la semana del " + inicio_semana.toString()+" al "+fin_semana+" - CourseId: "+this.getCourseId());
				}
				// HTML CODE
				String temp = "";
				temp = "<h4> Semana " + (i + 1) + " <small> (" + semanasList.get(i)[0] + " - " + semanasList.get(i)[1]
						+ ")</small>" + estado + "</h4>";
				temp = "<li class=\"list-group-item\">" + temp + "</li>";
				weeksItems.add(temp);
			} // ENDFOR
			
			String columnContent = "";
			for (int i = 0; i < weeksItems.size(); i++) {
				columnContent = columnContent + weeksItems.get(i);
				if (i % 5 == 0 && i > 0) {
					columns += "<div style=\"width:360px; display:inline-block; vertical-align:top;\">" + columnContent
							+ "</div>";
					columnContent = "";
				}
			}
			if (columns == "" || columnContent != "") {
				columns += "<div style=\"width:360px; display:inline-block; vertical-align:top;\">" + columnContent
						+ "</div>";
			}
		} catch (ConnectionNotAvailableException e) {
			e.printStackTrace();
			System.out.println("CED ERROR - " + e.getFullMessageTrace());
		} catch (ParseException e) {
			System.out.println("CED ERROR - " + e.getStackTrace().toString());
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("CED ERROR - " + e.getStackTrace().toString());
			e.printStackTrace();
		} catch (Exception ex){
			System.out.println("CED ERROR - " + ex.getStackTrace().toString());
			ex.printStackTrace();
		} 
		finally {
				try {
					if(rSet != null) {
						rSet.close();
						rSet = null;
					}
					
					if(preparedQuery != null){
						preparedQuery.close();
						preparedQuery = null;
					}
					
					if(conn != null) {
						cManager.releaseConnection(conn);
						conn.close();
						conn  = null;
						cManager = null;
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			
		}
		return columns;
	}

	public String renderTableHeaders() throws Exception {
		String row = "<th>Nombre</th>";
		for (int i = 0; i < semanasList.size(); i++) {
			String temp = "Semana " + (i + 1) + "<br>";
			temp += semanasList.get(i)[0] + "<br>";
			temp += semanasList.get(i)[1];
			temp = "<th>" + temp + "</th>";
			row += temp;
		}
		row = "<tr>" + row + "</tr>";
		return row;
	}

	public String renderTableBody() {

		List<User> users;
		String content = "";
		ConnectionManager cManager = null;
		Connection conn = null;
		PreparedStatement query = null;
		ResultSet rSet = null;
		
		try {
			users = UserDbLoader.Default.getInstance().loadByCourseId(ctx.getCourseId());
			for (int i = 0; i < users.size(); i++) {
				User currentUser = users.get(i);
				CourseMembership cm = CourseMembershipDbLoader.Default.getInstance()
						.loadByCourseAndUserId(this.ctx.getCourseId(), currentUser.getId());

				if (cm.getRole().getDbRole().getIdentifier().equalsIgnoreCase("S")) {
					String cell = "<td>" + currentUser.getGivenName() + " " + currentUser.getFamilyName() + "</td>";

					cManager = BbDatabase.getDefaultInstance().getConnectionManager();
					conn = cManager.getConnection();
					query = null;
					rSet = null;

					for (int j = 0; j < semanasList.size(); j++) {

						Date inicio_semana = new SimpleDateFormat("dd/MM/yyyy").parse(semanasList.get(j)[0]);
						Date fin_semana = new SimpleDateFormat("dd/MM/yyyy").parse(semanasList.get(j)[1]);

						String queryStringTemp = "SELECT Count(*) FROM lnoh_ced_response WHERE rut_estudiante='"
								+ currentUser.getStudentId() + "' AND id_curso='" + this.getCourseId()
								+ "' AND codigo IN(0,10) AND fecha_asistencia BETWEEN " + (inicio_semana.getTime() / 1000L)
								+ " AND " + (fin_semana.getTime() / 1000L);

						query = conn.prepareStatement(queryStringTemp, Statement.NO_GENERATED_KEYS);
						rSet = query.executeQuery();
						if (rSet.next()) {
							int count = rSet.getInt(1);
							if (count > 0) {
								String weekCell = "<td><img width=\"15\" src=\"Resources/check.png\"></td>";
								cell += weekCell;
							} else {
								String weekCell = "<td><input type=\"checkbox\" week=\"" + (j + 1) + "\" ></td>";
								cell += weekCell;
							}
						}
					}
					
					content += "<tr rut=\"" + currentUser.getStudentId() + "\">" + cell + "</tr>";
				}
			}
		} catch (KeyNotFoundException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (ConnectionNotAvailableException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception ex){
			ex.printStackTrace();
		} finally {
			try {
				if(query != null)
					query.close();
				if(rSet != null)
					rSet.close();
				if(!conn.isClosed())
					conn.close();
				cManager.close();
			} catch (SQLException e) {
				e.printStackTrace();
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