<%@ page language="java" contentType="text/html;UTF-8"
	pageEncoding="UTF-8"%>

<%@ page import="org.tempuri.settingsController"%>


<%@ taglib uri="/bbData" prefix="bbData"%>
<%@ taglib uri="/bbNG" prefix="bbNG"%>


<bbNG:learningSystemPage ctxId="bbContext">
	<bbNG:breadcrumbBar environment="CTRL_PANEL ">
		<bbNG:breadcrumb>Configuración CED</bbNG:breadcrumb>
		<bbNG:pageHeader>
			<bbNG:pageTitleBar title="Regularizar Asistencia">Configuración CED</bbNG:pageTitleBar>
		</bbNG:pageHeader>
	</bbNG:breadcrumbBar>
	<% settingsController controller = new settingsController(bbContext); %>
	<%
		if(!controller.message.isEmpty()) {
			out.print(controller.message);
		}
	%>
	<form method="POST">
		Web Service URL:<br>
		<input type="text" name="ws_url" id="ws_url" value="<% out.print(controller.getURL()); %>">
		<input type="submit" name="submit_form" value="Guardar">
	</form>	
</bbNG:learningSystemPage>