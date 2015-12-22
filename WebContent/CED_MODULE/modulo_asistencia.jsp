<!DOCTYPE> 
<%@ page language="java" contentType="text/html;UTF-8" pageEncoding="UTF-8" %>

<%@ page import="org.tempuri.Index" %>
<%@ page import="blackboard.data.user.User.SystemRole" %>

<%@taglib uri="/bbData" prefix="bbData" %>

<bbData:context id="ctx">
<head>
	<style>
	</style>
</head>
<body>
	<% if(ctx.getUser().getSystemRole().equals(SystemRole.DEFAULT)){		
		
			Index in = new Index(ctx);
			out.print("TEST");
			//out.print("FLAGS: " + in.flags);
	} else  {
		out.print("<p>Regulizar Asistencia</p>");
	}
	%>
</body>
</bbData:context>
