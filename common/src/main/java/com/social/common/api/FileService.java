package com.social.common.api;

import com.social.common.dto.FileDTO;
import com.social.common.enums.MediaType;

import java.util.List;

public interface FileService {

    FileDTO uploadFile(byte[] fileData, String originalFilename, Long postId, MediaType mediaType);

    boolean deleteFile(Long id);

    List<FileDTO> getFilesByPostId(Long postId);
}
