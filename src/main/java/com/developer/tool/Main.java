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

	static List<Snippet> snippets = new ArrayList<>();

	public static void main(String[] args) {

		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));

		Javalin app = Javalin.create().start(port);

		app.before(ctx -> {
			ctx.contentType("text/html");
		});

		// Serve the main page
		app.get("/", ctx -> {
//			ctx.contentType("text/html");
			ctx.result(getHtmlPage());
		});

		// Add snippet (from form submission via htmx)
		app.post("/snippets", ctx -> {

			String code = ctx.formParam("code");
			String description = ctx.formParam("description");

			if (code == null || code.trim().isEmpty()) {
				ctx.status(400).result("Code cannot be empty");
				return;
			}

			if (code.length() > 10000) { // 50KB limit
				ctx.status(400).result("Code too large");
				return;
			}

			if (snippets.size() >= 100) { // Max 500 snippets
				ctx.status(429).result("Storage limit reached");
				return;
			}

			Snippet snippet = new Snippet();
			snippet.setId(UUID.randomUUID().toString());

			snippet.setCode(code);
			snippet.setLanguage(ctx.formParam("language"));
			snippet.setDescription(description);
			String tagsParam = ctx.formParam("tags");
			if (tagsParam != null && !tagsParam.isEmpty()) {
				snippet.setTags(new ArrayList<>(Arrays.asList(tagsParam.split(",\\s*"))));
			} else {
				snippet.setTags(new ArrayList<>());
			}
			snippets.add(snippet);

			// Respond with updated snippet list HTML fragment for htmx swap
			ctx.result(renderSnippetsHtml(snippets));
		});

		// Return snippet list HTML for initial load or refresh
		app.get("/snippets", ctx -> {
			ctx.result(renderSnippetsHtml(snippets));
		});

		app.get("/snippets/search", ctx -> {
			String query = ctx.queryParam("search");
			List<Snippet> filtered = snippets.stream()
					.filter(s -> query == null || query.isEmpty()
							|| s.getDescription().toLowerCase().contains(query.toLowerCase())
							|| s.getCode().toLowerCase().contains(query.toLowerCase()))
					.toList();
			ctx.result(renderSnippetsHtml(filtered));

		});

		app.delete("/snippets/{id}", ctx -> {
			String id = ctx.pathParam("id");
			snippets.removeIf(s -> s.getId().equals(id));
			ctx.status(200);
			ctx.result("");
		});

	}

	private static String getHtmlPage() {
		return """
				<!DOCTYPE html>
				<html lang="en">
				<head>
				    <meta charset="UTF-8" />
				    <title>CodeCache</title>
				    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.8/dist/htmx.min.js"></script>
				    <link href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism.css" rel="stylesheet" />
				    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js"></script>
				    <link href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-okaidia.css" rel="stylesheet" />

				    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-java.min.js"></script>
				    <!-- Add other Prism language components as needed -->

				    <style>
				    	body{font-family:Arial,sans-serif;margin:30px}
				    	textarea{width:400px;height:100px}
				    	input,textarea{margin-bottom:10px;padding:5px}
				    	.snippet{background:#fff;border:1px solid #ddd;border-radius:8px;padding:15px;margin-bottom:20px;box-shadow:0 2px 5px rgba(0,0,0,.05);color:#222;max-width:720px;width:100%;}
				    	.code-block{position:relative;margin-top:10px;display:inline-block;max-width:720px;width:100%;overflow:visible}
				    	pre{background:#1e1e1e;color:#f8f8f2;border-radius:6px;padding:12px 14px;overflow-x:auto;max-width:720px;min-width:400px;margin:0;white-space:pre;font-family:"Fira Code","Courier New",monospace}
				    	.copy-btn{position:absolute;top:8px;right:8px;background:rgba(60,60,60,.9);color:#fff;border:0;padding:4px 8px;border-radius:4px;font-size:12px;cursor:pointer;box-shadow:0 1px 3px rgba(0,0,0,.4);opacity:.95;z-index:20;transition:background .2s}
				    	.copy-btn:hover{background:rgba(0,122,204,.95)}
				    	pre::-webkit-scrollbar{height:8px}
				    </style>

				</head>
				<body>
				    <h1>CodeCache - Snippet Store</h1>
				    <h2>Add Snippets</h2>
				    <form hx-post="/snippets" id="myForm" hx-target="#snippets" hx-swap="innerHTML">
				        <textarea name="code" placeholder="Code" required></textarea><br />
				        <input name="language" placeholder="Language (e.g. java)" required /><br />
				        <input name="description" placeholder="Description" /><br />
				        <input name="tags" placeholder="Tags (comma separated)" /><br />
				        <button type="submit">Add Snippet</button>
				       	<button type="button" id="cancelBtn">Cancel</button>
				        <div class='error-msg' style='color: red; margin-top: 10px;'></div>
				    </form>
				    
				     <h2>Search Snippets</h2>
				     <div id="search-snips">
				     	<input type="text" name="search"
					       hx-get="/snippets/search"
					       hx-trigger="keyup changed delay:300ms"
					       hx-target="#snippets"
					       placeholder="Search snippets..." />
				     </div>

				    <h2>Snippets List</h2>
				    <div id="snippets" hx-get="/snippets" hx-trigger="load"></div>

				    <script>
				    
				    document.getElementById('cancelBtn').addEventListener('click', function() {
					    const form = document.getElementById('myForm');
					    form.reset();
					    // Clear error messages as well
					    const errContainer = form.querySelector('.error-msg');
					    if (errContainer) errContainer.textContent = '';
				    });
				    
				    document.getElementById('myForm').addEventListener('htmx:afterRequest', function(event) {
					    const form = event.target;
					    const errContainer = form.querySelector('.error-msg');
					    const status = event.detail.xhr.status;
					    if (status === 400 || status === 429 || status !== 200) {
					      if(errContainer) errContainer.textContent = 'Error: ' + status + ' - ' + event.detail.xhr.responseText;
					    } else {
					      if(errContainer) errContainer.textContent = '';
					      form.reset();
					    }
					});

				    function copyCode(button){
					  const code = button.nextElementSibling.innerText;
					  navigator.clipboard.writeText(code);
					  button.textContent = "Copied!";
					  setTimeout(()=>button.textContent="Copy",1500);
					}
				        document.body.addEventListener('htmx:afterSwap', (e) => {
				            if (e.target.id === 'snippets') {
				                Prism.highlightAll();
				            }
				        });
				    </script>
				</body>
				</html>
				""";
	}

	// Render current snippets list as HTML for snippet container replacement
	private static String renderSnippetsHtml(List<Snippet> result) {
		StringBuilder html = new StringBuilder();
		for (Snippet s : result) {
			String languageRaw = s.getLanguage() != null ? s.getLanguage() : "text";
			String language = escapeHtml(languageRaw.toLowerCase());

			List<String> tags = s.getTags() != null ? s.getTags() : Collections.<String>emptyList();
			String tagsJoined = String.join(", ", tags);

			html.append("<div class=\"snippet\">").append("<div><strong>Description:</strong> ")
					.append(escapeHtml(s.getDescription() != null ? s.getDescription() : "")).append("</div>")
					.append("<div><strong>Language:</strong> ").append(escapeHtml(languageRaw)).append("</div>")
					.append("<div><strong>Tags:</strong> ").append(escapeHtml(tagsJoined)).append("</div>")
					.append("<div class=\"code-block\">")
					.append("<button class=\"copy-btn\" onclick=\"copyCode(this)\">Copy</button>")
					.append("<pre><code class=\"language-").append(language).append("\">")
					.append(escapeHtml(s.getCode() != null ? s.getCode() : "")).append("</code></pre>").append("</div>") // end
																															// code-block
					.append("<button hx-delete='/snippets/").append(s.getId())
					.append("' hx-target='closest .snippet' hx-swap='outerHTML'>").append("Delete").append("</button>")
					.append("</div>"); // end snippet
		}
		return html.toString();
	}

	// Simple HTML escape utility to prevent injection issues
	private static String escapeHtml(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class Snippet {

	private String id;
	private String title;
	private String code;
	private String language;
	private String description;
	private List<String> tags;
	private LocalDateTime createdAt;

}
