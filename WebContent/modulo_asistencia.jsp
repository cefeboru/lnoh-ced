<!DOCTYPE>
<%@ page language="java" contentType="text/html;UTF-8"
	pageEncoding="UTF-8"%>

<%@ page import="org.tempuri.Index"%>
<%@ page import="blackboard.data.user.User.SystemRole"%>
<%@ page import="blackboard.data.course.CourseMembership"%>
<%@ page import="blackboard.persist.course.CourseMembershipDbLoader"%>

<%@taglib uri="/bbData" prefix="bbData"%>
<link
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"
	rel="stylesheet"
	integrity="sha256-7s5uDGW3AHqw6xtJmNNtr+OBRJUlgkNJEo78P4b0yRw= sha512-nNo+yCHEyn0smMxSswnf/OnX6/KwJuZTlNZBjauKhTK0c+zT+q5JOCx0UFhXQ6rJR9jg6Es8gPuD2uZcYDLqSw=="
	crossorigin="anonymous">


<bbData:context id="ctx">
	<head>
<style>
	small{
		margin-right:5px;
	}

</style>
	</head>
	<body>
			<%
				CourseMembership cm = CourseMembershipDbLoader.Default.getInstance()
							.loadByCourseAndUserId(ctx.getCourseId(), ctx.getUserId());
					if (cm.getRole().getDbRole().getIdentifier().equalsIgnoreCase("Estudiante")) {
						Index in = new Index(ctx);
						out.print(in.renderWeeksStudent(ctx.getUser()));
					} else {
						out.print(
								"<p>Para Regulizar Asistencia dirigase hacia \"Herramientas del Curso > Regulizar Asistencia\"</p>");
					}
			%>
	</body>
</bbData:context>
