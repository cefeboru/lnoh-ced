package org.tempuri;

import java.text.ParseException;
import java.util.List;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import blackboard.data.course.CourseMembership;
import blackboard.data.user.User;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.persist.course.CourseMembershipDbLoader;
import blackboard.persist.user.UserDbLoader;
import blackboard.platform.context.Context;

public class test {

	private Context ctx;
	private final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(test.class);
	
	
	public static void main(String[] args) throws ParseException {
		int i= 1;
		System.out.print(i++);
	}

	public test(Context ctx) throws KeyNotFoundException, PersistenceException {
		this.ctx = ctx;
		System.out.println("HOLA !#!#!");
	}
	

	public String getRole() throws KeyNotFoundException, PersistenceException {
		List<User> users = UserDbLoader.Default.getInstance().loadByCourseId(ctx.getCourseId());
		String roles = "";
		for (int i = 0; i < users.size(); i++) {
			User currentUser = users.get(i);
			CourseMembership cm = CourseMembershipDbLoader.Default.getInstance()
					.loadByCourseAndUserId(this.ctx.getCourseId(), currentUser.getId());
			if(cm.getRole().getDbRole().getIdentifier().equalsIgnoreCase("Estudiante")){
				roles += "<p> UserName: " + currentUser.getUserName() + "</p>";
			}
		}
		return roles;
	}
}
