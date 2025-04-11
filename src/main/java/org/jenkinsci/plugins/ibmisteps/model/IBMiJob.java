package org.jenkinsci.plugins.ibmisteps.model;

import java.io.Serializable;

public record IBMiJob(String number, String user, String name) implements Serializable {
	@Override
	public final String toString() {
		return "%s/%s/%s".formatted(number, user, name);
	}
}