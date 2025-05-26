package org.sididev;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Statistics about file changes for commit message generation
 */
class ChangeStats {
    Map<String, Integer> extensions = new HashMap<>();
    Map<String, Integer> operations = new HashMap<>();
    Set<String> directories = new HashSet<>();
    Set<String> contexts = new HashSet<>();
}