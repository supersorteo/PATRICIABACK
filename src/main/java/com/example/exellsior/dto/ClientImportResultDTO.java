package com.example.exellsior.dto;

import java.util.ArrayList;
import java.util.List;

public class ClientImportResultDTO {
    private boolean dryRun;
    private int totalReceived;
    private int totalProcessed;
    private int created;
    private int skippedExisting;
    private int skippedDuplicateInPayload;
    private int invalid;
    private final List<RowResultDTO> rows = new ArrayList<>();

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getTotalReceived() {
        return totalReceived;
    }

    public void setTotalReceived(int totalReceived) {
        this.totalReceived = totalReceived;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getSkippedExisting() {
        return skippedExisting;
    }

    public void setSkippedExisting(int skippedExisting) {
        this.skippedExisting = skippedExisting;
    }

    public int getSkippedDuplicateInPayload() {
        return skippedDuplicateInPayload;
    }

    public void setSkippedDuplicateInPayload(int skippedDuplicateInPayload) {
        this.skippedDuplicateInPayload = skippedDuplicateInPayload;
    }

    public int getInvalid() {
        return invalid;
    }

    public void setInvalid(int invalid) {
        this.invalid = invalid;
    }

    public List<RowResultDTO> getRows() {
        return rows;
    }

    public void addRow(RowResultDTO row) {
        this.rows.add(row);
    }

    public static class RowResultDTO {
        private int rowNumber;
        private Long legacyId;
        private String name;
        private String identityKey;
        private String status;
        private String reason;
        private Long createdClientId;
        private Long matchedClientId;

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public Long getLegacyId() {
            return legacyId;
        }

        public void setLegacyId(Long legacyId) {
            this.legacyId = legacyId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdentityKey() {
            return identityKey;
        }

        public void setIdentityKey(String identityKey) {
            this.identityKey = identityKey;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Long getCreatedClientId() {
            return createdClientId;
        }

        public void setCreatedClientId(Long createdClientId) {
            this.createdClientId = createdClientId;
        }

        public Long getMatchedClientId() {
            return matchedClientId;
        }

        public void setMatchedClientId(Long matchedClientId) {
            this.matchedClientId = matchedClientId;
        }
    }
}
