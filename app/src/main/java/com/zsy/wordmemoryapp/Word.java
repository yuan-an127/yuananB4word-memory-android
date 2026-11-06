package com.zsy.wordmemoryapp;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 单词实体：记录单词本身、释义、难度和记忆状态，是复习计划的数据基础。
 */
@Entity(tableName = "word_table", indices = {@Index(value = {"word"}, unique = true)})
public class Word {

    /** 记忆状态：未学习 */
    public static final int STATE_UNLEARNED = 0;
    /** 记忆状态：学习中 */
    public static final int STATE_LEARNING = 1;
    /** 记忆状态：已掌握 */
    public static final int STATE_MASTERED = 2;

    /** 难度取值范围 */
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 5;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    /** 单词 */
    @NonNull
    @ColumnInfo(name = "word")
    private String word = "";

    /** 释义 */
    @NonNull
    @ColumnInfo(name = "meaning")
    private String meaning = "";

    /** 难度：1（最简单）~ 5（最难） */
    @ColumnInfo(name = "difficulty")
    private int difficulty = MIN_DIFFICULTY;

    /** 记忆状态：{@link #STATE_UNLEARNED} / {@link #STATE_LEARNING} / {@link #STATE_MASTERED} */
    @ColumnInfo(name = "memory_state")
    private int memoryState = STATE_UNLEARNED;

    @Ignore
    public Word() {
    }

    public Word(@NonNull String word, @NonNull String meaning, int difficulty, int memoryState) {
        this.word = word;
        this.meaning = meaning;
        setDifficulty(difficulty);
        setMemoryState(memoryState);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getWord() {
        return word;
    }

    public void setWord(@NonNull String word) {
        this.word = word;
    }

    @NonNull
    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(@NonNull String meaning) {
        this.meaning = meaning;
    }

    public int getDifficulty() {
        return difficulty;
    }

    /** 难度限制在 1~5，避免脏数据。 */
    public void setDifficulty(int difficulty) {
        this.difficulty = Math.max(MIN_DIFFICULTY, Math.min(MAX_DIFFICULTY, difficulty));
    }

    public int getMemoryState() {
        return memoryState;
    }

    public void setMemoryState(int memoryState) {
        this.memoryState = memoryState;
    }

    public boolean isMastered() {
        return memoryState == STATE_MASTERED;
    }

    @NonNull
    @Override
    public String toString() {
        return "Word{id=" + id + ", word='" + word + "', meaning='" + meaning
                + "', difficulty=" + difficulty + ", memoryState=" + memoryState + '}';
    }
}
