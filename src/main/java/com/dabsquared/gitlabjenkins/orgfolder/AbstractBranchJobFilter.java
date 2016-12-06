package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.views.ViewJobFilter;
import jenkins.scm.api.SCMHead;

import java.util.List;

abstract class AbstractBranchJobFilter extends ViewJobFilter {
    public AbstractBranchJobFilter() {}

    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all , View filteringView) {
        for (TopLevelItem i : all) {
            if (added.contains(i)) { continue; }

            Sniffer.BranchMatch b = Sniffer.matchBranch(i);
            if (b != null) {
                SCMHead head = b.getScmBranch().getHead();
                if(shouldShow(head)) {
                    added.add(i);
                }
            }
        }
        return added;
    }

    protected abstract boolean shouldShow(SCMHead head);
}
