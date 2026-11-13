package com.zsy.wordmemoryapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据仓库层：唯一负责访问 Room 的地方，向上只暴露异步回调与 LiveData。
 *
 * <p>说明：项目是 Java，无法直接使用 Kotlin 协程，这里用单线程
 * {@link ExecutorService}（等价于 Dispatchers.IO）执行数据库读写，再把结果
 * 切回主线程（等价于 withContext(Dispatchers.Main)），保证不阻塞 UI。</p>
 */
public class WordRepository {

    /** 统一的结果回调，回调在主线程执行。 */
    public interface Callback<T> {
        void onResult(T result);
    }

    private static volatile WordRepository INSTANCE;

    private final WordDao wordDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WordRepository(WordDao wordDao) {
        this.wordDao = wordDao;
    }

    public static WordRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (WordRepository.class) {
                if (INSTANCE == null) {
                    WordDao dao = AppDatabase.getInstance(context.getApplicationContext()).wordDao();
                    INSTANCE = new WordRepository(dao);
                }
            }
        }
        return INSTANCE;
    }

    // ==================== 单词：增 ====================

    /** 新增单词，同时生成一条立即到期的首次复习记录。回调参数为单词 id，失败为 -1。 */
    public void insertWordWithFirstReview(@NonNull Word word, Callback<Long> callback) {
        ioExecutor.execute(() -> {
            long wordId = -1;
            try {
                wordId = wordDao.insertWord(word);
                if (wordId > 0) {
                    // 新单词先排进复习队列：立即可复习
                    wordDao.insertReviewRecord(
                            ReviewRecord.pending(wordId, 0, System.currentTimeMillis()));
                }
            } catch (Exception e) {
                wordId = -1;
            }
            final long result = wordId;
            postToMain(() -> callback.onResult(result));
        });
    }

    /** 只插入单词，不生成复习记录。 */
    public void insertWord(@NonNull Word word, Callback<Long> callback) {
        ioExecutor.execute(() -> {
            final long id = wordDao.insertWord(word);
            postToMain(() -> callback.onResult(id));
        });
    }

    // ==================== 单词：删 ====================

    /** 删除单词，关联的复习记录由外键级联删除。 */
    public void deleteWord(@NonNull Word word, Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            wordDao.deleteWord(word);
            postToMain(() -> callback.onResult(1));
        });
    }

    public void deleteWordById(long wordId, Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            wordDao.deleteWordById(wordId);
            postToMain(() -> callback.onResult(1));
        });
    }

    // ==================== 单词：改 ====================

    /** 更新单词（释义、难度、记忆状态等）。 */
    public void updateWord(@NonNull Word word, Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            wordDao.updateWord(word);
            postToMain(() -> callback.onResult(1));
        });
    }

    public void updateMemoryState(long wordId, int memoryState, Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            wordDao.updateMemoryState(wordId, memoryState);
            postToMain(() -> callback.onResult(1));
        });
    }

    // ==================== 单词：查 ====================

    /** 观察全部单词：Room 在数据变化时自动重新查询并推送，不需要手动刷新。 */
    public LiveData<List<Word>> observeAllWords() {
        return wordDao.observeAllWords();
    }

    public void getWordById(long wordId, Callback<Word> callback) {
        ioExecutor.execute(() -> {
            final Word word = wordDao.getWordById(wordId);
            postToMain(() -> callback.onResult(word));
        });
    }

    public void getWord(String word, Callback<Word> callback) {
        ioExecutor.execute(() -> {
            final Word result = wordDao.getWord(word);
            postToMain(() -> callback.onResult(result));
        });
    }

    public void countWords(Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            final int count = wordDao.countWords();
            postToMain(() -> callback.onResult(count));
        });
    }

    // ==================== 待复习单词 ====================

    /** 查询当前时刻所有到期的待复习单词。 */
    public void getDueWords(long now, Callback<List<Word>> callback) {
        ioExecutor.execute(() -> {
            final List<Word> dueWords = wordDao.getDueWords(now);
            postToMain(() -> callback.onResult(dueWords));
        });
    }

    public void countDueWords(long now, Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            final int count = wordDao.countDueWords(now);
            postToMain(() -> callback.onResult(count));
        });
    }

    // ==================== 复习记录 ====================

    public void insertReviewRecord(@NonNull ReviewRecord record, Callback<Long> callback) {
        ioExecutor.execute(() -> {
            final long id = wordDao.insertReviewRecord(record);
            postToMain(() -> callback.onResult(id));
        });
    }

    public void getReviewRecordsForWord(long wordId, Callback<List<ReviewRecord>> callback) {
        ioExecutor.execute(() -> {
            final List<ReviewRecord> records = wordDao.getReviewRecordsForWord(wordId);
            postToMain(() -> callback.onResult(records));
        });
    }

    public void updateReviewRecord(@NonNull ReviewRecord record, Callback<Integer> callback) {
        ioExecutor.execute(() -> {
            wordDao.updateReviewRecord(record);
            postToMain(() -> callback.onResult(1));
        });
    }

    /** 今天需要复习的单词（含逾期），用于生成当日复习队列。 */
    public void getWordsByIds(List<Long> ids, Callback<List<Word>> callback) {
        ioExecutor.execute(() -> {
            final List<Word> words = wordDao.getWordsByIds(ids);
            postToMain(() -> callback.onResult(words));
        });
    }

    /** 所有待复习记录，用于艾宾浩斯算法生成队列。 */
    public void getPendingReviewRecords(Callback<List<ReviewRecord>> callback) {
        ioExecutor.execute(() -> {
            final List<ReviewRecord> records = wordDao.getPendingReviewRecords();
            postToMain(() -> callback.onResult(records));
        });
    }

    private void postToMain(Runnable action) {
        mainHandler.post(action);
    }
}
