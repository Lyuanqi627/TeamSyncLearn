package com.teamsync.dto;

import java.util.List;

public class AchievementDTO {
    private Long scheduleId;
    private List<ItemDTO> items;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public List<ItemDTO> getItems() { return items; }
    public void setItems(List<ItemDTO> items) { this.items = items; }

    public static class ItemDTO {
        private String contentType;
        private String content;
        private String fileName;
        private Integer fileIndex;
        private String existingFileUrl;

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Integer getFileIndex() { return fileIndex; }
        public void setFileIndex(Integer fileIndex) { this.fileIndex = fileIndex; }
        public String getExistingFileUrl() { return existingFileUrl; }
        public void setExistingFileUrl(String existingFileUrl) { this.existingFileUrl = existingFileUrl; }
    }
}
