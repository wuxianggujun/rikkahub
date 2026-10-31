"""Real-browser smoke tests for the shipped HTML, including hostile Markdown.

Requires Python Playwright and Chrome; no app credentials or user browser profile.
Run separately from the offline unittest suite because HTTPS CDN access is needed.
"""

import argparse
import base64
import html
from pathlib import Path
import re
import textwrap

from playwright.sync_api import sync_playwright


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
RICHTEXT = MAIN / "java/me/rerere/rikkahub/ui/components/richtext"
ERROR_TEXT = "无法渲染此 Markdown 预览。"


def encode(value):
    return base64.b64encode(value.encode("utf-8")).decode("ascii")


def mermaid_version():
    source = (RICHTEXT / "MarkdownWeb.kt").read_text(encoding="utf-8")
    return re.search(r'MERMAID_VERSION = "([^"]+)"', source).group(1)


def markdown_page(markdown):
    template = (MAIN / "assets/html/mark.html").read_text(encoding="utf-8")
    substitutions = {
        "MARKDOWN_BASE64": encode(markdown),
        "MERMAID_VERSION": mermaid_version(),
        "PREVIEW_TITLE": "预览",
        "RENDER_ERROR_BASE64": encode(ERROR_TEXT),
        "WEBVIEW_UNSUPPORTED_BASE64": encode("请更新 Android System WebView。"),
        "BACKGROUND_COLOR": "#fafafa",
        "SURFACE_COLOR": "#fafafa",
        "SURFACE_VARIANT_COLOR": "#eceff1",
        "PRIMARY_COLOR": "#1565c0",
        "OUTLINE_COLOR": "#78909c",
        "OUTLINE_VARIANT_COLOR": "#cfd8dc",
    }
    for token in re.findall(r"\{\{([A-Z0-9_]+)\}\}", template):
        value = substitutions.get(token, "#263238")
        template = template.replace("{{" + token + "}}", value)
    assert "{{" not in template, "Fixture left an unresolved template variable"
    return template


def diagram_page(code):
    source = (RICHTEXT / "Mermaid.kt").read_text(encoding="utf-8")
    template = source.split('return """', 1)[1].split('""".trimIndent()', 1)[0]
    template = template.replace("${MERMAID_VERSION}", mermaid_version())
    template = template.replace("${code.escapeHtml()}", html.escape(code, quote=True))
    colors = {"background": "#fafafa", "surface": "#ffffff", "primaryColor": "#e3f2fd"}
    template = re.sub(r"\$\{([A-Za-z]+)\}", lambda match: colors.get(match[1], "#263238"), template)
    return textwrap.dedent(template)


def check_markdown(browser, output):
    page = browser.new_page(viewport={"width": 900, "height": 900})
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    sample = r"""# 中文预览 😀

| Feature | Result |
| --- | --- |
| Unicode | 你好 🌍 |

- [x] task list

```kotlin
val greeting = "你好"
```

Math: $E = mc^2$ and chemistry: $\ce{H2O}$.

<details><summary>Safe HTML</summary>Preserved content</details>

```mermaid
flowchart LR
    A[中文] --> B[Preview]
```
"""
    page.set_content(markdown_page(sample), wait_until="networkidle", timeout=25_000)
    page.wait_for_selector("#content .mermaid svg", timeout=15_000)
    assert not errors, errors
    assert page.locator("h1").inner_text() == "中文预览 😀"
    assert page.locator("table tbody tr").count() == 1
    assert page.locator("input[type=checkbox]").is_checked()
    assert page.locator("pre code .hljs-keyword").count() > 0
    assert page.locator(".katex").count() >= 2
    assert page.locator(".katex-error").count() == 0
    assert page.locator("details summary").inner_text() == "Safe HTML"
    page.screenshot(path=str(output / "markdown-preview.png"), full_page=True)
    print("PASS: Markdown, Chinese/emoji, tables, highlighting, math, chemistry, Mermaid", flush=True)

    hostile = r"""# Injection regression

<img src=x onerror="window.__injected = true">
<svg onload="window.__injected = true"></svg>
<script>window.__injected = true</script>
<iframe srcdoc="<script>parent.__injected = true</script>"></iframe>
<style>body { display: none; }</style>
<a href="javascript:window.__injected=true">script link</a>
<a href="content://private/secret">local content</a>
<a href="file:///data/data/private/secret">local file</a>

Unsafe math: $\href{javascript:alert(1)}{bad}$.
"""
    page.set_content(markdown_page(hostile), wait_until="networkidle", timeout=15_000)
    page.wait_for_selector("h1", timeout=10_000)
    assert page.locator("h1").inner_text() == "Injection regression"
    assert page.evaluate("window.__injected === undefined")
    assert page.locator("#content script, #content iframe, #content style, #content [onerror], #content [onload]").count() == 0
    links = page.locator("#content a[href]").evaluate_all("nodes => nodes.map(node => node.getAttribute('href'))")
    assert all(not re.match(r"(?i)(javascript|file|content):", value) for value in links), links

    fence = '```mermaid\n</div><img src=x onerror="window.__injected=true">\n```'
    page.set_content(markdown_page(fence), wait_until="networkidle", timeout=15_000)
    page.wait_for_function("document.getElementById('content').textContent === " + repr(ERROR_TEXT), timeout=10_000)
    assert page.evaluate("window.__injected === undefined")
    assert page.locator("#content img").count() == 0
    print("PASS: raw HTML, unsafe URLs, untrusted math and Mermaid fence injection blocked", flush=True)
    page.close()


def check_diagram(browser, output):
    page = browser.new_page(viewport={"width": 1024, "height": 500})
    page.evaluate("window.AndroidInterface = { exportImage: value => { window.__exportedImage = value; } }")
    page.set_content(diagram_page("flowchart LR\nA[Hello] --> B[World]"), wait_until="networkidle", timeout=25_000)
    page.wait_for_selector(".mermaid svg", timeout=15_000)
    assert page.evaluate("mermaid.mermaidAPI.getConfig().securityLevel") == "strict"
    page.evaluate("exportSvgToPng()")
    page.wait_for_function("typeof window.__exportedImage === 'string'", timeout=10_000)
    assert page.evaluate("window.__exportedImage.startsWith('iVBOR')"), "PNG export failed"
    page.screenshot(path=str(output / "mermaid-preview.png"), full_page=True)
    print("PASS: dedicated Mermaid renderer and PNG export", flush=True)
    page.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "embedded/build/reports/web-previews")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(channel="chrome", headless=True)
        try:
            check_markdown(browser, output)
            check_diagram(browser, output)
        finally:
            browser.close()


if __name__ == "__main__":
    main()
