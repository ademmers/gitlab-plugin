package com.dabsquared.gitlabjenkins.scm;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.BuildChooserSetting;
import hudson.plugins.git.util.BuildChooser;
import hudson.plugins.git.util.DefaultBuildChooser;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class GitLabSCMSource extends SCMSource {

    /**
     * Credentials used to access the Gitlab REST API.
     */
    private String credentialsId;

    /**
     * Credentials used to clone the projectName/repositories.
     */
    private String checkoutCredentialsId;

    /**
     * Project owner.
     * Used to build the project URL.
     */
    private final String projectOwner;

    /**
     * Project name.
     * Used to build the project URL.
     */
    private final String projectName;

    /**
     * Ant match expression that indicates what branches to include in the retrieve process.
     */
    private String includes = "*";

    /**
     * Ant match expression that indicates what branches to exclude in the retrieve process.
     */
    private String excludes = "";

    /**
     * If true, a webhook will be auto-registered in the project managed by this source.
     */
    private boolean autoRegisterHook = false;

    /**
     * Gitlab Server URL.
     * An specific HTTP client is used if this field is not null.
     */
    private String gitlabServerUrl;

    /**
     * Port used by Gitlab Server for SSH clone.
     * 22 by default
     */
    private int sshPort = 22;

    /**
     * Gitlab API client connector.
     */
    private transient GitLabConnector gitLabConnector;

    private static final Logger LOGGER = Logger.getLogger(GitLabSCMSource.class.getName());

    @DataBoundConstructor
    public GitLabSCMSource(String id, String projectOwner, String projectName) {
        super(id);
        this.projectOwner = projectOwner;
        this.projectName = projectName;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return checkoutCredentialsId;
    }

    @DataBoundSetter
    public void setCheckoutCredentialsId(String checkoutCredentialsId) {
        this.checkoutCredentialsId = checkoutCredentialsId;
    }

    public String getIncludes() {
        return includes;
    }

    @DataBoundSetter
    public void setIncludes(@NonNull String includes) {
        Pattern.compile(getPattern(includes));
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    @DataBoundSetter
    public void setExcludes(@NonNull String excludes) {
        Pattern.compile(getPattern(excludes));
        this.excludes = excludes;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public String getProjectName() {
        return projectName;
    }

    @DataBoundSetter
    public void setAutoRegisterHook(boolean autoRegisterHook) {
        this.autoRegisterHook = autoRegisterHook;
    }

    public boolean isAutoRegisterHook() {
        return autoRegisterHook;
    }

    public int getSshPort() {
        return sshPort;
    }

    @DataBoundSetter
    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    @DataBoundSetter
    public void setGitlabServerUrl(String url) {
        this.gitlabServerUrl = Util.fixEmpty(url);
        if (this.gitlabServerUrl != null) {
            // Remove a possible trailing slash
            this.gitlabServerUrl = this.gitlabServerUrl.replaceAll("/$", "");
        }
    }

    @CheckForNull
    public String getGitlabServerUrl() {
        return gitlabServerUrl;
    }

    public void setGitLabConnector(@NonNull GitLabConnector gitLabConnector) {
        this.gitLabConnector = gitLabConnector;
    }

    private GitLabConnector getGitLabConnector() {
        if (gitLabConnector == null) {
            gitLabConnector = new GitLabConnector(gitlabServerUrl);
        }
        return gitLabConnector;
    }

    public String getRemote(@NonNull String projectOwner, @NonNull String projectName) {
        return getUriResolver().getProjectUri(projectOwner, projectName);
    }

    @CheckForNull
    GitLabApiToken getScanCredentials() {
        return getGitLabConnector().lookupCredentials(getOwner(), credentialsId, GitLabApiTokenImpl.class);
    }

    private StandardCredentials getCheckoutCredentials() {
        return getGitLabConnector().lookupCredentials(getOwner(), getCheckoutEffectiveCredentials(), StandardCredentials.class);
    }

    public String getRemoteName() {
        return "origin";
    }

    /**
     * Returns true if the branchName isn't matched by includes or is matched by excludes.
     *
     * @param branchName
     * @return true if branchName is excluded or is not included
     */
    private boolean isExcluded(String branchName) {
        return !Pattern.matches(getPattern(getIncludes()), branchName)
            || Pattern.matches(getPattern(getExcludes()), branchName);
    }

    /**
     * Returns the pattern corresponding to the branches containing wildcards.
     *
     * @param branches space separated list of expressions.
     *        For example "*" which would match all branches and branch* would match branch1, branch2, etc.
     * @return pattern corresponding to the branches containing wildcards (ready to be used by {@link Pattern})
     */
    private String getPattern(String branches) {
        StringBuilder quotedBranches = new StringBuilder();
        for (String wildcard : branches.split(" ")) {
            StringBuilder quotedBranch = new StringBuilder();
            for (String branch : wildcard.split("\\*")) {
                if (wildcard.startsWith("*") || quotedBranches.length() > 0) {
                    quotedBranch.append(".*");
                }
                quotedBranch.append(Pattern.quote(branch));
            }
            if (wildcard.endsWith("*")) {
                quotedBranch.append(".*");
            }
            if (quotedBranches.length() > 0) {
                quotedBranches.append("|");
            }
            quotedBranches.append(quotedBranch);
        }
        return quotedBranches.toString();
    }

    private ProjectUriResolver getUriResolver() {
        try {
            if (StringUtils.isBlank(checkoutCredentialsId)) {
                return new HttpsProjectUriResolver(gitlabServerUrl);
            } else {
                if (getCheckoutCredentials() instanceof SSHUserPrivateKey) {
                    return new SshProjectUriResolver(gitlabServerUrl, sshPort);
                } else {
                    // Defaults to HTTPS
                    return new HttpsProjectUriResolver(gitlabServerUrl);
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Gitlab URL is not valid", e);
            // The URL is validated before, so this should never happen
            throw new IllegalStateException(e);
        }
    }

    private String getCheckoutEffectiveCredentials() {
        if (DescriptorImpl.ANONYMOUS.equals(checkoutCredentialsId)) {
            return null;
        } else if (DescriptorImpl.SAME.equals(checkoutCredentialsId)) {
            return credentialsId;
        } else {
            return checkoutCredentialsId;
        }
    }

    @NonNull
    @Override
    protected void retrieve(@NonNull SCMHeadObserver scmHeadObserver, @NonNull TaskListener taskListener) throws IOException, InterruptedException {
        GitLabApiToken scanCredentials = getScanCredentials();
        if(scanCredentials == null) {
            taskListener.getLogger().format("Connecting to %s with no credentials, anonymous access%n", gitlabServerUrl == null ? "https://gitlab.com" : gitlabServerUrl);
        } else {
            taskListener.getLogger().format("Connecting to %s using %s%n", gitlabServerUrl == null ? "https://gitlab.com" : gitlabServerUrl, CredentialsNameProvider.name(scanCredentials));
        }
        retrieveBranches(scmHeadObserver, taskListener);
        retrieveMergeRequests(scmHeadObserver, taskListener);
    }

    private void retrieveMergeRequests(SCMHeadObserver scmHeadObserver, TaskListener taskListener) throws IOException {
        String fullName = projectOwner + "/" + projectName;
        taskListener.getLogger().println("Looking up " + fullName + " for merge requests");

        final GitLabApi gitlab = getGitLabConnector().create(projectOwner, projectName, getScanCredentials());
        Project p = gitlab.getProject(fullName);

        List<? extends MergeRequest> merges = gitlab.getMergeRequests(p.getId().toString(), State.opened, 0, 1000);
        for( final MergeRequest merge: merges) {
            Project sourceProj = gitlab.getProject(merge.getSourceProjectId().toString());
            taskListener.getLogger().println("Checking MR from " + sourceProj.getNamespace() + "/" + sourceProj.getName() + " and branch "
                + merge.getSourceBranch());

            Branch b = gitlab.getBranch(merge.getSourceProjectId().toString(), merge.getSourceBranch());
            String commitHash = b.getCommit().getId();
            observe(scmHeadObserver, taskListener,
                sourceProj.getNamespace().toString(),
                sourceProj.getName(),
                merge.getSourceBranch(),
                commitHash, merge.getId());

            if(!scmHeadObserver.isObserving()) {
                return;
            }
        }
    }

    private void retrieveBranches(SCMHeadObserver scmHeadObserver, TaskListener taskListener) throws IOException, InterruptedException {
        String fullName = projectOwner + "/" + projectName;
        taskListener.getLogger().println("Looking up " + fullName + " for branches");

        final GitLabApi gitlab = getGitLabConnector().create(projectOwner, projectName, getScanCredentials());
        Project p = gitlab.getProject(fullName);
        List<? extends Branch> branches = gitlab.getBranches(p.getId().toString());

        for(Branch branch : branches) {
            taskListener.getLogger().println("Checking branch " + branch.getName() + " from " + fullName);
            observe(scmHeadObserver, taskListener, projectOwner, projectName, branch.getName(), branch.getCommit().getId(), 0);
        }
    }

    private void observe(SCMHeadObserver scmHeadObserver, final TaskListener taskListener, final String projectOwner,
                         final String projectName, final String branchName, final String commitHash, final int mrId) throws IOException {
        if(isExcluded(branchName)) {
            return;
        }
        final GitLabApi gitlab = getGitLabConnector().create(projectOwner, projectName, getScanCredentials());
        SCMSourceCriteria branchCriteria = getCriteria();
        if(branchCriteria != null) {
            SCMSourceCriteria.Probe probe = new SCMSourceCriteria.Probe() {

                @Override
                public String name() {
                    return branchName;
                }

                @Override
                public long lastModified() {
                    Project p = gitlab.getProject(projectOwner);
                    Branch b = gitlab.getBranch(p.getId().toString(), branchName);
                    Commit commit = b.getCommit();
                    if(commit == null) {
                        taskListener.getLogger().format("Can not resolve commit by hash [%s] on projectName %s/%s%n",
                            commitHash, projectOwner, projectName);
                        return 0;
                    }
                    return commit.getAuthoredDate().getTime();
                }

                @Override
                public boolean exists(@NonNull String path) throws IOException {
                    Project p = gitlab.getProject(projectOwner + "/" + projectName);
                    try {
                        RepositoryFile file = gitlab.getRepositoryFile(p.getId().toString(), path, branchName);
                        return (file != null);
                    } catch (Exception ex) {
                        return false;
                    }
                }
            };
            if(branchCriteria.isHead(probe, taskListener)) {
                taskListener.getLogger().println("Met criteria");
            } else {
                taskListener.getLogger().println("Does not meet criteria");
                return;
            }

            SCMHeadWithProjectOwnerAndProjectName head = new SCMHeadWithProjectOwnerAndProjectName(projectOwner, projectName, branchName, mrId);
            SCMRevision revision = new AbstractGitSCMSource.SCMRevisionImpl(head, commitHash);
            scmHeadObserver.observe(head, revision);
        }
    }

    @Override
    public SCM build(@NonNull SCMHead scmHead, @CheckForNull SCMRevision scmRevision) {
        if(scmHead instanceof SCMHeadWithProjectOwnerAndProjectName) {
            SCMHeadWithProjectOwnerAndProjectName h = (SCMHeadWithProjectOwnerAndProjectName) scmHead;

            BuildChooser buildChooser = scmRevision instanceof AbstractGitSCMSource.SCMRevisionImpl ? new AbstractGitSCMSource.SpecificRevisionBuildChooser(
                (AbstractGitSCMSource.SCMRevisionImpl) scmRevision) : new DefaultBuildChooser();

            return new GitSCM( getGitRemoteConfigs(h), Collections.singletonList(new BranchSpec(h.getBranchName())), false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new BuildChooserSetting(buildChooser)));
        }
        throw new IllegalArgumentException("An SCMHeadWithProjectOwnerAndProjectName requires a parameter");
    }

    private List<UserRemoteConfig> getGitRemoteConfigs(SCMHeadWithProjectOwnerAndProjectName head) {
        List<UserRemoteConfig> result = new ArrayList<UserRemoteConfig>();
        String remote = getRemote(head.getProjectOwner(), head.getProjectName());
        result.add(new UserRemoteConfig(remote, getRemoteName(), "+refs/heads/" + head.getBranchName(), getCheckoutEffectiveCredentials()));
        return result;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        public static final String ANONYMOUS = "ANONYMOUS";
        public static final String SAME = "SAME";

        @Override
        public String getDisplayName() {
            return "Gitlab";
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if (!value.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for notifications");
            }
        }

        public static FormValidation doCheckGitlabServerUrl(@QueryParameter String gitlabServerUrl) {
            String url = Util.fixEmpty(gitlabServerUrl);
            if (url == null) {
                return FormValidation.ok();
            }
            try {
                new URL(gitlabServerUrl);
            } catch (MalformedURLException e) {
                return FormValidation.error("Invalid URL: " +  e.getMessage());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gitlabServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.includeEmptyValue();
            GitLabConnector.fillCredentials(result, context, gitlabServerUrl);
            return result;
        }

        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gitlabServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", SAME);
            result.add("- anonymous -", ANONYMOUS);
            GitLabConnector.fillCheckoutCredentials(result, context, gitlabServerUrl);
            return result;
        }

    }
}

