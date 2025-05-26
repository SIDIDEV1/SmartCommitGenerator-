package org.sididev;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.VcsDataKeys;

/**
 * Handles injection of commit messages into the commit UI
 */
class CommitMessageInjector {

    public static boolean injectMessage(AnActionEvent e, String message) {
        try {
            // Method 1: Try VCS workflow UI
            if (tryVcsWorkflowInjection(e, message)) {
                return true;
            }

            // Method 2: Try direct UI component search
            return tryDirectUIInjection(e, message);

        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean tryVcsWorkflowInjection(AnActionEvent e, String message) {
        Object commitWorkflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI);
        if (commitWorkflowUi == null) return false;

        try {
            // Try common method names via reflection
            String[] methodNames = {"setCommitMessage", "setText"};

            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = commitWorkflowUi.getClass().getMethod(methodName, String.class);
                    method.invoke(commitWorkflowUi, message);
                    return true;
                } catch (Exception ignored) {}
            }

            // Try getting commit message UI component
            try {
                java.lang.reflect.Method method = commitWorkflowUi.getClass().getMethod("getCommitMessageUi");
                Object commitMessageUi = method.invoke(commitWorkflowUi);
                if (commitMessageUi != null) {
                    java.lang.reflect.Method setTextMethod = commitMessageUi.getClass().getMethod("setText", String.class);
                    setTextMethod.invoke(commitMessageUi, message);
                    return true;
                }
            } catch (Exception ignored) {}

        } catch (Exception ignored) {}

        return false;
    }

    private static boolean tryDirectUIInjection(AnActionEvent e, String message) {
        try {
            java.awt.Component component = null;
            if (e.getInputEvent() instanceof java.awt.event.MouseEvent) {
                component = ((java.awt.event.MouseEvent) e.getInputEvent()).getComponent();
            }

            if (component != null) {
                javax.swing.JTextArea textArea = findCommitTextArea(component);
                if (textArea != null) {
                    textArea.setText(message);
                    return true;
                }
            }

        } catch (Exception ignored) {}

        return false;
    }

    private static javax.swing.JTextArea findCommitTextArea(java.awt.Component component) {
        if (component instanceof javax.swing.JTextArea) {
            javax.swing.JTextArea textArea = (javax.swing.JTextArea) component;
            // Check if this looks like a commit message field
            if (textArea.getRows() > 1 || textArea.getColumns() > 30) {
                return textArea;
            }
        }

        if (component instanceof java.awt.Container) {
            java.awt.Container container = (java.awt.Container) component;
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JTextArea result = findCommitTextArea(child);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}