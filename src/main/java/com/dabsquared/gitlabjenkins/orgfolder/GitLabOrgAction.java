package com.dabsquared.gitlabjenkins.orgfolder;

import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import hudson.model.InvisibleAction;

import java.io.IOException;
import java.net.URL;

public class GitLabOrgAction extends InvisibleAction {
    private final URL url;
    private final String name;
    private final URL avatar;

    GitLabOrgAction(User org) throws IOException {
        this.url = org.getWebUrl();
        this.name = org.getName();
        this.avatar = org.getAvatarUrl();
    }

    public URL getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public URL getAvatar() {
        return avatar;
    }
}

