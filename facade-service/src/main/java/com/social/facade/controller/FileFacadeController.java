package com.social.facade.controller;

import com.social.common.dto.FileDTO;
import com.social.common.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileFacadeController {

    private static final String FILE_SERVICE_URL = "http://file-service";

    @Autowired
    private RestTemplate restTemplate;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList("mp4", "mov", "avi", "webm");

    /**
     * 文件上传 - facade转发到file-service
     */
    @PostMapping("/upload")
    public ResponseEntity<Result<FileDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) Long postId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("File is empty"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ResponseEntity.badRequest().body(Result.badRequest("Invalid file name"));
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(extension) && !ALLOWED_VIDEO_TYPES.contains(extension)) {
            return ResponseEntity.badRequest().body(Result.error(5001, "Invalid file type"));
        }

        try {
            String fileServiceUrl = FILE_SERVICE_URL + "/files/upload";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            if (postId != null) {
                body.add("postId", postId.toString());
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Result> response = restTemplate.exchange(
                    fileServiceUrl, HttpMethod.POST, requestEntity, Result.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(5000, "File upload failed"));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Result.error(e.getStatusCode().value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(5000, "File upload failed: " + e.getMessage()));
        }
    }

    /**
     * 文件删除 - facade转发到file-service
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteFile(@PathVariable("id") Long id) {
        try {
            String fileServiceUrl = FILE_SERVICE_URL + "/files/" + id;
            restTemplate.delete(fileServiceUrl);
            return ResponseEntity.ok(Result.success());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(5000, "File delete failed"));
        }
    }

    /**
     * 获取文件 - 使用URLConnection直接转发
     */
    @GetMapping("/{type}/{filename:.+}")
    public ResponseEntity<byte[]> serveFile(
            @PathVariable("type") String type,
            @PathVariable("filename") String filename) {

        try {
            String fileServiceUrl = FILE_SERVICE_URL + "/files/" + type + "/" + filename;
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(fileServiceUrl, HttpMethod.GET, null, byte[].class);

            return new ResponseEntity<>(responseEntity.getBody(), responseEntity.getHeaders(), HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".webm")) return "video/webm";
        return "application/octet-stream";
    }
}
