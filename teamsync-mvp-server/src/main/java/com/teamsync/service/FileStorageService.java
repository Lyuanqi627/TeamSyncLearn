package com.teamsync.service;

import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 通用文件存储服务:把上传文件写入 upload.path 下的按日期分目录,返回相对路径。
 * 前端访问时拼 /uploads/ 前缀(见 WebMvcConfig 的静态资源映射)。
 */
@Service
public class FileStorageService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public String store(MultipartFile file) throws IOException {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String fileName = IdUtil.fastSimpleUUID() + "." + ext;
        String dirPath = uploadPath + "/" + datePath;
        Files.createDirectories(Paths.get(dirPath));
        String relativePath = datePath + "/" + fileName;
        file.transferTo(new File(uploadPath + "/" + relativePath));
        return relativePath;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
