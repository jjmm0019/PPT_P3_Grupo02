package ujaen.git.ppt;

import java.net.Socket;

public class ObtainIP
{
	Socket mSocket = null;
	String IP = null;
	Boolean fExec = true;
	
	public ObtainIP(Socket rSocket)
	{
		mSocket = rSocket;
	}
	public String getIP(){
		String auxIP = mSocket.getRemoteSocketAddress().toString();
		String auxIP2 = auxIP.substring(1, auxIP.length());
		
		String[] parts = auxIP2.split(":");
		if(parts.length > 2){
			for(int i = 0; i < parts.length - 1; i++){
				if(fExec){
					fExec = false;
					IP = parts[i];
				}
				else{
					IP += ":" + parts[i];
				}
			}
			fExec = true;
		}
		else{
			IP = parts[0];
		}
		
		return IP;
	}
}
