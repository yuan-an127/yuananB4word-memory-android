package com.zsy.wordmemoryapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.Collections;
import java.util.List;

/**
 * 单词页面的 ViewModel：界面只和它交互，它再调用 Repository，
 * 从而把“视图”和“数据访问”彻底分开；配置变化（旋转屏幕）时数据不丢失。
 */
public class WordViewModel extends AndroidViewModel {

    /** Loading 至少展示这么久，避免本地数据库返回太快导致 Loading 一闪而过。 */
    private static final long MIN_LOADING_MS = 500L;

    private final WordRepository repository;
    private final LiveData<List<Word>> allWords;
    private final MutableLiveData<List<Word>> dueWords =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> message = new MutableLiveData<>();

    /** 列表加载状态：首次从数据库读取完成前为 true。 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final long createdTime = System.currentTimeMillis();
    private final Runnable hideLoading = () -> loading.setValue(false);

    /** 单词表变化后：结束 Loading，并刷新“待复习”数量。 */
    private final Observer<List<Word>> wordsObserver = words -> {
        long remain = MIN_LOADING_MS - (System.currentTimeMillis() - createdTime);
        if (remain > 0) {
            mainHandler.postDelayed(hideLoading, remain);
        } else {
            loading.setValue(false);
        }
        refreshDueWords();
    };

    public WordViewModel(@NonNull Application application) {
        super(application);
        repository = WordRepository.getInstance(application);
        allWords = repository.observeAllWords();
        allWords.observeForever(wordsObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        allWords.removeObserver(wordsObserver);
        mainHandler.removeCallbacks(hideLoading);
    }

    public LiveData<List<Word>> getAllWords() {
        return allWords;
    }

    public LiveData<List<Word>> getDueWords() {
        return dueWords;
    }

    /** 一次性提示消息（保存成功 / 删除成功 / 输入校验）。 */
    public LiveData<String> getMessage() {
        return message;
    }

    /** 列表 Loading 状态：Loading UI 直接观察它。 */
    public LiveData<Boolean> isLoading() {
        return loading;
    }

    // ==================== 增 ====================

    public void addWord(String wordText, String meaning, int difficulty) {
        if (wordText == null || wordText.trim().isEmpty()) {
            message.setValue("请输入单词");
            return;
        }
        String word = wordText.trim();
        String meaningText = meaning == null ? "" : meaning.trim();
        if (meaningText.isEmpty()) {
            meaningText = "（暂无释义）";
        }

        Word newWord = new Word(word, meaningText, difficulty, Word.STATE_LEARNING);
        repository.insertWordWithFirstReview(newWord, wordId -> {
            if (wordId != null && wordId > 0) {
                message.setValue("已保存：" + word);
            } else {
                message.setValue("保存失败：" + word);
            }
        });
    }

    // ==================== 删 ====================

    public void deleteWord(Word word) {
        repository.deleteWord(word, ignored ->
                message.setValue("已删除：" + word.getWord()));
    }

    // ==================== 改 ====================

    /** 在“学习中 / 已掌握”之间切换记忆状态。 */
    public void toggleMastered(Word word) {
        int nextState = word.getMemoryState() == Word.STATE_MASTERED
                ? Word.STATE_LEARNING
                : Word.STATE_MASTERED;
        word.setMemoryState(nextState);
        repository.updateWord(word, ignored ->
                message.setValue(word.getWord() + (nextState == Word.STATE_MASTERED ? " 已标记为掌握" : " 已标记为学习中")));
    }

    // ==================== 查 ====================

    /** 刷新待复习单词列表（由 Repository 在 IO 线程查询）。 */
    public void refreshDueWords() {
        repository.getDueWords(System.currentTimeMillis(), list ->
                dueWords.setValue(list == null ? Collections.emptyList() : list));
    }
}
