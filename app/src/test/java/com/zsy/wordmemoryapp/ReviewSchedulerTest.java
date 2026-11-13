package com.zsy.wordmemoryapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 艾宾浩斯算法单元测试：不依赖 Android，直接跑在 JVM 上。
 */
public class ReviewSchedulerTest {

    private static final long MINUTE = 60 * 1000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long NOW = 1_700_000_000_000L;

    @Test
    public void intervals_followEbbinghausCurve() {
        assertEquals(5 * MINUTE, ReviewScheduler.intervalOf(0));
        assertEquals(30 * MINUTE, ReviewScheduler.intervalOf(1));
        assertEquals(12 * HOUR, ReviewScheduler.intervalOf(2));
        assertEquals(DAY, ReviewScheduler.intervalOf(3));
        assertEquals(2 * DAY, ReviewScheduler.intervalOf(4));
        assertEquals(4 * DAY, ReviewScheduler.intervalOf(5));
        assertEquals(7 * DAY, ReviewScheduler.intervalOf(6));
        assertEquals(15 * DAY, ReviewScheduler.intervalOf(7));
        // 超出范围时取最后一段
        assertEquals(15 * DAY, ReviewScheduler.intervalOf(99));
    }

    @Test
    public void remember_advancesStage_andUsesLongerInterval() {
        assertEquals(1, ReviewScheduler.nextStage(0, ReviewRecord.RESULT_KNOWN));
        assertEquals(NOW + 30 * MINUTE,
                ReviewScheduler.nextReviewTime(0, ReviewRecord.RESULT_KNOWN, NOW));
    }

    @Test
    public void forget_resetsToFirstStage_andSchedulesInFiveMinutes() {
        assertEquals(0, ReviewScheduler.nextStage(5, ReviewRecord.RESULT_FORGOT));
        assertEquals(NOW + 5 * MINUTE,
                ReviewScheduler.nextReviewTime(5, ReviewRecord.RESULT_FORGOT, NOW));
    }

    @Test
    public void vague_keepsCurrentStage() {
        assertEquals(3, ReviewScheduler.nextStage(3, ReviewRecord.RESULT_VAGUE));
        assertEquals(NOW + ReviewScheduler.intervalOf(3),
                ReviewScheduler.nextReviewTime(3, ReviewRecord.RESULT_VAGUE, NOW));
    }

    @Test
    public void stage_isCappedAtLastStage() {
        assertEquals(ReviewScheduler.MAX_STAGE,
                ReviewScheduler.nextStage(ReviewScheduler.MAX_STAGE, ReviewRecord.RESULT_KNOWN));
        assertTrue(ReviewScheduler.isMasteredStage(ReviewScheduler.MAX_STAGE));
        assertFalse(ReviewScheduler.isMasteredStage(ReviewScheduler.MAX_STAGE - 1));
    }

    @Test
    public void isDue_includesOverdue_andExactDueTime() {
        assertTrue(ReviewScheduler.isDue(NOW - 1, NOW));
        assertTrue(ReviewScheduler.isDue(NOW, NOW));
        assertFalse(ReviewScheduler.isDue(NOW + 1, NOW));
    }

    @Test
    public void dailyQueue_keepsOnlyPendingDueRecords_andSortsByTime() {
        List<ReviewRecord> records = new ArrayList<>();
        records.add(ReviewRecord.pending(2, 1, NOW - 1_000));        // 刚到期
        records.add(ReviewRecord.pending(1, 0, NOW - 60 * MINUTE));  // 逾期
        records.add(ReviewRecord.pending(3, 0, NOW + 3 * DAY));      // 未来，不应出现

        ReviewRecord reviewed = ReviewRecord.pending(4, 0, NOW - MINUTE);
        reviewed.setReviewResult(ReviewRecord.RESULT_KNOWN);         // 已复习，不应出现
        records.add(reviewed);

        records.add(ReviewRecord.pending(5, 2, NOW));                // 正好到期

        List<ReviewRecord> queue = ReviewScheduler.buildDailyQueue(records, NOW);

        assertEquals(3, queue.size());
        // 按到期时间升序：逾期 -> 刚到期 -> 正好到期
        assertEquals(1, queue.get(0).getWordId());
        assertEquals(2, queue.get(1).getWordId());
        assertEquals(5, queue.get(2).getWordId());
    }

    @Test
    public void dailyQueue_emptyWhenNothingDue() {
        List<ReviewRecord> records = new ArrayList<>();
        records.add(ReviewRecord.pending(1, 0, NOW + 1));
        assertTrue(ReviewScheduler.buildDailyQueue(records, NOW).isEmpty());
        assertTrue(ReviewScheduler.buildDailyQueue(null, NOW).isEmpty());
    }
}
