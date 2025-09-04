package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.IBMiMessage;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CallResult implements Serializable {
	@Serial
	private static final long serialVersionUID = -7184769518123385346L;

	private static final Pattern JOB_PATTERN = Pattern.compile(" (\\d{1,6})/([^/ ]{1,10})/([^/ ]{1,10}) ");

	private final List<IBMiMessage> messages = new ArrayList<>();
	private final boolean successful;

	public CallResult(final IBMi ibmi, final boolean successful, final AS400Message[] as400Messages) {
		this.successful = successful;
		Stream.of(as400Messages).map(message -> new IBMiMessage(ibmi, message)).forEach(messages::add);
	}

	public IBMiMessage getMessage(final String messageId) {
		return messages.stream() //
				.filter(m -> m.getID().equalsIgnoreCase(messageId))//
				.findFirst() //
				.orElse(null);
	}

	public List<IBMiMessage> getMessages() {
		return messages;
	}

	@CheckForNull
	public IBMiMessage getLastMessage() {
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
