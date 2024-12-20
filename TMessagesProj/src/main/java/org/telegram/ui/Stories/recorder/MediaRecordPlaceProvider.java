package org.telegram.ui.Stories.recorder;

import org.telegram.messenger.MediaController;

public interface MediaRecordPlaceProvider {
    boolean addPhotoAllowed();

    void onPhotoCreated(MediaController.PhotoEntry photoEntry);

    void onPhotoAdded(MediaController.PhotoEntry photoEntry);

    boolean addVideoAllowed();

    void onVideoCreated(MediaController.PhotoEntry photoEntry);

    void onVideoAdded(MediaController.PhotoEntry photoEntry);

    void onVideoCollageAdded(
            MediaController.PhotoEntry photoEntry,
            StoryEntry storyEntry,
            CollageLayoutView2 collageLayoutView
    );
}
