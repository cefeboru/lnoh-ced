package org.tempuri;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String temp = "2015;2;5773828;TSS124;3500;4;JUEVES;3;10:15 a 11:00;45;D;3;2015-10-28 00:00:00.000;2015-12-03 00:00:00.000;U201;320;EDIFICIO U;320;U201 AULA DE CLASES;320023;DIURNA;DES;DESARROLLO SOCIAL";
		String[] test = temp.split(";");
		System.out.print(arrayToString(test));
	}

	public static String arrayToString(String[] array) {
		String temp = "";
		System.out.println(array.length);
		StringBuilder BatchInsert = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			BatchInsert.append(array[i]).append(",");
		}
		String temp1 = BatchInsert.toString();
		temp1 = temp1.substring(0, temp1.length()-1);
		return temp1;
	}

}
