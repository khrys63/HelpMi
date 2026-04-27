package com.helpmi.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    void store(String key, InputStream content, long contentLength, String contentType) throws IOException;
    InputStream retrieve(String key) throws IOException;
    void delete(String key) throws IOException;
}
