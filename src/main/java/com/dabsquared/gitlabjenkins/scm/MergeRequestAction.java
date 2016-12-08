package com.dabsquared.gitlabjenkins.scm;

import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.actions.ChangeRequestAction;

import java.net.URL;

public class MergeRequestAction extends ChangeRequestAction {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final int iid;
    private final String title;
    private final String userLogin;
    private final String baseRef;

    MergeRequestAction(MergeRequest mr) {
        id = mr.getId();
        iid = mr.getIid();
        title = mr.getTitle();
        userLogin = mr.getAuthor().getName();
        baseRef = mr.getSourceBranch();
    }

    @NonNull
    @Override
    public String getId() {
        return Integer.toString(id);
    }

    @NonNull
    public String getIid() {
        return Integer.toString(iid);
    }

    @NonNull
    @Override
    public String getTitle() {
        return title;
    }

    @NonNull
    @Override
    public String getAuthor() {
        return userLogin;
    }

    @NonNull
    @Override
    public SCMHead getTarget() {
        return new BranchSCMHead(baseRef);
    }
}
