package com.zsy.wordmemoryapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 艾宾浩斯遗忘曲线核心算法（自定义业务逻辑，纯 Java，可脱离 Android 单元测试）。
 *
 * <p>规则：</p>
 * <ul>
 *     <li>记住（{@link ReviewRecord#RESULT_KNOWN}）：进入下一阶段，间隔变长；</li>
 *     <li>模糊（{@link ReviewRecord#RESULT_VAGUE}）：停留在当前阶段，按当前间隔再复习一次；</li>
 *     <li>遗忘（{@link ReviewRecord#RESULT_FORGOT}）：回到第一阶段，5 分钟后重新复习。</li>
 * </ul>
 *
 * <p>阶段与间隔（共 8 个阶段，0 开始）：5 分钟、30 分钟、12 小时、1 天、2 天、4 天、7 天、15 天。</p>
 */
public final class ReviewScheduler {

    private static final long MINUTE = 60 * 1000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    /** 艾宾浩斯复习间隔表，下标即复习阶段。 */
    private static final long[] INTERVALS = {
            5 * MINUTE,
            30 * MINUTE,
            12 * HOUR,
            1 * DAY,
            2 * DAY,
            4 * DAY,
            7 * DAY,
            15 * DAY
    };

    /** 最大复习阶段（15 天），到达后视为已掌握。 */
    public static final int MAX_STAGE = INTERVALS.length - 1;

    private ReviewScheduler() {
    }

    /** 指定阶段的复习间隔（毫秒）。 */
    public static long intervalOf(int stage) {
        int index = Math.max(0, Math.min(stage, MAX_STAGE));
        return INTERVALS[index];
    }

    /** 根据本次复习结果计算下一次复习所处的阶段。 */
    public static int nextStage(int currentStage, int result) {
        int stage = Math.max(0, Math.min(currentStage, MAX_STAGE));
        switch (result) {
            case ReviewRecord.RESULT_KNOWN:
                return Math.min(stage + 1, MAX_STAGE);
            case ReviewRecord.RESULT_VAGUE:
                return stage;
            case ReviewRecord.RESULT_FORGOT:
                return 0;
            default:
                return stage;
        }
    }

    /** 根据本次复习结果计算下一次复习时间（毫秒时间戳）。 */
    public static long nextReviewTime(int currentStage, int result, long reviewedAt) {
        return reviewedAt + intervalOf(nextStage(currentStage, result));
    }

    /** 是否已到达最后一个阶段（15 天），可以认为该单词已掌握。 */
    public static boolean isMasteredStage(int stage) {
        return stage >= MAX_STAGE;
    }

    /** 计划复习时间是否已经到达（含此前逾期未复习的）。 */
    public static boolean isDue(long nextReviewTime, long now) {
        return nextReviewTime <= now;
    }

    /**
     * 生成当日复习队列：从待复习记录中筛出“已经到期”的记录（含逾期），按计划时间升序排列。
     * 复习完成后对应记录不再是待复习状态，下一次复习会生成新的待复习记录，因此该词会从当前队列移除。
     */
    public static List<ReviewRecord> buildDailyQueue(List<ReviewRecord> pendingRecords, long now) {
        List<ReviewRecord> queue = new ArrayList<>();
        if (pendingRecords == null) {
            return queue;
        }
        for (ReviewRecord record : pendingRecords) {
            if (record.isPending() && isDue(record.getNextReviewTime(), now)) {
                queue.add(record);
            }
        }
        Collections.sort(queue, (left, right) ->
                Long.compare(left.getNextReviewTime(), right.getNextReviewTime()));
        return queue;
    }
}
