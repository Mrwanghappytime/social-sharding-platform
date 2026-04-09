package com.social.file.service;

import com.social.common.entity.File;
import com.social.common.enums.MediaType;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl {

    @Autowired
    private FileRepository fileRepository;

    @Value("${file.storage.path:/data/files}")
    private String storagePath;

    private static final int MAX_IMAGES_PER_POST = 9;

    @Transactional
    public File uploadFile(MultipartFile file, Long postId, MediaType mediaType) {
        if (mediaType == MediaType.IMAGE && postId != null) {
            long imageCount = fileRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
                    .filter(f -> f.getType() == MediaType.IMAGE)
                    .count();
            if (imageCount >= MAX_IMAGES_PER_POST) {
                throw new BusinessException(
                        ErrorCode.IMAGE_COUNT_EXCEEDED,
                        "Maximum " + MAX_IMAGES_PER_POST + " images allowed per post");
            }
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String subDir = mediaType == MediaType.IMAGE ? "images" : "videos";
        String newFilename = uuid + "." + extension;

        Path directoryPath = Paths.get(storagePath, subDir);
        Path filePath = directoryPath.resolve(newFilename);

        try {
            Files.createDirectories(directoryPath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Failed to save file: " + e.getMessage());
        }

        String url = "/files/" + subDir + "/" + uuid + "." + extension;

        int sortOrder = 0;
        if (postId != null) {
            List<File> existingFiles = fileRepository.findByPostIdOrderBySortOrderAsc(postId);
            sortOrder = existingFiles.size();
        }

        File fileEntity = new File();
        fileEntity.setPostId(postId);
        fileEntity.setUrl(url);
        fileEntity.setType(mediaType);
        fileEntity.setSortOrder(sortOrder);

        return fileRepository.save(fileEntity);
    }

    @Transactional
    public boolean deleteFile(Long id) {
        File file = fileRepository.findById(id).orElse(null);
        if (file == null) {
            return false;
        }

        String url = file.getUrl();
        String subDir = file.getType() == MediaType.IMAGE ? "images" : "videos";
        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path filePath = Paths.get(storagePath, subDir, filename);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but continue with database deletion
        }

        fileRepository.deleteById(id);
        return true;
    }

    public File getFileById(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    public List<File> getFilesByPostId(Long postId) {
        return fileRepository.findByPostIdOrderBySortOrderAsc(postId);
    }
}
