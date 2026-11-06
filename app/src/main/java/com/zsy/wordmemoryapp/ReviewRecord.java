package com.zsy.wordmemoryapp;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 复习记录实体：按艾宾浩斯遗忘曲线记录每个单词的复习时间与复习结果。
 */
@Entity(tableName = "review_record_table",
        foreignKeys = @ForeignKey(entity = Word.class,
                parentColumns = "id",
                childColumns = "word_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"word_id"})})
public class ReviewRecord {

    /** 复习结果：待复习（尚未产生结果） */
    public static final int RESULT_PENDING = 0;
    /** 复习结果：忘记了 */
    public static final int RESULT_FORGOT = 1;
    /** 复习结果：模糊 */
    public static final int RESULT_VAGUE = 2;
    /** 复习结果：记得 */
    public static final int RESULT_KNOWN = 3;

    /** 艾宾浩斯复习间隔：5 分钟、30 分钟、12 小时、1 天、2 天、4 天、7 天、15 天（毫秒）。 */
    private static final long[] EBBINGHAUS_INTERVALS = {
            5 * 60 * 1000L,
            30 * 60 * 1000L,
            12 * 60 * 60 * 1000L,
            24 * 60 * 60 * 1000L,
            2 * 24 * 60 * 60 * 1000L,
            4 * 24 * 60 * 60 * 1000L,
            7 * 24 * 60 * 60 * 1000L,
            15 * 24 * 60 * 60 * 1000L
    };

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    /** 关联的单词 id（外键，单词被删除时复习记录级联删除） */
    @ColumnInfo(name = "word_id")
    private long wordId;

    /** 艾宾浩斯复习阶段：0 表示第 1 次复习，依次递增 */
    @ColumnInfo(name = "review_stage")
    private int reviewStage;

    /** 计划复习时间（毫秒时间戳），到点后该单词进入“待复习”列表 */
    @ColumnInfo(name = "next_review_time")
    private long nextReviewTime;

    /** 复习结果：{@link #RESULT_PENDING} / {@link #RESULT_FORGOT} / {@link #RESULT_VAGUE} / {@link #RESULT_KNOWN} */
    @ColumnInfo(name = "review_result")
    private int reviewResult = RESULT_PENDING;

    /** 实际复习时间（毫秒时间戳），0 表示还没复习 */
    @ColumnInfo(name = "reviewed_at")
    private long reviewedAt;

    @Ignore
    public ReviewRecord() {
    }

    public ReviewRecord(long wordId, int reviewStage, long nextReviewTime, int reviewResult, long reviewedAt) {
        this.wordId = wordId;
        this.reviewStage = reviewStage;
        this.nextReviewTime = nextReviewTime;
        this.reviewResult = reviewResult;
        this.reviewedAt = reviewedAt;
    }

    /** 新建一条待复习记录。 */
    public static ReviewRecord pending(long wordId, int reviewStage, long nextReviewTime) {
        return new ReviewRecord(wordId, reviewStage, nextReviewTime, RESULT_PENDING, 0L);
    }

    /** 根据阶段获取艾宾浩斯复习间隔（毫秒），超出阶段后按最后一个间隔循环。 */
    public static long intervalOf(int stage) {
        int index = Math.max(0, Math.min(stage, EBBINGHAUS_INTERVALS.length - 1));
        return EBBINGHAUS_INTERVALS[index];
    }

    /** 当前阶段复习完成后，下一次复习应该安排的时间。 */
    public long nextTimeAfterReview(long reviewedAtMillis) {
        return reviewedAtMillis + intervalOf(reviewStage + 1);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getWordId() {
        return wordId;
    }

    public void setWordId(long wordId) {
        this.wordId = wordId;
    }

    public int getReviewStage() {
        return reviewStage;
    }

    public void setReviewStage(int reviewStage) {
        this.reviewStage = reviewStage;
    }

    public long getNextReviewTime() {
        return nextReviewTime;
    }

    public void setNextReviewTime(long nextReviewTime) {
        this.nextReviewTime = nextReviewTime;
    }

    public int getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(int reviewResult) {
        this.reviewResult = reviewResult;
    }

    public long getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(long reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public boolean isPending() {
        return reviewResult == RESULT_PENDING;
    }

    @NonNull
    @Override
    public String toString() {
        return "ReviewRecord{id=" + id + ", wordId=" + wordId + ", stage=" + reviewStage
                + ", nextReviewTime=" + nextReviewTime + ", result=" + reviewResult
                + ", reviewedAt=" + reviewedAt + '}';
    }
}
