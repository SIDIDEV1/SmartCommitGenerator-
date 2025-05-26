package org.sididev;

import com.intellij.openapi.vcs.changes.Change;

import java.util.*;

/**
 * Analyzes Git changes and extracts file information
 */
class ChangeAnalyzer {

    public List<FileChange> analyzeChanges(Collection<Change> changes) {
        List<FileChange> fileChanges = new ArrayList<>();

        for (Change change : changes) {
            FileChange fc = new FileChange();

            fc.fileName = extractFileName(change);
            fc.extension = extractFileExtension(fc.fileName);
            fc.directory = extractDirectory(change);
            fc.operation = extractOperation(change);
            fc.path = extractFilePath(change);
            fc.context = determineFileContext(fc.fileName, fc.directory);

            fileChanges.add(fc);
        }

        return fileChanges;
    }

    private String extractFileName(Change change) {
        try {
            if (change.getAfterRevision() != null) {
                return change.getAfterRevision().getFile().getName();
            } else if (change.getBeforeRevision() != null) {
                return change.getBeforeRevision().getFile().getName();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return null;
    }

    private String extractDirectory(Change change) {
        try {
            String path = extractFilePath(change);
            if (path != null) {
                String[] parts = path.split("/");
                if (parts.length > 1) {
                    return parts[parts.length - 2];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractFilePath(Change change) {
        try {
            if (change.getAfterRevision() != null) {
                return change.getAfterRevision().getFile().getPath();
            } else if (change.getBeforeRevision() != null) {
                return change.getBeforeRevision().getFile().getPath();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractOperation(Change change) {
        switch (change.getType()) {
            case NEW: return "add";
            case DELETED: return "remove";
            case MODIFICATION: return "update";
            case MOVED: return "move";
            default: return "modify";
        }
    }

    private String determineFileContext(String fileName, String directory) {
        if (fileName == null) return "file";

        String lowerName = fileName.toLowerCase();
        String lowerDir = directory != null ? directory.toLowerCase() : "";

        if (lowerName.contains("test") || lowerDir.contains("test")) return "test";
        if (lowerName.contains("config") || lowerDir.contains("config")) return "config";
        if (lowerName.contains("api") || lowerDir.contains("api")) return "api";
        if (lowerName.contains("auth") || lowerDir.contains("auth")) return "auth";
        if (lowerName.contains("database") || lowerName.contains("db") || lowerDir.contains("db")) return "database";
        if (lowerName.contains("ui") || lowerName.contains("component") || lowerDir.contains("component")) return "ui";
        if (lowerName.contains("style") || lowerName.contains("css")) return "style";
        if (lowerName.contains("doc") || lowerDir.contains("doc")) return "docs";
        if (lowerName.contains("build") || lowerName.contains("gradle") || lowerName.contains("maven")) return "build";

        return "file";
    }
}