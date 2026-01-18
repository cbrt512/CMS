package com.cms.core;

import com.cms.core.model.*;
import com.cms.core.repository.Repository;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.factory.ArticleContent;
import com.cms.patterns.factory.PageContent;
import com.cms.patterns.factory.ImageContent;
import com.cms.patterns.factory.VideoContent;
import com.cms.patterns.iterator.ContentIterator;
import com.cms.patterns.composite.Category;
import com.cms.patterns.composite.ContentItem;
import com.cms.patterns.composite.SiteComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Stream;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit test suite for Generics implementation.
 *
 * <p>
 * This test class validates the complete Generics usage ,
 * testing type safety, generic classes, generic methods, wildcards, and bounded
 * type parameters
 * throughout the CMS application.
 * </p>
 *
 * <p>
 * <strong>Testing Focus:</strong> JUnit Testing - Generics validation
 * - Tests type safety with generic collections and classes
 * - Validates generic repository implementations
 * - Tests bounded wildcards and type parameters
 * - Verifies compile-time type checking and runtime type safety
 * - Tests generic methods and type inference
 * </p>
 *
 * <p>
 * <strong>Generics Implementation Coverage:</strong>
 * - Repository&lt;T, ID&gt; with type-safe operations
 * - ContentIterator&lt;T&gt; with bounded type parameters
 * - Generic collections throughout the application
 * - Type-safe factory methods and content creation
 * - Wildcard usage for flexible API design
 * - Generic utility methods and functional interfaces
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Generics Implementation Tests")
public class GenericsTest {

    private ContentFactory contentFactory;
    private User testUser;
    private Site testSite;

    @BeforeEach
    void setUp() {
        contentFactory = new ContentFactory();
        testUser = new User("testUser", "Test User", "test@example.com", Role.EDITOR);
        testSite = new Site("Test Site", "test-site", testUser);
    }

    /**
     * Tests for generic repository implementations.
     */
    @Nested
    @DisplayName("Generic Repository Tests")
    class GenericRepositoryTests {

        @Test
        @DisplayName("Should provide type-safe repository operations")
        void shouldProvideTypeSafeRepositoryOperations() {
            // Arrange
            Repository<Content, String> contentRepository = new TypeSafeContentRepository<>();
            Repository<User, String> userRepository = new TypeSafeUserRepository<>();

            Content testContent = contentFactory.createContent("ARTICLE", "Test Article", "Content", testUser);
            User testUser2 = new User("user2", "User Two", "user2@example.com", Role.VIEWER);

            // Act & Assert - Type safety at compile time
            contentRepository.save(testContent); // Should accept Content
            userRepository.save(testUser2); // Should accept User

            // The following would not compile due to type safety:
            // contentRepository.save(testUser2); // Compilation error
            // userRepository.save(testContent); // Compilation error

            Content retrievedContent = contentRepository.findById(testContent.getId());
            User retrievedUser = userRepository.findById(testUser2.getUsername());

            assertNotNull(retrievedContent, "Should retrieve content");
            assertNotNull(retrievedUser, "Should retrieve user");
            assertEquals(testContent.getId(), retrievedContent.getId(), "Content should match");
            assertEquals(testUser2.getUsername(), retrievedUser.getUsername(), "User should match");
        }

        @Test
        @DisplayName("Should support generic repository inheritance")
        void shouldSupportGenericRepositoryInheritance() {
            // Arrange
            ExtendedContentRepository<ArticleContent> articleRepository = new ExtendedContentRepository<>();
            ExtendedContentRepository<PageContent> pageRepository = new ExtendedContentRepository<>();

            ArticleContent article = (ArticleContent) contentFactory.createContent("ARTICLE", "Article", "Content",
                    testUser);
            PageContent page = (PageContent) contentFactory.createContent("PAGE", "Page", "Content", testUser);

            // Act
            articleRepository.save(article);
            pageRepository.save(page);

            List<ArticleContent> articles = articleRepository.findByType(ArticleContent.class);
            List<PageContent> pages = pageRepository.findByType(PageContent.class);

            // Assert
            assertEquals(1, articles.size(), "Should find one article");
            assertEquals(1, pages.size(), "Should find one page");
            assertTrue(articles.get(0) instanceof ArticleContent, "Should maintain article type");
            assertTrue(pages.get(0) instanceof PageContent, "Should maintain page type");
        }

