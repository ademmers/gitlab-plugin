package com.dabsquared.gitlabjenkins.orgfolder;

import com.dabsquared.gitlabjenkins.scm.MergeRequestSCMHead;
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
        return head instanceof MergeRequestSCMHead;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return "GitLab Merge Request Jobs Only";
        }
    }
}
