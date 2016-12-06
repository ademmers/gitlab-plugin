package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.Extension;
import hudson.model.Job;
import jenkins.plugins.git.GitSCMSource;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.LibraryResolver;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Extension
public class GitLabLibraryResolver extends LibraryResolver {

    @Override
    public boolean isTrusted() {
        return false;
    }

    @Nonnull
    @Override
    public Collection<LibraryConfiguration> forJob(@Nonnull Job<?, ?> job, @Nonnull Map<String, String> libraryVersions) {
        List<LibraryConfiguration> libs = new ArrayList<>();
        for(Map.Entry<String,String> entry : libraryVersions.entrySet()) {
            if (entry.getKey().matches("gitlab[.]com/([^/]+)/([^/]+)")) {
                String name = entry.getKey();
                LibraryConfiguration lib = new LibraryConfiguration(name, new SCMSourceRetriever(new GitSCMSource(null, "https://" + name + ".git", "", "*", "", true)));
                lib.setDefaultVersion("master");
                libs.add(lib);
            }
        }
        return libs;
    }
}
