package ujaen.git.ppt;
import ujaen.git.ppt.mail.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;

import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.FileReader;

import ujaen.git.ppt.smtp.*;
import ujaen.git.ppt.mail.Mail;
import ujaen.git.ppt.mail.Mailbox;

public class Connection implements Runnable, RFC5322 {

	public static final int S_HELO = 0;
	public static final int S_EHLO = 1;
	public static final int S_MAIL = 2;
	public static final int S_RCPT = 3;
	public static final int S_DATA = 4;
	public static final int S_RSET = 5;
	public static final int S_QUIT = 6;

	protected Socket mSocket;
	protected int mEstado = S_HELO;;
	private boolean mFin = false;
	
	Mail mail = null;
	Mailbox mBox = null;
	ObtainIP sIP = null;
	
	boolean esHELO = false;
	boolean esMAIL = false;
	boolean esRCPT = false;
	boolean fDataExec = false;
	boolean mensajeEnviado = false;
	String mFrom = "", mTo = "";
	String mArgumentos = "";
	String hostname = "";
	String Argumento = "";
	String sMessID;
	String strDate;
	String IP = null;
	int dID = 0;

	public Connection(Socket s) {
		mSocket = s;
		mEstado = 0;
		mFin = false;
	}

	@Override
	public void run() {

		String inputData = null;
		String outputData = "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		Date now = new Date();
		

		if (mSocket != null) {
			try {
				
				File fID = new File("ID.txt");
				if(!fID.exists() && !fID.isFile()){
					PrintWriter writer = new PrintWriter(fID, "UTF-8");
					writer.println("0");
					writer.close();
				}
				// Inicialización de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(
						mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				// Envío del mensaje de bienvenida
				String response = RFC5321.getReply(RFC5321.R_220) + SP + RFC5321.MSG_WELCOME
						+ RFC5322.CRLF;
				output.write(response.getBytes());
				output.flush();

				while (!mFin && ((inputData = input.readLine()) != null)) {
					
					System.out.println("Servidor [Recibido]> " + inputData);
				
					
					// Todo análisis del comando recibido
					SMTPMessage m = new SMTPMessage(inputData);
					if(mEstado != S_DATA && m.getCommandId() == S_DATA)
					{
						fDataExec = true;
					}
					
					if(mEstado != S_DATA)
					{
						mEstado = m.getCommandId();
						mArgumentos = m.getArguments();
					}

					// TODO: Máquina de estados del protocolo
					switch (mEstado) {
					case S_HELO:
						if(!esHELO){
							Argumento = mArgumentos;
							
						}
						break;
						
					case S_EHLO:
						if(!esHELO){
							Argumento = mArgumentos;
							//isHELO = true;
						}
						break;
					case S_MAIL:
						if(esHELO && !esRCPT){
							mFrom = mArgumentos;
							esMAIL = true;
						}
						break;
					case S_RCPT:
						if(esHELO && esMAIL){
							if(Mailbox.checkRecipient(mArgumentos)){
								mTo = mArgumentos;
								esRCPT = true;
							}
							
							else{
								esRCPT = false;
							}
						}
						break;
					case S_DATA:
						if(esHELO && esMAIL && esRCPT){
							if(fDataExec){
								
								BufferedReader brID = new BufferedReader(new FileReader(fID));
								dID = Integer.parseInt(brID.readLine());
								brID.close();
								dID++;
								PrintWriter writer = new PrintWriter(fID, "UTF-8");
								writer.println(dID);
								writer.close();
								InetAddress addr;
							    addr = InetAddress.getLocalHost();
							    hostname = addr.getHostName();
							    sIP = new ObtainIP(mSocket);
							    IP = sIP.getIP();
							    
							    // MESSAGE-ID
							    sMessID = "<" + dID + "@" + hostname + ">";
							    
							    //date
							    strDate = sdf.format(now);
							    mail = new Mail();
							    mail.setHost(hostname);
							    mail.setMailfrom(mFrom);
							    mail.setRcptto(mTo);
							    System.out.println("Rcpt to: " + mTo);
							    mail.addHeader("Return-Path", mFrom);
							    mail.addHeader("Received", Argumento);
							    mail.addHeader("host", hostname);
							    mail.addHeader("IP", IP);
							    mail.addHeader("date", strDate);
							    mail.addHeader("Message-ID", sMessID);

							}
							else{
								mail.addMailLine(inputData);
								inputData += CRLF;
							}
							if(inputData.compareTo(ENDMSG) == 0){
								mensajeEnviado = true;
								esMAIL = false;
								esRCPT = false;
								mBox = new Mailbox(mail);
							}
						}
						break;
					case S_RSET:
						esMAIL = false;
						esRCPT = false;
						break;
					case S_QUIT:
						mFin = true;
						break;
						
					default:
						break;
					}

					// TODO montar la respuesta
					// El servidor responde con lo recibido
					outputData = RFC5321.getReply(RFC5321.R_220) + SP + inputData + CRLF;
					output.write(outputData.getBytes());
					output.flush();

				}
				System.out.println("Servidor [Conexión finalizada]> "
						+ mSocket.getInetAddress().toString() + ":"
						+ mSocket.getPort());

				input.close();
				output.close();
				mSocket.close();
			} catch (SocketException se) {
				se.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
}
