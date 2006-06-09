package de.dal33t.powerfolder.event;

import de.dal33t.powerfolder.event.FolderMembershipEvent;

public interface FolderMembershipListener extends ListenerInterface {
    public void memberJoined(FolderMembershipEvent folderEvent);
    public void memberLeft(FolderMembershipEvent folderEvent);
}
