package com.network.sftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

public class SFTPUtil {
	
	private Session session = null;
	private Channel channel = null;
	private ChannelSftp channelSftp = null;

   // SFTP 서버연결 
	public void connect(){
		
		String host = "";
		int port = 22;
		String user = "";
		String password = "";
		
		String initFilePath = "C:\\Users\\baese\\eclipse-workspace\\sftp-project\\src\\main\\resources\\sftp.ini";
		        
        try {
        	
			System.out.println("SFTP connect start");
        	
        	JSch jsch = new JSch();
        	Properties properties = new Properties();
			properties.load(new FileInputStream(initFilePath));
			
			host = properties.getProperty("ftp_host");
            port = Integer.parseInt(properties.getProperty("ftp_port"));
            user = properties.getProperty("ftp_user");
            password = properties.getProperty("ftp_password");
            
            //String url = host +":" + port;
		
            System.out.println("host :"+host+", port :"+port+",user : "+user+",password : "+password);
			//세션객체 생성 ( user , host, port ) 	
            session = jsch.getSession(user, host, port);
            
            //password 설정
            session.setPassword(password);
            
            //세션관련 설정정보 설정
            java.util.Properties config = new java.util.Properties();
            
            //호스트 정보 검사하지 않는다.
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            System.out.println("SFTP session connecting ...");
            //접속
            session.connect();

            //sftp 채널 접속
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            
			System.out.println("SFTP connect end");
			
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
	}

	// 단일 파일 업로드 
	public void upload(String uploadFilePath, String remoteUploadPath, String mode){
		FileInputStream fis = null;
		
		try{ 
			connect();
			
			System.out.println("SFTP upload start, uploadFilePath :" + uploadFilePath+", remoteUploadPath : " + remoteUploadPath);
			
			File file = new File(uploadFilePath);
			fis = new FileInputStream(file);
			
			channelSftp.cd(remoteUploadPath);
			channelSftp.put(fis,file.getName(),
				new SftpProgressMonitor() {
					private long max = 1;  //최대
					private long count = 0;  //계산을 위해 담아두는 변수
					private long percent = 0;  //퍼센트
					public void init(int op, String src, String dest, long max) {  //설정
						
						String operation = (op == SftpProgressMonitor.PUT)
                                ? "UPLOAD"
                                : "DOWNLOAD";
						
						System.out.println("init -- operation:"+operation+", max:"+max);
						
						this.max = max;
					}
					public void end() {
						System.out.println("업로드가 완료 되었습니다. ");
					}
					public boolean count(long bytes) {
						System.out.println("bytes : " + bytes); 
						this.count += bytes;  //전송한 바이트를 더한다.
						long percentNow = this.count*100/max;  //현재값에서 최대값을 뺀후
						System.out.println("this.count : " + this.count+", max:"+max+", percentNow:"+percentNow +", this.percent:"+this.percent); 
						if(percentNow>this.percent){  //퍼센트보다 크면
							this.percent = percentNow;
							System.out.println("progress : " + this.percent); //Progress
						}
						return true;//기본값은 false이며 false인 경우 count메소드를 호출하지 않는다.
					}
				},
				ChannelSftp.OVERWRITE);
			
		}catch(SftpException se){
			se.printStackTrace();
		}catch(FileNotFoundException fe){
			fe.printStackTrace();
		}finally{
			try{
				
				fis.close();
				System.out.println("SFTP File uploaded successfully!!");
				
			} catch(IOException ioe){
				ioe.printStackTrace();
			}
		}
		disconnect();
		
	}

	// 단일 파일 다운로드 
	public void download(String downloadFilePath, String localDownloadPath) throws Exception{
        byte[] buffer = new byte[1024];
        BufferedInputStream bis;

        try {
        	connect();
        	
            // Change to output directory
            String downloadFileDir = downloadFilePath.substring(0, downloadFilePath.lastIndexOf("/") + 1);
            channelSftp.cd(downloadFileDir);

            File downloadFile = new File(downloadFilePath);
            bis = new BufferedInputStream(channelSftp.get(downloadFile.getName()));

            File newFile = new File(localDownloadPath + "/" + downloadFile.getName());

            // Download file
            OutputStream os = new FileOutputStream(newFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            int readCount;
            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }
            bis.close();
            bos.close();
            
            System.out.println("SFTP File downloaded successfully!! - "+ downloadFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
        disconnect();
    }

	// 파일서버와 세션 종료
	public void disconnect(){
		channelSftp.quit();
		session.disconnect();
	}

	
	public static void main(String[] args) {
		String uploadFilePath = "D:\\자료실\\mariadb-10.3.7-winx64.msi"; 
        String remoteUploadPath = "/sftp_root/uploads";
        String downloadFilePath = "/sftp_root/uploads/loading.png";
        String localDownloadPath = "D:\\";

        try{
        	
        	SFTPUtil ftp = new SFTPUtil();
        	ftp.upload(uploadFilePath, remoteUploadPath, "overwrite");
            //ftp.download(downloadFilePath, localDownloadPath);
            
        } catch (Exception e)
        {
            System.out.println(e);
        }
	}
}