        @Test
        @DisplayName("Should handle bounded type parameters in repositories")
        void shouldHandleBoundedTypeParametersInRepositories() {
            // Arrange
            BoundedRepository<ArticleContent> boundedRepo = new BoundedRepository<>();

            ArticleContent article = (ArticleContent) contentFactory.createContent("ARTICLE", "Bounded Article",
                    "Content", testUser);

            // Act
            boundedRepo.save(article);
            List<ArticleContent> foundContent = boundedRepo.findByContentType("ARTICLE");

            // Assert
            assertEquals(1, foundContent.size(), "Should find content by type");
            assertTrue(foundContent.get(0) instanceof ArticleContent, "Should maintain type bounds");

            // Test type bounds - the following would not compile:
            // BoundedRepository<String> invalidRepo = new BoundedRepository<>(); //
            // Compilation error
            // boundedRepo.save("Not a Content"); // Compilation error
        }

        @ParameterizedTest
        @MethodSource("provideContentTypes")
        @DisplayName("Should handle multiple content types with generic repository")
        <T extends Content> void shouldHandleMultipleContentTypesWithGenericRepository(
                Class<T> contentType, String type, String title, String content) {
            // Arrange
            Repository<T, String> typedRepository = new TypeSafeRepository<>();
            T contentInstance = contentType.cast(contentFactory.createContent(type, title, content, testUser));

            // Act
            typedRepository.save(contentInstance);
            T retrieved = typedRepository.findById(contentInstance.getId());

            // Assert
            assertNotNull(retrieved, "Should retrieve typed content");
            assertEquals(contentType, retrieved.getClass(), "Should maintain exact type");
            assertEquals(title, retrieved.getTitle(), "Title should match");
        }

        private static Stream<Arguments> provideContentTypes() {
            return Stream.of(
                    Arguments.of(ArticleContent.class, "ARTICLE", "Test Article", "Article content"),
                    Arguments.of(PageContent.class, "PAGE", "Test Page", "Page content"),
                    Arguments.of(ImageContent.class, "IMAGE", "Test Image", "/uploads/test.jpg"),
                    Arguments.of(VideoContent.class, "VIDEO", "Test Video", "/uploads/test.mp4"));
        }
    }

    /**
     * Tests for generic collections and type safety.
     */
    @Nested
    @DisplayName("Generic Collections Tests")
    class GenericCollectionsTests {

        @Test
        @DisplayName("Should provide type-safe collection operations")
        void shouldProvideTypeSafeCollectionOperations() {
            // Arrange - Type-safe collections
            List<Content> contentList = new ArrayList<>();
            Set<User> userSet = new HashSet<>();
            Map<String, Content> contentMap = new HashMap<>();
            Queue<Content> processingQueue = new LinkedList<>();

            Content article = contentFactory.createContent("ARTICLE", "Article", "Content", testUser);
            Content page = contentFactory.createContent("PAGE", "Page", "Content", testUser);
            User editor = new User("editor", "Editor User", "editor@example.com", Role.EDITOR);

            // Act - Type-safe operations
            contentList.add(article);
            contentList.add(page);
            // contentList.add(editor); // Would not compile - type safety

            userSet.add(testUser);
            userSet.add(editor);
            // userSet.add(article); // Would not compile - type safety

            contentMap.put(article.getId(), article);
            contentMap.put(page.getId(), page);
            // contentMap.put("key", editor); // Would not compile - type safety

            processingQueue.offer(article);
            processingQueue.offer(page);

            // Assert
            assertEquals(2, contentList.size(), "Content list should have 2 items");
            assertEquals(2, userSet.size(), "User set should have 2 items");
            assertEquals(2, contentMap.size(), "Content map should have 2 items");
            assertEquals(2, processingQueue.size(), "Processing queue should have 2 items");

            // Type safety verification
            for (Content content : contentList) {
                assertNotNull(content.getId(), "All items should be Content instances");
            }

            for (User user : userSet) {
                assertNotNull(user.getUsername(), "All items should be User instances");
            }
        }

