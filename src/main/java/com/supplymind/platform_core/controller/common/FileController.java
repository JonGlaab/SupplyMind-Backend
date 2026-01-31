package com.supplymind.platform_core.controller.common;

import com.supplymind.platform_core.service.common.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storage;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER','STAFF')")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        String key = storage.upload(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(key);
    }

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','PROCUREMENT_OFFICER','STAFF')")
    public ResponseEntity<byte[]> download(@RequestParam("key") String key) {
        byte[] bytes = storage.download(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> delete(@RequestParam("key") String key) {
        storage.delete(key);
        return ResponseEntity.noContent().build();
    }
}

