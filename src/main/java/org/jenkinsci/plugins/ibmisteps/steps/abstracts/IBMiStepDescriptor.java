package org.jenkinsci.plugins.ibmisteps.steps.abstracts;

import com.google.common.collect.ImmutableSet;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.ibmisteps.model.IBMiContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

import java.util.Set;

public abstract class IBMiStepDescriptor extends StepDescriptor {

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
        return ImmutableSet.of(Run.class, TaskListener.class, FilePath.class, IBMiContext.class);
    }
}
