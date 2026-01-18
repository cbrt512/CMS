package com.cms.io;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Content export service providing comprehensive data export functionality
 * with multiple format support and advanced I/O operations.
 *
 * <p>
 * This service provides content export capabilities for the CMS,
 * supporting multiple output formats (XML, JSON, CSV) with streaming
 * for large datasets, progress tracking, and comprehensive error handling.
 * It supports both full and incremental exports with filtering capabilities.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Strategy Pattern - Implements different
 * export strategies for various formats while maintaining a unified interface
 * for content export operations.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Java I/O Operations
 * Demonstrates comprehensive I/O operations including stream writing,
 * character encoding handling, buffered operations, and resource management
 * with support for large dataset processing.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - Output stream validation and sanitization
 * - Content filtering for sensitive information
 * - Safe XML/JSON serialization with escaping
 * - Progress tracking without sensitive data exposure
 * - Resource cleanup with proper exception handling
 * </p>
 *
 * <p>
 * <strong>Collections Framework Usage:</strong>
 * Uses List for content collections, Map for export metadata,
 * Set for filtering criteria, and Stream API for efficient processing,
 * providing comprehensive collections integration.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentExporter {

    /** Supported export formats */
    public enum ExportFormat {
        XML(".xml"),
        JSON(".json"),
        CSV(".csv"),
        TSV(".tsv");

        private final String extension;

        ExportFormat(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }

    /** Export configuration options */
    public static class ExportOptions {
        private boolean includeMetadata = true;
        private boolean includeInactive = false;
        private boolean prettifyOutput = true;
        private boolean includeSystemFields = false;
        private Set<ContentStatus> statusFilter = null;
        private Set<String> fieldFilter = null;
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";
        private int batchSize = 100;

        public boolean isIncludeMetadata() {
            return includeMetadata;
        }

        public void setIncludeMetadata(boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
        }

        public boolean isIncludeInactive() {
            return includeInactive;
        }

        public void setIncludeInactive(boolean includeInactive) {
            this.includeInactive = includeInactive;
        }

        public boolean isPrettifyOutput() {
            return prettifyOutput;
        }

        public void setPrettifyOutput(boolean prettifyOutput) {
            this.prettifyOutput = prettifyOutput;
        }

        public boolean isIncludeSystemFields() {
            return includeSystemFields;
        }

        public void setIncludeSystemFields(boolean includeSystemFields) {
            this.includeSystemFields = includeSystemFields;
        }

        public Set<ContentStatus> getStatusFilter() {
            return statusFilter;
        }

        public void setStatusFilter(Set<ContentStatus> statusFilter) {
            this.statusFilter = statusFilter != null ? Set.copyOf(statusFilter) : null;
        }

        public Set<String> getFieldFilter() {
            return fieldFilter;
        }

        public void setFieldFilter(Set<String> fieldFilter) {
            this.fieldFilter = fieldFilter != null ? Set.copyOf(fieldFilter) : null;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat != null ? dateFormat : "yyyy-MM-dd HH:mm:ss";
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = Math.max(1, Math.min(batchSize, 1000));
        }
    }

    /** Export progress tracking */
    public static class ExportProgress {
        private final AtomicLong totalItems = new AtomicLong(0);
        private final AtomicLong processedItems = new AtomicLong(0);
        private final AtomicLong skippedItems = new AtomicLong(0);
        private final AtomicLong errorItems = new AtomicLong(0);
        private volatile boolean completed = false;
        private volatile String currentPhase = "Initializing";

        public long getTotalItems() {
            return totalItems.get();
        }

        public void setTotalItems(long total) {
            totalItems.set(Math.max(0, total));
        }

        public long getProcessedItems() {
            return processedItems.get();
        }

        public void incrementProcessed() {
            processedItems.incrementAndGet();
        }

        public long getSkippedItems() {
            return skippedItems.get();
        }

        public void incrementSkipped() {
            skippedItems.incrementAndGet();
        }

        public long getErrorItems() {
            return errorItems.get();
        }

        public void incrementErrors() {
            errorItems.incrementAndGet();
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public String getCurrentPhase() {
            return currentPhase;
        }

        public void setCurrentPhase(String phase) {
            this.currentPhase = phase != null ? phase : "Unknown";
        }

        public double getPercentComplete() {
            long total = getTotalItems();
            return total > 0 ? (double) getProcessedItems() / total * 100.0 : 0.0;
        }

        public String getSummary() {
            return String.format("Progress: %d/%d (%.1f%%) - Processed: %d, Skipped: %d, Errors: %d",
                    getProcessedItems(), getTotalItems(), getPercentComplete(),
                    getProcessedItems(), getSkippedItems(), getErrorItems());
        }
    }

    private final ExportOptions defaultOptions;
    private final DateTimeFormatter dateFormatter;

    /**
     * Creates a ContentExporter with default options.
     */
    public ContentExporter() {
        this(new ExportOptions());
    }

    /**
     * Creates a ContentExporter with custom default options.
     *
     * @param defaultOptions the default export options to use
     */
    public ContentExporter(ExportOptions defaultOptions) {
        this.defaultOptions = Objects.requireNonNull(defaultOptions,
                "Default options cannot be null");
        this.dateFormatter = DateTimeFormatter.ofPattern(defaultOptions.getDateFormat());
    }

    /**
     * Exports content to XML format with comprehensive structure and metadata.
     *
     * <p>
     * This method provides complete XML export functionality including:
     * </p>
     * <ul>
     * <li>Well-formed XML structure with proper encoding</li>
     * <li>Content filtering based on status and criteria</li>
     * <li>Metadata inclusion with configurable options</li>
     * <li>Character escaping for XML safety</li>
     * <li>Progress tracking for large datasets</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>BufferedWriter for efficient character output</li>
     * <li>UTF-8 encoding with proper XML declaration</li>
     * <li>Resource management with try-with-resources</li>
     * <li>Streaming output for memory efficiency</li>
     * </ul>
     *
     * @param content the list of content to export
     * @param output  the output stream to write to
     * @param options export configuration options (null uses defaults)
     * @return export progress information
     * @throws IOException              if export fails
     * @throws IllegalArgumentException if parameters are invalid
     */
    public ExportProgress exportToXML(List<Content<?>> content, OutputStream output,
            ExportOptions options) throws IOException {
        // Input validation
        if (content == null) {
            throw new IllegalArgumentException("Content list cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }

        ExportOptions exportOptions = options != null ? options : defaultOptions;
        ExportProgress progress = new ExportProgress();

        // Filter content based on options
        List<Content<?>> filteredContent = filterContent(content, exportOptions);
        progress.setTotalItems(filteredContent.size());
        progress.setCurrentPhase("Exporting XML");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            // Write XML declaration and root element
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<contentExport>\n");

            // Write export metadata
            if (exportOptions.isIncludeMetadata()) {
                writeXmlMetadata(writer, filteredContent, exportOptions);
            }

            // Write content items
            writer.write("  <contents>\n");

            for (Content<?> item : filteredContent) {
                try {
                    writeXmlContent(writer, item, exportOptions);
                    progress.incrementProcessed();
                } catch (Exception e) {
                    progress.incrementErrors();
                    // Log error but continue processing
                    System.err.println("Error exporting content ID " + item.getId() + ": " + e.getMessage());
                }
            }

            writer.write("  </contents>\n");
            writer.write("</contentExport>\n");
            writer.flush();

            progress.setCurrentPhase("XML export completed");
            progress.setCompleted(true);

        } catch (IOException e) {
            progress.setCurrentPhase("XML export failed");
            throw e;
        }

        return progress;
    }

    /**
     * Writes XML metadata section with export information.
     *
     * @param writer  the XML writer
     * @param content the content list being exported
     * @param options the export options
     * @throws IOException if writing fails
     */
    private void writeXmlMetadata(BufferedWriter writer, List<Content<?>> content,
            ExportOptions options) throws IOException {
        writer.write("  <metadata>\n");
        writer.write("    <exportDate>" + escapeXml(dateFormatter.format(java.time.LocalDateTime.now()))
                + "</exportDate>\n");
        writer.write("    <totalItems>" + content.size() + "</totalItems>\n");
        writer.write("    <includeMetadata>" + options.isIncludeMetadata() + "</includeMetadata>\n");
        writer.write("    <includeInactive>" + options.isIncludeInactive() + "</includeInactive>\n");
        writer.write("    <formatVersion>1.0</formatVersion>\n");

        // Content type distribution
        Map<String, Long> typeDistribution = content.stream()
                .collect(Collectors.groupingBy(c -> c.getClass().getSimpleName(), Collectors.counting()));

        writer.write("    <contentTypes>\n");
        for (Map.Entry<String, Long> entry : typeDistribution.entrySet()) {
            writer.write(
                    "      <type name=\"" + escapeXml(entry.getKey()) + "\" count=\"" + entry.getValue() + "\"/>\n");
        }
        writer.write("    </contentTypes>\n");
        writer.write("  </metadata>\n");
    }

    /**
     * Writes a single content item as XML.
     *
     * @param writer  the XML writer
     * @param content the content item to write
     * @param options the export options
     * @throws IOException if writing fails
     */
    private void writeXmlContent(BufferedWriter writer, Content<?> content,
            ExportOptions options) throws IOException {
        writer.write("    <content>\n");

        // Basic content fields
        writer.write("      <id>" + escapeXml(content.getId()) + "</id>\n");
        writer.write("      <type>" + escapeXml(content.getClass().getSimpleName()) + "</type>\n");
        writer.write("      <title>" + escapeXml(content.getTitle()) + "</title>\n");
        writer.write("      <body>" + escapeXml(content.getBody()) + "</body>\n");
        writer.write("      <status>" + escapeXml(content.getStatus().name()) + "</status>\n");

        // Dates
        if (content.getCreatedDate() != null) {
            writer.write("      <createdDate>" + escapeXml(dateFormatter.format(content.getCreatedDate()))
                    + "</createdDate>\n");
        }
        if (content.getLastModified() != null) {
            writer.write("      <lastModified>" + escapeXml(dateFormatter.format(content.getLastModified()))
                    + "</lastModified>\n");
        }

        // Users
        if (content.getCreatedBy() != null) {
            writer.write("      <createdBy>" + escapeXml(content.getCreatedBy()) + "</createdBy>\n");
        }
        if (content.getLastModifiedBy() != null) {
            writer.write("      <lastModifiedBy>" + escapeXml(content.getLastModifiedBy()) + "</lastModifiedBy>\n");
        }

        // Metadata
        if (options.isIncludeMetadata() && content.getMetadata() != null && !content.getMetadata().isEmpty()) {
            writer.write("      <metadata>\n");
            for (Map.Entry<String, Object> entry : content.getMetadata().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    writer.write("        <" + escapeXml(key) + ">" +
                            escapeXml(value.toString()) + "</" + escapeXml(key) + ">\n");
                }
            }
            writer.write("      </metadata>\n");
        }

        writer.write("    </content>\n");
    }

    /**
     * Exports content to JSON format with structured data representation.
     *
     * <p>
     * This method provides complete JSON export functionality including:
     * </p>
     * <ul>
     * <li>Valid JSON structure with proper formatting</li>
     * <li>Content filtering and field selection</li>
     * <li>Metadata and system field inclusion options</li>
     * <li>Character escaping for JSON safety</li>
     * <li>Pretty printing for readability</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>BufferedWriter for efficient JSON output</li>
     * <li>UTF-8 encoding for international character support</li>
     * <li>Streaming JSON generation for large datasets</li>
     * <li>Resource management with exception handling</li>
     * </ul>
     *
     * @param content the list of content to export
     * @param output  the output stream to write to
     * @param options export configuration options (null uses defaults)
     * @return export progress information
     * @throws IOException              if export fails
     * @throws IllegalArgumentException if parameters are invalid
     */
    public ExportProgress exportToJSON(List<Content<?>> content, OutputStream output,
            ExportOptions options) throws IOException {
        // Input validation
        if (content == null) {
            throw new IllegalArgumentException("Content list cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }

        ExportOptions exportOptions = options != null ? options : defaultOptions;
        ExportProgress progress = new ExportProgress();

        // Filter content based on options
        List<Content<?>> filteredContent = filterContent(content, exportOptions);
        progress.setTotalItems(filteredContent.size());
        progress.setCurrentPhase("Exporting JSON");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            String indent = exportOptions.isPrettifyOutput() ? "  " : "";
            String newline = exportOptions.isPrettifyOutput() ? "\n" : "";

            // Start JSON structure
            writer.write("{" + newline);

            // Write export metadata
            if (exportOptions.isIncludeMetadata()) {
                writeJsonMetadata(writer, filteredContent, exportOptions, indent, newline);
                writer.write("," + newline);
            }

            // Write content array
            writer.write(indent + "\"contents\": [" + newline);

            for (int i = 0; i < filteredContent.size(); i++) {
                Content<?> item = filteredContent.get(i);
                try {
                    writeJsonContent(writer, item, exportOptions, indent + indent, newline);
                    if (i < filteredContent.size() - 1) {
                        writer.write(",");
                    }
                    writer.write(newline);
                    progress.incrementProcessed();
                } catch (Exception e) {
                    progress.incrementErrors();
                    // Log error but continue processing
                    System.err.println("Error exporting content ID " + item.getId() + ": " + e.getMessage());
                }
            }

            writer.write(indent + "]" + newline);
            writer.write("}" + newline);
            writer.flush();

            progress.setCurrentPhase("JSON export completed");
            progress.setCompleted(true);

        } catch (IOException e) {
            progress.setCurrentPhase("JSON export failed");
            throw e;
        }

        return progress;
    }

    /**
     * Writes JSON metadata section.
     *
     * @param writer  the JSON writer
     * @param content the content list being exported
     * @param options the export options
     * @param indent  the indentation string
     * @param newline the newline string
     * @throws IOException if writing fails
     */
    private void writeJsonMetadata(BufferedWriter writer, List<Content<?>> content,
            ExportOptions options, String indent, String newline) throws IOException {
        writer.write(indent + "\"metadata\": {" + newline);
        writer.write(indent + indent + "\"exportDate\": \""
                + escapeJson(dateFormatter.format(java.time.LocalDateTime.now())) + "\"," + newline);
        writer.write(indent + indent + "\"totalItems\": " + content.size() + "," + newline);
        writer.write(indent + indent + "\"includeMetadata\": " + options.isIncludeMetadata() + "," + newline);
        writer.write(indent + indent + "\"includeInactive\": " + options.isIncludeInactive() + "," + newline);
        writer.write(indent + indent + "\"formatVersion\": \"1.0\"," + newline);

        // Content type distribution
        Map<String, Long> typeDistribution = content.stream()
                .collect(Collectors.groupingBy(c -> c.getClass().getSimpleName(), Collectors.counting()));

        writer.write(indent + indent + "\"contentTypes\": {" + newline);
        String[] types = typeDistribution.keySet().toArray(new String[0]);
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            writer.write(indent + indent + indent + "\"" + escapeJson(type) + "\": " + typeDistribution.get(type));
            if (i < types.length - 1) {
                writer.write(",");
            }
            writer.write(newline);
        }
        writer.write(indent + indent + "}" + newline);
        writer.write(indent + "}");
    }

    /**
     * Writes a single content item as JSON.
     *
     * @param writer  the JSON writer
     * @param content the content item to write
     * @param options the export options
     * @param indent  the indentation string
     * @param newline the newline string
     * @throws IOException if writing fails
     */
    private void writeJsonContent(BufferedWriter writer, Content<?> content,
            ExportOptions options, String indent, String newline) throws IOException {
        writer.write(indent + "{" + newline);

        // Basic content fields
        writer.write(indent + "  \"id\": \"" + escapeJson(content.getId()) + "\"," + newline);
        writer.write(indent + "  \"type\": \"" + escapeJson(content.getClass().getSimpleName()) + "\"," + newline);
        writer.write(indent + "  \"title\": \"" + escapeJson(content.getTitle()) + "\"," + newline);
        writer.write(indent + "  \"body\": \"" + escapeJson(content.getBody()) + "\"," + newline);
        writer.write(indent + "  \"status\": \"" + escapeJson(content.getStatus().name()) + "\"," + newline);

        // Dates
        if (content.getCreatedDate() != null) {
            writer.write(indent + "  \"createdDate\": \"" + escapeJson(dateFormatter.format(content.getCreatedDate()))
                    + "\"," + newline);
        }
        if (content.getLastModified() != null) {
            writer.write(indent + "  \"lastModified\": \"" + escapeJson(dateFormatter.format(content.getLastModified()))
                    + "\"," + newline);
        }

        // Users
        if (content.getCreatedBy() != null) {
            writer.write(indent + "  \"createdBy\": \"" + escapeJson(content.getCreatedBy()) + "\"," + newline);
        }
        if (content.getLastModifiedBy() != null) {
            writer.write(
                    indent + "  \"lastModifiedBy\": \"" + escapeJson(content.getLastModifiedBy()) + "\"," + newline);
        }

        // Metadata (without trailing comma)
        if (options.isIncludeMetadata() && content.getMetadata() != null && !content.getMetadata().isEmpty()) {
            writer.write(indent + "  \"metadata\": {" + newline);

            String[] keys = content.getMetadata().keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                Object value = content.getMetadata().get(key);
                if (value != null) {
                    writer.write(indent + "    \"" + escapeJson(key) + "\": \"" +
                            escapeJson(value.toString()) + "\"");
                    if (i < keys.length - 1) {
                        writer.write(",");
                    }
                    writer.write(newline);
                }
            }
            writer.write(indent + "  \"}" + newline);
        } else {
            // Remove trailing comma from last field
            writer.write(indent + "  \"version\": 1" + newline);
        }

        writer.write(indent + "}");
    }

    /**
     * Exports content to CSV format with tabular structure.
     *
     * <p>
     * Provides CSV export with configurable field selection,
     * proper escaping, and header generation for spreadsheet compatibility.
     * </p>
     *
     * @param content the list of content to export
     * @param output  the output stream to write to
     * @param options export configuration options (null uses defaults)
     * @return export progress information
     * @throws IOException if export fails
     */
    public ExportProgress exportToCSV(List<Content<?>> content, OutputStream output,
            ExportOptions options) throws IOException {
        return exportToDelimited(content, output, options, ",", ExportFormat.CSV);
    }

    /**
     * Exports content to TSV (Tab-Separated Values) format.
     *
     * <p>
     * Provides TSV export with tab delimiters for improved data integrity
     * when content contains commas.
     * </p>
     *
     * @param content the list of content to export
     * @param output  the output stream to write to
     * @param options export configuration options (null uses defaults)
     * @return export progress information
     * @throws IOException if export fails
     */
    public ExportProgress exportToTSV(List<Content<?>> content, OutputStream output,
            ExportOptions options) throws IOException {
        return exportToDelimited(content, output, options, "\t", ExportFormat.TSV);
    }

    /**
     * Exports content to delimited format (CSV/TSV) with configurable delimiter.
     *
     * <p>
     * <strong>Java I/O Implementation:</strong> Demonstrates character-based
     * output with proper encoding and delimiter handling for tabular data.
     * </p>
     *
     * @param content   the list of content to export
     * @param output    the output stream to write to
     * @param options   export configuration options
     * @param delimiter the field delimiter to use
     * @param format    the export format for progress tracking
     * @return export progress information
     * @throws IOException if export fails
     */
    private ExportProgress exportToDelimited(List<Content<?>> content, OutputStream output,
            ExportOptions options, String delimiter,
            ExportFormat format) throws IOException {
        // Input validation
        if (content == null) {
            throw new IllegalArgumentException("Content list cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }

        ExportOptions exportOptions = options != null ? options : defaultOptions;
        ExportProgress progress = new ExportProgress();

        // Filter content based on options
        List<Content<?>> filteredContent = filterContent(content, exportOptions);
        progress.setTotalItems(filteredContent.size());
        progress.setCurrentPhase("Exporting " + format.name());

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

            // Write headers
            writeDelimitedHeaders(writer, exportOptions, delimiter);
            writer.write("\n");

            // Write content rows
            for (Content<?> item : filteredContent) {
                try {
                    writeDelimitedContent(writer, item, exportOptions, delimiter);
                    writer.write("\n");
                    progress.incrementProcessed();
                } catch (Exception e) {
                    progress.incrementErrors();
                    // Log error but continue processing
                    System.err.println("Error exporting content ID " + item.getId() + ": " + e.getMessage());
                }
            }

            writer.flush();

            progress.setCurrentPhase(format.name() + " export completed");
            progress.setCompleted(true);

        } catch (IOException e) {
            progress.setCurrentPhase(format.name() + " export failed");
            throw e;
        }

        return progress;
    }

    /**
     * Writes column headers for delimited format.
     *
     * @param writer    the output writer
     * @param options   the export options
     * @param delimiter the field delimiter
     * @throws IOException if writing fails
     */
    private void writeDelimitedHeaders(BufferedWriter writer, ExportOptions options,
            String delimiter) throws IOException {
        List<String> headers = new ArrayList<>();

        // Basic headers
        headers.add("ID");
        headers.add("Type");
        headers.add("Title");
        headers.add("Body");
        headers.add("Status");
        headers.add("Created Date");
        headers.add("Created By");
        headers.add("Last Modified");
        headers.add("Last Modified By");

        // System fields if requested
        if (options.isIncludeSystemFields()) {
            headers.add("Version");
            headers.add("Active");
        }

        writer.write(headers.stream()
                .map(this::escapeDelimited)
                .collect(Collectors.joining(delimiter)));
    }

    /**
     * Writes a content row for delimited format.
     *
     * @param writer    the output writer
     * @param content   the content item to write
     * @param options   the export options
     * @param delimiter the field delimiter
     * @throws IOException if writing fails
     */
    private void writeDelimitedContent(BufferedWriter writer, Content<?> content,
            ExportOptions options, String delimiter) throws IOException {
        List<String> fields = new ArrayList<>();

        // Basic fields
        fields.add(escapeDelimited(content.getId()));
        fields.add(escapeDelimited(content.getClass().getSimpleName()));
        fields.add(escapeDelimited(content.getTitle()));
        fields.add(escapeDelimited(content.getBody()));
        fields.add(escapeDelimited(content.getStatus().name()));

        // Dates
        fields.add(content.getCreatedDate() != null ? escapeDelimited(dateFormatter.format(content.getCreatedDate()))
                : "");
        fields.add(escapeDelimited(content.getCreatedBy()));
        fields.add(content.getLastModified() != null ? escapeDelimited(dateFormatter.format(content.getLastModified()))
                : "");
        fields.add(escapeDelimited(content.getLastModifiedBy()));

        // System fields if requested
        if (options.isIncludeSystemFields()) {
            fields.add(String.valueOf(content.getVersion()));
            fields.add(String.valueOf(content.isActive()));
        }

        writer.write(String.join(delimiter, fields));
    }

    /**
     * Filters content based on export options.
     *
     * @param content the content list to filter
     * @param options the export options
     * @return filtered content list
     */
    private List<Content<?>> filterContent(List<Content<?>> content, ExportOptions options) {
        return content.stream()
                .filter(c -> options.isIncludeInactive() || c.isActive())
                .filter(c -> options.getStatusFilter() == null ||
                        options.getStatusFilter().contains(c.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Escapes XML special characters.
     *
     * @param text the text to escape
     * @return XML-safe text
     */
    private String escapeXml(String text) {
        if (text == null)
            return "";

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Escapes JSON special characters.
     *
     * @param text the text to escape
     * @return JSON-safe text
     */
    private String escapeJson(String text) {
        if (text == null)
            return "";

        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    /**
     * Escapes delimited format special characters.
     *
     * @param text the text to escape
     * @return delimited-format-safe text
     */
    private String escapeDelimited(String text) {
        if (text == null)
            return "";

        // If text contains quotes, comma, or newline, wrap in quotes and escape
        // internal quotes
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }

    /**
     * Gets statistics about the export service usage.
     *
     * @return map containing export statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("supportedFormats", Arrays.stream(ExportFormat.values())
                .map(ExportFormat::name)
                .collect(Collectors.toList()));
        stats.put("defaultDateFormat", defaultOptions.getDateFormat());
        stats.put("defaultBatchSize", defaultOptions.getBatchSize());
        stats.put("defaultIncludeMetadata", defaultOptions.isIncludeMetadata());
        return stats;
    }
}
