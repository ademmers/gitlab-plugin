package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.model.Action;

import java.net.URL;

public class GitLabLink implements Action {

    private final String image;
    private final String url;

    public GitLabLink(String logo, String webUrl) {
        this.image = logo;
        this.url = webUrl;
    }

    public GitLabLink(String logo, URL webUrl) {
        this(logo, webUrl.toExternalForm());
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/gitlab-organization-folder/images/" + image + "/24x24.png";
    }

    @Override
    public String getDisplayName() {
        return "GitLab";
    }

    @Override
    public String getUrlName() {
        return url;
    }
}
