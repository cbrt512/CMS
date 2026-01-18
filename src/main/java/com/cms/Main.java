package com.cms;

import com.cms.util.LoggerUtil;
import com.cms.core.model.*;
import com.cms.patterns.factory.ContentFactory;
import com.cms.core.repository.ContentRepository;
import com.cms.security.InputSanitizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Main demonstration class for the JavaCMS Content Management System.
 *
 * <p>
 * This class provides a simple demonstration of the core CMS functionality,
 * showcasing the implemented design patterns and core features. It serves as
 * an entry point for testing and demonstrating the system's capabilities.
 * </p>
 *
 * <p>
 * <strong>Patterns Demonstrated:</strong>
 * - Factory Pattern: Content creation via ContentFactory
 * - Repository Pattern: Data access via ContentRepository
 * - Exception Shielding: Controlled error handling
 * - Collections Framework: Use of Maps and Lists
 * - Generics: Type-safe operations throughout
 * </p>
 *
 * <p>
 * <strong>Features Implemented:</strong>
 * - Factory Pattern
 * - Collections Framework
 * - Generics
 * - Logging Framework
 * - Input Sanitization Security
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class Main {

    public static void main(String[] args) {
        LoggerUtil.logInfo("Main", "Starting JavaCMS demonstration...");

        try {
            // Initialize repository
            ContentRepository repository = new ContentRepository();
            LoggerUtil.logInfo("Main", "Content repository initialized");

            // Create a sample user
            User sampleUser = createSampleUser();
            LoggerUtil.logInfo("Main", "Sample user created: " + sampleUser.getUsername());

            // Demonstrate Factory Pattern - Create different content types
            demonstrateFactoryPattern(repository, sampleUser);

            // Demonstrate Repository Pattern - CRUD operations
            demonstrateRepositoryPattern(repository);

            // Demonstrate Collections Framework and Generics
            demonstrateCollectionsAndGenerics(repository);

            // Demonstrate Input Sanitization
            demonstrateSecurityFeatures();

            LoggerUtil.logInfo("Main", "JavaCMS demonstration completed successfully");

        } catch (Exception e) {
            LoggerUtil.logError("Main", "Demonstration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a sample user for demonstration purposes.
     *
     * @return A configured sample user
     */
    private static User createSampleUser() {
        try {
            // Create user with basic info
            User user = new User("demo-user", "demo@example.com", "Demo User", "password123");
            LoggerUtil.logInfo("Main", "Created demo user successfully");
            return user;
        } catch (Exception e) {
            LoggerUtil.logError("Main", "Failed to create demo user", e);
            throw new RuntimeException("Demo user creation failed", e);
        }
    }

    /**
     * Demonstrates the Factory Pattern implementation by creating different content
     * types.
     *
     * @param repository The content repository to save created content
     * @param user       The user creating the content
     */
    private static void demonstrateFactoryPattern(ContentRepository repository, User user) {
        LoggerUtil.logInfo("Main", "=== Demonstrating Factory Pattern ===");

        try {
            // Create Article content
            Map<String, Object> articleProperties = new HashMap<>();
            articleProperties.put("title", "Sample Article");
            articleProperties.put("body", "This is a sample article created using the Factory Pattern.");
            articleProperties.put("category", "Technology");

            Content articleContent = ContentFactory.createContent("ARTICLE", articleProperties, user.getUsername());
            repository.save(articleContent);

            LoggerUtil.logInfo("Main", "Created Article: " + articleContent.getId());

            // Create Page content
            Map<String, Object> pageProperties = new HashMap<>();
            pageProperties.put("title", "Sample Page");
            pageProperties.put("body", "This is a sample page created using the Factory Pattern.");
            pageProperties.put("template", "default");

            Content pageContent = ContentFactory.createContent("PAGE", pageProperties, user.getUsername());
            repository.save(pageContent);

            LoggerUtil.logInfo("Main", "Created Page: " + pageContent.getId());

            LoggerUtil.logInfo("Main", "Factory Pattern demonstration completed");

        } catch (Exception e) {
            LoggerUtil.logError("Main", "Factory Pattern demonstration failed", e);
        }
    }

    /**
     * Demonstrates Repository Pattern with CRUD operations.
     *
     * @param repository The content repository to demonstrate
     */
    private static void demonstrateRepositoryPattern(ContentRepository repository) {
        LoggerUtil.logInfo("Main", "=== Demonstrating Repository Pattern ===");

        try {
            // Count total content
            long totalContent = repository.count();
            LoggerUtil.logInfo("Main", "Total content in repository: " + totalContent);

            // Find all content
            var allContent = repository.findAll();
            LoggerUtil.logInfo("Main", "Retrieved " + allContent.size() + " content items");

            if (!allContent.isEmpty()) {
                // Find by ID
                Content firstContent = allContent.get(0);
                var foundContent = repository.findById(firstContent.getId());

                if (foundContent.isPresent()) {
                    LoggerUtil.logInfo("Main", "Found content by ID: " + foundContent.get().getTitle());
                }

                // Check existence
                boolean exists = repository.existsById(firstContent.getId());
                LoggerUtil.logInfo("Main", "Content exists check: " + exists);
            }

            LoggerUtil.logInfo("Main", "Repository Pattern demonstration completed");

        } catch (Exception e) {
            LoggerUtil.logError("Main", "Repository Pattern demonstration failed", e);
        }
    }

    /**
     * Demonstrates Collections Framework and Generics usage.
     *
     * @param repository The content repository for querying
     */
    private static void demonstrateCollectionsAndGenerics(ContentRepository repository) {
        LoggerUtil.logInfo("Main", "=== Demonstrating Collections Framework & Generics ===");

        try {
            // Demonstrate generics with type-safe collections
            Map<String, String> contentSummary = new HashMap<>();

            // Get all content and demonstrate stream operations
            var allContent = repository.findAll();

            for (Content content : allContent) {
                contentSummary.put(content.getId(), content.getTitle());
            }

            LoggerUtil.logInfo("Main", "Created type-safe content summary with " + contentSummary.size() + " entries");

            // Demonstrate Collections Framework operations
            LoggerUtil.logInfo("Main", "Content IDs: " + contentSummary.keySet());
            LoggerUtil.logInfo("Main", "Content titles: " + contentSummary.values());

            LoggerUtil.logInfo("Main", "Collections & Generics demonstration completed");

        } catch (Exception e) {
            LoggerUtil.logError("Main", "Collections & Generics demonstration failed", e);
        }
    }

    /**
     * Demonstrates security features including input sanitization.
     */
    private static void demonstrateSecurityFeatures() {
        LoggerUtil.logInfo("Main", "=== Demonstrating Security Features ===");

        try {
            // Test HTML sanitization
            String maliciousHtml = "<script>alert('XSS')</script><p>Safe content</p>";
            String sanitizedHtml = InputSanitizer.sanitizeHtml(maliciousHtml);

            LoggerUtil.logInfo("Main", "Original: " + maliciousHtml);
            LoggerUtil.logInfo("Main", "Sanitized: " + sanitizedHtml);

            // Test filename sanitization
            String maliciousFilename = "../../../etc/passwd";
            String sanitizedFilename = InputSanitizer.sanitizeFileName(maliciousFilename);

            LoggerUtil.logInfo("Main", "Original filename: " + maliciousFilename);
            LoggerUtil.logInfo("Main", "Sanitized filename: " + sanitizedFilename);

            LoggerUtil.logInfo("Main", "Security features demonstration completed");

        } catch (Exception e) {
            LoggerUtil.logError("Main", "Security features demonstration failed", e);
        }
    }
}
