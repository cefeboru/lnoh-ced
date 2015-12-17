package org.tempuri;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class testClass {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String fecha_i = "2015-10-11 00:00:0000";
		String fecha_f = "2015-11-28 00:00:0000";
		String [][] dates;
		try {
			Date date_i = new SimpleDateFormat("yyyy-M-d").parse(fecha_i);
			Date date_f = new SimpleDateFormat("yyyy-M-d").parse(fecha_f);
			
			Calendar cal = Calendar.getInstance(); 
			cal.setTime(date_i);
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			date_i = cal.getTime();
			cal.setTime(date_f);
			cal.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
			date_f = cal.getTime();
			long delta = date_f.getTime() - date_i.getTime();
			delta = (delta/1000L)/(60*60*24)+1;
			int weeks = (int)( Math.round((delta/7)));
			
			dates = new String[weeks][2];
			cal.setTime(date_i);
			for(int i=0;i<weeks;i++){
				dates[i][0] = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
				cal.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
				dates[i][1] = new SimpleDateFormat("dd/MM/yyyy").format(cal.getTime());
				cal.add(Calendar.DATE, 1);
				System.out.println("Semana "+(i+1)+"\n " + dates[i][0] + " - " + dates[i][1]);
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

}
