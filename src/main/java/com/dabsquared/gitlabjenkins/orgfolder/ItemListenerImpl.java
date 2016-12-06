package com.dabsquared.gitlabjenkins.orgfolder;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ItemListenerImpl extends ItemListener {
    @Inject
    private MainLogic main;

    @Override
    public void onUpdated(Item item) {
        maybeApply(item);
    }

    @Override
    public void onCreated(Item item) {
        maybeApply(item);
    }

    private void maybeApply(Item item) {
        try {
            Sniffer.OrgMatch f = Sniffer.matchOrg(item);
            if(f != null) {
                main.applyOrg(f.folder, f.scm);
            }

            Sniffer.ProjectMatch p = Sniffer.matchProject(item);
            if(p != null) {
                main.applyProject(p.project, p.scm);
            }

            Sniffer.BranchMatch b = Sniffer.matchBranch(item);
            if(b != null) {
                main.applyBranch(b.branch, b.project, b.scm);
            }
            // TODO: extra catch needed when system hooks are implemented (filenotfoundexception)
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to apply, GitLab Org Folder theme to " + item.getFullName(), e);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ItemListenerImpl.class.getName());
}
