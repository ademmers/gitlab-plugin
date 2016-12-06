package com.dabsquared.gitlabjenkins.orgfolder;

import com.dabsquared.gitlabjenkins.scm.GitLabSCMNavigator;
import hudson.model.Item;
import jenkins.branch.Branch;
import jenkins.branch.OrganizationFolder;
import jenkins.scm.api.SCMNavigator;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.util.List;

public class Sniffer {

    static class OrgMatch {
        final OrganizationFolder folder;
        final GitLabSCMNavigator scm;

        public OrgMatch(OrganizationFolder folder, GitLabSCMNavigator scm) {
            this.folder = folder;
            this.scm = scm;
        }
    }

    public static OrgMatch matchOrg(Object item) {
        if (item instanceof  OrganizationFolder) {
            OrganizationFolder of = (OrganizationFolder) item;
            List<SCMNavigator> navigators = of.getNavigators();
            if(navigators != null && navigators.size() > 0) {
                SCMNavigator navigator = navigators.get(0);
                if (navigator instanceof GitLabSCMNavigator) {
                    return new OrgMatch(of, (GitLabSCMNavigator) navigator);
                }
            }
        }
        return null;
    }

    static class ProjectMatch extends OrgMatch {
        final WorkflowMultiBranchProject project;

        public ProjectMatch(OrgMatch x , WorkflowMultiBranchProject project) {
            super(x.folder, x.scm);
            this.project = project;
        }
    }

    public static ProjectMatch matchProject(Object item) {
        if (item instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) item;
            OrgMatch orgMatch = matchOrg(project.getParent());

            if (orgMatch != null) {
                return new ProjectMatch(orgMatch, project);
            }
        }
        return null;
    }

    static class BranchMatch extends ProjectMatch {
        final WorkflowJob branch;

        public BranchMatch(ProjectMatch x, WorkflowJob branch) {
            super(x, x.project);
            this.branch = branch;
        }

        public Branch getScmBranch() {
            return project.getProjectFactory().getBranch(branch);
        }
    }

    public static BranchMatch matchBranch(Item item) {
        if (item instanceof WorkflowJob) {
            WorkflowJob branch = (WorkflowJob) item;
            ProjectMatch x = matchProject(item.getParent());

            if(x != null) {
                return new BranchMatch(x, branch);
            }
        }
        return null;
    }
}
