# Lux 照度计 — 项目说明 · Project

<a id="proj-nav"></a>
## 目录 · Contents

**中文：** [概述](#proj-zh-overview) · [功能结构](#proj-zh-structure) · [技术栈](#proj-zh-stack) · [仓库布局](#proj-zh-layout) · [本地开发](#proj-zh-dev) · [便携版区别](#proj-zh-portable)

**English:** [Overview](#proj-en-overview) · [Feature map](#proj-en-structure) · [Stack](#proj-en-stack) · [Layout](#proj-en-layout) · [Local dev](#proj-en-dev) · [Portable workspace](#proj-en-portable)

---

<a id="proj-zh"></a>
## 中文

<a id="proj-zh-overview"></a>
### 概述

Android 应用 **LuxApp（照度计）**：利用手机 **光线传感器** 测量环境照度（**lux**），提供两大能力：

1. **照度计**：中央大字实时显示，两位小数；可调刷新间隔（**200ms / 500ms / 1s / 2s**）；支持数据保持、亮度指示条、亮度等级与「等效场景」文案。  
2. **场景检查**：按生活化分类选择场景与分区，将当前照度与内置 **推荐 lux 区间** 对比，输出偏暗 / 合适 / 偏亮及简短建议。

实机界面截图见仓库根目录 **`Photos/`**。

<a id="proj-zh-structure"></a>
### 功能结构（与代码对应）

| 模块 | 要点 |
|------|------|
| 传感器 | `Sensor.TYPE_LIGHT`，`SensorEvent` 更新 `lastLux`；UI 按 `refreshIntervalMs` 定时刷新。 |
| 刷新间隔 | `refreshRateOptionsMs`：200 / 500 / 1000 / 2000；持久键 `KEY_REFRESH_INTERVAL_MS`。 |
| 数据保持 | 开关持久化 `KEY_DATA_HOLD_ENABLED`；开启时显示冻结值。 |
| 照度 UI | `lux_format` 两位小数；对数映射到 `ProgressBar`；`getBrightnessLevelLabel` 等中文等级。 |
| 场景数据 | `sceneCategories` 内嵌多组 `SceneRange(min,max)` 与文案资源。 |
| 场景 UI | 双 `WheelPicker`、底部导航、`MaterialCardView` 状态色。 |
| 主题 | `AppCompatDelegate` 日夜间模式，偏好持久化。 |

<a id="proj-zh-stack"></a>
### 技术栈

- **语言**：Kotlin  
- **构建**：Gradle 8.4（Kotlin DSL）、Android Gradle Plugin 8.2.x  
- **最低 SDK**：24；**目标 / 编译 SDK**：34  
- **主要依赖**：AndroidX、Material、`powerwheelpicker`（滚轮选择器）  

<a id="proj-zh-layout"></a>
### 仓库布局

| 路径 | 说明 |
|------|------|
| `app/` | 应用模块源码与资源 |
| `Photos/` | 实机运行截图（文档用） |
| `gradle/wrapper/` | Gradle Wrapper（含 `gradle-wrapper.jar`） |
| `settings.gradle.kts` | 工程与 Maven 仓库配置 |
| `local.properties.example` | SDK 路径示例；克隆后复制为 `local.properties` 或由 Android Studio 自动生成 |

<a id="proj-zh-dev"></a>
### 本地开发

1. 安装 **JDK 17** 与 **Android SDK**（API 34）。  
2. 克隆仓库后，在工程根目录放置 `local.properties`，其中设置 `sdk.dir=...`（可参考 `local.properties.example`）。  
3. 使用 **Android Studio** 打开工程根目录，执行 **Sync Project with Gradle Files**，再运行到真机（光线传感器需真机）。  

<a id="proj-zh-portable"></a>
### 与便携版工作区的区别

本仓库仅包含 **可版本控制的源码与 Wrapper** 及 **文档用截图**。不包含嵌入式 JDK、Python、Android SDK 等便携工具链；这些应在本机或 CI 中单独安装。

<p align="right"><a href="#proj-nav">↑ 回到目录</a> · <a href="#proj-en">English →</a></p>

---

<a id="proj-en"></a>
## English

<a id="proj-en-overview"></a>
### Overview

**LuxApp** is an Android **light meter** app that reads ambient **lux** via the **ambient light sensor** and offers:

1. **Lux meter**: Large numeric display, two decimals; configurable refresh (**200 / 500 / 1000 / 2000 ms**); hold, brightness bar, level labels, and “equivalent scene” hints.  
2. **Scene check**: Pick a scene category and zone, compare live lux to **built-in suggested ranges**, and show too dark / OK / too bright with short guidance.

Screenshots live under **`Photos/`** at the repo root.

<a id="proj-en-structure"></a>
### Feature map (code alignment)

| Area | Notes |
|------|--------|
| Sensor | `Sensor.TYPE_LIGHT`; `SensorEvent` updates `lastLux`; UI ticks at `refreshIntervalMs`. |
| Refresh | `refreshRateOptionsMs`: 200 / 500 / 1000 / 2000; persisted `KEY_REFRESH_INTERVAL_MS`. |
| Hold | `KEY_DATA_HOLD_ENABLED`; when on, display is frozen. |
| Lux UI | `lux_format` with two decimals; log mapping to `ProgressBar`; Chinese level labels in `getBrightnessLevelLabel`. |
| Scenes | `sceneCategories` with `SceneRange(min,max)` and string resources. |
| Scene UI | Dual `WheelPicker`, bottom nav, `MaterialCardView` status colors. |
| Theme | `AppCompatDelegate` night/day; prefs persisted. |

<a id="proj-en-stack"></a>
### Stack

- **Language**: Kotlin  
- **Build**: Gradle 8.4 (Kotlin DSL), Android Gradle Plugin 8.2.x  
- **minSdk** 24; **compile/target** SDK 34  
- **Deps**: AndroidX, Material, `powerwheelpicker`  

<a id="proj-en-layout"></a>
### Repository layout

| Path | Purpose |
|------|---------|
| `app/` | App module source & resources |
| `Photos/` | On-device screenshots for docs |
| `gradle/wrapper/` | Gradle Wrapper (incl. `gradle-wrapper.jar`) |
| `settings.gradle.kts` | Project & Maven repos |
| `local.properties.example` | SDK path template; copy to `local.properties` or let Android Studio create it |

<a id="proj-en-dev"></a>
### Local development

1. Install **JDK 17** and **Android SDK** (API 34).  
2. After clone, add `local.properties` at the repo root with `sdk.dir=...` (see `local.properties.example`).  
3. Open the root in **Android Studio**, **Sync Project with Gradle Files**, run on a **physical device** (real light sensor).

<a id="proj-en-portable"></a>
### vs. portable workspace

This repo only ships **versionable source and the Wrapper** plus **doc screenshots**. It does **not** bundle embedded JDK, Python, Android SDK, etc.—install those locally or in CI.

<p align="right"><a href="#proj-nav">↑ Back to contents</a> · <a href="#proj-zh">← 中文</a></p>
