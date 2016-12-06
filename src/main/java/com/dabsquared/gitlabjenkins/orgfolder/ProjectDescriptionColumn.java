package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.Extension;
import hudson.model.Item;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;

public class ProjectDescriptionColumn extends ListViewColumn {

    @DataBoundConstructor
    public ProjectDescriptionColumn() {}

    public GitLabProjectAction getPropertyOf(Item item) {
        if (item instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject job = (WorkflowMultiBranchProject) item;
            return job.getAction(GitLabProjectAction.class);
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "GitLab Project Description";
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }
    }
}

