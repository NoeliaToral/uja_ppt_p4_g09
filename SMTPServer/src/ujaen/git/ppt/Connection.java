package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;

public class Connection implements Runnable, RFC5322 {

	public static final int S_HELO = 0;
	public static final int S_MAIL = 1;
	public static final int S_RCPT = 2;
	public static final int S_DATA = 3;
	protected Socket mSocket;
	protected int mEstado = S_HELO;;
	private boolean mFin = false;

	public Connection(Socket s) {
		mSocket = s;
		mEstado = 0;
		mFin = false;
	}

	@Override
	public void run() {

		String inputData = null;
		String outputData = "";
		

		if (mSocket != null) {
			try {
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

					// TODO: Máquina de estados del protocolo
					if (m.hasError()==false){
						switch (mEstado) {
						case S_HELO:
							if (m.getCommand().equalsIgnoreCase("helo")){
								outputData = RFC5321.getReply(RFC5321.R_250) + SP + RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							}
							break;
						case S_MAIL:
							if (m.getCommand().equalsIgnoreCase("Mail From")){
								outputData = RFC5321.getReply(RFC5321.R_250) + SP + RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							}
							break;
						case S_RCPT:
							if (m.getCommand().equalsIgnoreCase("RCPT TO")){
								outputData = RFC5321.getReply(RFC5321.R_250) + SP + RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							}
							break;
						case S_DATA:
							if (m.getCommand().equalsIgnoreCase("DATA")){
								outputData = RFC5321.getReply(RFC5321.R_354) + SP + RFC5321.getReplyMsg(RFC5321.R_354) + CRLF;
							}
						default:
							break;
						}
					}
					else{
						outputData = RFC5321.getError((RFC5321.E_500_SINTAXERROR)) + SP + RFC5321.getErrorMsg(RFC5321.E_500_SINTAXERROR) + CRLF;
						
					}
					// TODO montar la respuesta
					// El servidor responde con lo recibido
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
