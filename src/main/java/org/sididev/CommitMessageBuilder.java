package org.sididev;

import java.util.*;

/**
 * Builds intelligent commit messages from analyzed file changes
 */
class CommitMessageBuilder {

    public String buildMessage(List<FileChange> fileChanges) {
        if (fileChanges.isEmpty()) {
            return "chore: no changes detected";
        }

        ChangeStats stats = analyzeChangeStats(fileChanges);

        String type = determineCommitType(stats);
        String scope = determineScope(stats);
        String shortDescription = generateShortDescription(fileChanges, stats);
        String longDescription = generateLongDescription(fileChanges);

        return constructFinalMessage(type, scope, shortDescription, longDescription);
    }

    private ChangeStats analyzeChangeStats(List<FileChange> fileChanges) {
        ChangeStats stats = new ChangeStats();

        for (FileChange fc : fileChanges) {
            if (fc.extension != null) {
                stats.extensions.put(fc.extension, stats.extensions.getOrDefault(fc.extension, 0) + 1);
            }
            stats.operations.put(fc.operation, stats.operations.getOrDefault(fc.operation, 0) + 1);
            if (fc.directory != null) stats.directories.add(fc.directory);
            if (fc.context != null) stats.contexts.add(fc.context);
        }

        return stats;
    }

    private String determineCommitType(ChangeStats stats) {
        if (stats.contexts.contains("test") || stats.extensions.containsKey("test")) {
            return "test";
        }

        if (stats.extensions.containsKey("md") || stats.extensions.containsKey("txt") ||
                stats.extensions.containsKey("rst") || stats.contexts.contains("docs")) {
            return "docs";
        }

        if (stats.extensions.containsKey("css") || stats.extensions.containsKey("scss") ||
                stats.extensions.containsKey("sass") || stats.extensions.containsKey("less") ||
                stats.contexts.contains("style")) {
            return "style";
        }

        if (stats.extensions.containsKey("json") || stats.extensions.containsKey("xml") ||
                stats.extensions.containsKey("yml") || stats.extensions.containsKey("yaml") ||
                stats.extensions.containsKey("properties") || stats.extensions.containsKey("gradle") ||
                stats.contexts.contains("config")) {
            return "chore";
        }

        if (stats.contexts.contains("build") || stats.extensions.containsKey("gradle") ||
                stats.extensions.containsKey("maven")) {
            return "build";
        }

        if (stats.operations.getOrDefault("add", 0) > stats.operations.getOrDefault("update", 0)) {
            return "feat";
        }

        if (stats.operations.getOrDefault("remove", 0) > 0) {
            return "refactor";
        }

        if (stats.extensions.containsKey("java") || stats.extensions.containsKey("php") ||
                stats.extensions.containsKey("js") || stats.extensions.containsKey("py") ||
                stats.extensions.containsKey("kt") || stats.extensions.containsKey("ts")) {
            return "fix";
        }

        return "chore";
    }

    private String determineScope(ChangeStats stats) {
        if (stats.contexts.contains("auth")) return "auth";
        if (stats.contexts.contains("api")) return "api";
        if (stats.contexts.contains("database")) return "database";
        if (stats.contexts.contains("ui")) return "ui";

        for (String dir : stats.directories) {
            String lowerDir = dir.toLowerCase();
            if (lowerDir.contains("api") || lowerDir.contains("service")) return "api";
            if (lowerDir.contains("ui") || lowerDir.contains("component") || lowerDir.contains("view")) return "ui";
            if (lowerDir.contains("auth") || lowerDir.contains("security")) return "auth";
            if (lowerDir.contains("database") || lowerDir.contains("db") || lowerDir.contains("model")) return "database";
            if (lowerDir.contains("config") || lowerDir.contains("setting")) return "config";
            if (lowerDir.contains("util") || lowerDir.contains("helper")) return "utils";
            if (lowerDir.contains("test")) return "test";
        }

        if (stats.extensions.containsKey("java") || stats.extensions.containsKey("php")) return "backend";
        if (stats.extensions.containsKey("js") || stats.extensions.containsKey("ts") ||
                stats.extensions.containsKey("vue") || stats.extensions.containsKey("jsx") ||
                stats.extensions.containsKey("html")) return "frontend";
        if (stats.extensions.containsKey("sql")) return "database";

        return "";
    }

    private String generateShortDescription(List<FileChange> fileChanges, ChangeStats stats) {
        if (fileChanges.size() == 1) {
            return generateSingleFileDescription(fileChanges.get(0));
        }
        return generateMultiFileDescription(stats);
    }

