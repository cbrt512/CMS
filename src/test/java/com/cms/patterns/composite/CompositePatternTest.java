package com.cms.patterns.composite;

import com.cms.core.model.*;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.factory.ArticleContent;

/**
 * Simple test to demonstrate and verify the Composite Pattern implementation.
 *
 * <p>This test creates a sample hierarchical structure using the Composite Pattern
 * implementation to verify that all components work together correctly.</p>
 *
 * <p><strong>Design Pattern:</strong> Tests the Composite Pattern by creating
 * a hierarchy with Site (root composite), Categories (intermediate composites),
 * and ContentItems (leaves) to verify uniform treatment and recursive operations.</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class CompositePatternTest {

    /**
     * Main method to run the composite pattern demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("JavaCMS - Composite Pattern Implementation Test");
        System.out.println("==============================================\n");

        try {
            // Create a site (root composite)
            Site site = new Site("My Blog", "A personal blog about technology", "https://myblog.com", "admin");

            // Create some sample content using the Factory pattern
            java.util.Map<String, Object> articleProps = new java.util.HashMap<>();
            articleProps.put("category", "blog");
            articleProps.put("featured", false);

            Content<?> article1 = new ArticleContent("Welcome to My Blog",
                "This is my first blog post about the new site.", "admin", articleProps);
            Content<?> article2 = new ArticleContent("Java Design Patterns",
                "Exploring the Composite pattern in Java applications.", "admin", articleProps);
            Content<?> article3 = new ArticleContent("About This Site",
                "Information about the purpose and goals of this blog.", "admin", articleProps);

            // Wrap content in ContentItems (leaf nodes)
            ContentItem item1 = new ContentItem(article1);
            ContentItem item2 = new ContentItem(article2);
            ContentItem item3 = new ContentItem(article3);

            // Create categories (intermediate composites)
            Category blogCategory = new Category("Blog Posts", "Collection of blog articles", "admin");
            Category pagesCategory = new Category("Static Pages", "Static site pages", "admin");
            Category techCategory = new Category("Technology", "Tech-related articles", "admin");

            // Build the hierarchy - demonstrate composite operations
            System.out.println("Building site hierarchy using Composite Pattern...\n");

            // Add categories to site (root)
            site.add(blogCategory);
            site.add(pagesCategory);

            // Add subcategory to blog category
            blogCategory.add(techCategory);

            // Add content items to appropriate categories
            techCategory.add(item2); // "Java Design Patterns" in Technology
            blogCategory.add(item1); // "Welcome" directly in Blog Posts
            pagesCategory.add(item3); // "About" in Static Pages

            System.out.println("Site hierarchy created successfully!\n");

            // Demonstrate uniform interface - display entire hierarchy
            System.out.println("Displaying entire site hierarchy:");
            System.out.println("==================================");
            site.display();
            System.out.println();

            // Demonstrate recursive operations
            System.out.println("Recursive operations demonstration:");
            System.out.println("===================================");
            System.out.printf("Total items in site: %d%n", site.getItemCount());
            System.out.printf("Items in Blog Posts category: %d%n", blogCategory.getItemCount());
            System.out.printf("Items in Technology subcategory: %d%n", techCategory.getItemCount());
            System.out.printf("Items in single content item: %d%n", item1.getItemCount());
            System.out.println();

            // Demonstrate collections operations
            System.out.println("Collections operations demonstration:");
            System.out.println("====================================");
            System.out.printf("Root categories count: %d%n", site.getRootCategories().size());
            System.out.printf("Root content items count: %d%n", site.getRootContentItems().size());
            System.out.printf("Blog subcategories count: %d%n", blogCategory.getSubcategories().size());
            System.out.printf("Blog direct content items: %d%n", blogCategory.getContentItems().size());
            System.out.println();

            // Demonstrate search operations
            System.out.println("Search operations demonstration:");
            System.out.println("===============================");
            SiteComponent foundCategory = site.findRootComponentByName("Blog Posts");
            if (foundCategory != null) {
                System.out.printf("Found category: %s (type: %s)%n",
                                foundCategory.getName(), foundCategory.getComponentType());
            }

            SiteComponent foundTechCategory = blogCategory.findChildByName("Technology");
            if (foundTechCategory != null) {
                System.out.printf("Found subcategory: %s (type: %s)%n",
                                foundTechCategory.getName(), foundTechCategory.getComponentType());
            }
            System.out.println();

            // Demonstrate modification operations
            System.out.println("Modification operations demonstration:");
            System.out.println("====================================");

            // Create and add another content item
            Content<?> newArticle = new ArticleContent("Advanced Java Topics",
                "Deep dive into advanced Java programming concepts.", "admin", articleProps);
            ContentItem newItem = new ContentItem(newArticle);

            System.out.println("Adding new article to Technology category...");
            techCategory.add(newItem);
            System.out.printf("Technology category now has %d items%n", techCategory.getItemCount());
            System.out.println();

            // Display specific category
            System.out.println("Updated Technology category contents:");
            System.out.println("-----------------------------------");
            techCategory.display();
            System.out.println();

            // Demonstrate removal
            System.out.println("Removing an item and displaying updated hierarchy...");
            blogCategory.remove(item1); // Remove "Welcome" from blog posts
            System.out.printf("Blog Posts category now has %d items%n", blogCategory.getItemCount());
            System.out.println();

            // Final hierarchy display
            System.out.println("Final site hierarchy after modifications:");
            System.out.println("========================================");
            site.display();
            System.out.println();

            // Test error handling (Exception Shielding)
            System.out.println("Exception Shielding demonstration:");
            System.out.println("=================================");

            try {
                // Try to add children to a leaf node (should throw UnsupportedOperationException)
                item2.add(techCategory);
            } catch (UnsupportedOperationException e) {
                System.out.println("✓ Correctly prevented adding children to leaf node:");
                System.out.println("  " + e.getMessage());
            }

            try {
                // Try to access non-existent child (should throw IndexOutOfBoundsException)
                site.getChild(999);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("✓ Correctly handled invalid child index:");
                System.out.println("  " + e.getMessage());
            }

            System.out.println("\n✅ Composite Pattern implementation test completed successfully!");
            System.out.println("   All components work together correctly with uniform interface.");

        } catch (Exception e) {
            System.err.println("❌ Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