        @Test
        @DisplayName("Should support generic collection transformations")
        void shouldSupportGenericCollectionTransformations() {
            // Arrange
            List<Content> contentList = Arrays.asList(
                    contentFactory.createContent("ARTICLE", "Java Tutorial", "Learn Java", testUser),
                    contentFactory.createContent("ARTICLE", "Spring Guide", "Spring basics", testUser),
                    contentFactory.createContent("PAGE", "About", "About us", testUser),
                    contentFactory.createContent("IMAGE", "Logo", "/uploads/logo.png", testUser));

            // Act - Generic transformations
            List<String> titles = extractProperty(contentList, Content::getTitle);
            List<String> creators = extractProperty(contentList, Content::getCreatedBy);
            List<ContentStatus> statuses = extractProperty(contentList, Content::getStatus);

            Map<String, List<Content>> groupedByType = groupBy(contentList,
                    content -> content.getClass().getSimpleName().replace("Content", ""));

            List<Content> filteredContent = filter(contentList,
                    content -> content.getTitle().contains("Java"));

            // Assert
            assertEquals(4, titles.size(), "Should extract all titles");
            assertTrue(titles.contains("Java Tutorial"), "Should contain Java Tutorial");
            assertTrue(titles.contains("Spring Guide"), "Should contain Spring Guide");

            assertEquals(4, creators.size(), "Should extract all creators");
            assertTrue(creators.stream().allMatch(creator -> creator.equals(testUser.getUsername())),
                    "All creators should be the test user");

            assertEquals(4, statuses.size(), "Should extract all statuses");
            assertTrue(statuses.stream().allMatch(status -> status == ContentStatus.DRAFT),
                    "All content should be in draft status");

            assertEquals(3, groupedByType.size(), "Should group into 3 types");
            assertEquals(2, groupedByType.get("Article").size(), "Should have 2 articles");
            assertEquals(1, groupedByType.get("Page").size(), "Should have 1 page");
            assertEquals(1, groupedByType.get("Image").size(), "Should have 1 image");

            assertEquals(1, filteredContent.size(), "Should find 1 Java-related content");
            assertEquals("Java Tutorial", filteredContent.get(0).getTitle(), "Should find Java Tutorial");
        }

        @Test
        @DisplayName("Should handle nested generic collections")
        void shouldHandleNestedGenericCollections() {
            // Arrange - Nested generics
            Map<String, List<Content>> categoryContent = new HashMap<>();
            Map<Role, Set<User>> roleUsers = new HashMap<>();
            List<Map<String, Object>> complexStructure = new ArrayList<>();

            // Act - Populate nested structures
            categoryContent.put("articles", Arrays.asList(
                    contentFactory.createContent("ARTICLE", "Article 1", "Content", testUser),
                    contentFactory.createContent("ARTICLE", "Article 2", "Content", testUser)));
            categoryContent.put("pages", Arrays.asList(
                    contentFactory.createContent("PAGE", "Page 1", "Content", testUser)));

            roleUsers.put(Role.ADMIN, Set.of(
                    new User("admin1", "Admin One", "admin1@example.com", Role.ADMIN),
                    new User("admin2", "Admin Two", "admin2@example.com", Role.ADMIN)));
            roleUsers.put(Role.EDITOR, Set.of(testUser));

            Map<String, Object> contentInfo = new HashMap<>();
            contentInfo.put("type", "ARTICLE");
            contentInfo.put("count", 2);
            contentInfo.put("published", false);
            complexStructure.add(contentInfo);

            // Assert
            assertEquals(2, categoryContent.get("articles").size(), "Should have 2 articles");
            assertEquals(1, categoryContent.get("pages").size(), "Should have 1 page");

            assertEquals(2, roleUsers.get(Role.ADMIN).size(), "Should have 2 admins");
            assertEquals(1, roleUsers.get(Role.EDITOR).size(), "Should have 1 editor");

            assertEquals(1, complexStructure.size(), "Should have 1 complex entry");
            assertEquals("ARTICLE", complexStructure.get(0).get("type"), "Should contain article type");
            assertEquals(2, complexStructure.get(0).get("count"), "Should contain count");
        }

