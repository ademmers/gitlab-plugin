package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.views.ViewJobFilter;
import jenkins.scm.api.SCMHead;
import org.kohsuke.stapler.DataBoundConstructor;

public class BranchJobFilter extends AbstractBranchJobFilter {
    @DataBoundConstructor
    public BranchJobFilter() {}

    @Override
    protected boolean shouldShow(SCMHead head) {
        // TODO: implement MergeRequestSCMHead in gitlab-branch-source
        //return !(head instanceof MergeRequestSCMHead);
        return true;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return "GitLab Branch Jobs Only";
        }
    }
}
