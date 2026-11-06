package com.zsy.wordmemoryapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Room DAO：单词的增删改查 + 待复习单词查询 + 复习记录读写。
 */
@Dao
public interface WordDao {

    // ==================== 单词：增 ====================

    /** 插入单词，单词已存在时覆盖。返回自增主键。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertWord(Word word);

    /** 批量插入单词。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertWords(List<Word> words);

    // ==================== 单词：删 ====================

    @Delete
    void deleteWord(Word word);

    @Query("DELETE FROM word_table WHERE id = :wordId")
    void deleteWordById(long wordId);

    @Query("DELETE FROM word_table")
    void deleteAllWords();

    // ==================== 单词：改 ====================

    @Update
    void updateWord(Word word);

    @Query("UPDATE word_table SET memory_state = :state WHERE id = :wordId")
    void updateMemoryState(long wordId, int state);

    // ==================== 单词：查 ====================

    @Query("SELECT * FROM word_table WHERE id = :wordId")
    Word getWordById(long wordId);

    @Query("SELECT * FROM word_table WHERE word = :word")
    Word getWord(String word);

    @Query("SELECT * FROM word_table ORDER BY id DESC")
    List<Word> getAllWords();

    @Query("SELECT * FROM word_table WHERE memory_state = :state ORDER BY id DESC")
    List<Word> getWordsByMemoryState(int state);

    @Query("SELECT COUNT(*) FROM word_table")
    int countWords();

    // ==================== 复习记录 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertReviewRecord(ReviewRecord record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertReviewRecords(List<ReviewRecord> records);

    @Update
    void updateReviewRecord(ReviewRecord record);

    @Delete
    void deleteReviewRecord(ReviewRecord record);

    @Query("DELETE FROM review_record_table")
    void deleteAllReviewRecords();

    @Query("SELECT * FROM review_record_table WHERE word_id = :wordId ORDER BY next_review_time ASC")
    List<ReviewRecord> getReviewRecordsForWord(long wordId);

    /** 待复习单词的最近一次到期时间。 */
    @Query("SELECT MIN(next_review_time) FROM review_record_table WHERE word_id = :wordId AND review_result = 0")
    Long getNextReviewTime(long wordId);

    // ==================== 待复习单词 ====================

    /**
     * 查询待复习单词：存在“待复习”记录且计划复习时间已经到达。
     * 按到期时间从早到晚排序，保证最该复习的单词排在最前面。
     */
    @Query("SELECT w.* FROM word_table w INNER JOIN ("
            + "SELECT word_id, MIN(next_review_time) AS due_time FROM review_record_table "
            + "WHERE review_result = 0 AND next_review_time <= :now GROUP BY word_id"
            + ") d ON w.id = d.word_id ORDER BY d.due_time ASC")
    List<Word> getDueWords(long now);

    /** 待复习单词数量。 */
    @Query("SELECT COUNT(DISTINCT word_id) FROM review_record_table "
            + "WHERE review_result = 0 AND next_review_time <= :now")
    int countDueWords(long now);
}
