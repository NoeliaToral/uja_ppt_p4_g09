package ujaen.git.ppt.smtp;

import ujaen.git.ppt.mail.Mailbox;

public class SMTPMessage implements RFC5322 {

	protected String mCommand = null;
	protected int mCommandId = RFC5321.C_NOCOMMAND;
	protected String mArguments = null;
	protected String[] mParameters = null;
	protected boolean mHasError = false;
	protected int mErrorCode = 0;

	/**
	 * The input string is processed to analyze the format of the message
	 * 
	 * @param data
	 */
	public SMTPMessage(String data) {

		if (data.length() > 998) {
			mHasError = true;

		} else
			mHasError = parseCommand(data);

	}

	/**
	 * 
	 * @param data
	 * @return true if there were errors
	 */
	protected boolean parseCommand(String data) {

		if (data.indexOf(":") > 0) {
			String[] commandParts = data.split(":");
			checkCommand(commandParts[0]);
			checkArguments(commandParts[1]);
			if (mCommandId != -1 && checkArguments(commandParts[1]) == true) {
				return false;
			}

		} else if (data.indexOf(" ") > 0) {
			String[] commandParts = data.split(" ");
			checkCommand(commandParts[0]);

			if (mCommandId != -1 && checkArguments(commandParts[1]) == true) {
				return false;
			}

		} else {
			if (checkCommand(data) != -1) {
				return false;
			}
		}
		if (mErrorCode == 0) {
			mErrorCode = RFC5321.E_500_SINTAXERROR;
		}
		return true;
	}

	public String toString() {
		if (!mHasError) {
			String result = "";
			result = this.mCommand;
			if (this.mCommandId == RFC5321.C_MAIL
					|| this.mCommandId == RFC5321.C_RCPT)
				result = result + ":";
			if (this.mArguments != null)
				result = result + this.mArguments;
			if (this.mParameters != null)
				for (String s : this.mParameters)
					result = result + SP + s;

			result = result + CRLF;
			// opcional
			result = result + "id=" + this.mCommandId;
			return result;
		} else
			return "Error";
	}

	/**
	 * 
	 * @param data
	 * @return The id of the SMTP command
	 */
	protected boolean checkArguments(String data) {
		mArguments = data;
		if (mCommand == null) {
			return false;
		} else if (mCommand.equalsIgnoreCase("HELO") || mCommand.equalsIgnoreCase("EHLO")) {
			return true;
		} else if (mCommand.equalsIgnoreCase("MAIL FROM")) {
			return mArguments.contains("@");
		} else if (mCommand.equals("RCPT TO")) {
			if (Mailbox.checkRecipient(mArguments) == true) {
				return true;
			} else {
				mErrorCode = RFC5321.E_551_USERNOTLOCAL;
				return false;
			}
		}
		return false;

	}

	protected int checkCommand(String data) {
		int index = 0;

		this.mCommandId = RFC5321.C_NOCOMMAND; // inicializa a comando de no id
												// -1

		for (String c : RFC5321.SMTP_COMMANDS) {
			if (data.compareToIgnoreCase(c) == 0) { // comparamos el comando con
													// todos los comandos
													// aumentado el index a 1
													// cada vez
				this.mCommandId = index; // el numero de la vuelta que sea sera
											// el id del comando
			}
			index++;

		}

		mCommand = RFC5321.getCommand(mCommandId);
		return this.mCommandId;
	}

	public String getCommand() {
		return mCommand;
	}

	public void setCommand(String mCommand) {
		this.mCommand = mCommand;
	}

	public int getCommandId() {
		return mCommandId;
	}

	public void setCommandId(int mCommandId) {
		this.mCommandId = mCommandId;
	}

	public String getArguments() {
		return mArguments;
	}

	public void setArguments(String mArguments) {
		this.mArguments = mArguments;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public void setParameters(String[] mParameters) {
		this.mParameters = mParameters;
	}

	public boolean hasError() {
		return mHasError;
	}

	public int getErrorCode() {
		return mErrorCode;
	}

	public void setErrorCode(int mErrorCode) {
		this.mErrorCode = mErrorCode;
	}

}
