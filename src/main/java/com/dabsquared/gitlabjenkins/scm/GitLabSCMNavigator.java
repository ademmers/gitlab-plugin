package com.dabsquared.gitlabjenkins.scm;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class GitLabSCMNavigator extends SCMNavigator {

    private final String projectOwner;
    private final String credentialsId;
    private final String checkoutCredentialsId;
    private String pattern = ".*";
    private boolean autoRegisterHooks = false;
    private final String gitlabServerUrl;
    private int sshPort = 22;

    private transient GitLabConnector gitLabConnector;

    @DataBoundConstructor
    public GitLabSCMNavigator(String gitlabServerUrl, String projectOwner, String credentialsId, String checkoutCredentialsId){
        this.gitlabServerUrl = Util.fixEmpty(gitlabServerUrl);
        this.projectOwner = projectOwner;
        this.credentialsId = Util.fixEmpty(credentialsId);
        this.checkoutCredentialsId = checkoutCredentialsId;
    }

    @DataBoundSetter
    public void setPattern(String pattern) {
        Pattern.compile(pattern);
        this.pattern = pattern;
    }

    @DataBoundSetter
    public void setAutoRegisterHooks(boolean autoRegisterHooks) {
        this.autoRegisterHooks = autoRegisterHooks;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return checkoutCredentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isAutoRegisterHooks() {
        return autoRegisterHooks;
    }

    public int getSshPort() {
        return sshPort;
    }

    @DataBoundSetter
    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    @CheckForNull
    public String getGitLabServerUrl() {
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

    @Override
    public void visitSources(@Nonnull SCMSourceObserver observer) throws IOException, InterruptedException {
        TaskListener listener = observer.getListener();

        if(StringUtils.isBlank(projectOwner)) {
            listener.getLogger().format("Must specify a project owner%n");
            return;
        }

        GitLabApiToken credentials = getGitLabConnector().lookupCredentials(observer.getContext(), credentialsId, GitLabApiTokenImpl.class);

        if(credentials == null) {
            listener.getLogger().format("Connecting to %s with no credentials, anonymous access%n", gitlabServerUrl == null ? "https://gitlab.com" : gitlabServerUrl);
            return;
        } else {
            listener.getLogger().format("DEBUG: GitlabSCMNavigator.visitSources");
            listener.getLogger().format("Connecting to %s using %s%n", gitlabServerUrl == null ? "https://gitlab.com" : gitlabServerUrl, CredentialsNameProvider.name(credentials));
        }

        GitLabApi gitLab = getGitLabConnector().create(projectOwner, credentials);
        List<? extends Project> projects = gitLab.getProjects(); // Use all user accessible projects

        for (Project p : projects) {
            // filter out on owner(namespace)
            if(p.getNamespace().getPath().equals(projectOwner)) {
                add(listener, observer, p);
            }
        }
    }

    private void add(TaskListener listener, SCMSourceObserver observer, Project project) throws InterruptedException {
        String projectName = project.getName();

        if(!Pattern.compile(pattern).matcher(projectName).matches()) {
            listener.getLogger().format("Ignoring %s%n", projectName);
            return;
        }

        listener.getLogger().format("Proposing %s%n", projectName);
        if(Thread.interrupted()){
            throw new InterruptedException();
        }

        SCMSourceObserver.ProjectObserver projectObserver = observer.observe(projectName);
        GitLabSCMSource scmSource = new GitLabSCMSource(null, projectOwner, projectName);
        scmSource.setGitLabConnector(getGitLabConnector());
        scmSource.setCredentialsId(credentialsId);
        scmSource.setAutoRegisterHook(isAutoRegisterHooks());
        scmSource.setGitlabServerUrl(gitlabServerUrl);
        scmSource.setSshPort(sshPort);
        projectObserver.addSource(scmSource);
        projectObserver.complete();
    }

    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        public static final String ANONYMOUS = GitLabSCMSource.DescriptorImpl.ANONYMOUS;
        public static final String SAME = GitLabSCMSource.DescriptorImpl.SAME;

        @Override
        public String getDisplayName() {
            return "GitLab Group/Project";
        }

        @Override
        public String getDescription() {
            return "Scans a Gitlab Service for all repositories matching some defined markers.";
        }

        @Override
        public String getIconFilePathPattern() {
            return "plugin/gitlab-branch-source/images/:size/gitlab-scmnavigator.png";
        }

        @Override
        public SCMNavigator newInstance(String name) {
            return new GitLabSCMNavigator(null ,name, null, GitLabSCMSource.DescriptorImpl.SAME);
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if (!value.isEmpty()) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Credentials are required for build notifications");
            }
        }

        public FormValidation doCheckGitlabServerUrl(@QueryParameter String gitlabServerUrl) {
            return GitLabSCMSource.DescriptorImpl.doCheckGitlabServerUrl(gitlabServerUrl);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gitlabServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.includeEmptyValue();
            GitLabConnector.fillCredentials(result, context, gitlabServerUrl);
            return result;
        }

        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String gitlabServerUrl) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.add("- same as scan credentials -", GitLabSCMSource.DescriptorImpl.SAME);
            result.add("- anonymous -", GitLabSCMSource.DescriptorImpl.ANONYMOUS);
            GitLabConnector.fillCheckoutCredentials(result, context, gitlabServerUrl);
            return result;
        }

    }
}

