package com.developer.tool;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.javalin.Javalin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Main {
    private static final SnippetDAO snippetDAO = new SnippetDAO();
    private static final UserDAO userDAO = new UserDAO();
  
    

    public static void main(String[] args) {
        // Initialize database
        DatabaseManager.initializeDatabase();
        
        
        AuthController authController = new AuthController(userDAO);
       
        
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        Javalin app = Javalin.create(
        		config -> {
        			 config.jetty.modifyServletContextHandler(handler -> {
        		            handler.setSessionHandler(new org.eclipse.jetty.server.session.SessionHandler());
        		        });
        		})
        		.start(port);

        app.before(ctx -> ctx.contentType("text/html"));

       
        
     // Auth check for protected routes
        app.before("/snippets*", ctx -> {
            if (ctx.sessionAttribute("userId") == null && 
                !ctx.path().equals("/login") && !ctx.path().equals("/register")) {
                ctx.redirect("/login");
            }
        });
        
        // Serve main page
        app.get("/login", authController::showLogin);
        app.post("/login", authController::login); // Login handler
        app.get("/register", authController::registerUserForm);  // Register page
        app.post("/register", authController::registerUser); // Register handler

        // Logout
        app.get("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.redirect("/login");
        });
        
        app.get("/", ctx -> {
            String userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.redirect("/login");
                return;
            }
            User user = userDAO.findByUserId(userId);
            ctx.result(getHtmlPage(user.username()));
        });
        // Get all snippets (for initial load)
        app.get("/snippets", ctx -> {
        	String userId = ctx.sessionAttribute("userId");
        	List<Snippet> snippets = snippetDAO.findByUserId(userId);
        	ctx.result(renderSnippetsHtml(snippets));	
        });
        
        // Create snippet
        app.post("/snippets", ctx -> {
        	String userId = ctx.sessionAttribute("userId");
            Snippet snippet = new Snippet();
            snippet.setId(UUID.randomUUID().toString());
            snippet.setCode(ctx.formParam("code"));
            snippet.setLanguage(ctx.formParam("language"));
            snippet.setDescription(ctx.formParam("description"));
            
            String tagsParam = ctx.formParam("tags");
            if (tagsParam != null && !tagsParam.isEmpty()) {
                snippet.setTags(new ArrayList<>(Arrays.asList(tagsParam.split(",\\s*"))));
            } else {
                snippet.setTags(new ArrayList<>());
            }
            
            snippetDAO.create(snippet,userId);
            
            // Return updated list + reset form using hx-swap-oob
            ctx.result(renderSnippetsWithFormReset(snippetDAO.findByUserId(userId)));
        });

       
        
        app.get("/snippets/search", ctx -> {
        	String userId = ctx.sessionAttribute("userId");
            String query = ctx.queryParam("search");
            if (query == null || query.isEmpty()) {
                ctx.result(renderSnippetsHtml());
            } else {
                ctx.result(renderSnippetsHtml(snippetDAO.search(query,userId)));
            }
        });

        // Cancel edit (return default form)
        app.get("/cancel", ctx -> ctx.result(getDefaultFormHtml()));

        // Get single snippet for editing
        app.get("/snippets/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Snippet snippet = snippetDAO.findById(id);
            
            if (snippet == null) {
                ctx.status(404).result("Snippet not found");
                return;
            }
            
            ctx.result(renderEditForm(snippet));
        });

        // Update snippet
        app.put("/snippets/{id}", ctx -> {
        	String userId = ctx.sessionAttribute("userId");
            String id = ctx.pathParam("id");
            Snippet snippet = snippetDAO.findById(id);
            
            if (snippet == null) {
                ctx.status(404).result("Snippet not found");
                return;
            }
            
            snippet.setCode(ctx.formParam("code"));
            snippet.setLanguage(ctx.formParam("language"));
            snippet.setDescription(ctx.formParam("description"));
            
            String tagsParam = ctx.formParam("tags");
            if (tagsParam != null && !tagsParam.isEmpty()) {
                snippet.setTags(new ArrayList<>(Arrays.asList(tagsParam.split(",\\s*"))));
            } else {
                snippet.setTags(new ArrayList<>());
            }
            
            snippetDAO.update(snippet,userId);
            
            // Return updated list + reset form using hx-swap-oob
            ctx.result(renderSnippetsWithFormReset(snippetDAO.findByUserId(userId)));
        });

        // Delete snippet
        app.delete("/snippets/{id}", ctx -> {
        	String userId = ctx.sessionAttribute("userId");
            String id = ctx.pathParam("id");
            snippetDAO.delete(id,userId);
            ctx.status(200).result("");
        });

        // Search snippets
       
    }

    private static String getHtmlPage(String username) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <title>CodeCache</title>
                <script src="https://unpkg.com/htmx.org@2.0.8"></script>
                <link href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-okaidia.css" rel="stylesheet" />
                <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-java.min.js"></script>
                <style>
                    body{font-family:Arial,sans-serif;margin:30px}
                    textarea{width:400px;height:100px}
                    input,textarea{margin-bottom:10px;padding:5px}
                    button{margin-right:10px;padding:5px 10px;cursor:pointer}
                    .snippet{background:#fff;border:1px solid #ddd;border-radius:8px;padding:15px;margin-bottom:20px;box-shadow:0 2px 5px rgba(0,0,0,.05);color:#222;max-width:720px;width:100%%;}
                    .code-block{position:relative;margin-top:10px;display:inline-block;max-width:720px;width:100%%;overflow:visible}
                    pre{background:#1e1e1e;color:#f8f8f2;border-radius:6px;padding:12px 14px;overflow-x:auto;max-width:720px;min-width:400px;margin:0;white-space:pre;font-family:"Fira Code","Courier New",monospace}
                    .copy-btn{position:absolute;top:8px;right:8px;background:rgba(60,60,60,.9);color:#fff;border:0;padding:4px 8px;border-radius:4px;font-size:12px;cursor:pointer;box-shadow:0 1px 3px rgba(0,0,0,.4);opacity:.95;z-index:20;transition:background .2s}
                    .copy-btn:hover{background:rgba(0,122,204,.95)}
                    .button-group{margin-top:10px;padding:5px 0}
                </style>
            </head>
            <body>
               
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                 <h1>CodeCache - Snippet Manager</h1>
		            <div>
		                <span style="margin-right: 15px;">Hi, %s</span>
		                <a href="/logout" style="padding: 8px 16px; background: #dc3545; color: white; text-decoration: none; border-radius: 4px;">Logout</a>
		            </div>
		        </div>
                
                <div id="snippet-form">
                    %s
                </div>

                <h2>Search Snippets</h2>
				     <div id="search-snips">
				     	<input type="text" name="search"
					       hx-get="/snippets/search"
					       hx-trigger="keyup changed delay:300ms"
					       hx-target="#snippets"
        				   hx-indicator="#loading"
					       placeholder="Search snippets..." />
				     </div>
                      
                 <span id="loading" class="htmx-indicator">Searching...</span>
                 <h2>Snippets List</h2>
                <div id="snippets" hx-get="/snippets" hx-trigger="load"></div>

                <script>
                    function copyCode(button){
                        const code = button.nextElementSibling.innerText;
                        navigator.clipboard.writeText(code);
                        button.textContent = "Copied!";
                        setTimeout(()=>button.textContent="Copy",1500);
                    }
                    document.body.addEventListener('htmx:afterSwap', (e) => {
                        if (e.target.id === 'snippets' || e.target.closest('.snippet')) {
                            Prism.highlightAll();
                        }
                    });
                </script>
            </body>
            </html>
        """.formatted(username,getDefaultFormHtml());
    }

    private static String getDefaultFormHtml() {
        return """
            <form hx-post="/snippets" hx-target="#snippets" hx-swap="innerHTML">
                <textarea name="code" placeholder="Code" required></textarea><br />
                <input name="language" placeholder="Language (e.g. java)" required /><br />
                <input name="description" placeholder="Description" /><br />
                <input name="tags" placeholder="Tags (comma separated)" /><br />
                <button type="submit">Add Snippet</button>
                <button type="button" hx-get="/cancel" hx-target="#snippet-form" hx-swap="innerHTML">Cancel</button>
            </form>
        """;
    }

    private static String renderEditForm(Snippet snippet) {
        return """
            <form hx-put="/snippets/%s" hx-target="#snippets" hx-swap="innerHTML">
                <textarea name="code" required>%s</textarea><br />
                <input name="language" value="%s" required /><br />
                <input name="description" value="%s" /><br />
                <input name="tags" value="%s" /><br />
                <button type="submit">Update Snippet</button>
                <button type="button" hx-get="/cancel" hx-target="#snippet-form" hx-swap="innerHTML">Cancel</button>
            </form>
        """.formatted(
            snippet.getId(),
            escapeHtml(snippet.getCode()),
            escapeHtml(snippet.getLanguage()),
            escapeHtml(snippet.getDescription() != null ? snippet.getDescription() : ""),
            String.join(", ", snippet.getTags())
        );
    }

    private static String renderSnippetsWithFormReset(List<Snippet> snippets) {
        // Use hx-swap-oob to update both snippets list AND reset form
        return """
            <div id="snippets">
                %s
            </div>
            <div id="snippet-form" hx-swap-oob="true">
                %s
            </div>
        """.formatted(renderSnippetsHtml(snippets), getDefaultFormHtml());
    }

    private static String renderSnippetsHtml() {
        return renderSnippetsHtml(snippetDAO.findAll());
    }

    private static String renderSnippetsHtml(List<Snippet> snippets) {
        if (snippets.isEmpty()) {
            return "<p>No snippets yet. Add your first one above!</p>";
        }
        
        StringBuilder html = new StringBuilder();
        for (Snippet s : snippets) {
            String languageRaw = s.getLanguage() != null ? s.getLanguage() : "text";
            String language = escapeHtml(languageRaw.toLowerCase());
            
            List<String> tags = s.getTags() != null ? s.getTags() : Collections.emptyList();
            String tagsJoined = String.join(", ", tags);
            
            html.append("<div class='snippet'>")
                .append("<div><strong>Description:</strong> ")
                .append(escapeHtml(s.getDescription() != null ? s.getDescription() : ""))
                .append("</div>")
                .append("<div><strong>Language:</strong> ")
                .append(escapeHtml(languageRaw))
                .append("</div>")
                .append("<div><strong>Tags:</strong> ")
                .append(escapeHtml(tagsJoined))
                .append("</div>")
                .append("<div class='code-block'>")
                .append("<button class='copy-btn' onclick='copyCode(this)'>Copy</button>")
                .append("<pre><code class='language-")
                .append(language)
                .append("'>")
                .append(escapeHtml(s.getCode() != null ? s.getCode() : ""))
                .append("</code></pre>")
                .append("</div>")
                .append("<div class='button-group'>")
                .append("<button hx-get='/snippets/")
                .append(s.getId())
                .append("' hx-target='#snippet-form' hx-swap='innerHTML'>Edit</button>")
                .append("<button hx-delete='/snippets/")
                .append(s.getId())
                .append("' hx-target='closest .snippet' hx-swap='outerHTML'>Delete</button>")
                .append("</div>")
                .append("</div>");
        }
        return html.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class Snippet {

	private String id;
	private String userId;
	private String title;
	private String code;
	private String language;
	private String description;
	private List<String> tags;
	private LocalDateTime createdAt;

}
