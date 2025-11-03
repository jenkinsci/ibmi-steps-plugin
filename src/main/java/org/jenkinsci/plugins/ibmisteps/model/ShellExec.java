package org.jenkinsci.plugins.ibmisteps.model;

import java.io.Serializable;

public record ShellExec(int code, String output) implements Serializable {
	@Override
	public String toString() {
		return "Code %d; Output: %s".formatted(code, output);
	}
}