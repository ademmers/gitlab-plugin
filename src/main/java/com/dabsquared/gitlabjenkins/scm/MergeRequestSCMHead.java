package com.dabsquared.gitlabjenkins.scm;

import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import jenkins.scm.api.SCMHead;

import java.util.LinkedList;
import java.util.List;

public final class MergeRequestSCMHead extends SCMHead
{
    private static final long serialVersionUID = 1;

    private final MergeRequestAction metadata;
    private Boolean merge;
    private final Boolean trusted;

    public MergeRequestSCMHead(MergeRequest mr, String name, boolean merge, boolean trusted) {
        super(name);
        metadata = new MergeRequestAction(mr);
        this.merge = merge;
        this.trusted = trusted;
    }

    public String getPronoun() {
        return Messages.MergeRequestSCMHead_Pronoun();
    }

    public int getNumber() {
        if (metadata != null) {
            return Integer.parseInt(metadata.getIid());
        } else { // settings compatibility
            // if predating PullRequestAction, then also predate -merged/-unmerged suffices
            return Integer.parseInt(getName().substring("MR-".length()));
        }
    }

    /** Default for old settings. */
    private Object readResolve() {
        if (merge == null) {
            merge = true;
        }
        // leave trusted at false to be on the safe side
        return this;
    }

    /**
     * Whether we intend to build the merge of the PR head with the base branch.
     *
     */
    public boolean isMerge() {
        return merge;
    }

    /**
     * Whether this PR was observed to have come from a trusted author.
     */
    public boolean isTrusted() {
        return trusted;
    }

    @Override
    public List<? extends Action> getAllActions() {
        List<Action> actions = new LinkedList<Action>(super.getAllActions());
        actions.add(metadata);
        return actions;
    }
}
