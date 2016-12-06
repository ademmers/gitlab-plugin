package com.dabsquared.gitlabjenkins.orgfolder;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.FolderIcon;
import com.cloudbees.hudson.plugins.folder.FolderIconDescriptor;
import hudson.Extension;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import java.net.URL;

public class GitLabOrgIcon extends FolderIcon {

    private AbstractFolder<?> folder;

    @DataBoundConstructor
    public GitLabOrgIcon() {}

    @Override
    protected void setOwner(AbstractFolder<?> folder) {
        this.folder = folder;
    }

    @Override
    public String getImageOf(String s) {
        String url = getAvatarUrl().toString();
        if(url == null) {
            // fall back to the generic gitlab org icon
            return Stapler.getCurrentRequest().getContextPath() + Hudson.RESOURCE_PATH + "/plugin/gitlab-organization-folder/images/logo/" + s + ".png";
        } else {
            String[] xy = s.split("x");
            if (xy.length == 0) {
                return url;
            }
            if (url.contains("?")) {
                return url + "&s=" + xy[0];
            } else {
                return url + "?s=" + xy[0];
            }
        }
    }

    private URL getAvatarUrl() {
        if (folder == null) { return null; }
        GitLabOrgAction p = folder.getAction(GitLabOrgAction.class);
        if(p == null) { return null; }
        return p.getAvatar();
    }

    @Override
    public String getDescription() {
        return folder != null ? folder.getName() : "GitLab";
    }

    @Extension
    public static class DescriptorImpl extends FolderIconDescriptor {
        @Override
        public String getDisplayName() {
            return "GitLab Organization Avatar";
        }
    }
}
