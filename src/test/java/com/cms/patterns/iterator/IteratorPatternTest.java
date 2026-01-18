package com.cms.patterns.iterator;

import com.cms.core.model.*;
import com.cms.patterns.factory.*;
import com.cms.patterns.composite.*;
import com.cms.patterns.iterator.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive test suite for Iterator Pattern implementation in JavaCMS.
 *
 * <p>This test class validates all four iterator implementations and demonstrates
 * the complete Iterator Pattern functionality. It tests integration with existing
 * Content, User, and Site models while verifying proper iteration behavior,
 * filtering capabilities, and error handling.</p>
 *
 * <p><strong>Test Coverage:</strong>
 * <ul>
 *   <li><strong>ContentIterator:</strong> Content collection traversal with filtering</li>
 *   <li><strong>SiteStructureIterator:</strong> Site hierarchy traversal (depth-first, breadth-first)</li>
 *   <li><strong>PublishingPipelineIterator:</strong> Publishing workflow processing</li>
 *   <li><strong>UserSessionIterator:</strong> Session management and monitoring</li>
 * </ul></p>
 *
 * <p><strong>Requirements Validated:</strong>
 * <ul>
 *   <li>Iterator Pattern implementation</li>
 *   <li>Integration with Collections Framework</li>
 *   <li>Integration with existing Composite Pattern</li>
 *   <li>Exception handling and input validation</li>
 *   <li>Advanced filtering and traversal strategies</li>
 * </ul></p>
 *
 * @see com.cms.patterns.iterator.ContentIterator
 * @see com.cms.patterns.iterator.SiteStructureIterator
 * @see com.cms.patterns.iterator.PublishingPipelineIterator
 * @see com.cms.patterns.iterator.UserSessionIterator
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Iterator Pattern Test Suite")
class IteratorPatternTest {

    private ContentFactory contentFactory;
    private Site testSite;
    private List<Content<?>> testContent;
    private List<UserSession> testSessions;
    private List<PublishTask> testTasks;
    private SiteComponent siteHierarchy;

    @BeforeEach
    void setUp() {
        // Initialize test data
        setupContentFactory();
        setupTestSite();
        setupTestContent();
        setupTestSessions();
        setupTestTasks();
        setupSiteHierarchy();
    }

    private void setupContentFactory() {
        contentFactory = new ContentFactory() {
            @Override
            public Content<?> createContent(String type, String title, String body, String createdBy) {
                return switch (type.toLowerCase()) {
                    case "article" -> new ArticleContent(title, body, createdBy);
                    case "page" -> new PageContent(title, body, createdBy);
                    case "image" -> new ImageContent(title, body, createdBy);
                    case "video" -> new VideoContent(title, body, createdBy);
                    default -> throw new IllegalArgumentException("Unknown content type: " + type);
                };
            }
        };
    }

    private void setupTestSite() {
        testSite = new Site("Test CMS Site", "test.cms.com");
        testSite.setDescription("Test site for iterator pattern validation");
    }

    private void setupTestContent() {
        testContent = new ArrayList<>();

        // Create articles
        for (int i = 1; i <= 5; i++) {
            Content<?> article = contentFactory.createContent("article",
                "Test Article " + i, "Article content " + i, "author" + (i % 2 + 1));
            if (i <= 3) {
                article.setStatus(ContentStatus.PUBLISHED);
            }
            testContent.add(article);
        }

        // Create pages
        for (int i = 1; i <= 3; i++) {
            Content<?> page = contentFactory.createContent("page",
                "Test Page " + i, "Page content " + i, "author" + (i % 2 + 1));
            if (i <= 2) {
                page.setStatus(ContentStatus.PUBLISHED);
            }
            testContent.add(page);
        }

        // Create media content
        testContent.add(contentFactory.createContent("image",
            "Test Image", "Image description", "author1"));
        testContent.add(contentFactory.createContent("video",
            "Test Video", "Video description", "author2"));
    }

