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
  <a href="README-zh.md">中文说明</a>
</p>

---

## 🎯 The Problem

As a man trying to take photos for my girlfriend in the pretty sight, I always struggled with posing guidance. I'm not a professional photographer, and I often wondered:

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

[![Demo Video](demo/1.jpg)](demo/demo.mp4)
> *Click the image above to view the demo video*

### 📸 Screenshots

<p align="center">
  <img src="demo/1.jpg" width="19%" />
  <img src="demo/2.jpg" width="19%" />
  <img src="demo/3.jpg" width="19%" />
  <img src="demo/4.jpg" width="19%" />
  <img src="demo/5.jpg" width="19%" />
</p>
<p align="center">
  <img src="demo/6.jpg" width="19%" />
  <img src="demo/7.jpg" width="19%" />
  <img src="demo/8.jpg" width="19%" />
  <img src="demo/9.jpg" width="19%" />
  <img src="demo/10.jpg" width="19%" />
</p>

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

<p align="center">
  Made with ❤️ by <a href="https://github.com/jorgen-zhao">Jorgen Zhao</a>
</p>
