package com.photobogota.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogFileInfo {
    private String name;
    private long size;
    private long lastModified;
}