        @Test
        @DisplayName("Should support concurrent generic collections")
        void shouldSupportConcurrentGenericCollections() throws InterruptedException {
            // Arrange
            Map<String, Content> concurrentContentMap = new ConcurrentHashMap<>();
            Queue<Content> concurrentQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

            int threadCount = 10;
            int contentPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            // Act - Concurrent operations
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < contentPerThread; j++) {
                        Content content = contentFactory.createContent("ARTICLE",
                                "Thread " + threadId + " Article " + j, "Content", testUser);

                        concurrentContentMap.put(content.getId(), content);
                        concurrentQueue.offer(content);
                    }
                });
                threads[i].start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }

            // Assert
            assertEquals(threadCount * contentPerThread, concurrentContentMap.size(),
                    "Concurrent map should contain all content");
            assertEquals(threadCount * contentPerThread, concurrentQueue.size(),
                    "Concurrent queue should contain all content");

            // Verify type safety after concurrent operations
            for (Content content : concurrentContentMap.values()) {
                assertNotNull(content.getId(), "All values should be valid Content");
                assertTrue(content instanceof Content, "Type safety should be maintained");
            }
        }

        // Generic utility methods for testing
        private <T, R> List<R> extractProperty(List<T> list, Function<T, R> extractor) {
            List<R> results = new ArrayList<>();
            for (T item : list) {
                results.add(extractor.apply(item));
            }
            return results;
        }

        private <T, K> Map<K, List<T>> groupBy(List<T> list, Function<T, K> classifier) {
            Map<K, List<T>> groups = new HashMap<>();
            for (T item : list) {
                K key = classifier.apply(item);
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }
            return groups;
        }

        private <T> List<T> filter(List<T> list, Predicate<T> predicate) {
            List<T> filtered = new ArrayList<>();
            for (T item : list) {
                if (predicate.test(item)) {
                    filtered.add(item);
                }
            }
            return filtered;
        }
    }

    /**
     * Tests for generic iterators and type-safe iteration.
     */
    @Nested
    @DisplayName("Generic Iterator Tests")
    class GenericIteratorTests {

        @Test
        @DisplayName("Should provide type-safe content iteration")
        void shouldProvideTypeSafeContentIteration() {
            // Arrange
            List<Content> contentList = Arrays.asList(
                    contentFactory.createContent("ARTICLE", "Article 1", "Content", testUser),
                    contentFactory.createContent("PAGE", "Page 1", "Content", testUser),
                    contentFactory.createContent("IMAGE", "Image 1", "/uploads/img1.jpg", testUser));

            // Act - Type-safe iteration
            ContentIterator<Content> iterator = ContentIterator.all(contentList);
            ContentIterator<ArticleContent> articleIterator = ContentIterator.byType(
                    contentList, ArticleContent.class);

            // Assert - General content iteration
            List<Content> iteratedContent = new ArrayList<>();
            while (iterator.hasNext()) {
                Content content = iterator.next(); // Type-safe - guaranteed to be Content
                iteratedContent.add(content);
                assertNotNull(content.getId(), "Should be valid Content instance");
            }
            assertEquals(3, iteratedContent.size(), "Should iterate over all content");

            // Assert - Type-specific iteration
            List<ArticleContent> iteratedArticles = new ArrayList<>();
            while (articleIterator.hasNext()) {
                ArticleContent article = articleIterator.next(); // Type-safe - guaranteed to be ArticleContent
                iteratedArticles.add(article);
                assertTrue(article instanceof ArticleContent, "Should be ArticleContent instance");
                assertNotNull(article.getWordCount(), "Should have article-specific methods");
            }
            assertEquals(1, iteratedArticles.size(), "Should iterate over articles only");
        }

        @Test
        @DisplayName("Should support bounded type parameters in iterators")
        void shouldSupportBoundedTypeParametersInIterators() {
            // Arrange
            List<Content> contentList = Arrays.asList(
                    contentFactory.createContent("ARTICLE", "Published Article", "Content", testUser),
                    contentFactory.createContent("PAGE", "Draft Page", "Content", testUser));
            contentList.get(0).publish(); // Publish the article

            // Act - Bounded iterator usage
            BoundedContentIterator<? extends Content> boundedIterator = new BoundedContentIterator<>(contentList);

            TypedContentIterator<ArticleContent> typedIterator = new TypedContentIterator<>(contentList,
                    ArticleContent.class);

            // Assert
            List<Content> boundedContent = new ArrayList<>();
            while (boundedIterator.hasNext()) {
                Content content = boundedIterator.next();
                boundedContent.add(content);
                assertTrue(content instanceof Content, "Should respect upper bound");
            }
            assertEquals(2, boundedContent.size(), "Should iterate over bounded content");

            List<ArticleContent> typedContent = new ArrayList<>();
            while (typedIterator.hasNext()) {
                ArticleContent article = typedIterator.next();
                typedContent.add(article);
                assertTrue(article instanceof ArticleContent, "Should maintain type specificity");
            }
            assertEquals(1, typedContent.size(), "Should iterate over typed content only");
        }

        @Test
        @DisplayName("Should handle wildcard types in generic methods")
        void shouldHandleWildcardTypesInGenericMethods() {
            // Arrange
            List<ArticleContent> articles = Arrays.asList(
                    (ArticleContent) contentFactory.createContent("ARTICLE", "Article 1", "Content", testUser),
                    (ArticleContent) contentFactory.createContent("ARTICLE", "Article 2", "Content", testUser));

            List<PageContent> pages = Arrays.asList(
                    (PageContent) contentFactory.createContent("PAGE", "Page 1", "Content", testUser));

            // Act & Assert - Upper bounded wildcards
            int articleCount = countContent(articles); // List<ArticleContent>
            int pageCount = countContent(pages); // List<PageContent>

            assertEquals(2, articleCount, "Should count articles");
            assertEquals(1, pageCount, "Should count pages");

            // Lower bounded wildcards
            List<Content> allContent = new ArrayList<>();
            addAllContent(allContent, articles); // List<? super ArticleContent>
            addAllContent(allContent, pages); // List<? super PageContent>

            assertEquals(3, allContent.size(), "Should add all content types");

            // Unbounded wildcards
            printContentInfo(articles);
            printContentInfo(pages);
        }

        // Generic methods with wildcards for testing
        private int countContent(List<? extends Content> contentList) {
            return contentList.size();
        }

        private void addAllContent(List<? super Content> destination, List<? extends Content> source) {
            destination.addAll(source);
        }

        private void printContentInfo(List<?> contentList) {
            assertNotNull(contentList, "Content list should not be null");
            assertTrue(contentList.size() >= 0, "Content list should have valid size");
        }
    }

    /**
     * Tests for generic composite pattern implementations.
     */
    @Nested
    @DisplayName("Generic Composite Pattern Tests")
    class GenericCompositePatternTests {

        @Test
        @DisplayName("Should support type-safe composite hierarchy")
        void shouldSupportTypeSafeCompositeHierarchy() {
            // Arrange
            GenericCategory<Content> rootCategory = new GenericCategory<>("Root", "Root category");
            GenericCategory<ArticleContent> articleCategory = new GenericCategory<>("Articles", "Article category");
            GenericCategory<PageContent> pageCategory = new GenericCategory<>("Pages", "Page category");

            ArticleContent article = (ArticleContent) contentFactory.createContent("ARTICLE", "Article", "Content",
                    testUser);
            PageContent page = (PageContent) contentFactory.createContent("PAGE", "Page", "Content", testUser);

            GenericContentItem<ArticleContent> articleItem = new GenericContentItem<>(article);
            GenericContentItem<PageContent> pageItem = new GenericContentItem<>(page);

            // Act
            articleCategory.addChild(articleItem);
            pageCategory.addChild(pageItem);

            // The following would not compile due to type safety:
            // articleCategory.addChild(pageItem); // Type mismatch
            // pageCategory.addChild(articleItem); // Type mismatch

            // Assert
            assertEquals(1, articleCategory.getChildCount(), "Article category should have 1 child");
            assertEquals(1, pageCategory.getChildCount(), "Page category should have 1 child");

            List<GenericContentItem<ArticleContent>> articleItems = articleCategory
                    .getTypedChildren(GenericContentItem.class);
            List<GenericContentItem<PageContent>> pageItems = pageCategory.getTypedChildren(GenericContentItem.class);

            assertEquals(1, articleItems.size(), "Should have 1 article item");
            assertEquals(1, pageItems.size(), "Should have 1 page item");

            assertTrue(articleItems.get(0).getContent() instanceof ArticleContent, "Should maintain article type");
            assertTrue(pageItems.get(0).getContent() instanceof PageContent, "Should maintain page type");
        }

        @Test
        @DisplayName("Should handle generic site component operations")
        void shouldHandleGenericSiteComponentOperations() {
            // Arrange
            TypedSite<Content> contentSite = new TypedSite<>("Content Site", "content-site", testUser);
            List<Content> siteContent = Arrays.asList(
                    contentFactory.createContent("ARTICLE", "Site Article", "Content", testUser),
                    contentFactory.createContent("PAGE", "Site Page", "Content", testUser),
                    contentFactory.createContent("IMAGE", "Site Image", "/uploads/site.jpg", testUser));

            // Act
            for (Content content : siteContent) {
                contentSite.addTypedContent(content);
            }

            List<Content> retrievedContent = contentSite.getTypedContent();
            List<ArticleContent> articles = contentSite.getContentByType(ArticleContent.class);
            List<PageContent> pages = contentSite.getContentByType(PageContent.class);

            // Assert
            assertEquals(3, retrievedContent.size(), "Should contain all content");
            assertEquals(1, articles.size(), "Should have 1 article");
            assertEquals(1, pages.size(), "Should have 1 page");

            assertTrue(articles.get(0) instanceof ArticleContent, "Should maintain article type");
            assertTrue(pages.get(0) instanceof PageContent, "Should maintain page type");
        }
    }

    /**
     * Tests for generic method implementations and type inference.
     */
    @Nested
    @DisplayName("Generic Methods and Type Inference Tests")
    class GenericMethodsTests {

        @Test
        @DisplayName("Should support generic factory methods with type inference")
        void shouldSupportGenericFactoryMethodsWithTypeInference() {
            // Act - Type inference from context
            ArticleContent article = createTypedContent(ArticleContent.class, "ARTICLE", "Article", "Content",
                    testUser);
            PageContent page = createTypedContent(PageContent.class, "PAGE", "Page", "Content", testUser);
            ImageContent image = createTypedContent(ImageContent.class, "IMAGE", "Image", "/uploads/img.jpg", testUser);

            // Assert
            assertTrue(article instanceof ArticleContent, "Should create ArticleContent");
            assertTrue(page instanceof PageContent, "Should create PageContent");
            assertTrue(image instanceof ImageContent, "Should create ImageContent");

            assertEquals("Article", article.getTitle(), "Article should have correct title");
            assertEquals("Page", page.getTitle(), "Page should have correct title");
            assertEquals("Image", image.getTitle(), "Image should have correct title");

            // Test type safety
            assertNotNull(article.getWordCount(), "Article should have word count");
            assertNotNull(page.getSlug(), "Page should have slug");
            assertNotNull(image.getMimeType(), "Image should have MIME type");
        }

        @Test
        @DisplayName("Should handle generic utility methods")
        void shouldHandleGenericUtilityMethods() {
            // Arrange
            List<Content> contentList = Arrays.asList(
                    contentFactory.createContent("ARTICLE", "Article 1", "Content", testUser),
                    contentFactory.createContent("PAGE", "Page 1", "Content", testUser),
                    contentFactory.createContent("IMAGE", "Image 1", "/uploads/img1.jpg", testUser));

            List<User> userList = Arrays.asList(
                    testUser,
                    new User("editor", "Editor User", "editor@example.com", Role.EDITOR),
                    new User("viewer", "Viewer User", "viewer@example.com", Role.VIEWER));

            // Act - Generic utility methods
            Content firstContent = getFirstElement(contentList);
            User firstUser = getFirstElement(userList);

            List<Content> reversedContent = reverseList(contentList);
            List<User> reversedUsers = reverseList(userList);

            boolean hasArticle = containsType(contentList, ArticleContent.class);
            boolean hasAdmin = containsType(userList, User.class);

            // Assert
            assertNotNull(firstContent, "Should get first content");
            assertNotNull(firstUser, "Should get first user");
            assertTrue(firstContent instanceof Content, "Should maintain content type");
            assertTrue(firstUser instanceof User, "Should maintain user type");

            assertEquals(3, reversedContent.size(), "Reversed content should have same size");
            assertEquals(3, reversedUsers.size(), "Reversed users should have same size");
            assertEquals("Image 1", reversedContent.get(0).getTitle(), "Should reverse content order");
            assertEquals("viewer", reversedUsers.get(0).getUsername(), "Should reverse user order");

            assertTrue(hasArticle, "Should find article content");
            assertTrue(hasAdmin, "Should find user type");
        }

        @Test
        @DisplayName("Should handle multiple type parameters")
        void shouldHandleMultipleTypeParameters() {
            // Arrange
            Pair<String, Content> contentPair = new Pair<>("article-1",
                    contentFactory.createContent("ARTICLE", "Paired Article", "Content", testUser));

            Pair<Role, User> userPair = new Pair<>(Role.EDITOR, testUser);

            Triple<String, ContentStatus, Content> contentTriple = new Triple<>(
                    "published-article",
                    ContentStatus.PUBLISHED,
                    contentFactory.createContent("ARTICLE", "Published Article", "Content", testUser));

            // Act
            String contentKey = contentPair.getFirst();
            Content contentValue = contentPair.getSecond();

            Role userRole = userPair.getFirst();
            User userValue = userPair.getSecond();

            String tripleKey = contentTriple.getFirst();
            ContentStatus tripleStatus = contentTriple.getSecond();
            Content tripleContent = contentTriple.getThird();

            // Assert
            assertEquals("article-1", contentKey, "Should maintain first type");
            assertTrue(contentValue instanceof Content, "Should maintain second type");

            assertEquals(Role.EDITOR, userRole, "Should maintain role type");
            assertTrue(userValue instanceof User, "Should maintain user type");

            assertEquals("published-article", tripleKey, "Should maintain first type in triple");
            assertEquals(ContentStatus.PUBLISHED, tripleStatus, "Should maintain second type in triple");
            assertTrue(tripleContent instanceof Content, "Should maintain third type in triple");
        }

        // Generic utility methods for testing
        @SuppressWarnings("unchecked")
        private <T extends Content> T createTypedContent(Class<T> type, String contentType,
                String title, String content, User creator) {
            Content created = contentFactory.createContent(contentType, title, content, creator);
            return type.cast(created);
        }

        private <T> T getFirstElement(List<T> list) {
            return list.isEmpty() ? null : list.get(0);
        }

        private <T> List<T> reverseList(List<T> list) {
            List<T> reversed = new ArrayList<>(list);
            Collections.reverse(reversed);
            return reversed;
        }

        private <T> boolean containsType(List<?> list, Class<T> type) {
            return list.stream().anyMatch(type::isInstance);
        }
    }

    // Helper classes for testing generics

    /**
     * Generic repository with type safety.
     */
    private static class TypeSafeRepository<T, ID> implements Repository<T, ID> {
        private final Map<ID, T> storage = new HashMap<>();

        @Override
        public T save(T entity) {
            // Assuming ID extraction logic based on entity type
            @SuppressWarnings("unchecked")
            ID id = (ID) extractId(entity);
            storage.put(id, entity);
            return entity;
        }

        @Override
        public T findById(ID id) {
            return storage.get(id);
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public T update(T entity) {
            return save(entity);
        }

        @Override
        public void delete(ID id) {
            storage.remove(id);
        }

        @Override
        public boolean existsById(ID id) {
            return storage.containsKey(id);
        }

        @Override
        public long count() {
            return storage.size();
        }

        private Object extractId(T entity) {
            if (entity instanceof Content) {
                return ((Content) entity).getId();
            } else if (entity instanceof User) {
                return ((User) entity).getUsername();
            }
            return entity.toString();
        }
    }

    /**
     * Content-specific repository implementation.
     */
    private static class TypeSafeContentRepository<T extends Content> extends TypeSafeRepository<T, String> {
        // Specialized content operations
    }

    /**
     * User-specific repository implementation.
     */
    private static class TypeSafeUserRepository<T extends User> extends TypeSafeRepository<T, String> {
        // Specialized user operations
    }

    /**
     * Extended repository with additional type-specific methods.
     */
    private static class ExtendedContentRepository<T extends Content> extends TypeSafeRepository<T, String> {
        public List<T> findByType(Class<T> type) {
            return findAll().stream()
                    .filter(type::isInstance)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    /**
     * Bounded repository that only accepts Content and its subtypes.
     */
    private static class BoundedRepository<T extends Content> {
        private final List<T> storage = new ArrayList<>();

        public void save(T content) {
            storage.add(content);
        }

        public List<T> findByContentType(String contentType) {
            return storage.stream()
                    .filter(content -> content.getClass().getSimpleName().toUpperCase().contains(contentType))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        public List<T> findAll() {
            return new ArrayList<>(storage);
        }
    }

    /**
     * Bounded iterator for content types.
     */
    private static class BoundedContentIterator<T extends Content> implements Iterator<T> {
        private final Iterator<T> iterator;

        @SuppressWarnings("unchecked")
        public BoundedContentIterator(List<? extends Content> content) {
            this.iterator = (Iterator<T>) content.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }
    }

    /**
     * Typed iterator for specific content types.
     */
    private static class TypedContentIterator<T extends Content> implements Iterator<T> {
        private final List<T> typedContent;
        private int index = 0;

        @SuppressWarnings("unchecked")
        public TypedContentIterator(List<? extends Content> content, Class<T> type) {
            this.typedContent = content.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        @Override
        public boolean hasNext() {
            return index < typedContent.size();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return typedContent.get(index++);
        }
    }

    /**
     * Generic category for type-safe composite hierarchy.
     */
    private static class GenericCategory<T extends Content> {
        private final String name;
        private final String description;
        private final List<Object> children = new ArrayList<>(); // Mixed types for flexibility

        public GenericCategory(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public void addChild(GenericContentItem<T> child) {
            children.add(child);
        }

        public int getChildCount() {
            return children.size();
        }

        @SuppressWarnings("unchecked")
        public <C> List<C> getTypedChildren(Class<C> type) {
            return children.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Generic content item wrapper.
     */
    private static class GenericContentItem<T extends Content> {
        private final T content;

        public GenericContentItem(T content) {
            this.content = content;
        }

        public T getContent() {
            return content;
        }
    }

    /**
     * Typed site implementation.
     */
    private static class TypedSite<T extends Content> {
        private final String name;
        private final String slug;
        private final User owner;
        private final List<T> content = new ArrayList<>();

        public TypedSite(String name, String slug, User owner) {
            this.name = name;
            this.slug = slug;
            this.owner = owner;
        }

        public void addTypedContent(T content) {
            this.content.add(content);
        }

        public List<T> getTypedContent() {
            return new ArrayList<>(content);
        }

        @SuppressWarnings("unchecked")
        public <C extends T> List<C> getContentByType(Class<C> type) {
            return content.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }

        public User getOwner() {
            return owner;
        }
    }

    /**
     * Generic pair class for multiple type parameters.
     */
    private static class Pair<T, U> {
        private final T first;
        private final U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public T getFirst() {
            return first;
        }

        public U getSecond() {
            return second;
        }
    }

    /**
     * Generic triple class for multiple type parameters.
     */
    private static class Triple<T, U, V> {
        private final T first;
        private final U second;
        private final V third;

        public Triple(T first, U second, V third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public T getFirst() {
            return first;
        }

        public U getSecond() {
            return second;
        }

        public V getThird() {
            return third;
        }
    }
}
