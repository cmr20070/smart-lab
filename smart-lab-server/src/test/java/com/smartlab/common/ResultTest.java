package com.smartlab.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_returnsCode200() {
        Result<String> r = Result.success("data");
        assertEquals(200, r.getCode());
        assertEquals("操作成功", r.getMessage());
        assertEquals("data", r.getData());
    }

    @Test
    void success_noArg_returnsNullData() {
        Result<Void> r = Result.success();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void error_returnsCode500ByDefault() {
        Result<Void> r = Result.error("something wrong");
        assertEquals(500, r.getCode());
        assertEquals("something wrong", r.getMessage());
    }

    @Test
    void error_withCode_returnsGivenCode() {
        Result<Void> r = Result.error(401, "未登录");
        assertEquals(401, r.getCode());
        assertEquals("未登录", r.getMessage());
    }
}