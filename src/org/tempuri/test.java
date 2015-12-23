package org.tempuri;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class test {

	public static void main(String[] args) throws SocketException, IOException {
		// TODO Auto-generated method stub
		FTPClient ftp = new FTPClient();
		
		String remoteFile = "mirror/FreeBSD/README.TXT";
		
		ftp.connect("localhost",2222);
		ftp.enterLocalPassiveMode();
		ftp.setFileType(FTP.BINARY_FILE_TYPE);
		
		int reply = ftp.getReplyCode();
		
		if(!FTPReply.isPositiveCompletion(reply)) {
	        ftp.disconnect();
	        System.err.println("FTP server refused connection, code: "+ reply);
	        System.exit(1);
	      }
		
		String filename = "sitemap.sql";
		File downloadFile2 = new File(filename);
		OutputStream outputStream2 = new BufferedOutputStream(new FileOutputStream(filename));
		
		InputStream inputStream = ftp.retrieveFileStream(remoteFile);
		
		byte[] bytesArray = new byte[4096];
        int bytesRead = -1;
        
        while ((bytesRead = inputStream.read(bytesArray)) != -1) {
            outputStream2.write(bytesArray, 0, bytesRead);
        }
		
        boolean success = ftp.completePendingCommand();
        if (success) {
            System.out.println("File #2 has been downloaded successfully.");
        }
        
        outputStream2.close();
        inputStream.close();
	}

}
