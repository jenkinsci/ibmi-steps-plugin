package org.jenkinsci.plugins.ibmisteps.model;

import org.jenkinsci.plugins.ibmisteps.Messages;

import java.util.function.Supplier;

public enum OnMessageWait {

	FAIL(Messages::OnMessageWait_fail),
	KILL(Messages::OnMessageWait_kill),
	RESUME(Messages::OnMessageWait_resume),
	WAIT(Messages::OnMessageWait_wait);

	private final Supplier<String> displayNameSupplier;

	OnMessageWait(final Supplier<String> displayNameSupplier) {
		this.displayNameSupplier = displayNameSupplier;
	}

	public String getDisplayName() {
		return displayNameSupplier.get();
	}
}
