# Lux 照度计 — 项目说明

## 概述

Android 应用 **LuxApp**：通过手机光线传感器读取环境照度（lux），在界面中央大字显示，约每 0.5 秒刷新，数值保留两位小数。

## 技术栈

- **语言**：Kotlin  
- **构建**：Gradle 8.4（Kotlin DSL）、Android Gradle Plugin 8.2.x  
- **最低 SDK**：24；**目标 / 编译 SDK**：34  

## 仓库布局

| 路径 | 说明 |
|------|------|
| `app/` | 应用模块源码与资源 |
| `gradle/wrapper/` | Gradle Wrapper（含 `gradle-wrapper.jar`） |
| `settings.gradle.kts` | 工程与 Maven 仓库配置 |
| `local.properties.example` | SDK 路径示例；克隆后复制为 `local.properties` 或由 Android Studio 自动生成 |

## 本地开发

1. 安装 **JDK 17** 与 **Android SDK**（API 34）。  
2. 克隆仓库后，在工程根目录放置 `local.properties`，其中设置 `sdk.dir=...`（可参考 `local.properties.example`）。  
3. 使用 **Android Studio** 打开工程根目录，执行 **Sync Project with Gradle Files**，再运行到真机（光线传感器需真机）。  

## 与便携版工作区的区别

本仓库仅包含 **可版本控制的源码与 Wrapper**。不包含嵌入式 JDK、Python、Android SDK 等便携工具链；这些应在本机或 CI 中单独安装。
