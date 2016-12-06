package com.dabsquared.gitlabjenkins.orgfolder;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import hudson.model.InvisibleAction;

import java.net.MalformedURLException;
import java.net.URL;

public class GitLabProjectAction extends InvisibleAction {
    private final URL url;
    private final String description;
    private final String homepage;

    GitLabProjectAction(Project project) throws MalformedURLException {
        this.url = new URL(project.getHttpUrlToRepo());
        this.description = project.getName();
        this.homepage = project.getWebUrl();
    }

    public URL getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getHomepage() {
        return homepage;
    }
}
