package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;

import java.io.IOException;

public interface TempFileTask {
	void run(IFSFile tempFile) throws IOException, InterruptedException, AS400SecurityException, ErrorCompletingRequestException;
}
