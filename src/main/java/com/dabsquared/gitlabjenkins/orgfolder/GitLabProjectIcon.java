package com.dabsquared.gitlabjenkins.orgfolder;

import com.cloudbees.hudson.plugins.folder.FolderIcon;
import com.cloudbees.hudson.plugins.folder.FolderIconDescriptor;
import hudson.Extension;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

public class GitLabProjectIcon extends FolderIcon {
    @DataBoundConstructor
    public GitLabProjectIcon() {}

    @Override
    public String getImageOf(String s) {
        return Stapler.getCurrentRequest().getContextPath() + Hudson.RESOURCE_PATH + "/plugin/gitlab-organization-folder/images/project/" + s + ".png";
    }

    @Override
    public String getDescription() {
        return "Project";
    }

    @Extension
    public static class DescriptorImpl extends FolderIconDescriptor {
        @Override
        public String getDisplayName() {
            return "GitLab Project Icon";
        }
    }

}
