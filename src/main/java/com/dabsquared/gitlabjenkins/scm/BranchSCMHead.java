package com.dabsquared.gitlabjenkins.scm;

import jenkins.scm.api.SCMHead;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;

/**
 * Head corresponding to a branch.
 * @since FIXME
 */
public class BranchSCMHead extends SCMHead {
    /**
     * {@inheritDoc}
     */
    public BranchSCMHead(@NonNull String name) {
        super(name);
    }

    public String getPronoun() {
        return Messages.BranchSCMHead_Pronoun();
    }
}
