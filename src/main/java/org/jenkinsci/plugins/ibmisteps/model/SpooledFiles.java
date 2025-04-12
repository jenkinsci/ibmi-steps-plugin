package org.jenkinsci.plugins.ibmisteps.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class SpooledFiles extends ArrayList<SpooledFiles.SpooledFile> {
	@Serial
	private static final long serialVersionUID = 1551585884795126297L;

	public String toJSON() throws JsonProcessingException {
		final ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(this);
	}

	public static class SpooledFile implements Serializable {
		@Serial
		private static final long serialVersionUID = -4169248633194708888L;

		private final String jobName;
		private final String jobUser;
		private final String jobNumber;
		private final String name;
		private final int number;
		private final long size;
		private final String userData;

		private final String fileName;

		public SpooledFile(final String name, final int number, final long size, final String userData,
		                   final String jobName, final String jobUser, final String jobNumber) {
			this.name = name;
			this.number = number;
			this.jobName = jobName;
			this.jobUser = jobUser;
			this.jobNumber = jobNumber;
			this.size = size;
			this.userData = userData;
			fileName = String.format("%s_%s.txt", name, number);
		}

		@JsonIgnore
		public boolean exists() {
			return size > 0;
		}

		public String getJobName() {
			return jobName;
		}

		public String getJobNumber() {
			return jobNumber;
		}

		public String getJobUser() {
			return jobUser;
		}

		public String getName() {
			return name;
		}

		public int getNumber() {
			return number;
		}

		public String getUserData() {
			return userData;
		}

		public String getFileName() {
			return fileName;
		}

		public long getSize() {
			return size;
		}
	}
}
