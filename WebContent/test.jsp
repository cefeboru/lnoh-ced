<%@ page language="java" contentType="text/html;UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="blackboard.data.course.CourseMembership"%>
<%@ page import="org.tempuri.test"%>
<%@taglib uri="/bbData" prefix="bbData" %>

<bbData:context id="ctx">
	<% 
		test prueba = new test(ctx);
		out.print(prueba.getRole());
	%>
</bbData:context>


