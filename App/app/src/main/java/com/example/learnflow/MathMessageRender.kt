
package com.example.learnflow
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup

class MathMessageRenderer(
    context: Context,
    private val webView: WebView
) {
    init {
        webView.settings.javaScriptEnabled = true
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = WebViewClient()
    }

    fun render(message: String) {
        val htmlContent = formatMessage(message)

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport"
                    content="width=device-width, initial-scale=1.0">
              <style>
                body {
                  background: transparent;
                  color: #ffffff;
                  font-family: sans-serif;
                  font-size: 14px;
                  line-height: 1.5;
                  margin: 0;
                  padding: 8px;
                  overflow-wrap: anywhere;
                }
                h1, h2, h3 { line-height: 1.3; }
                .equation {
                  overflow-x: auto;
                  max-width: 100%;
                }
                p { margin: 8px 0 16px; }
                li { margin-bottom: 8px; }
                pre, code {
                  white-space: pre-wrap;
                  overflow-wrap: anywhere;
                }
              </style>

              <script>
                window.MathJax = {
                  tex: {
                    inlineMath: [['\\\\(', '\\\\)']],
                    displayMath: [['\\\\[', '\\\\]'], ['$$', '$$']],
                    processEscapes: true
                  },
                  options: {
                    skipHtmlTags: ['script', 'style', 'textarea', 'pre', 'code']
                  },
                  startup: {
                    typeset: false
                  }
                };
              </script>
              <script defer
                src="https://cdn.jsdelivr.net/npm/mathjax@4/tex-chtml.js">
              </script>
            </head>
            <body>
              $htmlContent
              <script>
                window.addEventListener('load', async () => {
                  if (window.MathJax) {
                    await MathJax.startup.promise;
                    await MathJax.typesetPromise();
                  }
                });
              </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(
            "https://learnflow.local/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun formatMessage(message: String): String {
        // Escape HTML first so AI text cannot inject HTML into the page.
        var text = message
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        // Convert bracket-wrapped LaTeX equations to display math.
        // Example: [ \int \tan x\,dx = ... ]
        text = text.replace(
            Regex("""(?m)^\s*\[\s*(\\(?:int|frac|tan|cos|sin|ln|boxed|displaystyle)\b.*?)\s*]\s*$""")
        ) { match ->
            """\[${match.groupValues[1]}\]"""
        }

        // Basic Markdown formatting.
        text = text.replace(
            Regex("""(?m)^#{1,3}\s+(.+)$""")
        ) { match ->
            "<h3>${match.groupValues[1]}</h3>"
        }

        text = text.replace(
            Regex("""\*\*(.+?)\*\*""")
        ) { match ->
            "<strong>${match.groupValues[1]}</strong>"
        }

        // Basic paragraphs and line breaks.
        text = text
            .replace("\r\n", "\n")
            .replace("\n\n", "</p><p>")
            .replace("\n", "<br>")

        return "<p>$text</p>"
    }
}