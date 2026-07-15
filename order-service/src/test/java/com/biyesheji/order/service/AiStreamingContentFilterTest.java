package com.biyesheji.order.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiStreamingContentFilterTest {
    @Test
    void filtersKeywordSplitAcrossStreamChunks() {
        AiStreamingContentFilter filter = new AiStreamingContentFilter("虚假承诺, phishing");

        String result = filter.accept("这是虚假") + filter.accept("承诺，请勿 PHISH")
                + filter.accept("ING") + filter.finish();

        assertEquals("这是[内容已过滤]，请勿 [内容已过滤]", result);
    }

    @Test
    void leavesOutputUntouchedWithoutConfiguredKeywords() {
        AiStreamingContentFilter filter = new AiStreamingContentFilter(null);

        assertEquals("正常回答", filter.accept("正常回答") + filter.finish());
    }
}
