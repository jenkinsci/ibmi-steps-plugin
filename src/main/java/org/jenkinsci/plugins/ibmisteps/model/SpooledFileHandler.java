package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.list.OpenListException;
import hudson.FilePath;
import org.jenkinsci.plugins.ibmisteps.model.SpooledFiles.SpooledFile;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

public interface SpooledFileHandler extends Serializable {

	void writeSpooledFile(IBMi ibmi, SpooledFile spooledFile, FilePath target)
			throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException,
			ErrorCompletingRequestException;

	SpooledFiles listSpooledFiles(IBMi ibmi, String jobNumber, String jobUser, String jobName)
			throws SQLException, AS400SecurityException, ObjectDoesNotExistException, IOException, InterruptedException,
			ErrorCompletingRequestException, OpenListException;
}
