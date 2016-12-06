package com.dabsquared.gitlabjenkins.scm;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class ProjectUriResolver {

    private boolean server = false;
    private URL url;

    public ProjectUriResolver(String baseUrl) throws MalformedURLException {
        if (baseUrl != null) {
            this.url = new URL(baseUrl);
        }
        if (!StringUtils.isBlank(baseUrl)) {
            server = true;
        }
    }

    public abstract String getProjectUri(String projectOwner, String projectName);

    public String getUrlAsString() {
        return url == null ? "https://gitlab.com" : url.toExternalForm();
    }

    @CheckForNull
    @NonNull
    public URL getUrl() {
        return url;
    }

    /**
     * @return the authority part of the {@link #url} (which is host:port)
     */
    public String getAuthority() {
        if (isServer()) {
            return url.getAuthority();
        }
        return "gitlab.org";
    }

    /**
     * @return the host part of the {@link #url}
     */
    public String getHost() {
        if (isServer()) {
            return url.getHost();
        }
        return "gitlab.com";
    }

    protected boolean isServer() {
        return server;
    }
}

