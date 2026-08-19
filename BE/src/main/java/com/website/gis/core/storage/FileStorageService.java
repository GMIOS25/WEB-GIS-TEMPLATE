package com.website.gis.core.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String subDirectory);
    Resource loadAsResource(String storedFileName);
    void delete(String storedFileName);
}
