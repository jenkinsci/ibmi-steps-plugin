package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.MessageFile;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Supplier;

public class IBMiMessage implements Serializable {
	private final AS400Message as400Message;
	private final transient Supplier<String> substitutionDataRetriever;

	public IBMiMessage(final IBMi ibmi, final AS400Message as400Message) {
		this.as400Message = as400Message;
		this.substitutionDataRetriever = () -> ibmi.getCharConverter().byteArrayToString(as400Message.getSubstitutionData());
	}

	public String getDefaultReply() {
		return this.as400Message.getDefaultReply();
	}

	public String getFileName() {
		return this.as400Message.getFileName();
	}

	public String getHelp() {
		return MessageFile.substituteFormattingCharacters(this.as400Message.getHelp());
	}

	public String getID() {
		return this.as400Message.getID();
	}

	public String getLibraryName() {
		return this.as400Message.getLibraryName();
	}

	public String getPath() {
		return this.as400Message.getPath();
	}

	public int getSeverity() {
		return this.as400Message.getSeverity();
	}

	public String getText() {
		return this.as400Message.getText();
	}

	public int getType() {
		return this.as400Message.getType();
	}

	public Calendar getDate() {
		return this.as400Message.getDate();
	}

	public Date getCreateDate() {
		return as400Message.getCreateDate();
	}

	public Date getModificationDate() {
		return as400Message.getModificationDate();
	}

	public String getMessageFileLibrarySpecified() {
		return as400Message.getMessageFileLibrarySpecified();
	}

	public byte[] getKey() {
		return as400Message.getKey();
	}

	public String getSendingProgramName() {
		return as400Message.getSendingProgramName();
	}

	public String getSendingProgramInstructionNumber() {
		return as400Message.getSendingProgramInstructionNumber();
	}

	public String getReceivingProgramName() {
		return as400Message.getReceivingProgramName();
	}

	public String getReceivingProgramInstructionNumber() {
		return as400Message.getReceivingProgramInstructionNumber();
	}

	public String getSendingType() {
		return as400Message.getSendingType();
	}

	public String getReceivingType() {
		return as400Message.getReceivingType();
	}

	public int getTextCcsidConversionStatusIndicator() {
		return as400Message.getTextCcsidConversionStatusIndicator();
	}

	public int getDataCcsidConversionStatusIndicator() {
		return as400Message.getDataCcsidConversionStatusIndicator();
	}

	public String getAlertOption() {
		return as400Message.getAlertOption();
	}

	public String getSubstitutionData() {
		return substitutionDataRetriever != null ? substitutionDataRetriever.get() : "";
	}
}
