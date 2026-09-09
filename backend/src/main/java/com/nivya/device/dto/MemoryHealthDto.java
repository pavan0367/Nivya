package com.nivya.device.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemoryHealthDto {

    private Long totalBytes;
    private Long usedBytes;
    private Long freeBytes;
    private Double usedPct;

    @JsonProperty("isLowRam")
    private boolean isLowRam;

    public MemoryHealthDto() {
    }

    public MemoryHealthDto(Long totalBytes, Long usedBytes, Long freeBytes, Double usedPct, boolean isLowRam) {
        this.totalBytes = totalBytes;
        this.usedBytes = usedBytes;
        this.freeBytes = freeBytes;
        this.usedPct = usedPct;
        this.isLowRam = isLowRam;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(Long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public Long getFreeBytes() {
        return freeBytes;
    }

    public void setFreeBytes(Long freeBytes) {
        this.freeBytes = freeBytes;
    }

    public Double getUsedPct() {
        return usedPct;
    }

    public void setUsedPct(Double usedPct) {
        this.usedPct = usedPct;
    }

    public boolean isLowRam() {
        return isLowRam;
    }

    public void setLowRam(boolean lowRam) {
        isLowRam = lowRam;
    }
}
