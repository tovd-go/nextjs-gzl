# GodzillaNextJsPayload

本仓库为 Godzilla 提供了 Next.js 的有效载荷、加密器支持

## 项目简介

GodzillaNextJsPayload 是一个为 Godzilla 平台提供的插件项目（基于 Java + Maven 构建）。本仓库以 Maven 为构建工具，生成的 jar 可作为 Godzilla 客户端的插件使用。

**重要提醒**：请在合规、合法并取得目标系统授权的前提下使用本仓库提供的任何代码或插件。

## 功能特性

### Payload
- **NextJsDynamicPayload**: Next.js 平台的动态 Payload，支持文件操作、命令执行等功能

### 加密器
- **JS_AES_WEBSHELL**: AES-128-CBC 加密的文件型 Webshell，适用于 Next.js API Routes
- **JS_AES_MEMSHELL**: AES-128-ECB 加密的内存马，通过 Next.js Server Actions 原型污染注入

### 特性
- ✅ 支持文件型 Webshell 和内存马两种模式
- ✅ 基于 Header 的隐蔽路由（Accept: gzipp）
- ✅ AES 加密通信
- ✅ 自动生成内存马注入 JSON
- ✅ 支持 .js 和 .ts 两种文件格式

## 先决条件

- Java JDK（建议 8 或更高）
- Maven（命令行工具）
- Git（用于克隆仓库）
- Godzilla 客户端（用于加载插件）

## 构建（生成插件 Jar）

在仓库根目录下执行以下命令以生成可供 Godzilla 客户端加载的 jar：

```bash
# 克隆仓库（如尚未克隆）
git clone <repository-url>
cd GodzillaNextJsPayload-master-1

# 使用 Maven 生成 jar
mvn clean package
```

构建完成后，产物通常位于 `target/` 目录下，名称类似于：
```
target/NextJsPayload-1.0-SNAPSHOT.jar
```

## 在 Godzilla 客户端中添加插件

1. 打开 Godzilla 客户端。
2. 进入 **配置**（或 **设置**）→ **插件配置**（或 **Plugin Configuration**）。
3. 点击 **添加/上传** 插件（Add Plugin / Upload）。
4. 选择上一步构建得到的 jar 文件（例如 `target/NextJsPayload-1.0-SNAPSHOT.jar`）。
5. 保存配置并根据 Godzilla 客户端的说明重启或刷新插件加载（如有必要）。

完成后，客户端应能在插件列表中看到并启用该插件。

## 使用方法

### 生成 Webshell

1. 在 Godzilla 中选择 **生成** → **Webshell**
2. 选择 Payload: **NextJsDynamicPayload**
3. 选择加密器: **JS_AES_WEBSHELL** 或 **JS_AES_MEMSHELL**
4. 填写密码和密钥
5. 选择文件后缀（.js 或 .ts）
6. 点击生成

### 部署 Webshell（JS_AES_WEBSHELL）

1. 将生成的 `shell.js` 或 `shell.ts` 文件重命名为 `route.js` 或 `route.ts`
2. 部署到 Next.js 项目的 `app/api/[任意名称]/route.js` 目录
3. 重启 Next.js 服务
4. 在 Godzilla 中连接：
   - URL: `http://target.com/api/[路由名称]`（任意路径都可以）
   - 密码: 生成时填写的密码
   - 加密器: **JS_AES_WEBSHELL**

### 注入内存马（JS_AES_MEMSHELL）

1. 生成时会同时生成 `nextjs_memshell_[password]_[timestamp].json` 文件
2. 找到 Next.js Server Actions 端点（通常在 `/_next/data/` 或表单提交端点）
3. 发送 JSON 文件内容到该端点：
   ```bash
   curl -X POST http://target.com/_next/data/xxx \
     -H "Content-Type: application/json" \
     -d @nextjs_memshell_xxx.json
   ```
4. 在 Godzilla 中连接：
   - URL: `http://target.com/`（任意路径都可以）
   - 密码: 生成时填写的密码
   - 密钥: 生成时填写的密钥
   - 加密器: **JS_AES_MEMSHELL**

## 常见命令（示例）

```bash
# 清理并生成 jar（推荐）
mvn clean package

# 仅编译（不打包）
mvn compile

# 查看 target 目录下的 jar
ls -lh target/*.jar
```

## 项目结构

```
GodzillaNextJsPayload-master-1/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── shells/
        │       ├── payloads/
        │       │   └── nextjs/
        │       │       └── NextJsShell.java
        │       └── cryptions/
        │           └── jsAes/
        │               ├── Generate.java
        │               ├── JsAesWebshell.java
        │               └── JsAesMemShell.java
        └── resources/
            └── shells/
                ├── payloads/
                │   └── nextjs/
                │       └── assets/
                │           └── payload.js
                └── cryptions/
                    └── jsAes/
                        └── template/
                            ├── aesApiGlobalCode.bin
                            ├── aesApiCode.bin
                            ├── base64GlobalCode_memshell.bin
                            ├── shellApi.js
                            └── shellApi.ts
```

## 安全与合规

- 本仓库可能包含供测试或研究用途的功能。请务必仅在得到授权的环境中使用插件。
- 作者与维护者对任何未授权或非法使用本仓库内容导致的问题不承担责任。

## 贡献

欢迎提交 issue 或 PR。如需对 README 或插件功能进行改进，请：
1. Fork 本仓库
2. 新建分支并提交改动
3. 发起 Pull Request，描述改动内容与目的

## 许可证

请参考项目根目录的 LICENSE 文件（如有）。
