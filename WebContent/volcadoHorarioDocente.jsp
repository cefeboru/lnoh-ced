<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="blackboard.db.BbDatabase"%>
<%@ page import="org.tempuri.VolcadoHorarioDocente"%>

<% 
	VolcadoHorarioDocente volcado = new VolcadoHorarioDocente();
	out.print(volcado.VolcadoHorarioDocente(request));
%>

