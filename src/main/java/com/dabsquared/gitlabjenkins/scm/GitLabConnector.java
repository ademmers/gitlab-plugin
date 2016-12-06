package com.dabsquared.gitlabjenkins.scm;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl;
import com.dabsquared.gitlabjenkins.gitlab.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabSession;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

public class GitLabConnector {

    private String serverUrl;

    public GitLabConnector() {
    }

    public GitLabConnector(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public GitLabApi create(String projectOwner, String projectName, @NonNull GitLabApiToken token) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader obj = mapper.reader(String.class);
        obj.getClass().getClassLoader();

        return (GitLabApi) GitLabClientBuilder.buildClient(serverUrl, token.getId(), true, 3, 30);
    }

    public static File findFileOnClassPath(final String fileName) {
        final String classpath = System.getProperty("java.class.path");
        final String pathSeparator = System.getProperty("path.separator");
        final StringTokenizer tokenizer = new StringTokenizer(classpath, pathSeparator);

        while (tokenizer.hasMoreTokens()) {
            final String pathElement = tokenizer.nextToken();
            System.out.println("pathElement=" + pathElement);
            final File directoryOrJar = new File(pathElement);
            final File absoluteDirectoryOrJar = directoryOrJar.getAbsoluteFile();
            if (absoluteDirectoryOrJar.isFile()) {
                final File target = new File(absoluteDirectoryOrJar.getParent(), fileName);

                if (target.exists()) {
                    return target;
                }
            } else {
                final File target = new File(directoryOrJar, fileName);
                if (target.exists()) {
                    return target;
                }
            }
        }

        return null;
    }

    public GitLabApi create(String projectOwner, @NonNull GitLabApiToken token) {
        return (GitLabApi) GitLabClientBuilder.buildClient(serverUrl, token.getId(), true, 3, 30);
    }

    public GitLabApi create(String projectOwner, String projectName, StandardUsernamePasswordCredentials creds) throws IOException {
        GitlabSession session = GitlabAPI.connect(serverUrl, creds.getUsername(), creds.getPassword().getPlainText());
        GitLabApiTokenImpl token = (GitLabApiTokenImpl) resolveToken(session.getPrivateToken());
        return (GitLabApi) GitLabClientBuilder.buildClient(serverUrl, token.getId(), true, 3, 30);
    }

    private GitLabApiToken resolveToken(String privateToken) throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                String tokenId = UUID.randomUUID().toString();
                GitLabApiTokenImpl token = new GitLabApiTokenImpl(CredentialsScope.SYSTEM, tokenId, "GitLab API Token", Secret.fromString(privateToken));
                credentialsStore.addCredentials(domains.get(0), token);
                return token;
            }
        }
        return null;
    }

    @CheckForNull
    public <T extends StandardCredentials> T lookupCredentials(@CheckForNull SCMSourceOwner context, @CheckForNull String id, Class<T> type) {
        if (Util.fixEmpty(id) == null) {
            return null;
        } else {
            if (id != null) {
                return CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(type, context, ACL.SYSTEM,
                        gitlabDomainRequirements(serverUrl)),
                    CredentialsMatchers.allOf(
                        CredentialsMatchers.withId(id),
                        CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(type))));
            }
            return null;
        }
    }

    public static ListBoxModel fillCheckoutCredentials(StandardListBoxModel result, SCMSourceOwner context, String apiUri) {
        result.withMatching(gitlabCheckoutCredentialsMatcher(), CredentialsProvider.lookupCredentials(
            StandardCredentials.class, context, ACL.SYSTEM, gitlabDomainRequirements(apiUri)
        ));
        return result;
    }

    public static ListBoxModel fillCredentials(StandardListBoxModel result, SCMSourceOwner context, String apiUri) {
        result.withMatching(gitlabCredentialsMatcher(), CredentialsProvider.lookupCredentials(
            GitLabApiTokenImpl.class, context, ACL.SYSTEM, gitlabDomainRequirements(apiUri)
        ));
        return result;
    }

    /* package */
    static CredentialsMatcher gitlabCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(GitLabApiTokenImpl.class));
    }

    /* package */
    static CredentialsMatcher gitlabCheckoutCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardCredentials.class));
    }


    static List<DomainRequirement> gitlabDomainRequirements(String apiUri) {
        if (apiUri == null) {
            return URIRequirementBuilder.fromUri("https://gitlab.com").build();
        } else {
            return URIRequirementBuilder.fromUri(apiUri).build();
        }
    }
}

