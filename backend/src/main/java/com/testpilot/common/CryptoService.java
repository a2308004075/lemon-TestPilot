package com.testpilot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 本机加密（AES-256-GCM + 主密钥文件）。
 * 主密钥存放于 data/keys/master.key，不入库不入 Git；换机后密钥文件不同则历史密文不可解，
 * 与原版 DPAPI / 钥匙串方案保持一致的安全语义。
 * 密文格式：enc:Base64(iv + ciphertext + tag)；读取时兼容存量明文（无前缀原样返回）。
 */
@Service
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    private static final String PREFIX = "enc:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final Path keyFile = Paths.get("data", "keys", "master.key");
    private SecretKey masterKey;

    @PostConstruct
    public void init() {
        try {
            if (Files.exists(keyFile)) {
                byte[] raw = Base64.getDecoder().decode(new String(Files.readAllBytes(keyFile),
                        StandardCharsets.UTF_8).trim());
                masterKey = new SecretKeySpec(raw, "AES");
                log.info("已加载本机主密钥：{}", keyFile.toAbsolutePath());
            } else {
                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(256);
                masterKey = generator.generateKey();
                Files.createDirectories(keyFile.getParent());
                Files.write(keyFile, Base64.getEncoder().encode(masterKey.getEncoded()));
                log.info("首次启动已生成本机主密钥：{}", keyFile.toAbsolutePath());
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化加密主密钥失败：" + e.getMessage(), e);
        }
    }

    /** 加密为 enc: 密文；空值原样返回。 */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败：" + e.getMessage(), e);
        }
    }

    /** 解密 enc: 密文；无前缀视为存量明文原样返回。 */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败（主密钥文件不匹配？）：" + e.getMessage(), e);
        }
    }
}
