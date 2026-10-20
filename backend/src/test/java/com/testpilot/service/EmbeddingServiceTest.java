package com.testpilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 向量序列化 / 反序列化 / 余弦相似度单测（纯算法，不依赖 Spring 与数据库）。
 */
class EmbeddingServiceTest {

    @Test
    void serializeProducesFourByteAlignedLittleEndian() {
        float[] vector = new float[]{1.0f, -2.5f, 0.0f};
        byte[] bytes = EmbeddingService.serialize(vector);
        assertEquals(12, bytes.length);
        assertEquals(0, bytes.length % 4);
        // 小端断言：1.0f 的 IEEE-754 为 0x3F800000，小端字节序 00 00 80 3F
        assertEquals(0x00, bytes[0] & 0xFF);
        assertEquals(0x00, bytes[1] & 0xFF);
        assertEquals(0x80, bytes[2] & 0xFF);
        assertEquals(0x3F, bytes[3] & 0xFF);
    }

    @Test
    void roundtripKeepsValuesWithinFloatPrecision() {
        float[] vector = new float[]{0.12f, -3.14159f, 100000.5f, -0.0001f};
        float[] back = EmbeddingService.deserialize(EmbeddingService.serialize(vector));
        assertArrayEquals(vector, back);
    }

    @Test
    void deserializeRejectsMisalignedOrEmptyBytes() {
        assertNull(EmbeddingService.deserialize(null));
        assertNull(EmbeddingService.deserialize(new byte[0]));
        assertNull(EmbeddingService.deserialize(new byte[6]));
    }

    @Test
    void cosineOfIdenticalVectorIsOne() {
        float[] v = new float[]{0.3f, 0.4f, 0.5f};
        assertEquals(1.0, EmbeddingService.cosine(v, v), 1e-9);
    }

    @Test
    void cosineOfKnownVectors() {
        // 正交向量 → 0；反向向量 → -1
        assertEquals(0.0, EmbeddingService.cosine(new float[]{1f, 0f}, new float[]{0f, 1f}), 1e-9);
        assertEquals(-1.0, EmbeddingService.cosine(new float[]{1f, 0f}, new float[]{-2f, 0f}), 1e-9);
        // [1,2,3] 与 [4,5,6]：dot=32，norm=√14·√77 → 约 0.9746
        assertEquals(0.9746, EmbeddingService.cosine(new float[]{1f, 2f, 3f}, new float[]{4f, 5f, 6f}), 1e-3);
    }

    @Test
    void cosineGuardsZeroVectorAndDimensionMismatch() {
        assertEquals(0.0, EmbeddingService.cosine(new float[]{0f, 0f}, new float[]{1f, 1f}), 1e-9);
        assertEquals(0.0, EmbeddingService.cosine(new float[]{1f, 2f}, new float[]{1f}), 1e-9);
        assertEquals(0.0, EmbeddingService.cosine(null, new float[]{1f}), 1e-9);
    }
}
