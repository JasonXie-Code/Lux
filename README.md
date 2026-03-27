# Lux 照度计

读取手机光线传感器，在屏幕中央大字显示当前照度（lux），每 0.5 秒更新，保留两位小数。

## 从 Git 克隆后

1. 用 **Android Studio** 打开本仓库根目录（与 `settings.gradle.kts` 同级）。  
2. 若根目录没有 `local.properties`：复制 `local.properties.example` 为 `local.properties`，将 `sdk.dir` 设为本机 Android SDK 路径；或在 Android Studio 中同步工程，IDE 常会代为创建。  
3. **不要**将含本机路径的 `local.properties` 提交到远程仓库（已在 `.gitignore` 中忽略）。  
4. 若 Gradle 报 JDK 问题，请在 **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** 中指定 **JDK 17**，或在 `gradle.properties` 中按需设置 `org.gradle.java.home`（勿提交含隐私路径的注释块）。

## 环境要求

- Android Studio Ladybug (2024.2.1) 或更高版本（或兼容的 AGP 8.x）
- JDK 17
- Android SDK 34，minSdk 24

## 构建与运行

1. 用 **Android Studio** 打开项目目录 `LuxApp`。
2. 若提示缺少 Gradle Wrapper 或 `gradle-wrapper.jar`，使用菜单 **File → Sync Project with Gradle Files**；或在本机已安装 Gradle 时在项目根目录执行 `gradle wrapper` 生成。
3. 连接真机（光线传感器需真机）或选择模拟器，点击 **Run** 运行。

## 命令行构建

在项目根目录 `LuxApp` 下（需先有 `gradle-wrapper.jar`，用 Android Studio 同步或执行 `gradle wrapper` 生成）：

```bash
# 调试版 APK
./gradlew assembleDebug

# 输出：app/build/outputs/apk/debug/app-debug.apk
```

## 说明

- 使用 `Sensor.TYPE_LIGHT` 获取照度，单位 lux。
- 界面每 500ms 刷新一次显示，数值格式为两位小数。
- 无光线传感器或权限弹窗，直接安装即可使用。
