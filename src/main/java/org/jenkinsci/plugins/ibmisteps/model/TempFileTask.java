package org.jenkinsci.plugins.ibmisteps.model;

import com.ibm.as400.access.IFSFile;

import java.io.IOException;

public interface TempFileTask {
    void run(IFSFile tempFile) throws IOException, InterruptedException;
}