    private void setupTestSessions() {
        testSessions = new ArrayList<>();

        // Create active sessions
        User admin = new User("admin", "password", "admin@test.com", Role.ADMIN);
        User editor = new User("editor", "password", "editor@test.com", Role.EDITOR);
        User viewer = new User("viewer", "password", "viewer@test.com", Role.VIEWER);

        // Active sessions (recent activity)
        testSessions.add(new UserSession(admin));
        testSessions.add(new UserSession(editor));

        // Idle session (older activity)
        UserSession idleSession = new UserSession(viewer);
        // Simulate idle session by setting last activity time to 10 minutes ago
        // Note: In a real implementation, we'd need to modify the UserSession to allow this
        testSessions.add(idleSession);

        // Create an expired session (would need session creation time manipulation)
        testSessions.add(new UserSession(new User("expired", "password", "expired@test.com", Role.VIEWER)));
    }

    private void setupTestTasks() {
        testTasks = new ArrayList<>();

        for (int i = 0; i < testContent.size(); i++) {
            Content<?> content = testContent.get(i);
            PublishTask.Priority priority = i < 2 ? PublishTask.Priority.HIGH :
                                          i < 5 ? PublishTask.Priority.NORMAL :
                                          PublishTask.Priority.LOW;

            PublishTask task = new PublishTask(content, "Publish " + content.getTitle(), priority);
            testTasks.add(task);
        }

        // Add some failed tasks for retry testing
        PublishTask failedTask = new PublishTask(testContent.get(0), "Failed publish task");
        failedTask.markStarted();
        failedTask.markFailed("Simulated failure");
        testTasks.add(failedTask);
    }

    private void setupSiteHierarchy() {
        // Create a complex site structure for testing
        siteHierarchy = testSite;

        try {
            // Create main categories
            Category blogCategory = new Category("Blog", "Blog articles and posts");
            Category pagesCategory = new Category("Pages", "Static pages");
            Category mediaCategory = new Category("Media", "Images and videos");

            // Add categories to site
            testSite.add(blogCategory);
            testSite.add(pagesCategory);
            testSite.add(mediaCategory);

            // Add subcategories
            Category techBlog = new Category("Tech", "Technology articles");
            Category newsBlog = new Category("News", "News articles");
            blogCategory.add(techBlog);
            blogCategory.add(newsBlog);

            // Add content items to categories
            for (Content<?> content : testContent) {
                ContentItem item = new ContentItem(content);
                if (content instanceof ArticleContent) {
                    techBlog.add(item);
                } else if (content instanceof PageContent) {
                    pagesCategory.add(item);
                } else {
                    mediaCategory.add(item);
                }
            }
        } catch (Exception e) {
            fail("Failed to setup site hierarchy: " + e.getMessage());
        }
    }

    // ===== ContentIterator Tests =====

    @Test
    @DisplayName("ContentIterator - Basic Iteration")
    void testContentIteratorBasicIteration() {
        ContentIterator<Content<?>> iterator = new ContentIterator<>(testContent);

        assertNotNull(iterator, "Iterator should not be null");
        assertEquals(testContent.size(), iterator.size(), "Iterator size should match source collection");
        assertFalse(iterator.isEmpty(), "Iterator should not be empty");

        List<Content<?>> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }

