<!DOCTYPE>
<%@ page language="java" contentType="text/html;UTF-8"
	pageEncoding="UTF-8"%>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">

<%@ page import="org.tempuri.Index"%>
<%@ page import="blackboard.data.user.User.SystemRole"%>
<%@ page import="blackboard.data.course.CourseMembership"%>
<%@ page import="blackboard.persist.course.CourseMembershipDbLoader"%>

<%@taglib uri="/bbData" prefix="bbData"%>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">


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
				
			if(!ctx.getUser().getSystemRole().equals(SystemRole.SYSTEM_ADMIN)) {
				CourseMembership cm = CourseMembershipDbLoader.Default.getInstance()
						.loadByCourseAndUserId(ctx.getCourseId(), ctx.getUserId());
				if (cm.getRole().getDbRole().getIdentifier().equalsIgnoreCase("S")) {
					Index in = new Index(ctx);
					out.print(in.renderWeeksStudent(ctx.getUser()));
				} 
			} else {
				String course_id = ctx.getRequest().getParameter("course_id");
				out.print(
					"<p>Para Regularizar Asistencia dirígase hacia "
					+"<a href=\"/webapps/LNOH-AIEP%20CED-BBLEARN/regulizador.jsp?course_id="
					+course_id+"\" target=\"_blank\">Regularizar Asistencia</a></p>");
			}
			%>
	</body>
</bbData:context>
