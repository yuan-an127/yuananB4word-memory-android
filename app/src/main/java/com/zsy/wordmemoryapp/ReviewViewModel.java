package com.zsy.wordmemoryapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 复习页 ViewModel：生成当日复习队列，并根据「记住 / 忘记」调用艾宾浩斯算法安排下次复习。
 */
public class ReviewViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final MutableLiveData<List<Word>> queue =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public ReviewViewModel(@NonNull Application application) {
        super(application);
        repository = WordRepository.getInstance(application);
    }

    public LiveData<List<Word>> getQueue() {
        return queue;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    /** 生成当日复习队列：先取待复习记录，再用 {@link ReviewScheduler} 算法筛出已到期的。 */
    public void loadQueue() {
        final long now = System.currentTimeMillis();
        repository.getPendingReviewRecords(records -> {
            List<ReviewRecord> dueRecords = ReviewScheduler.buildDailyQueue(records, now);
            if (dueRecords.isEmpty()) {
                loading.setValue(false);
                queue.setValue(Collections.emptyList());
                return;
            }
            final Map<Long, Long> dueTimeByWord = new HashMap<>();
            List<Long> wordIds = new ArrayList<>();
            for (ReviewRecord record : dueRecords) {
                wordIds.add(record.getWordId());
                dueTimeByWord.put(record.getWordId(), record.getNextReviewTime());
            }
            repository.getWordsByIds(wordIds, words -> {
                List<Word> ordered = new ArrayList<>();
                if (words != null) {
                    Collections.sort(words, (left, right) -> Long.compare(
                            dueTimeByWord.get(left.getId()), dueTimeByWord.get(right.getId())));
                    ordered.addAll(words);
                }
                loading.setValue(false);
                queue.setValue(ordered);
            });
        });
    }

    /**
     * 记录一次复习结果：更新当前待复习记录，并按艾宾浩斯算法生成下一次复习记录。
     *
     * @param result {@link ReviewRecord#RESULT_KNOWN} 或 {@link ReviewRecord#RESULT_FORGOT}
     */
    public void review(Word word, int result) {
        repository.getReviewRecordsForWord(word.getId(), records -> {
            ReviewRecord pending = findPending(records);
            if (pending == null) {
                message.setValue("「" + word.getWord() + "」没有待复习记录");
                loadQueue();
                return;
            }

            long now = System.currentTimeMillis();
            int oldStage = pending.getReviewStage();
            int newStage = ReviewScheduler.nextStage(oldStage, result);
            long nextTime = ReviewScheduler.nextReviewTime(oldStage, result, now);

            // 1. 本次复习结果写回历史记录
            pending.setReviewResult(result);
            pending.setReviewedAt(now);
            repository.updateReviewRecord(pending, ignored ->
                    // 2. 按算法生成下一次待复习记录
                    repository.insertReviewRecord(ReviewRecord.pending(word.getId(), newStage, nextTime), recordId -> {
                        String label = result == ReviewRecord.RESULT_KNOWN ? "记住了" : "忘记了";
                        message.setValue("「" + word.getWord() + "」" + label
                                + "，下次复习：" + formatTime(nextTime));
                        // 3. 到达最后阶段视为已掌握
                        if (ReviewScheduler.isMasteredStage(newStage)) {
                            word.setMemoryState(Word.STATE_MASTERED);
                            repository.updateWord(word, updated -> { });
                        }
                        loadQueue();
                    }));
        });
    }

    private ReviewRecord findPending(List<ReviewRecord> records) {
        if (records == null) {
            return null;
        }
        for (ReviewRecord record : records) {
            if (record.isPending()) {
                return record;
            }
        }
        return null;
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(time));
    }
}
