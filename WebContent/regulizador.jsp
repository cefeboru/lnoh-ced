<%@ page language="java" contentType="text/html;UTF-8" pageEncoding="UTF-8" %>

<%@ page import="org.tempuri.Index" %>
<%@ page import="java.util.List" %>
<%@ page import="blackboard.data.course.CourseMembership;" %>


<%@taglib uri="/bbData" prefix="bbData" %>
<%@ taglib uri="/bbNG" prefix="bbNG"%>

<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet" integrity="sha256-7s5uDGW3AHqw6xtJmNNtr+OBRJUlgkNJEo78P4b0yRw= sha512-nNo+yCHEyn0smMxSswnf/OnX6/KwJuZTlNZBjauKhTK0c+zT+q5JOCx0UFhXQ6rJR9jg6Es8gPuD2uZcYDLqSw=="  crossorigin="anonymous">

<style>
	thead tr th p, thead tr th{
		text-align:center;
		vertical-align:middle;
	}
	
	#containerdiv {
		width:100%;
	}
</style>

<bbNG:learningSystemPage ctxId="bbContext">
	<bbNG:breadcrumbBar environment="COURSE">
		<bbNG:breadcrumb>Regulizador de Asistencia</bbNG:breadcrumb>
		<bbNG:pageHeader>
			<bbNG:pageTitleBar title="Regulizar Asistencia">Regulizar Asistencia</bbNG:pageTitleBar>
			
        </bbNG:pageHeader>
	</bbNG:breadcrumbBar>
			<% Index in = new Index(bbContext); %>
			
			<form method="post" id="regulizarForm">
				<input type="hidden" name="jsonData">
			</form>
			<table class="table table-bordered table-hover">
				<thead><%= in.renderTableHeaders() %></thead>
				<tbody><%= in.renderTableBody() %></tbody>
			</table>
			<script>
				function dataToJson(){
					//TODO Convertir las selecciones a una cadena Json y enviarla por POST
				}
			</script>
</bbNG:learningSystemPage>