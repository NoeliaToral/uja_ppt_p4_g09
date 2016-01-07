package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import ujaen.git.ppt.mail.Mail;
import ujaen.git.ppt.mail.Mailbox;
import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;

public class Connection implements Runnable, RFC5322 {

	public static final int S_HELO = 0;
	public static final int S_MAIL = 1;
	public static final int S_RCPT = 2;
	public static final int S_DATA = 3;
	public static final int S_HEAD = 4;
	public static final int S_MENSAJE = 5;
	public static final int S_RSET = 6;
	protected Socket mSocket;
	protected int mEstado = S_HELO;
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
		String mmensaje = "";
		Collection<String> rcpts = new ArrayList<String>();
		if (mSocket != null) {
			try {
				// Inicialización de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(
						mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				// Envío del mensaje de bienvenida
				String response = RFC5321.getReply(RFC5321.R_220) + SP
						+ RFC5321.MSG_WELCOME + RFC5322.CRLF;
				output.write(response.getBytes());
				output.flush();

				while (!mFin && ((inputData = input.readLine()) != null)) {

					System.out.println("Servidor [Recibido]> " + inputData);

					// Todo análisis del comando recibido

					SMTPMessage m = new SMTPMessage(inputData);

					// TODO: Máquina de estados del protocolo
					if (m.hasError() == false || mEstado == S_MENSAJE
							|| mEstado == S_HEAD) {
						if (m.getCommandId() == 6) {
							outputData = RFC5321.getReply(RFC5321.R_221)+SP+RFC5321.getReplyMsg(RFC5321.R_221)+CRLF;
							break;
						}
						if (m.getCommandId() == 5) {
							mEstado = S_RSET;
						}
						if (m.getCommandId() == 3 && mEstado == S_DATA){
							mEstado = S_RCPT;
						}
						switch (mEstado) {
						case S_HELO:
							if ((m.getCommand().equalsIgnoreCase("helo") || m.getCommand().equalsIgnoreCase("ehlo"))
									&& m.getArguments() != null) {
								outputData = RFC5321.getReply(RFC5321.R_250)
										+ SP
										+ RFC5321.getReplyMsg(RFC5321.R_250)
										+ CRLF;
								mEstado = S_MAIL;
							} else
								outputData = RFC5321
										.getError(RFC5321.E_503_BADSEQUENCE)
										+ SP
										+ RFC5321
												.getErrorMsg(RFC5321.E_503_BADSEQUENCE)
										+ CRLF;
							break;
						case S_MAIL:
							if (m.getCommand().equalsIgnoreCase("Mail From")) {
								mmensaje = "";
								outputData = RFC5321.getReply(RFC5321.R_250)
										+ SP
										+ RFC5321.getReplyMsg(RFC5321.R_250)
										+ CRLF;
								mEstado = S_RCPT;
							} else
								outputData = RFC5321
										.getError(RFC5321.E_503_BADSEQUENCE)
										+ SP
										+ RFC5321
												.getErrorMsg(RFC5321.E_503_BADSEQUENCE)
										+ CRLF;
							break;
						case S_RCPT:
							if (m.getCommand().equalsIgnoreCase("RCPT TO")) {
								outputData = RFC5321.getReply(RFC5321.R_250)
										+ SP
										+ RFC5321.getReplyMsg(RFC5321.R_250)
										+ CRLF;
								rcpts.add(m.getArguments());
								mEstado = S_DATA;
							} else
								outputData = RFC5321
										.getError(RFC5321.E_503_BADSEQUENCE)
										+ SP
										+ RFC5321
												.getErrorMsg(RFC5321.E_503_BADSEQUENCE)
										+ CRLF;
							break;
						case S_DATA:
							if (m.getCommand().equalsIgnoreCase("DATA")) {
								outputData = RFC5321.getReply(RFC5321.R_354)
										+ SP
										+ RFC5321.getReplyMsg(RFC5321.R_354)
										+ CRLF;
								mEstado = S_HEAD;

							} else
								outputData = RFC5321
										.getError(RFC5321.E_503_BADSEQUENCE)
										+ SP
										+ RFC5321
												.getErrorMsg(RFC5321.E_503_BADSEQUENCE)
										+ CRLF;
							break;
						case S_HEAD:
							outputData = "";
							if (inputData.equals("")) {
								Calendar calendario = Calendar.getInstance();
								mmensaje = mmensaje
										+ "Destination Date Field: "
										+ Integer.toString(calendario
												.get(Calendar.HOUR_OF_DAY))
										+ ":"
										+ Integer.toString(Calendar.MINUTE)
										+ " "
										+ Integer.toString(calendario
												.get(Calendar.DAY_OF_MONTH))
										+ "/"
										+ Integer.toString(calendario
												.get(Calendar.MONTH))
										+ "/"
										+ Integer.toString(calendario
												.get(Calendar.YEAR)) + CRLF;
								mmensaje = mmensaje
										+ "Message-ID:"
										+ Long.toString((Math.round(((Math
												.random() * 10000))))) + CRLF;
								mEstado = S_MENSAJE;
							}
							mmensaje = mmensaje + inputData + CRLF;
							break;
						case S_MENSAJE:
							mmensaje = mmensaje + inputData + CRLF;
							outputData = "";
							if (inputData.equals(".")) {
								for (String string : rcpts) {
									Mailbox fichero = new Mailbox(string);
									fichero.newMail(mmensaje);
								}
								outputData = RFC5321.getReply(RFC5321.R_250)
										+ SP
										+ RFC5321.getReplyMsg(RFC5321.R_250)
										+ CRLF;
								mEstado = S_MAIL;
							}
							break;
						case S_RSET:
							mEstado = S_MAIL;
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							break;
						default:
							break;
						}
					} else {
						outputData = RFC5321.getError((m.getErrorCode())) + SP
								+ RFC5321.getErrorMsg(m.getErrorCode()) + CRLF;

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
