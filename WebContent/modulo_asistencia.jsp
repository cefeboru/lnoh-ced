<!DOCTYPE> 
<%@ page language="java" contentType="text/html;UTF-8" pageEncoding="UTF-8" %>

<%@ page import="org.tempuri.Index" %>
<%@ page import="blackboard.data.user.User.SystemRole" %>

<%@taglib uri="/bbData" prefix="bbData" %>


<bbData:context id="ctx">
<head>
	<style>
	th, td {
		text-align:center;
	}
	table {
		width: 100%;
		border-width:0;
	}
</style>
</head>
<body>
	<table>
		<tbody>
			<% if(ctx.getUser().getSystemRole().equals(SystemRole.DEFAULT)){		
					Index in = new Index(ctx);
					out.print( in.renderWeeksStudent(ctx.getUser()) );
			} else  {
				out.print("<p>Para Regulizar Asistencia dirigase hacia \"Herramientas del Curso > Regulizar Asistencia\"</p>");
			} %>
		</tbody>
	</table>
			
</body>
</bbData:context>
