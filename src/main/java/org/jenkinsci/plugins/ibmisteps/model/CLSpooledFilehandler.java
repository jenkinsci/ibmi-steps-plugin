package org.jenkinsci.plugins.ibmisteps.model;

import java.io.IOException;
import java.util.UUID;

import org.jenkinsci.plugins.ibmisteps.model.SpooledFiles.SpooledFile;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.list.OpenListException;
import com.ibm.as400.access.list.SpooledFileListItem;
import com.ibm.as400.access.list.SpooledFileOpenList;

import hudson.FilePath;

public class CLSpooledFilehandler implements SpooledFileHandler {
	private static final long serialVersionUID = -2465788947902207300L;

	CLSpooledFilehandler() {

	}

	@Override
	public void writeSpooledFile(final IBMi ibmi, final SpooledFile spooledFile, final FilePath toFile)
			throws IOException, InterruptedException, AS400SecurityException, ErrorCompletingRequestException {
		final IFSFile workFolder = new IFSFile(ibmi.getIbmiConnection(), "/tmp", UUID.randomUUID() + ".jenkins.temp");
		try {
			workFolder.mkdirs();
			final IFSFile targetFile = new IFSFile(workFolder, spooledFile.getName() + "_" + spooledFile.getNumber() + ".txt");
			final IFSFile workFile = new IFSFile(workFolder, targetFile.getName() + ".work");

			final CallResult cpysplf = ibmi
					.executeCommand(String.format(
							"CPYSPLF FILE(%s) SPLNBR(%s) TOFILE(*TOSTMF) JOB(%s/%s/%s) TOSTMF('%s')",
							spooledFile.getName(), spooledFile.getNumber(),
							spooledFile.getJobNumber(), spooledFile.getJobUser(), spooledFile.getJobName(),
							workFile));
			if (!cpysplf.isSuccessful()) {
				throw new IOException("CPYSPLF failed: " + cpysplf.getPrettyMessages());
			}

			// Convert to UTF-8
			final CallResult cpy = ibmi
					.executeCommand(String.format("CPY OBJ('%s') TOOBJ('%s') TOCCSID(1208) DTAFMT(*TEXT)",
							workFile, targetFile));
			if (!cpysplf.isSuccessful()) {
				throw new IOException("CPY failed: " + cpy.getPrettyMessages());
			}

			ibmi.download(targetFile, toFile);
		} finally {
			ibmi.executeCommand(String.format("RMDIR DIR('%s') SUBTREE(*ALL) RMVLNK(*YES)", workFolder));
		}
	}

	@Override
	public SpooledFiles listSpooledFiles(final IBMi ibmi, final String jobNumber, final String jobUser, final String jobName)
			throws IOException, InterruptedException, AS400SecurityException, ErrorCompletingRequestException, ObjectDoesNotExistException, OpenListException {
		final SpooledFiles spooledFiles = new SpooledFiles();
		final SpooledFileOpenList spooledList = new SpooledFileOpenList(ibmi.getIbmiConnection());
		spooledList.setFilterJobInformation(jobName, jobUser, jobNumber);
		try {
			spooledList.open();
			for (final Object object : spooledList.getItems(-1, 0)) {
				final SpooledFileListItem spooledFileItem = (SpooledFileListItem) object;
				final SpooledFile spooledFile = new SpooledFile(spooledFileItem.getName(),
						spooledFileItem.getNumber(),
						spooledFileItem.getSize(), spooledFileItem.getUserData(),
						spooledFileItem.getJobName(), spooledFileItem.getJobUser(), spooledFileItem.getJobNumber());
				spooledFiles.add(spooledFile);
			}
		} finally {
			spooledList.close();
		}
		return spooledFiles;
	}
}
