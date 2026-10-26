package com.smartlab.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void of_setsAllFields() {
        List<String> records = List.of("a", "b");
        PageResult<String> pr = PageResult.of(100L, 2, 10, records);
        assertEquals(100L, pr.getTotal());
        assertEquals(2L, pr.getPage());
        assertEquals(10L, pr.getSize());
        assertEquals(records, pr.getRecords());
    }
}