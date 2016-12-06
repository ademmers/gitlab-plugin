package com.dabsquared.gitlabjenkins.orgfolder;

import com.cloudbees.hudson.plugins.folder.computed.ComputedFolder;
import com.cloudbees.hudson.plugins.folder.computed.FolderComputation;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.util.AlternativeUiTextProvider;

@Extension
public class AlternativeUiTextProviderImpl extends AlternativeUiTextProvider {
    @Override
    public <T> String getText(Message<T> text, T context) {
        if(text == AbstractItem.PRONOUN) {
            AbstractItem i = AbstractItem.PRONOUN.cast(context);
            if(Sniffer.matchProject(i) != null) {
                return "Project";
            }
            if(Sniffer.matchBranch(i) != null) {
                return "Branch";
            }
        }

        if(text == FolderComputation.DISPLAY_NAME) {
            ComputedFolder<?> f = FolderComputation.DISPLAY_NAME.cast(context).getParent();
            if(Sniffer.matchOrg(f) != null) {
                return "Re-scan Organization";
            }
        }
        return null;
    }
}
