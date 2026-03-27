# Lux 照度计 — 项目说明

## 概述

Android 应用 **LuxApp（照度计）**：利用手机 **光线传感器** 测量环境照度（**lux**），提供两大能力：

1. **照度计**：中央大字实时显示，两位小数；可调刷新间隔（**200ms / 500ms / 1s / 2s**）；支持数据保持、亮度指示条、亮度等级与「等效场景」文案。  
2. **场景检查**：按生活化分类选择场景与分区，将当前照度与内置 **推荐 lux 区间** 对比，输出偏暗 / 合适 / 偏亮及简短建议。

实机界面截图见仓库根目录 **`Photos/`**。

## 功能结构（与代码对应）

| 模块 | 要点 |
|------|------|
| 传感器 | `Sensor.TYPE_LIGHT`，`SensorEvent` 更新 `lastLux`；UI 按 `refreshIntervalMs` 定时刷新。 |
| 刷新间隔 | `refreshRateOptionsMs`：200 / 500 / 1000 / 2000；持久键 `KEY_REFRESH_INTERVAL_MS`。 |
| 数据保持 | 开关持久化 `KEY_DATA_HOLD_ENABLED`；开启时显示冻结值。 |
| 照度 UI | `lux_format` 两位小数；对数映射到 `ProgressBar`；`getBrightnessLevelLabel` 等中文等级。 |
| 场景数据 | `sceneCategories` 内嵌多组 `SceneRange(min,max)` 与文案资源。 |
| 场景 UI | 双 `WheelPicker`、底部导航、`MaterialCardView` 状态色。 |
| 主题 | `AppCompatDelegate` 日夜间模式，偏好持久化。 |

## 技术栈

- **语言**：Kotlin  
- **构建**：Gradle 8.4（Kotlin DSL）、Android Gradle Plugin 8.2.x  
- **最低 SDK**：24；**目标 / 编译 SDK**：34  
- **主要依赖**：AndroidX、Material、`powerwheelpicker`（滚轮选择器）  

## 仓库布局

| 路径 | 说明 |
|------|------|
| `app/` | 应用模块源码与资源 |
| `Photos/` | 实机运行截图（文档用） |
| `gradle/wrapper/` | Gradle Wrapper（含 `gradle-wrapper.jar`） |
| `settings.gradle.kts` | 工程与 Maven 仓库配置 |
| `local.properties.example` | SDK 路径示例；克隆后复制为 `local.properties` 或由 Android Studio 自动生成 |

## 本地开发

1. 安装 **JDK 17** 与 **Android SDK**（API 34）。  
2. 克隆仓库后，在工程根目录放置 `local.properties`，其中设置 `sdk.dir=...`（可参考 `local.properties.example`）。  
3. 使用 **Android Studio** 打开工程根目录，执行 **Sync Project with Gradle Files**，再运行到真机（光线传感器需真机）。  

## 与便携版工作区的区别

本仓库仅包含 **可版本控制的源码与 Wrapper** 及 **文档用截图**。不包含嵌入式 JDK、Python、Android SDK 等便携工具链；这些应在本机或 CI 中单独安装。