    private String generateSingleFileDescription(FileChange fc) {
        String action = getActionVerb(fc.operation);

        if (fc.context != null && !fc.context.equals("file")) {
            switch (fc.context) {
                case "config":
                    return String.format("%s %s configuration", action, getFileTypeDescription(fc));
                case "api":
                    return String.format("%s %s API endpoint", action, getFileTypeDescription(fc));
                case "ui":
                    return String.format("%s %s component", action, getFileTypeDescription(fc));
                case "test":
                    return String.format("%s %s tests", action, getFileTypeDescription(fc));
                case "database":
                    return String.format("%s %s schema", action, getFileTypeDescription(fc));
                case "auth":
                    return String.format("%s %s authentication", action, getFileTypeDescription(fc));
                default:
                    return String.format("%s %s %s", action, fc.context, getFileTypeDescription(fc));
            }
        }

        return String.format("%s %s", action, getFileTypeDescription(fc));
    }

    private String generateMultiFileDescription(ChangeStats stats) {
        String primaryAction = stats.operations.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> getActionVerb(entry.getKey()))
                .orElse("update");

        if (stats.contexts.size() == 1) {
            String context = stats.contexts.iterator().next();
            return String.format("%s %s implementation", primaryAction, context);
        }

        String mainExtension = stats.extensions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("project");

        switch (mainExtension) {
            case "java": return String.format("%s Java implementation", primaryAction);
            case "js": return String.format("%s JavaScript functionality", primaryAction);
            case "php": return String.format("%s PHP implementation", primaryAction);
            case "py": return String.format("%s Python modules", primaryAction);
            case "css": return String.format("%s styling system", primaryAction);
            case "html": return String.format("%s UI templates", primaryAction);
            case "json":
            case "xml": return String.format("%s configuration files", primaryAction);
            case "md": return String.format("%s documentation", primaryAction);
            default: return String.format("%s project structure", primaryAction);
        }
    }

    private String generateLongDescription(List<FileChange> fileChanges) {
        if (fileChanges.size() <= 1) {
            return "";
        }

        StringBuilder description = new StringBuilder();
        Map<String, List<FileChange>> groupedByOperation = new HashMap<>();

        for (FileChange fc : fileChanges) {
            groupedByOperation.computeIfAbsent(fc.operation, k -> new ArrayList<>()).add(fc);
        }

        boolean first = true;
        for (Map.Entry<String, List<FileChange>> entry : groupedByOperation.entrySet()) {
            String operation = entry.getKey();
            List<FileChange> files = entry.getValue();

            if (!first) {
                description.append("\n");
            }
            first = false;

            String verb = capitalizeFirst(getActionVerb(operation));
            if (files.size() == 1) {
                FileChange fc = files.get(0);
                description.append(String.format("- %s %s", verb, getDetailedFileDescription(fc)));
            } else {
                description.append(String.format("- %s %d files:", verb, files.size()));
                for (FileChange fc : files) {
                    description.append(String.format("\n  • %s", getDetailedFileDescription(fc)));
                }
            }
        }

        return description.toString();
    }

    private String constructFinalMessage(String type, String scope, String shortDescription, String longDescription) {
        StringBuilder message = new StringBuilder();

        if (scope.isEmpty()) {
            message.append(String.format("%s: %s", type, shortDescription));
        } else {
            message.append(String.format("%s(%s): %s", type, scope, shortDescription));
        }

        if (!longDescription.isEmpty()) {
            message.append("\n\n");
            message.append(longDescription);
        }

        return message.toString();
    }

    private String getActionVerb(String operation) {
        switch (operation.toLowerCase()) {
            case "add": return "add";
            case "remove": return "remove";
            case "update": return "update";
            case "move": return "move";
            default: return "modify";
        }
    }

    private String getFileTypeDescription(FileChange fc) {
        if (fc.fileName == null) return "file";

        String extension = fc.extension != null ? fc.extension : "";

        switch (extension) {
            case "java": return "Java class";
            case "js": return "JavaScript module";
            case "php": return "PHP script";
            case "py": return "Python module";
            case "css": return "stylesheet";
            case "html": return "HTML template";
            case "json": return "JSON config";
            case "xml": return "XML config";
            case "md": return "documentation";
            case "gradle": return "build script";
            case "yml":
            case "yaml": return "YAML config";
            default: return fc.fileName;
        }
    }

    private String getDetailedFileDescription(FileChange fc) {
        if (fc.fileName == null) return "unknown file";

        StringBuilder desc = new StringBuilder();
        desc.append(fc.fileName);

        if (fc.context != null && !fc.context.equals("file")) {
            switch (fc.context) {
                case "config": desc.append(" (configuration)"); break;
                case "api": desc.append(" (API layer)"); break;
                case "ui": desc.append(" (user interface)"); break;
                case "test": desc.append(" (test suite)"); break;
                case "database": desc.append(" (database layer)"); break;
                case "auth": desc.append(" (authentication)"); break;
                case "build": desc.append(" (build system)"); break;
                default: desc.append(String.format(" (%s)", fc.context));
            }
        }

        return desc.toString();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}