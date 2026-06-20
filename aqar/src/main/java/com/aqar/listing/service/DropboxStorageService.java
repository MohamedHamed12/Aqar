package com.aqar.listing.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DropboxStorageService {

    private final DbxClientV2 client;

    public DropboxStorageService(DbxClientV2 client) {
        this.client = client;
    }

    public FileMetadata upload(String path, InputStream data, long size) {
        try {
            return client.files().uploadBuilder(path)
                    .withMode(WriteMode.ADD)
                    .uploadAndFinish(data, size);
        } catch (DbxException | IOException e) {
            throw new RuntimeException("Failed to upload to Dropbox: " + path, e);
        }
    }

    public InputStream download(String path) {
        try {
            return client.files().download(path).getInputStream();
        } catch (DbxException e) {
            throw new RuntimeException("Failed to download from Dropbox: " + path, e);
        }
    }

    public void delete(String path) {
        try {
            client.files().deleteV2(path);
        } catch (DbxException e) {
            throw new RuntimeException("Failed to delete from Dropbox: " + path, e);
        }
    }

    public String createSharedLink(String path) {
        try {
            SharedLinkSettings settings = SharedLinkSettings.newBuilder()
                    .withRequestedVisibility(RequestedVisibility.PUBLIC)
                    .build();
            SharedLinkMetadata link = client.sharing().createSharedLinkWithSettings(path, settings);
            return link.getUrl();
        } catch (com.dropbox.core.v2.sharing.CreateSharedLinkWithSettingsErrorException e) {
            if (e.errorValue.isSharedLinkAlreadyExists()) {
                try {
                    var links = client.sharing().listSharedLinksBuilder()
                            .withPath(path)
                            .withDirectOnly(true)
                            .start();
                    return links.getLinks().stream()
                            .findFirst()
                            .map(SharedLinkMetadata::getUrl)
                            .orElse(null);
                } catch (DbxException ex) {
                    throw new RuntimeException("Failed to list shared links for: " + path, ex);
                }
            }
            throw new RuntimeException("Failed to create shared link for: " + path, e);
        } catch (DbxException e) {
            throw new RuntimeException("Failed to create shared link for: " + path, e);
        }
    }

    public String getTemporaryLink(String path) {
        try {
            return client.files().getTemporaryLink(path).getLink();
        } catch (DbxException e) {
            throw new RuntimeException("Failed to get temporary link for: " + path, e);
        }
    }

    public FileMetadata getMetadata(String path) {
        try {
            return (FileMetadata) client.files().getMetadata(path);
        } catch (DbxException e) {
            throw new RuntimeException("Failed to get metadata for: " + path, e);
        }
    }
}
