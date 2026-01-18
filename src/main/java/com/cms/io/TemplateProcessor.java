package com.cms.io;

import com.cms.util.CMSLogger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template processing service for dynamic content generation with variable
 * substitution, caching, and comprehensive I/O operations.
 *
 * <p>
 * This service provides template processing capabilities for the CMS,
 * allowing dynamic content generation through variable substitution.
 * It supports multiple template formats, caching for performance,
 * and comprehensive error handling with security validation.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Template Method Pattern - Provides
 * a framework for template processing with customizable variable resolution
 * and formatting strategies.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Java I/O Operations
 * Demonstrates file reading, stream processing, character encoding handling,
 * and resource management with comprehensive I/O operations.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - Path traversal prevention for template files
 * - Input sanitization for template variables
 * - Safe variable substitution to prevent injection
 * - Template caching with proper validation
 * - Resource cleanup with try-with-resources
 * </p>
 *
 * <p>
 * <strong>Collections Framework Usage:</strong>
 * Uses Map for variables and caching, Set for supported formats,
 * and List for processing results, providing comprehensive
 * collections integration throughout template operations.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class TemplateProcessor {

    /** Default template directory name */
    private static final String DEFAULT_TEMPLATE_DIR = "templates";

    /** Pattern for variable substitution (${variable} format) */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");

    /** Pattern for conditional blocks ({{#if condition}} content {{/if}}) */
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}(.*?)\\{\\{/if\\}\\}",
            Pattern.DOTALL);

    /** Pattern for loop blocks ({{#each items}} content {{/each}}) */
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}(.*?)\\{\\{/each\\}\\}",
            Pattern.DOTALL);

    /** Set of supported template file extensions */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("html", "htm", "txt", "xml", "json", "css", "js",
            "template");

    private final Path templateDirectory;
    private final Map<String, CachedTemplate> templateCache;
    private final Map<String, Object> globalVariables;
    private final TemplateConfiguration configuration;

    /** Logger instance for template processing operations */
    private static final CMSLogger logger = CMSLogger.getInstance();

    /**
     * Template configuration for processing behavior.
     */
    public static class TemplateConfiguration {
        private boolean enableCaching = true;
        private boolean enableConditionals = true;
        private boolean enableLoops = true;
        private boolean strictMode = false;
        private long cacheTimeout = 300000; // 5 minutes
        private int maxCacheSize = 100;

        public boolean isEnableCaching() {
            return enableCaching;
        }

        public void setEnableCaching(boolean enableCaching) {
            this.enableCaching = enableCaching;
        }

        public boolean isEnableConditionals() {
            return enableConditionals;
        }

        public void setEnableConditionals(boolean enableConditionals) {
            this.enableConditionals = enableConditionals;
        }

        public boolean isEnableLoops() {
            return enableLoops;
        }

        public void setEnableLoops(boolean enableLoops) {
            this.enableLoops = enableLoops;
        }

        public boolean isStrictMode() {
            return strictMode;
        }

        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        public long getCacheTimeout() {
            return cacheTimeout;
        }

        public void setCacheTimeout(long cacheTimeout) {
            this.cacheTimeout = Math.max(0, cacheTimeout);
        }

        public int getMaxCacheSize() {
            return maxCacheSize;
        }

        public void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = Math.max(1, maxCacheSize);
        }
    }

    /**
     * Cached template data with timestamp for expiration management.
     */
    private static class CachedTemplate {
        final String content;
        final Instant timestamp;
        final Path sourcePath;
        final long sourceLastModified;

        CachedTemplate(String content, Path sourcePath, long sourceLastModified) {
            this.content = Objects.requireNonNull(content, "Content cannot be null");
            this.timestamp = Instant.now();
            this.sourcePath = sourcePath;
            this.sourceLastModified = sourceLastModified;
        }

        boolean isExpired(long timeoutMs) {
            return timeoutMs > 0 &&
                    Instant.now().toEpochMilli() - timestamp.toEpochMilli() > timeoutMs;
        }

        boolean isSourceModified() throws IOException {
            return Files.exists(sourcePath) &&
                    Files.getLastModifiedTime(sourcePath).toMillis() != sourceLastModified;
        }
    }

    /**
     * Creates a TemplateProcessor with default configuration.
     *
     * <p>
     * Uses default template directory and configuration for
     * standard template processing scenarios.
     * </p>
     *
     * @throws IOException if template directory cannot be accessed
     */
    public TemplateProcessor() throws IOException {
        this(Paths.get(DEFAULT_TEMPLATE_DIR), new TemplateConfiguration());
    }

    /**
     * Creates a TemplateProcessor with custom directory and configuration.
     *
     * @param templateDirectory directory containing template files
     * @param configuration     processing configuration
     * @throws IOException              if template directory cannot be accessed
     * @throws IllegalArgumentException if parameters are invalid
     */
    public TemplateProcessor(Path templateDirectory, TemplateConfiguration configuration)
            throws IOException {
        this.templateDirectory = Objects.requireNonNull(templateDirectory,
                "Template directory cannot be null");
        this.configuration = Objects.requireNonNull(configuration,
                "Configuration cannot be null");
        this.templateCache = new ConcurrentHashMap<>();
        this.globalVariables = new ConcurrentHashMap<>();

        // Initialize template directory
        initializeTemplateDirectory();

        // Initialize global variables
        initializeGlobalVariables();
    }

    /**
     * Initializes template directory and validates accessibility.
     *
     * @throws IOException if directory cannot be created or accessed
     */
    private void initializeTemplateDirectory() throws IOException {
        if (!Files.exists(templateDirectory)) {
            Files.createDirectories(templateDirectory);
        }

        if (!Files.isReadable(templateDirectory)) {
            throw new IOException("Template directory is not readable: " + templateDirectory);
        }
    }

    /**
     * Initializes global variables available to all templates.
     */
    private void initializeGlobalVariables() {
        globalVariables.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        globalVariables.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        globalVariables.put("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        globalVariables.put("version", "1.0");
        globalVariables.put("systemName", "JavaCMS");
    }

    /**
     * Processes a template file with variable substitution.
     *
     * <p>
     * This is the main template processing method that provides:
     * </p>
     * <ul>
     * <li>Template file reading with proper encoding</li>
     * <li>Variable substitution with security validation</li>
     * <li>Conditional and loop processing</li>
     * <li>Caching for improved performance</li>
     * <li>Comprehensive error handling</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>File reading with BufferedReader and UTF-8 encoding</li>
     * <li>Character stream processing</li>
     * <li>Resource management with try-with-resources</li>
     * <li>File system validation and security checks</li>
     * </ul>
     *
     * @param templatePath path to the template file (relative to template
     *                     directory)
     * @param variables    map of variables for substitution
     * @return processed template content with variables substituted
     * @throws IOException                 if template file cannot be read
     * @throws IllegalArgumentException    if parameters are invalid
     * @throws TemplateProcessingException if template processing fails
     */
    public String processTemplate(Path templatePath, Map<String, Object> variables)
            throws IOException, TemplateProcessingException {

        // Input validation
        if (templatePath == null) {
            throw new IllegalArgumentException("Template path cannot be null");
        }

        // Security validation - prevent path traversal
        Path resolvedPath = templateDirectory.resolve(templatePath).normalize();
        if (!resolvedPath.startsWith(templateDirectory)) {
            throw new SecurityException("Template path outside template directory: " + templatePath);
        }

        // Validate template file exists and is readable
        if (!Files.exists(resolvedPath) || !Files.isReadable(resolvedPath)) {
            throw new FileNotFoundException("Template file not found or not readable: " + templatePath);
        }

        // Validate file extension
        String filename = templatePath.getFileName().toString();
        String extension = getFileExtension(filename).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new TemplateProcessingException("Unsupported template file type: " + extension);
        }

        // Load template content (with caching if enabled)
        String templateContent = loadTemplate(resolvedPath);

        // Merge variables (global + provided)
        Map<String, Object> allVariables = mergeVariables(variables);

        // Process template with variable substitution
        long startTime = System.currentTimeMillis();
        String result = processTemplateContent(templateContent, allVariables);
        long renderTime = System.currentTimeMillis() - startTime;

        // Log template processing performance
        logger.logTemplateProcessing(filename, renderTime, templatePath.toString(),
                result.length(), allVariables.size());

        return result;
    }

    /**
     * Loads template content from file with caching support.
     *
     * <p>
     * <strong>Java I/O Implementation:</strong> Demonstrates file reading
     * with proper character encoding, buffered I/O, and resource management.
     * </p>
     *
     * @param templatePath the resolved template file path
     * @return template content as string
     * @throws IOException if file reading fails
     */
    private String loadTemplate(Path templatePath) throws IOException {
        String cacheKey = templatePath.toString();

        // Check cache if enabled
        if (configuration.isEnableCaching()) {
            CachedTemplate cached = templateCache.get(cacheKey);
            if (cached != null && !cached.isExpired(configuration.getCacheTimeout())
                    && !cached.isSourceModified()) {
                return cached.content;
            }
        }

        // Read template file with proper encoding
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(templatePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        }

        String templateContent = content.toString();

        // Cache the template if enabled
        if (configuration.isEnableCaching()) {
            cacheTemplate(cacheKey, templateContent, templatePath);
        }

        return templateContent;
    }

    /**
     * Caches template content with size and expiration management.
     *
     * @param cacheKey   the cache key
     * @param content    the template content
     * @param sourcePath the source file path
     * @throws IOException if file metadata cannot be read
     */
    private void cacheTemplate(String cacheKey, String content, Path sourcePath)
            throws IOException {
        // Manage cache size
        if (templateCache.size() >= configuration.getMaxCacheSize()) {
            // Remove oldest entries (simple FIFO strategy)
            templateCache.entrySet().removeIf(entry -> entry.getValue().isExpired(configuration.getCacheTimeout()));

            // If still full, remove one arbitrary entry
            if (templateCache.size() >= configuration.getMaxCacheSize()) {
                String firstKey = templateCache.keySet().iterator().next();
                templateCache.remove(firstKey);
            }
        }

        long lastModified = Files.getLastModifiedTime(sourcePath).toMillis();
        templateCache.put(cacheKey, new CachedTemplate(content, sourcePath, lastModified));
    }

    /**
     * Merges global and provided variables with validation.
     *
     * @param providedVariables the variables provided for this processing
     * @return merged variable map
     */
    private Map<String, Object> mergeVariables(Map<String, Object> providedVariables) {
        Map<String, Object> merged = new HashMap<>(globalVariables);

        if (providedVariables != null) {
            // Sanitize variable names and values
            providedVariables.forEach((key, value) -> {
                if (key != null && isValidVariableName(key)) {
                    merged.put(key, sanitizeVariableValue(value));
                }
            });
        }

        return merged;
    }

    /**
     * Processes template content with variable substitution and advanced features.
     *
     * <p>
     * Implements comprehensive template processing including:
     * </p>
     * <ul>
     * <li>Variable substitution with ${variable} syntax</li>
     * <li>Conditional blocks with {{#if}} syntax</li>
     * <li>Loop blocks with {{#each}} syntax</li>
     * <li>Nested variable resolution</li>
     * </ul>
     *
     * @param templateContent the template content to process
     * @param variables       the variables for substitution
     * @return processed content with variables substituted
     * @throws TemplateProcessingException if processing fails
     */
    private String processTemplateContent(String templateContent, Map<String, Object> variables)
            throws TemplateProcessingException {

        String processed = templateContent;

        try {
            // Process conditional blocks first
            if (configuration.isEnableConditionals()) {
                processed = processConditionals(processed, variables);
            }

            // Process loop blocks
            if (configuration.isEnableLoops()) {
                processed = processLoops(processed, variables);
            }

            // Process variable substitutions
            processed = processVariables(processed, variables);

            return processed;

        } catch (Exception e) {
            throw new TemplateProcessingException("Template processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Processes variable substitutions in template content.
     *
     * <p>
     * <strong>Security Feature:</strong> Validates variable names and
     * sanitizes values to prevent injection attacks.
     * </p>
     *
     * @param content   the content to process
     * @param variables the variables for substitution
     * @return content with variables substituted
     */
    private String processVariables(String content, Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = resolveVariable(variableName, variables);

            if (value != null) {
                String replacement = sanitizeVariableValue(value).toString();
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else if (configuration.isStrictMode()) {
                throw new IllegalArgumentException("Undefined variable: " + variableName);
            } else {
                // In non-strict mode, leave undefined variables as-is
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Processes conditional blocks in template content.
     *
     * <p>
     * Supports {{#if condition}} content {{/if}} syntax for conditional rendering.
     * </p>
     *
     * @param content   the content to process
     * @param variables the variables for condition evaluation
     * @return content with conditionals processed
     */
    private String processConditionals(String content, Map<String, Object> variables)
            throws TemplateProcessingException {
        StringBuffer result = new StringBuffer();
        Matcher matcher = CONDITIONAL_PATTERN.matcher(content);

        while (matcher.find()) {
            String condition = matcher.group(1);
            String conditionalContent = matcher.group(2);

            if (evaluateCondition(condition, variables)) {
                // Process the conditional content recursively
                String processedContent = processTemplateContent(conditionalContent, variables);
                matcher.appendReplacement(result, Matcher.quoteReplacement(processedContent));
            } else {
                // Remove the conditional block
                matcher.appendReplacement(result, "");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Processes loop blocks in template content.
     *
     * <p>
     * Supports {{#each items}} content {{/each}} syntax for iterative rendering.
     * </p>
     *
     * @param content   the content to process
     * @param variables the variables containing collections to iterate
     * @return content with loops processed
     */
    private String processLoops(String content, Map<String, Object> variables) throws TemplateProcessingException {
        StringBuffer result = new StringBuffer();
        Matcher matcher = LOOP_PATTERN.matcher(content);

        while (matcher.find()) {
            String collectionName = matcher.group(1);
            String loopContent = matcher.group(2);

            Object collection = resolveVariable(collectionName, variables);
            StringBuilder loopResult = new StringBuilder();

            if (collection instanceof Collection) {
                Collection<?> items = (Collection<?>) collection;
                int index = 0;
                for (Object item : items) {
                    Map<String, Object> loopVariables = new HashMap<>(variables);
                    loopVariables.put("item", item);
                    loopVariables.put("index", index++);
                    loopVariables.put("first", index == 1);
                    loopVariables.put("last", index == items.size());

                    String processedLoop = processTemplateContent(loopContent, loopVariables);
                    loopResult.append(processedLoop);
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(loopResult.toString()));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Resolves a variable from the variables map with dot notation support.
     *
     * <p>
     * Supports nested property access using dot notation (e.g., user.name).
     * </p>
     *
     * @param variableName the variable name (may include dots)
     * @param variables    the variables map
     * @return the resolved value, or null if not found
     */
    private Object resolveVariable(String variableName, Map<String, Object> variables) {
        if (!variableName.contains(".")) {
            return variables.get(variableName);
        }

        // Handle dot notation for nested properties
        String[] parts = variableName.split("\\.");
        Object current = variables.get(parts[0]);

        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                // Could extend to handle Java bean properties via reflection
                return null;
            }
        }

        return current;
    }

    /**
     * Evaluates a condition for conditional blocks.
     *
     * <p>
     * Simple condition evaluation supporting variable existence and equality.
     * </p>
     *
     * @param condition the condition to evaluate
     * @param variables the variables for evaluation
     * @return true if condition is met, false otherwise
     */
    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        condition = condition.trim();

        // Simple existence check
        if (!condition.contains(" ")) {
            Object value = resolveVariable(condition, variables);
            return value != null && !isFalsy(value);
        }

        // Simple equality check (var == value)
        if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            if (parts.length == 2) {
                Object left = resolveVariable(parts[0].trim(), variables);
                String rightStr = parts[1].trim();

                // Handle string literals (quoted values)
                if (rightStr.startsWith("\"") && rightStr.endsWith("\"")) {
                    rightStr = rightStr.substring(1, rightStr.length() - 1);
                }

                return Objects.equals(String.valueOf(left), rightStr);
            }
        }

        return false;
    }

    /**
     * Checks if a value is considered falsy for condition evaluation.
     *
     * @param value the value to check
     * @return true if value is falsy (null, false, empty, etc.)
     */
    private boolean isFalsy(Object value) {
        if (value == null)
            return true;
        if (value instanceof Boolean)
            return !(Boolean) value;
        if (value instanceof String)
            return ((String) value).isEmpty();
        if (value instanceof Collection)
            return ((Collection<?>) value).isEmpty();
        if (value instanceof Map)
            return ((Map<?, ?>) value).isEmpty();
        if (value instanceof Number)
            return ((Number) value).doubleValue() == 0.0;
        return false;
    }

    /**
     * Validates variable name against security requirements.
     *
     * @param name the variable name to validate
     * @return true if name is valid, false otherwise
     */
    private boolean isValidVariableName(String name) {
        return name != null &&
                !name.trim().isEmpty() &&
                name.matches("[a-zA-Z_][a-zA-Z0-9_.]*");
    }

    /**
     * Sanitizes variable value for safe substitution.
     *
     * <p>
     * <strong>Security Feature:</strong> Prevents injection attacks by
     * sanitizing variable values before substitution.
     * </p>
     *
     * @param value the value to sanitize
     * @return sanitized value safe for template substitution
     */
    private Object sanitizeVariableValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String) {
            String str = (String) value;
            // Basic HTML escaping for security
            return str.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
        }

        return value;
    }

    /**
     * Gets file extension from filename.
     *
     * @param filename the filename
     * @return file extension (without dot)
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }

    /**
     * Lists all available templates in the template directory.
     *
     * <p>
     * <strong>Java I/O Feature:</strong> Directory traversal using
     * Files.walk() for template discovery.
     * </p>
     *
     * @return list of template file paths relative to template directory
     * @throws IOException if directory access fails
     */
    public List<Path> listTemplates() throws IOException {
        List<Path> templates = new ArrayList<>();

        if (!Files.exists(templateDirectory)) {
            return templates;
        }

        try (var stream = Files.walk(templateDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        String extension = getFileExtension(filename).toLowerCase();
                        return SUPPORTED_EXTENSIONS.contains(extension);
                    })
                    .forEach(path -> templates.add(templateDirectory.relativize(path)));
        }

        return templates;
    }

    /**
     * Sets a global variable available to all templates.
     *
     * @param name  the variable name
     * @param value the variable value
     * @throws IllegalArgumentException if name is invalid
     */
    public void setGlobalVariable(String name, Object value) {
        if (!isValidVariableName(name)) {
            throw new IllegalArgumentException("Invalid variable name: " + name);
        }
        globalVariables.put(name, sanitizeVariableValue(value));
    }

    /**
     * Gets a global variable value.
     *
     * @param name the variable name
     * @return the variable value, or null if not set
     */
    public Object getGlobalVariable(String name) {
        return globalVariables.get(name);
    }

    /**
     * Removes a global variable.
     *
     * @param name the variable name to remove
     * @return the previous value, or null if not set
     */
    public Object removeGlobalVariable(String name) {
        return globalVariables.remove(name);
    }

    /**
     * Clears the template cache.
     */
    public void clearCache() {
        templateCache.clear();
    }

    /**
     * Gets cache statistics.
     *
     * @return map containing cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", templateCache.size());
        stats.put("maxCacheSize", configuration.getMaxCacheSize());
        stats.put("cacheTimeout", configuration.getCacheTimeout());

        long expired = templateCache.values().stream()
                .mapToLong(cached -> cached.isExpired(configuration.getCacheTimeout()) ? 1 : 0)
                .sum();
        stats.put("expiredEntries", expired);

        return stats;
    }

    /**
     * Gets the template directory path.
     *
     * @return the template directory path
     */
    public Path getTemplateDirectory() {
        return templateDirectory;
    }

    /**
     * Gets the template configuration.
     *
     * @return the template configuration
     */
    public TemplateConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Exception thrown when template processing fails.
     */
    public static class TemplateProcessingException extends Exception {
        public TemplateProcessingException(String message) {
            super(message);
        }

        public TemplateProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
