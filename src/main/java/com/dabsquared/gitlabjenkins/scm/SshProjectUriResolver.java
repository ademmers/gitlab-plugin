package com.dabsquared.gitlabjenkins.scm;

import java.net.MalformedURLException;
import java.net.URL;

public class SshProjectUriResolver extends ProjectUriResolver {

    private int sshPort;

    public SshProjectUriResolver(String baseUrl, int sshPort) throws MalformedURLException {
        super(baseUrl);
        if (sshPort > 0) {
            this.sshPort = sshPort;
        } else {
            URL url = getUrl();
            if (url != null) {
                if (url.getPort() > 0){
                    this.sshPort = url.getPort();
                } else {
                    this.sshPort = url.getDefaultPort();
                }
            }
        }
    }

    @Override
    public String getProjectUri(String projectOwner, String projectName) {
        if (isServer()) {
            return "ssh://gitlab@" + getHost() + ":" + sshPort + "/" + projectOwner + "/" + projectName + ".git";
        } else {
            return "gitlab@" + getHost() + ":" + projectOwner + "/" + projectName + ".git";
        }
    }
}

