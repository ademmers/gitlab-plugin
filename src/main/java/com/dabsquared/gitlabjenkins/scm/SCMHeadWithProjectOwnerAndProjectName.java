package com.dabsquared.gitlabjenkins.scm;

import jenkins.scm.api.SCMHead;

import javax.annotation.CheckForNull;

public class SCMHeadWithProjectOwnerAndProjectName extends SCMHead {

    private static final long serialVersionUID = 1L;

    private final String projectOwner;

    private final String projectName;

    private final Integer mergeRequestId;

    private static final String MR_BRANCH_PREFIX = "MR-";

    public SCMHeadWithProjectOwnerAndProjectName(String projectOwner, String projectName, String branchName, Integer mergeRequestId) {
        super(branchName);
        this.projectOwner = projectOwner;
        this.projectName = projectName;
        this.mergeRequestId = mergeRequestId;
    }

    public SCMHeadWithProjectOwnerAndProjectName(String projectOwner, String projectName, String branchName) {
        this(projectOwner, projectName, branchName, null);
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * @return the original branch name without the "MR-owner-" part.
     */
    public String getBranchName() {
        return super.getName();
    }

    /**
     * Returns the prettified branch name by adding "MR-[ID]" if the branch is coming from a MR.
     * Use {@link #getBranchName()} to get the real branch name.
     */
    @Override
    public String getName() {
        return mergeRequestId != null ? MR_BRANCH_PREFIX + mergeRequestId : getBranchName();
    }

    @CheckForNull
    public Integer getMergeRequestId() {
        return mergeRequestId;
    }

}