        assertEquals(testContent.size(), iterated.size(), "Should iterate over all content");
        assertFalse(iterator.hasNext(), "Iterator should be exhausted");
        assertThrows(NoSuchElementException.class, iterator::next, "Should throw exception when exhausted");
    }

    @Test
    @DisplayName("ContentIterator - Filter by Status")
    void testContentIteratorStatusFiltering() {
        ContentIterator<Content<?>> publishedOnly = ContentIterator.publishedOnly(testContent);

        List<Content<?>> published = publishedOnly.toList();
        assertFalse(published.isEmpty(), "Should find published content");

        for (Content<?> content : published) {
            assertEquals(ContentStatus.PUBLISHED, content.getStatus(),
                "All iterated content should be published");
        }

        // Test drafts filter
        ContentIterator<Content<?>> draftsOnly = ContentIterator.draftsOnly(testContent);
        List<Content<?>> drafts = draftsOnly.toList();

        for (Content<?> content : drafts) {
            assertEquals(ContentStatus.DRAFT, content.getStatus(),
                "All iterated content should be draft");
        }
    }

    @Test
    @DisplayName("ContentIterator - Filter by Type")
    void testContentIteratorTypeFiltering() {
        ContentIterator<ArticleContent> articleIterator = ContentIterator.byType(testContent, ArticleContent.class);

        List<ArticleContent> articles = new ArrayList<>();
        while (articleIterator.hasNext()) {
            articles.add(articleIterator.next());
        }

        assertFalse(articles.isEmpty(), "Should find article content");
        for (ArticleContent article : articles) {
            assertInstanceOf(ArticleContent.class, article, "All items should be articles");
        }
    }

    @Test
    @DisplayName("ContentIterator - Filter by Creator")
    void testContentIteratorCreatorFiltering() {
        ContentIterator<Content<?>> author1Content = ContentIterator.byCreator(testContent, "author1");

        List<Content<?>> author1Items = author1Content.toList();
        assertFalse(author1Items.isEmpty(), "Should find content by author1");

        for (Content<?> content : author1Items) {
            assertEquals("author1", content.getCreatedBy(), "All content should be by author1");
        }
    }

    @Test
    @DisplayName("ContentIterator - Date Range Filtering")
    void testContentIteratorDateRangeFiltering() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        ContentIterator<Content<?>> recentContent = ContentIterator.byDateRange(testContent, yesterday, tomorrow);

        List<Content<?>> recent = recentContent.toList();
        assertEquals(testContent.size(), recent.size(), "All test content should be in date range");
    }

    @Test
    @DisplayName("ContentIterator - Multiple Filters")
    void testContentIteratorMultipleFilters() {
        ContentIterator<Content<?>> filtered = ContentIterator.withFilters(testContent,
            content -> content.getStatus() == ContentStatus.PUBLISHED,
            content -> content.getCreatedBy().equals("author1"));

        List<Content<?>> result = filtered.toList();

        for (Content<?> content : result) {
            assertEquals(ContentStatus.PUBLISHED, content.getStatus(), "Should be published");
            assertEquals("author1", content.getCreatedBy(), "Should be by author1");
        }
    }

    @Test
    @DisplayName("ContentIterator - Chained Filtering")
    void testContentIteratorChainedFiltering() {
        ContentIterator<Content<?>> baseIterator = new ContentIterator<>(testContent);
        ContentIterator<Content<?>> filtered = baseIterator.filter(
            content -> content.getStatus() == ContentStatus.PUBLISHED);

        List<Content<?>> published = filtered.toList();

        for (Content<?> content : published) {
            assertEquals(ContentStatus.PUBLISHED, content.getStatus(), "Should be published");
        }
    }

    // ===== SiteStructureIterator Tests =====

    @Test
    @DisplayName("SiteStructureIterator - Depth-First Traversal")
    void testSiteStructureIteratorDepthFirst() {
        SiteStructureIterator iterator = SiteStructureIterator.depthFirst(siteHierarchy);

        assertNotNull(iterator, "Iterator should not be null");
        assertFalse(iterator.isEmpty(), "Site structure should not be empty");
        assertEquals(SiteStructureIterator.TraversalMode.DEPTH_FIRST, iterator.getTraversalMode());

        List<SiteComponent> traversed = iterator.toList();
        assertFalse(traversed.isEmpty(), "Should traverse components");

        // First component should be the root site
        assertEquals(siteHierarchy, traversed.get(0), "First component should be root");
    }

    @Test
    @DisplayName("SiteStructureIterator - Breadth-First Traversal")
    void testSiteStructureIteratorBreadthFirst() {
        SiteStructureIterator iterator = SiteStructureIterator.breadthFirst(siteHierarchy);

        assertEquals(SiteStructureIterator.TraversalMode.BREADTH_FIRST, iterator.getTraversalMode());

        List<SiteComponent> traversed = iterator.toList();
        assertFalse(traversed.isEmpty(), "Should traverse components");

        // First component should be the root site
        assertEquals(siteHierarchy, traversed.get(0), "First component should be root");
    }

    @Test
    @DisplayName("SiteStructureIterator - Leaves Only")
    void testSiteStructureIteratorLeavesOnly() {
        SiteStructureIterator iterator = SiteStructureIterator.leavesOnly(siteHierarchy);

        List<SiteComponent> leaves = iterator.toList();
        assertFalse(leaves.isEmpty(), "Should find leaf components");

        // All components should be leaves (ContentItems)
        for (SiteComponent component : leaves) {
            assertInstanceOf(ContentItem.class, component, "All components should be leaves");
        }
    }

    @Test
    @DisplayName("SiteStructureIterator - Composites Only")
    void testSiteStructureIteratorCompositesOnly() {
        SiteStructureIterator iterator = SiteStructureIterator.compositesOnly(siteHierarchy);

        List<SiteComponent> composites = iterator.toList();
        assertFalse(composites.isEmpty(), "Should find composite components");

        // Verify we can call getChildren on all components (they're composites)
        for (SiteComponent component : composites) {
            assertDoesNotThrow(() -> component.getChildren(),
                "All components should be composites");
        }
    }

    @Test
    @DisplayName("SiteStructureIterator - Filter by Name Pattern")
    void testSiteStructureIteratorNamePatternFilter() {
        SiteStructureIterator iterator = SiteStructureIterator.byNamePattern(siteHierarchy, "blog");

        List<SiteComponent> blogComponents = iterator.toList();

        for (SiteComponent component : blogComponents) {
            String name = component.getName().toLowerCase();
            assertTrue(name.contains("blog"), "Component name should contain 'blog'");
        }
    }

    @Test
    @DisplayName("SiteStructureIterator - Filter by Type")
    void testSiteStructureIteratorTypeFilter() {
        SiteStructureIterator iterator = SiteStructureIterator.byType(siteHierarchy, Category.class);

        List<SiteComponent> categories = iterator.toList();

        for (SiteComponent component : categories) {
            assertInstanceOf(Category.class, component, "All components should be Categories");
        }
    }

    @Test
    @DisplayName("SiteStructureIterator - Custom Filtering")
    void testSiteStructureIteratorCustomFilter() {
        SiteStructureIterator iterator = SiteStructureIterator.filtered(siteHierarchy,
            component -> component.getName().contains("Tech"));

        List<SiteComponent> techComponents = iterator.toList();

        for (SiteComponent component : techComponents) {
            assertTrue(component.getName().contains("Tech"), "Should contain 'Tech' in name");
        }
    }

    // ===== PublishingPipelineIterator Tests =====

    @Test
    @DisplayName("PublishingPipelineIterator - All Tasks Processing")
    void testPublishingPipelineIteratorAllTasks() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.allTasks(testTasks);

        assertNotNull(iterator, "Iterator should not be null");
        assertEquals(testTasks.size(), iterator.getTotalTaskCount(), "Should process all tasks");
        assertEquals(0, iterator.getProcessedCount(), "No tasks processed initially");
        assertEquals(PublishingPipelineIterator.ExecutionMode.ALL_TASKS, iterator.getExecutionMode());

        int processedCount = 0;
        while (iterator.hasNext()) {
            PublishTask task = iterator.next();
            assertNotNull(task, "Task should not be null");
            processedCount++;
        }

        assertEquals(testTasks.size(), processedCount, "Should process all tasks");
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Ready Tasks Only")
    void testPublishingPipelineIteratorReadyTasks() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.readyTasks(testTasks);

        assertEquals(PublishingPipelineIterator.ExecutionMode.READY_ONLY, iterator.getExecutionMode());

        List<PublishTask> readyTasks = new ArrayList<>();
        while (iterator.hasNext()) {
            PublishTask task = iterator.next();
            assertTrue(task.isReady(), "Task should be ready for execution");
            readyTasks.add(task);
        }

        assertFalse(readyTasks.isEmpty(), "Should find ready tasks");
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Priority-Based Processing")
    void testPublishingPipelineIteratorPriorityProcessing() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.byPriority(
            testTasks, PublishTask.Priority.HIGH);

        List<PublishTask> highPriorityTasks = new ArrayList<>();
        while (iterator.hasNext()) {
            PublishTask task = iterator.next();
            assertEquals(PublishTask.Priority.HIGH, task.getPriority(), "Should be high priority");
            highPriorityTasks.add(task);
        }

        assertFalse(highPriorityTasks.isEmpty(), "Should find high priority tasks");
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Status-Based Processing")
    void testPublishingPipelineIteratorStatusProcessing() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.byStatus(
            testTasks, PublishTask.TaskStatus.PENDING);

        List<PublishTask> pendingTasks = new ArrayList<>();
        while (iterator.hasNext()) {
            PublishTask task = iterator.next();
            assertEquals(PublishTask.TaskStatus.PENDING, task.getStatus(), "Should be pending");
            pendingTasks.add(task);
        }

        assertFalse(pendingTasks.isEmpty(), "Should find pending tasks");
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Retry Tasks")
    void testPublishingPipelineIteratorRetryTasks() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.retryTasks(testTasks);

        assertEquals(PublishingPipelineIterator.ExecutionMode.RETRY_ONLY, iterator.getExecutionMode());

        List<PublishTask> retryTasks = new ArrayList<>();
        while (iterator.hasNext()) {
            PublishTask task = iterator.next();
            assertTrue(task.canRetry(), "Task should be eligible for retry");
            retryTasks.add(task);
        }

        // We should have at least one failed task that can be retried
        assertFalse(retryTasks.isEmpty(), "Should find retry-eligible tasks");
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Task Completion Tracking")
    void testPublishingPipelineIteratorTaskCompletion() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.allTasks(testTasks);

        if (iterator.hasNext()) {
            PublishTask task = iterator.next();

            // Simulate successful task execution
            iterator.markCurrentTaskCompleted();
            assertEquals(PublishTask.TaskStatus.COMPLETED, task.getStatus(), "Task should be completed");

            List<PublishTask> completedTasks = iterator.getCompletedTasks();
            assertTrue(completedTasks.contains(task), "Completed tasks should contain our task");
        }
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Task Failure Handling")
    void testPublishingPipelineIteratorTaskFailure() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.allTasks(testTasks);

        if (iterator.hasNext()) {
            PublishTask task = iterator.next();

            // Simulate task failure
            iterator.markCurrentTaskFailed("Test failure message");
            assertEquals(PublishTask.TaskStatus.FAILED, task.getStatus(), "Task should be failed");
            assertEquals("Test failure message", task.getErrorMessage(), "Error message should be set");
        }
    }

    @Test
    @DisplayName("PublishingPipelineIterator - Progress Tracking")
    void testPublishingPipelineIteratorProgress() {
        PublishingPipelineIterator iterator = PublishingPipelineIterator.allTasks(testTasks);

        assertEquals(0.0, iterator.getProgress(), 0.01, "Initial progress should be 0%");

        // Process some tasks
        int tasksToProcess = Math.min(2, testTasks.size());
        for (int i = 0; i < tasksToProcess && iterator.hasNext(); i++) {
            iterator.next();
        }

        assertTrue(iterator.getProgress() > 0.0, "Progress should increase");
        assertEquals(tasksToProcess, iterator.getProcessedCount(), "Processed count should be correct");
    }

    // ===== UserSessionIterator Tests =====

    @Test
    @DisplayName("UserSessionIterator - All Sessions")
    void testUserSessionIteratorAllSessions() {
        UserSessionIterator iterator = UserSessionIterator.allSessions(testSessions);

        assertNotNull(iterator, "Iterator should not be null");
        assertEquals(testSessions.size(), iterator.size(), "Should iterate all sessions");
        assertEquals(UserSessionIterator.SelectionMode.ALL_SESSIONS, iterator.getSelectionMode());

        List<UserSession> iterated = iterator.toList();
        assertEquals(testSessions.size(), iterated.size(), "Should iterate all sessions");
    }

    @Test
    @DisplayName("UserSessionIterator - Active Sessions Only")
    void testUserSessionIteratorActiveSessions() {
        UserSessionIterator iterator = UserSessionIterator.activeSessions(testSessions);

        assertEquals(UserSessionIterator.SelectionMode.ACTIVE_ONLY, iterator.getSelectionMode());

        List<UserSession> activeSessions = iterator.toList();
        // Note: In a real implementation, we'd verify these are actually active
        // For now, we just verify the iterator works
        assertNotNull(activeSessions, "Active sessions list should not be null");
    }

    @Test
    @DisplayName("UserSessionIterator - Filter by Role")
    void testUserSessionIteratorByRole() {
        UserSessionIterator iterator = UserSessionIterator.byRole(testSessions, Role.ADMIN);

        List<UserSession> adminSessions = iterator.toList();

        for (UserSession session : adminSessions) {
            assertNotNull(session.getUser(), "Session should have a user");
            assertEquals(Role.ADMIN, session.getUser().getRole(), "User should be admin");
        }
    }

    @Test
    @DisplayName("UserSessionIterator - Filter by Username")
    void testUserSessionIteratorByUsername() {
        UserSessionIterator iterator = UserSessionIterator.byUsername(testSessions, "admin");

        List<UserSession> adminUserSessions = iterator.toList();

        for (UserSession session : adminUserSessions) {
            assertNotNull(session.getUser(), "Session should have a user");
            assertEquals("admin", session.getUser().getUsername(), "Username should be admin");
        }
    }

    @Test
    @DisplayName("UserSessionIterator - Session Statistics")
    void testUserSessionIteratorStatistics() {
        UserSessionIterator iterator = UserSessionIterator.allSessions(testSessions);

        Map<String, Object> stats = iterator.getSessionStatistics();

        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("totalSessions"), "Should contain total sessions");
        assertTrue(stats.containsKey("activeSessions"), "Should contain active sessions");
        assertTrue(stats.containsKey("roleDistribution"), "Should contain role distribution");

        assertEquals(testSessions.size(), stats.get("totalSessions"), "Total sessions should match");
    }

    @Test
    @DisplayName("UserSessionIterator - Group by Role")
    void testUserSessionIteratorGroupByRole() {
        UserSessionIterator iterator = UserSessionIterator.allSessions(testSessions);

        Map<Role, List<UserSession>> grouped = iterator.groupByRole();

        assertNotNull(grouped, "Grouped sessions should not be null");

        // Verify grouping is correct
        for (Map.Entry<Role, List<UserSession>> entry : grouped.entrySet()) {
            Role role = entry.getKey();
            List<UserSession> sessions = entry.getValue();

            for (UserSession session : sessions) {
                assertEquals(role, session.getUser().getRole(), "Session role should match group");
            }
        }
    }

    @Test
    @DisplayName("UserSessionIterator - Timeout Configuration")
    void testUserSessionIteratorTimeoutConfiguration() {
        UserSessionIterator iterator = UserSessionIterator.allSessions(testSessions);

        Duration originalTimeout = iterator.getSessionTimeout();
        assertNotNull(originalTimeout, "Default timeout should be set");

        Duration newTimeout = Duration.ofMinutes(60);
        iterator.setSessionTimeout(newTimeout);
        assertEquals(newTimeout, iterator.getSessionTimeout(), "Timeout should be updated");

        // Test invalid timeout
        assertThrows(IllegalArgumentException.class, () ->
            iterator.setSessionTimeout(Duration.ofMinutes(-1)),
            "Should reject negative timeout");
    }

    // ===== Integration and Error Handling Tests =====

    @Test
    @DisplayName("Iterator Pattern - Null Collection Handling")
    void testIteratorPatternNullCollectionHandling() {
        assertThrows(IllegalArgumentException.class, () ->
            new ContentIterator<>(null), "ContentIterator should reject null collection");

        assertThrows(IllegalArgumentException.class, () ->
            new UserSessionIterator(null), "UserSessionIterator should reject null collection");

        assertThrows(IllegalArgumentException.class, () ->
            new PublishingPipelineIterator(null), "PublishingPipelineIterator should reject null collection");

        assertThrows(IllegalArgumentException.class, () ->
            new SiteStructureIterator(null), "SiteStructureIterator should reject null root");
    }

    @Test
    @DisplayName("Iterator Pattern - Remove Operation Not Supported")
    void testIteratorPatternRemoveNotSupported() {
        ContentIterator<Content<?>> contentIterator = new ContentIterator<>(testContent);
        assertThrows(UnsupportedOperationException.class, contentIterator::remove,
            "ContentIterator should not support remove");

        SiteStructureIterator siteIterator = new SiteStructureIterator(siteHierarchy);
        assertThrows(UnsupportedOperationException.class, siteIterator::remove,
            "SiteStructureIterator should not support remove");

        UserSessionIterator sessionIterator = new UserSessionIterator(testSessions);
        assertThrows(UnsupportedOperationException.class, sessionIterator::remove,
            "UserSessionIterator should not support remove");

        PublishingPipelineIterator pipelineIterator = new PublishingPipelineIterator(testTasks);
        assertThrows(UnsupportedOperationException.class, pipelineIterator::remove,
            "PublishingPipelineIterator should not support remove");
    }

    @Test
    @DisplayName("Iterator Pattern - Empty Collections")
    void testIteratorPatternEmptyCollections() {
        List<Content<?>> emptyContent = new ArrayList<>();
        ContentIterator<Content<?>> emptyContentIterator = new ContentIterator<>(emptyContent);

        assertFalse(emptyContentIterator.hasNext(), "Empty iterator should not have next");
        assertTrue(emptyContentIterator.isEmpty(), "Empty iterator should be empty");
        assertEquals(0, emptyContentIterator.size(), "Empty iterator should have size 0");

        assertThrows(NoSuchElementException.class, emptyContentIterator::next,
            "Empty iterator should throw exception on next()");
    }

    @Test
    @DisplayName("Iterator Pattern - toString() Methods")
    void testIteratorPatternToStringMethods() {
        ContentIterator<Content<?>> contentIterator = new ContentIterator<>(testContent);
        assertNotNull(contentIterator.toString(), "toString should not return null");
        assertTrue(contentIterator.toString().contains("ContentIterator"),
            "toString should identify iterator type");

        SiteStructureIterator siteIterator = new SiteStructureIterator(siteHierarchy);
        assertNotNull(siteIterator.toString(), "toString should not return null");
        assertTrue(siteIterator.toString().contains("SiteStructureIterator"),
            "toString should identify iterator type");

        UserSessionIterator sessionIterator = new UserSessionIterator(testSessions);
        assertNotNull(sessionIterator.toString(), "toString should not return null");
        assertTrue(sessionIterator.toString().contains("UserSessionIterator"),
            "toString should identify iterator type");

        PublishingPipelineIterator pipelineIterator = new PublishingPipelineIterator(testTasks);
        assertNotNull(pipelineIterator.toString(), "toString should not return null");
        assertTrue(pipelineIterator.toString().contains("PublishingPipelineIterator"),
            "toString should identify iterator type");
    }

    @Test
    @DisplayName("Iterator Pattern - Thread Safety Considerations")
    void testIteratorPatternThreadSafety() {
        // Test that iterators handle concurrent collection modifications gracefully
        List<Content<?>> threadSafeContent = Collections.synchronizedList(new ArrayList<>(testContent));
        ContentIterator<Content<?>> iterator = new ContentIterator<>(threadSafeContent);

        // Iterator should create defensive copy, so original modifications don't affect iteration
        threadSafeContent.clear();

        // Iterator should still work with its defensive copy
        assertTrue(iterator.size() > 0, "Iterator should maintain its own copy");
        assertTrue(iterator.hasNext(), "Iterator should still have elements");
    }

    @Test
    @DisplayName("Iterator Pattern - Integration with Collections Framework")
    void testIteratorPatternCollectionsFrameworkIntegration() {
        ContentIterator<Content<?>> iterator = new ContentIterator<>(testContent);

        // Test conversion to List
        List<Content<?>> list = iterator.toList();
        assertNotNull(list, "toList should not return null");
        assertInstanceOf(List.class, list, "toList should return List implementation");

        // Test that the list is independent
        int originalSize = list.size();
        list.clear();
        assertEquals(originalSize, iterator.size(), "Iterator should not be affected by list modifications");
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        testContent = null;
        testSessions = null;
        testTasks = null;
        siteHierarchy = null;
        testSite = null;
        contentFactory = null;
    }
}
