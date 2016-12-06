package com.dabsquared.gitlabjenkins.orgfolder;

import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.scm.GitLabConnector;
import com.dabsquared.gitlabjenkins.scm.GitLabSCMNavigator;
import hudson.BulkChange;
import hudson.Extension;
import hudson.model.AllView;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ListView;
import hudson.model.listeners.ItemListener;
import hudson.util.DescribableList;
import hudson.views.JobColumn;
import hudson.views.ListViewColumn;
import hudson.views.StatusColumn;
import hudson.views.WeatherColumn;
import jenkins.branch.Branch;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.actions.ChangeRequestAction;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

@Extension
public class MainLogic {
    public void applyOrg(OrganizationFolder of, GitLabSCMNavigator scm) throws IOException {
        if(UPDATING.get().add(of)) {
            BulkChange bc = new BulkChange(of);

            try {
                GitLabApi gitLab = connect(of, scm);
                User u = gitLab.getCurrentUser();

                of.setIcon(new GitLabOrgIcon());
                of.replaceAction(new GitLabOrgAction(u));
                of.replaceAction(new GitLabLink("logo", u.getWebUrl()));

                if(of.getDisplayNameOrNull() == null ) {
                    of.setDisplayName((u.getName()));
                }

                if(of.getView("Projects") == null &&  of.getView("All") instanceof AllView) {
                    ListView lv = new ListView("Projects");
                    lv.getColumns().replaceBy(asList(
                        new StatusColumn(),
                        new WeatherColumn(),
                        new CustomNameJobColumn(Messages.class, Messages._ListViewColumn_Project()),
                        new ProjectDescriptionColumn()
                    ));
                    lv.setIncludeRegex(".*");
                    of.addView(lv);
                    of.deleteView(of.getView("All"));
                    of.setPrimaryView(lv);
                }

                // TODO:
                // When looking at the github part of this code, this places a webhook on a user/group(org)
                // This does not exist in that fashion in gitlab.
                // This next part is actually what maps to system hooks in gitlab
                // this is not (yet) forseen in eighter gitlab-plugin or gitlab-branch-source
                // this requires a review of the credentials on the acuired gitlab client instance, as
                //   manipulation of system hooks requires an administrative account
                // might be this code only will implement the hook internal (within Jenkins) registration
                // and the actual placement of the system hook remains a onetime job for an actual administrative person
                /*
                FileBoolean projectHook = new FileBoolean(new File(of.getRootDir(), "GitlabProjectHook." + scm.getProjectOwner()));
                if(orghook.isOff()) {
                    projectHook {
                        String url = Jenkins.getActiveInstance().getRootUrl() + "gitlab-webhook/";
                        if(!existsHook(url,scm)) {
                            //non existing yet: gitlab.addSystemHook();
                        }
                    }
                }*/

                bc.commit();
            } finally {
                bc.abort();
                UPDATING.get().remove(of);
            }
        }
    }

    public void applyProject(WorkflowMultiBranchProject item, GitLabSCMNavigator scm) throws IOException {
        if(UPDATING.get().add(item)) {
            BulkChange bc = new BulkChange(item);

            try {
                GitLabApi gitLab = connect(item, scm);
                Project project = gitLab.getProject(scm.getProjectOwner() + "/" + item.getName());

                item.setIcon(new GitLabProjectIcon());
                item.replaceAction(new GitLabProjectAction(project));
                item.replaceAction(new GitLabLink("project", project.getWebUrl()));

                if(item.getView("Branches") == null && item.getView("All") instanceof AllView) {
                    ListView bv = new ListView("Branches");
                    DescribableList<ListViewColumn, Descriptor<ListViewColumn>> cols = bv.getColumns();
                    JobColumn name = cols.get(JobColumn.class);

                    if(name != null) {
                        cols.replace(name, new CustomNameJobColumn(Messages.class, Messages._ListViewColumn_Branch()));
                    }
                    bv.getJobFilters().add(new BranchJobFilter());

                    ListView mv = new ListView("Merge Requests");
                    cols = mv.getColumns();
                    name = cols.get(JobColumn.class);

                    if(name != null) {
                        cols.replace(name, new CustomNameJobColumn(Messages.class, Messages._ListViewColumn_MergeRequest()));
                    }
                    mv.getJobFilters().add(new MergeRequestJobFilter());

                    item.addView(bv);
                    item.addView(mv);
                    item.deleteView(item.getView("All"));
                    item.setPrimaryView(bv);
                }

                bc.commit();
            } finally {
                bc.abort();
                UPDATING.get().remove(item);
            }
        }
    }

    public void applyBranch(WorkflowJob branch, WorkflowMultiBranchProject project, GitLabSCMNavigator scm) throws IOException {
        if (UPDATING.get().add(branch)) {
            BulkChange bc = new BulkChange(branch);

            try {
                Branch b = project.getProjectFactory().getBranch(branch);
                GitLabLink projectLink = project.getAction(GitLabLink.class);

                if(projectLink != null) {
                    ChangeRequestAction action = b.getHead().getAction(ChangeRequestAction.class);
                    String url;
                    if(action == null) {
                        url = projectLink.getUrl() + "/tree/" + b.getName();
                    } else {
                        url = projectLink.getUrl() + "/merge_requests/" + action.getId();
                    }
                    branch.replaceAction(new GitLabLink("branch", url));
                    bc.commit();
                }
            } finally {
                bc.abort();
                UPDATING.get().remove(branch);
            }
        }
    }

    GitLabApi connect(SCMSourceOwner of, GitLabSCMNavigator n) throws IOException {
        GitLabConnector connector = new GitLabConnector(n.getGitLabServerUrl());
        GitLabApiToken credentials = connector.lookupCredentials(of, n.getGitLabServerUrl(), GitLabApiTokenImpl.class);
        return connector.create(n.getProjectOwner(), credentials);
    }

    public static MainLogic get() {
        return Jenkins.getActiveInstance().getInjector().getInstance(MainLogic.class);
    }

    /**
     * Keeps track of what we are updating to avoid recursion, because {@link AbstractItem#save()}
     * triggers {@link ItemListener}.
     */
    private final ThreadLocal<Set<Item>> UPDATING = new ThreadLocal<Set<Item>>() {
        @Override
        protected Set<Item> initialValue() {
            return new HashSet<>();
        }
    };

    private static final Logger LOGGER = Logger.getLogger(MainLogic.class.getName());
}
