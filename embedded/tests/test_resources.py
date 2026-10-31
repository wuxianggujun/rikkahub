"""Offline contracts for TinaIDE's selectively maintained RikkaHub resources."""

import hashlib
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
RICHTEXT = MAIN / "java/me/rerere/rikkahub/ui/components/richtext"
UPSTREAM_RESOURCE_SHA256 = {
    "res/drawable/placeholder.webp": "51a3d1a5011493badd7fb4913ec25feaba462094f2b0f50fd952f06aa09c8337",
    "res/drawable/placeholder_dark.webp": "f2d9b1893054bf48d709cdab69284947a363fa4fa0d1d74d47f12413001d2ab6",
    "assets/icons/gemma-color.svg": "670a8641f518cb67ef5015f5745bba503229ced4e9205f71c22e4a7ffe897b14",
    "assets/icons/kimi-color.svg": "c1877b878e9358e9618d1dcff9d8b3db44e25a4ec4d773e970446114c00806a9",
    "assets/icons/qwen-color.svg": "5a2e10727f1daa3926743981352c6bde36d33f272d3288d8f9f3948d60f7a783",
}


class EmbeddedResourceTest(unittest.TestCase):
    def setUp(self):
        self.template = (MAIN / "assets/html/mark.html").read_text(encoding="utf-8")
        self.builder = (RICHTEXT / "MarkdownWeb.kt").read_text(encoding="utf-8")

    def test_selected_resources_match_reviewed_upstream_blobs(self):
        for relative, expected in UPSTREAM_RESOURCE_SHA256.items():
            with self.subTest(path=relative):
                content = (MAIN / relative).read_bytes()
                if relative.endswith(".svg"):
                    content = content.replace(b"\r\n", b"\n")
                self.assertEqual(hashlib.sha256(content).hexdigest(), expected)

    def test_placeholders_remain_small_webp_with_unchanged_resource_names(self):
        for name in ("placeholder", "placeholder_dark"):
            with self.subTest(name=name):
                content = (MAIN / f"res/drawable/{name}.webp").read_bytes()
                self.assertEqual(content[:4], b"RIFF")
                self.assertEqual(content[8:12], b"WEBP")
                self.assertLess(len(content), 10_000)
                self.assertFalse((MAIN / f"res/drawable/{name}.png").exists())

    def test_unused_font_and_banners_do_not_return(self):
        self.assertFalse((MAIN / "res/font/google_sans_flex.ttf").exists())
        theme = MAIN / "java/me/rerere/rikkahub/ui/theme"
        self.assertFalse((theme / "GoogleSans.kt").exists())
        for source in theme.glob("*.kt"):
            text = source.read_text(encoding="utf-8")
            self.assertNotIn("GoogleSansFlex", text)
            self.assertNotIn("google_sans_flex", text)
        self.assertFalse(list((MAIN / "assets/banner").glob("*.png")))

    def test_active_resources_and_standalone_dictionary_are_preserved(self):
        for relative in (
            "res/font/jetbrains_mono.ttf",
            "assets/emoji/categories.with.modifiers.min.json",
            "assets/simple_dict/jieba.dict.utf8",
        ):
            with self.subTest(path=relative):
                self.assertTrue((MAIN / relative).is_file())
        build = (ROOT / "embedded/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('ignoreAssetsPattern = "simple_dict:banner"', build)

    def test_model_icon_matcher_has_no_missing_assets(self):
        matcher = (MAIN / "java/me/rerere/rikkahub/utils/AIIconMatcher.kt").read_text(encoding="utf-8")
        icons = re.findall(r'"([^"\n]+\.(?:svg|png))"', matcher)
        self.assertTrue(icons)
        for icon in icons:
            with self.subTest(icon=icon):
                self.assertTrue((MAIN / "assets/icons" / icon).is_file())
        for name in ("gemma-color.svg", "kimi-color.svg", "qwen-color.svg"):
            ET.parse(MAIN / "assets/icons" / name)

    def test_preview_dependencies_are_pinned_to_reviewed_versions(self):
        for spec in (
            "markdown-it@15.0.3",
            "@vscode/markdown-it-katex@1.1.2?deps=katex@0.19.1",
            "markdown-it-task-lists@2.1.1",
            "katex@0.19.1",
            "highlight.js@11.12.0",
            "dompurify@3.4.17",
        ):
            self.assertIn(spec, self.template)
        self.assertIn('MERMAID_VERSION = "12.2.0"', self.builder)
        self.assertIn("mermaid@{{MERMAID_VERSION}}", self.template)
        mermaid = (RICHTEXT / "Mermaid.kt").read_text(encoding="utf-8")
        self.assertIn("mermaid@${MERMAID_VERSION}/", mermaid)
        self.assertNotIn("js-base64", self.template)

    def test_markdown_is_sanitized_and_mermaid_source_is_escaped(self):
        self.assertIn("innerHTML = DOMPurify.sanitize(html", self.template)
        self.assertIn("md.utils.escapeHtml(token.content.trim())", self.template)
        self.assertNotIn("innerHTML = html", self.template)
        self.assertNotIn("error.message", self.template)
        self.assertIn("textContent = decodeBase64('{{RENDER_ERROR_BASE64}}')", self.template)

    def test_both_renderers_use_strict_mermaid_and_untrusted_katex(self):
        for source in (self.template, (RICHTEXT / "Mermaid.kt").read_text(encoding="utf-8")):
            self.assertIn("securityLevel: 'strict'", source)
            self.assertNotIn("securityLevel: 'loose'", source)
        self.assertIn("trust: false", self.template)
        self.assertNotIn("trust: true", self.template)
        self.assertIn("maxExpand: 1000", self.template)
        self.assertIn("katex@0.19.1/dist/contrib/mhchem.mjs", self.template)

    def test_template_placeholders_are_bound_and_text_is_utf8(self):
        placeholders = set(re.findall(r"\{\{[A-Z0-9_]+\}\}", self.template))
        bound = set(re.findall(r'\.replace\(\s*"(\{\{[A-Z0-9_]+\}\})"', self.builder))
        self.assertIn("{{MARKDOWN_BASE64}}", placeholders)
        self.assertFalse(placeholders - bound, f"Unbound placeholders: {placeholders - bound}")
        self.assertIn("bufferedReader(Charsets.UTF_8)", self.builder)
        self.assertIn("new TextDecoder('utf-8')", self.template)
        self.assertIn('<meta charset="UTF-8">', self.template)
        for locale in ("values", "values-zh"):
            path = MAIN / f"res/{locale}/strings.xml"
            self.assertFalse(path.read_bytes().startswith(b"\xef\xbb\xbf"))
            resources = {entry.attrib.get("name"): entry.text for entry in ET.parse(path).getroot()}
            for key in ("markdown_preview_render_error", "markdown_preview_unsupported_webview"):
                self.assertTrue(resources.get(key))

    def test_webview_disposal_destroys_native_resources_but_reset_does_not(self):
        source = (MAIN / "java/me/rerere/rikkahub/ui/components/webview/WebView.kt").read_text(encoding="utf-8")
        release = source.split("private fun WebView.release(", 1)[1].split('\n}', 1)[0]
        for operation in ("resetState(interfaces, clearClients = true)", 'loadUrl("about:blank")', "destroy()"):
            self.assertIn(operation, release)
        on_release = source.split("onRelease = {", 1)[1].split("update =", 1)[0]
        self.assertLess(on_release.index("state.webView = null"), on_release.index("it.release(state.interfaces)"))
        on_reset = source.split("onReset = {", 1)[1].split("onRelease =", 1)[0]
        self.assertNotIn("destroy()", on_reset)
        self.assertNotIn("it.release(", on_reset)


if __name__ == "__main__":
    unittest.main()
