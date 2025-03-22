package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400Message;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CallResult implements Serializable {
    private static final long serialVersionUID = -7184769518123385346L;

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
        return messages.isEmpty() ? null : messages.get(messages.size()-1);
    }

    public String getPrettyMessages() {
        return messages.stream()
                .map(m -> MessageFormat.format("[{0}][{1}] {2}", m.getID(), m.getSeverity(), m.getText()))
                .collect(Collectors.joining("\n"));
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Successful: {0}; Messages: {1}",
                successful,
                getPrettyMessages());
    }
}
