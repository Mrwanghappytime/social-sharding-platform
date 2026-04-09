package com.social.file.controller;

import com.social.common.dto.Result;
import com.social.file.service.FileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileServiceImpl fileService;

    @Value("${file.storage.path:G:/claude-project/csdn-wanghaof/data/files}")
    private String storagePath;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList("mp4", "mov", "avi", "webm");
    private static final long MAX_VIDEO_SIZE = 52428800L; // 50MB

    @PostMapping("/upload")
    public ResponseEntity<Result<com.social.common.dto.FileDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) Long postId,
            @RequestParam(value = "type", required = false) String typeStr) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("File is empty"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ResponseEntity.badRequest().body(Result.badRequest("Invalid file name"));
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        com.social.common.enums.MediaType mediaType;

        // Determine media type based on extension
        if (ALLOWED_IMAGE_TYPES.contains(extension)) {
            mediaType = com.social.common.enums.MediaType.IMAGE;
        } else if (ALLOWED_VIDEO_TYPES.contains(extension)) {
            mediaType = com.social.common.enums.MediaType.VIDEO;
        } else {
            return ResponseEntity.badRequest().body(Result.error(5001, "Invalid file type"));
        }

        // Validate video size
        if (mediaType == com.social.common.enums.MediaType.VIDEO && file.getSize() > MAX_VIDEO_SIZE) {
            return ResponseEntity.badRequest().body(Result.error(5002, "Video file too large"));
        }

        com.social.common.entity.File savedFile = fileService.uploadFile(file, postId, mediaType);
        com.social.common.dto.FileDTO fileDTO = new com.social.common.dto.FileDTO();
        fileDTO.setId(savedFile.getId());
        fileDTO.setUrl(savedFile.getUrl());
        fileDTO.setType(savedFile.getType());
        fileDTO.setSortOrder(savedFile.getSortOrder());

        return ResponseEntity.ok(Result.success(fileDTO));
    }

    @GetMapping("/{type}/{uuid}.{ext}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable(name = "type") String type,
            @PathVariable(name = "uuid") String uuid,
            @PathVariable(name = "ext") String ext) {

        String fullFilename = uuid + "." + ext;
        Path filePath;

        if ("images".equals(type)) {
            filePath = Paths.get(storagePath, "images").resolve(fullFilename);
        } else if ("videos".equals(type)) {
            filePath = Paths.get(storagePath, "videos").resolve(fullFilename);
        } else {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                if ("images".equals(type) && ALLOWED_IMAGE_TYPES.contains(ext.toLowerCase())) {
                    contentType = "image/" + (ext.equalsIgnoreCase("jpg") ? "jpeg" : ext.toLowerCase());
                } else if ("videos".equals(type) && ext.equalsIgnoreCase("mp4")) {
                    contentType = "video/mp4";
                } else if ("videos".equals(type) && ext.equalsIgnoreCase("webm")) {
                    contentType = "video/webm";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteFile(@PathVariable(name = "id") Long id) {
        boolean deleted = fileService.deleteFile(id);
        if (deleted) {
            return ResponseEntity.ok(Result.success());
        } else {
            return ResponseEntity.status(404).body(Result.notFound("File not found"));
        }
    }
}
