package org.sididev;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

/**
 * Main action class for Smart Commit Generator plugin.
 * Analyzes Git changes and generates intelligent commit messages.
 */
public class SmartCommitAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showWarningDialog("No project found", "Warning");
            return;
        }

        try {
            String message = generateCommitMessage(project);

            // Try to inject message directly into commit field
            boolean injected = CommitMessageInjector.injectMessage(e, message);

            if (!injected) {
                // Fallback: show message for manual copy-paste
                showFallbackDialog(project, message);
            }

        } catch (Exception ex) {
            Messages.showErrorDialog(project,
                    "Error generating commit message: " + ex.getMessage(),
                    "Smart Commit Generator");
        }
    }

    /**
     * Generate intelligent commit message based on Git changes
     */
    private String generateCommitMessage(Project project) {
        try {
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            Collection<Change> changes = changeListManager.getDefaultChangeList().getChanges();

            if (changes.isEmpty()) {
                return "chore: no staged changes found";
            }

            ChangeAnalyzer analyzer = new ChangeAnalyzer();
            List<FileChange> fileChanges = analyzer.analyzeChanges(changes);

            CommitMessageBuilder builder = new CommitMessageBuilder();
            return builder.buildMessage(fileChanges);

        } catch (Exception e) {
            return "chore: update project files";
        }
    }

    /**
     * Show fallback dialog when direct injection fails
     */
    private void showFallbackDialog(Project project, String message) {
        String finalMessage = Messages.showInputDialog(
                project,
                "Generated commit message:",
                "Smart Commit Generator",
                null,
                message,
                null
        );

        if (finalMessage != null && !finalMessage.trim().isEmpty()) {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(finalMessage), null);

            Messages.showInfoMessage(project,
                    "Message copied to clipboard",
                    "Smart Commit Generator"
            );
        }
    }
}