package com.ibm.as400.access;

import org.jenkinsci.plugins.ibmisteps.model.IBMi;

import java.io.Serial;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;
import java.util.function.Supplier;

public class IBMiMessage extends AS400Message implements Serializable {
	@Serial
	private static final long serialVersionUID = 4119612591789660630L;

	private final transient Supplier<String> substitutionDataRetriever;
	private final Calendar date;

	public IBMiMessage(final IBMi ibmi, final AS400Message as400Message) {
		super(as400Message.getID(),
				as400Message.getText(),
				as400Message.getFileName(),
				as400Message.getLibraryName(),
				as400Message.getSeverity(),
				as400Message.getType(),
				as400Message.getSubstitutionData(),
				as400Message.getHelp());
		this.setDefaultReply(as400Message.getDefaultReply());
		this.date = as400Message.getDate();
		this.substitutionDataRetriever = () -> ibmi.getCharConverter().byteArrayToString(as400Message.getSubstitutionData());
	}

	@Override
	public Calendar getDate() {
		return date;
	}

	public String getSubstitutionDataAsString() {
		return substitutionDataRetriever != null ? substitutionDataRetriever.get() : "";
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof AS400Message m &&
				checkEquals(getID(), m.getID()) &&
				checkEquals(getText(), m.getText()) &&
				checkEquals(getFileName(), m.getFileName()) &&
				checkEquals(getLibraryName(), m.getLibraryName()) &&
				checkEquals(getSeverity(), m.getSeverity()) &&
				checkEquals(getType(), m.getType()) &&
				checkEquals(getHelp(), m.getHelp()) &&
				checkEquals(getDate(), m.getDate()) &&
				checkEquals(getDefaultReply(), m.getDefaultReply());
	}

	private boolean checkEquals(Object a, Object b) {
		if (a == null) {
			return b == null;
		} else {
			return a.equals(b);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(getID(), getText(), getFileName(), getLibraryName(), getSeverity(), getType(), getHelp(), getDate(), getDefaultReply());
	}
}
