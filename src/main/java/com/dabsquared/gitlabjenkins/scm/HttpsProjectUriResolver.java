package com.dabsquared.gitlabjenkins.scm;

import java.net.MalformedURLException;

public class HttpsProjectUriResolver extends ProjectUriResolver {

    public HttpsProjectUriResolver(String baseUrl) throws MalformedURLException {
        super(baseUrl);
    }

    @Override
    public String getProjectUri(String projectOwner, String projectName) {
        if (getUrl() == null || getUrl().toString().startsWith("https://")) {
            return "https://" + getAuthority() + "/" + projectOwner + "/" + projectName + ".git";
        } else {
            return "http://" + getAuthority() + "/" + projectOwner + "/" + projectName + ".git";
        }
    }
}
