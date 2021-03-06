<%@ page language="java" contentType="text/html;UTF-8"
	pageEncoding="UTF-8"%>

<%@ page import="org.tempuri.Index"%>
<%@ page import="java.util.List"%>
<%@ page import="blackboard.data.course.CourseMembership"%>
<%@ page import="blackboard.data.user.User.SystemRole" %>
<%@ page import="java.util.Map"%>


<%@ taglib uri="/bbData" prefix="bbData"%>
<%@ taglib uri="/bbNG" prefix="bbNG"%>

<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">

<style>
thead tr th p, thead tr th {
	text-align: center;
	vertical-align: middle;
}
#containerdiv {
	width: 100%;
}
</style>

<bbNG:learningSystemPage ctxId="bbContext">
	<bbNG:breadcrumbBar environment="CTRL_PANEL ">
		<bbNG:breadcrumb>Regularizador de Asistencia</bbNG:breadcrumb>
		<bbNG:pageHeader>
			<bbNG:pageTitleBar title="Regularizar Asistencia">Regularizar Asistencia</bbNG:pageTitleBar>

		</bbNG:pageHeader>
	</bbNG:breadcrumbBar>
	<% 
		if(!bbContext.getUser().getSystemRole().equals(SystemRole.SYSTEM_ADMIN)){
			throw new Exception("Regularizar asistencia solo es permitido por: \"Administradores\".");	
		}
		Index in = new Index(bbContext);
	%>

	<form method="post" id="regulizarForm">
		<input type="hidden" name="jsonData" id="jsonDataInput">
	</form>
	<table class="table table-bordered table-hover">
		<thead><%=in.renderTableHeaders()%></thead>
		<tbody><%=in.renderTableBody()%></tbody>
	</table>
	<button id="btnRegistrar" onclick="sendData()" type="button"
		class="btn btn-default">Registrar Asistencia</button>
	<script>
	document.getElementsByTagName("head")[0].innerHTML = document.getElementsByTagName("head")[0].innerHTML + '<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">'
	
	
		function sendData() {
			var form = document.getElementById("regulizarForm");
			if (form) {
				var input = document.getElementById("jsonDataInput");
				var checkboxes = $$("input[week]");
				var index = 0;
				for (var i = 0; i < checkboxes.length; i++) {
					if (checkboxes[i].checked) {
						index++;
						var rut = checkboxes[i].parentElement.parentElement
								.getAttribute("rut");
						var week = checkboxes[i].getAttribute("week");
						var inputElement = document
								.getElementById("jsonDataInput");
						inputElement.value += rut + ":" + week + ",";
					}
				}
				if(index != 0)
				form.submit();
			}
		}
	</script>
</bbNG:learningSystemPage>