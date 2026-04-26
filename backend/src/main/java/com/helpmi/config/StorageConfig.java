package com.helpmi.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class StorageConfig {

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(storagePath));
    }

    public Path getStoragePath() {
        return Path.of(storagePath);
    }
}
