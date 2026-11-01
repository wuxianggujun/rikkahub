# Embedded 资源维护记录

核验日期：2026-10-31。此轮是基于 `97971ed8` 的定点维护，**不是将 RikkaHub 整体升级到 3.x**。
保留 TinaIDE 的嵌入入口、宿主工具、Provider 限制、数据库配置和裁剪边界。

## 上游来源

- 资源基线：[RikkaHub 3.1.1](https://github.com/rikkahub/rikkahub/releases/tag/3.1.1)，
  commit `ad2c4430d50ba52392b2941367f98ef59614cb73`。
- 占位图：回移 [97cbb599](https://github.com/rikkahub/rikkahub/commit/97cbb599) 中的两张 WebP，
  沿用 `R.drawable.placeholder` / `placeholder_dark`，不改变调用处。
- 模型图标：仅更新当前 `AIIconMatcher` 已使用的 Gemma、Kimi、Qwen SVG；
  未导入新 Provider、新模型目录或没有消费代码的内置 skills。
- WebView 释放：回移 [2c980642](https://github.com/rikkahub/rikkahub/commit/2c980642) 的
  `onRelease` 销毁逻辑。可复用的 `onReset` 不销毁 WebView。
- 五个上游资源的 SHA-256 固定在 `tests/test_resources.py`，便于后续更新时核验来源。

## 网页预览安全更新

3.1.1 上游的 `mark.html` 仍引用旧版库，不能直接覆盖后声称已修复漏洞。
此轮另行查询 npm Registry 和 [OSV](https://osv.dev/)，固定以下直接依赖：

| 组件 | 原值 | 本轮固定值 | 许可证 |
| --- | --- | --- | --- |
| markdown-it | 14.0.0 | 15.0.3 | MIT |
| KaTeX | 0.16.8 | 0.19.1 | MIT |
| @vscode/markdown-it-katex | 未固定 | 1.1.2 | MIT |
| markdown-it-task-lists | 2.1.1 | 2.1.1 | ISC |
| highlight.js | 11.9.0 | 11.12.0 | BSD-3-Clause |
| Mermaid | Markdown 为 10.6.1，独立图表为浮动的 11 | 12.2.0 | MIT |
| DOMPurify | 无 | 3.4.17 | MPL-2.0 或 Apache-2.0，选择 Apache-2.0 |
| js-base64 | 3.7.5 | 移除，改用 `atob` + UTF-8 `TextDecoder` | 不再加载 |

旧直接依赖命中的公告包括：

- KaTeX URL 协议校验绕过：[GHSA-3wc5-fcw2-2329 / CVE-2024-28246](https://osv.dev/vulnerability/GHSA-3wc5-fcw2-2329)。
- markdown-it ReDoS：[GHSA-38c4-r59v-3vqw / CVE-2026-2327](https://osv.dev/vulnerability/GHSA-38c4-r59v-3vqw)。
- Mermaid 内嵌 DOMPurify 的原型污染：[GHSA-m4gq-x24j-jpmf](https://osv.dev/vulnerability/GHSA-m4gq-x24j-jpmf)。

对本轮固定版本进行 OSV 精确版本查询时未返回已知公告。这只覆盖上述直接 npm 坐标，
不是整套 Maven/原生依赖、CDN 传递依赖或整个 RikkaHub 的无漏洞证明。

预览边界同步修复：

1. 原始 Markdown HTML 经过 DOMPurify，禁止 `style` / `iframe` / `form` 和 `srcdoc`。
2. Mermaid fence 先 HTML 转义；两个图表入口均使用 `securityLevel: 'strict'`。
3. KaTeX 保持 `trust: false` 和展开限制；插件显式接收更新后的 KaTeX，启用同实例的 mhchem。
4. ESM 依赖覆盖固定 KaTeX / DOMPurify，避免插件回退到旧 KaTeX。
5. 错误提示使用 `textContent`，不把异常消息当 HTML 插入；中英文提示来自 Android 字符串资源。
6. 模板读取、Base64 编解码和新增文件均使用 UTF-8，无 BOM。

仍沿用在线 CDN 加载，不把约数 MB 的 Mermaid bundle 放进 APK。
离线首开网页预览仍不在本轮保证范围；普通原生 Markdown 展示不依赖此模板。
未新增 Maven/JNI/反射依赖，因此本轮不增加 R8 keep 规则。

## 瘦身范围与计量口径

| 资源 | 原始字节 | 更新后字节 | 说明 |
| --- | ---: | ---: | --- |
| 两张 PNG 占位图 | 2,272,027 | 12,040 | 换为同资源名 WebP |
| 未使用的 Google Sans Flex 字体 | 4,153,300 | 0 | 删除无运行时引用的封装及注释配置 |
| 三张未使用 banner | 5,243,893 | 0 | 原本已被 embedded 排除，只减少源码占用 |

前两项合计减少 **6,413,287 字节（约 6.12 MiB）原始资源**。
这不是最终 APK 压缩后差值：Release 的 R8/resource shrinker 可能已经去掉未使用字体，
压缩率也不同。APK 收益必须用同配置、同 ABI 的前后产物实测。

保留 JetBrains Mono、emoji、实际使用的模型图标和独立版仍使用的 `simple_dict`。
`embedded` 继续排除 `simple_dict:banner`，防止后续上游同步重新引入大资源。

## 验证入口

在 RikkaHub 子模块目录中运行离线资源、引用、编码和安全配置契约：

```bash
python -m unittest discover -s embedded/tests -p 'test_*.py' -v
```

主仓库已有 Python unittest discovery 通过 `tools/checks/test_rikkahub_resources.py`
运行同一套契约，不复制测试实现。

真实浏览器回归需要 Chrome、Python Playwright 和 CDN 网络访问。
可将仅测试使用的依赖安装到模块自己的 build 目录，不修改全局 Python：

```powershell
py -3 -m pip install --target embedded/build/browser-test-deps playwright==1.64.1
$env:PYTHONPATH = (Resolve-Path embedded/build/browser-test-deps).Path
py -3 -X utf8 embedded/tests/check_web_previews.py
```

覆盖中文/emoji、表格、任务列表、代码高亮、数学/化学公式、Mermaid、PNG 导出、
恶意 HTML/本地文件 URL/不可信公式/Mermaid fence 注入。
截图位于 `embedded/build/reports/web-previews/`。这是桌面 Chrome 验证，不能替代 Android WebView 真机验收。

在 TinaIDE 主仓库目录中验证 embedded 编译与 AAR 打包：

```bash
./gradlew :rikkahub:embedded:compileDebugKotlin :rikkahub:embedded:bundleDebugAar --no-daemon --console=plain
```

其他会话正在构建时只做静态检查，不并行启动 Gradle。
最终仍需设备检查聊天预览、图表导出、反复打开关闭预览、暗色占位图和图标。

### 本轮执行结果

- 资源契约 **10/10 通过**；接入主仓库 discovery 后 Python checks **30/30 通过**。
- Chrome 真实渲染、PNG 导出及注入回归通过；已人工检查 Markdown 预览截图。
- `compileDebugKotlin` 和 `bundleDebugAar` 通过，最终构建耗时 3 分 42 秒。
  首次冷构建超过本轮设定的时限，未将超时当作通过。
- 已检查生成的 AAR：包含新 WebP、JetBrains Mono、emoji 和更新后的 HTML；
  不含 Google Sans、旧 PNG、banner、simple_dict，也没有遗留 GoogleSans 字体封装 class。
- 主仓库文档、文件操作基线、embedded 资源冲突及 i18n 检查通过。
- Windows 全量 Python checks 需使用 Git Bash，而不是系统目录下的 WSL `bash.exe`；
  本轮仅调整测试子进程的 PATH，未修改系统配置。
- 后续按用户要求于 2026-11-01 完成 `:app:assembleArm64Release`，耗时 30 分 16 秒；
  版本由宿主构建规则递增为 `0.18.37`（基础版本码 `1838`，ARM64 APK 版本码 `18382`）。
  APK 为 151,441,701 字节（144.43 MiB），R8/资源压缩、Release Lint、APK v2 签名及 zipalign 校验通过。
  已按内容哈希确认新 WebP、JetBrains Mono、HTML 和模型图标进入 APK，旧 Google Sans 字体未进入产物；
  banner 和 simple_dict 仍被排除，R8 mapping 已本地归档并核对哈希。
- 未执行 Android 真机验收，也未上传或发布；分发门禁保持原有阻断策略。

## 提交与回退边界

资源维护本身不修改 app 版本、数据库 schema、AGP/Kotlin/MCP SDK，也不改变许可证或分发门禁；
后续用户要求的 Release 构建按宿主既有规则递增 app 版本，如上所记。
发布时先提交并推送本子模块，再提交主仓库 gitlink。
不要用整体重置子模块的方式回退资源改动，否则会丢失 TinaIDE 适配；
安全相关版本不可为了恢复旧视觉效果一起回退到有公告的旧版本。

## 2026-11-01：同步上游纯 AGPL-3.0 许可

资源维护之后，核验并以 `cherry-pick -x` 回移官方提交
[`65a63e4ec9c649bd5404ebcbbedb55aad5741d84`](https://github.com/rikkahub/rikkahub/commit/65a63e4ec9c649bd5404ebcbbedb55aad5741d84)
（作者 re-ovo，2026-07-18，`docs(license): 改为纯 AGPL-3.0 许可证`）。
该提交只更新 `LICENSE` 和三份 README，删除分段双重许可中的非商业和用户数附加限制；
没有导入 3.x 功能代码，也没有撤销上述安全更新或 TinaIDE 适配。

- 当前 `LICENSE` 与该上游提交的 GNU AGPL 第 3 版全文一致。
- Git blob：`be3f7b28e564e7dd05eaf59d64adba1a4065ac0e`。
- SHA-256（UTF-8、换行规范为 LF）：
  `0d96a4ff68ad6d4b6f1f30f713b18d5184912ba8dd389f86aa7710db079abcb0`。
- RikkaHub 部分继续受 AGPL-3.0 约束，不因此变成 GPL 或宽松许可；与 GPLv3 宿主组合时
  依两份许可证的第 13 条处理，并保留对应源码、修改说明、版权与许可证文本。
- TinaIDE 的审查依据、随 APK 展示的许可全文和源码入口、内容哈希门禁由宿主维护；
  历史 `0.18.37` 构建的未解除门禁记录不追溯改写。

发布者必须先推送本子模块，再提交宿主 gitlink，使最终产物的对应源码可被递归检出。
