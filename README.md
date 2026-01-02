# PoseBaby 📸

> Your AI Photography Coach - Professional posing guidance powered by LLM

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="PoseBaby Logo" width="120"/>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#demo">Demo</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#中文说明">中文说明</a>
</p>

---

## 🎯 The Problem

As a male photographer trying to take photos for my girlfriend, I always struggled with posing guidance. I'm not a professional photographer, and I often wondered:

> *"What if an AI could act like a professional photographer standing by my side, teaching me how to shoot?"*

I searched for existing solutions but found none that truly addressed this need. So I decided to create one myself.

**PoseBaby** is an AI-powered photography assistant that analyzes your scene and provides real-time posing suggestions—like having a professional photographer coaching you through every shot.

## ✨ Features

### 🖼️ Image Mode (图片模式)
- **Scene Analysis**: AI analyzes your photo's lighting, background, and mood
- **Pose Suggestions**: Get 4 creative, scene-specific posing ideas
- **Reference Generation**: Generate professional reference images using Doubao AI
- **Grid Layouts**: Choose from 1×1, 1×2, 2×2, or 3×3 grid layouts
- **Props Support**: Add photography props (flowers, books, umbrellas, etc.)
- **Split Viewer**: View and select individual poses from generated grids
- **Photo Capture**: Take photos directly from the overlay screen

### 📝 Text Mode (文本模式)
- **Skeleton Overlay**: Real-time pose skeleton guidance
- **Pinch-to-Zoom**: Adjust skeleton reference size
- **Pose Matching**: ML Kit pose detection for alignment

## 🎬 Demo

> 🎥 **Demo Video Coming Soon**

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ MainActivity│  │ Composables │  │  Overlay Screens    │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                     │             │
│         └────────────────┼─────────────────────┘             │
│                          ▼                                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   MainViewModel                        │  │
│  │    • State Management (AppState enum)                  │  │
│  │    • Flow orchestration                                │  │
│  │    • Prompt assembly                                   │  │
│  └────────────────────────┬──────────────────────────────┘  │
│                           │                                  │
├───────────────────────────┼──────────────────────────────────┤
│                     Data Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ ZRepository  │  │DoubaoRepo    │  │ ZhipuRepository  │   │
│  │ (Scene AI)   │  │(Image Gen)   │  │ (Alternative)    │   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │
│         │                 │                    │             │
│         ▼                 ▼                    ▼             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    External APIs                         ││
│  │   • Zhipu AI (GLM-4.5v) - Scene Analysis                ││
│  │   • Doubao (Volcano Engine) - Image Generation          ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    ML Kit                                ││
│  │   • Pose Detection - Skeleton overlay                   ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```

### App Flow

```
Mode Selection → Source Selection → Camera/Gallery
       │
       ▼
   Analyzing (AI)
       │
       ▼
  Pose Selection (4 suggestions)
       │
       ├── Text Mode ──→ Skeleton Overlay ──→ Photo Capture
       │
       └── Image Mode ──→ Grid Selection ──→ Props Selection
                                │
                                ▼
                         Image Generation (Doubao)
                                │
                                ▼
                         Split Viewer (select pose)
                                │
                                ▼
                         Reference Overlay ──→ Photo Capture
```

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Architecture** | MVVM with StateFlow |
| **Camera** | CameraX |
| **ML** | Google ML Kit (Pose Detection) |
| **Networking** | OkHttp |
| **Image Loading** | Coil |
| **AI - Scene Analysis** | Zhipu AI (GLM-4.5v) |
| **AI - Image Generation** | Doubao (Volcano Engine) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 35 |

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 11+
- Android device/emulator with API 24+

### API Keys Setup

This app requires API keys from:
1. **Zhipu AI** (智谱AI) - For scene analysis
2. **Doubao** (豆包/Volcano Engine) - For image generation

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/jorgen-zhao/posebaby.git
   cd posebaby
   ```

2. **Configure API Keys**
   
   Create or edit `local.properties` in the project root:
   ```properties
   # Android SDK path (auto-generated)
   sdk.dir=/path/to/android/sdk
   
   # API Keys (required)
   ZHIPU_API_KEY=your_zhipu_api_key_here
   DOUBAO_API_KEY=your_doubao_api_key_here
   ```

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click Run.

### Getting API Keys

- **Zhipu AI**: Register at [open.bigmodel.cn](https://open.bigmodel.cn/)
- **Doubao**: Register at [volcengine.com](https://www.volcengine.com/)

## 📄 License

```
Copyright 2025 Jorgen Zhao

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

# 中文说明

## 🎯 项目背景

作为一个男性摄影师，我经常需要给女朋友拍照，但我并不擅长指导她如何摆姿势。我一直在想：

> *"如果有一个AI能像专业摄影师一样站在我身边，教我如何拍摄，那该多好？"*

我搜索了现有的解决方案，但没有找到真正能解决这个问题的应用。所以我决定自己创造一个。

**PoseBaby** 是一款AI驱动的摄影助手，它能分析你的拍摄场景并提供实时的姿势建议——就像有一位专业摄影师在你身边指导你拍摄。

## ✨ 功能特性

### 🖼️ 图片模式
- **场景分析**：AI分析照片的光线、背景和氛围
- **姿势建议**：获得4个创意的、场景匹配的姿势建议
- **参考图生成**：使用豆包AI生成专业参考图
- **网格布局**：支持 1×1、1×2、2×2、3×3 多种布局
- **道具支持**：添加摄影道具（鲜花、书本、雨伞等）
- **分割查看器**：从生成的网格中查看和选择单个姿势
- **拍照功能**：直接从叠加层界面拍照

### 📝 文本模式
- **骨骼叠加**：实时姿势骨骼引导
- **双指缩放**：调整骨骼参考大小
- **姿势匹配**：使用ML Kit进行姿势检测和对齐

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11+
- API 24+ 的Android设备或模拟器

### 配置API密钥

1. 在项目根目录创建或编辑 `local.properties`：
   ```properties
   ZHIPU_API_KEY=你的智谱AI密钥
   DOUBAO_API_KEY=你的豆包API密钥
   ```

2. 构建并运行项目

### 获取API密钥

- **智谱AI**：访问 [open.bigmodel.cn](https://open.bigmodel.cn/) 注册
- **豆包**：访问 [volcengine.com](https://www.volcengine.com/) 注册

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/jorgen-zhao">Jorgen Zhao</a>
</p>
