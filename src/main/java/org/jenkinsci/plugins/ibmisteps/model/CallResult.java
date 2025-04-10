package org.jenkinsci.plugins.ibmisteps.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ibm.as400.access.AS400Message;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class CallResult implements Serializable {
	private static final long serialVersionUID = -7184769518123385346L;

	private static final Pattern JOB_PATTERN = Pattern.compile(" (\\d{1,6})\\/(.{1,10})\\/(.{1,10}) ");

	private final List<AS400Message> messages = new ArrayList<>();
	private final boolean successful;

	public CallResult(final boolean successful, final AS400Message[] ibmiMessages) {
		this.successful = successful;
		Collections.addAll(messages, ibmiMessages);
	}

	public AS400Message getMessage(final String messageId) {
		return messages.stream() //
				.filter(m -> m.getID().equalsIgnoreCase(messageId))//
				.findFirst() //
				.orElse(null);
	}

	public List<AS400Message> getMessages() {
		return messages;
	}

	@CheckForNull
	public AS400Message getLastMessage() {
		return messages.isEmpty() ? null : messages.get(messages.size() - 1);
	}

	public String getPrettyMessages() {
		return getPrettyMessages("");
	}

	public String getPrettyMessages(final String prefix) {
		return messages.stream()
				.map(m -> MessageFormat.format("{0}[{1}][{2}] {3}", prefix, m.getID(), m.getSeverity(), m.getText()))
				.collect(Collectors.joining("\n"));
	}

	public boolean isSuccessful() {
		return successful;
	}

	public List<IBMiJob> getSubmittedJobs() {
		return messages.stream()
				.filter(message -> message.getID().equals("CPC1221"))
				.map(message -> JOB_PATTERN.matcher(message.getText()))
				.filter(Matcher::find)
				.map(matcher -> new IBMiJob(matcher.group(1), matcher.group(2), matcher.group(3)))
				.toList();
	}

	@Override
	public String toString() {
		return MessageFormat.format("Successful: {0}; Messages: {1}",
				successful,
				getPrettyMessages());
	}
}
