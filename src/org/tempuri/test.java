package org.tempuri;

import java.rmi.RemoteException;

public class test {

	public static void main(String[] args) throws RemoteException {
		// TODO Auto-generated method stub
		String data = "15579969:1,15579969:2,";
		String[] data2 = data.split(",");
		System.out.println(data2[1]);
	}

}
