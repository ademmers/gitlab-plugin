package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.views.ViewJobFilter;
import jenkins.scm.api.SCMHead;
import org.kohsuke.stapler.DataBoundConstructor;

public class MergeRequestJobFilter extends AbstractBranchJobFilter {
    @DataBoundConstructor
    public MergeRequestJobFilter() {}

    @Override
    protected boolean shouldShow(SCMHead head) {
        // TODO: actually implement MergeRequestSCMHead in gitlab-branch-source
        // return head instanceof MergeRequestSCMHead;
        // filter everything for now (opposite of branch filter, where merge jobs will be listed as well)
        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return "GitLab Merge Request Jobs Only";
        }
    }
}
