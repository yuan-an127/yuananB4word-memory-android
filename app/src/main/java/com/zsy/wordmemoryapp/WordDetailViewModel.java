package com.zsy.wordmemoryapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 单词详情页的 ViewModel：负责按 id 读取单词，以及新增 / 编辑 / 删除的持久化。
 */
public class WordDetailViewModel extends AndroidViewModel {

    private final WordRepository repository;
    private final MutableLiveData<Word> word = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    /** 保存或删除成功后置为 true，界面据此返回列表。 */
    private final MutableLiveData<Boolean> done = new MutableLiveData<>();

    public WordDetailViewModel(@NonNull Application application) {
        super(application);
        repository = WordRepository.getInstance(application);
    }

    public LiveData<Word> getWord() {
        return word;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getDone() {
        return done;
    }

    /** 编辑模式：按 id 读取已有单词。 */
    public void load(long wordId) {
        if (wordId <= 0) {
            return;
        }
        repository.getWordById(wordId, loaded -> word.setValue(loaded));
    }

    /** 保存：wordId > 0 为更新，否则为新增（同时生成首条复习记录）。 */
    public void save(long wordId, String wordText, String meaning, int difficulty, int memoryState) {
        Word newWord = new Word(wordText, meaning, difficulty, memoryState);
        if (wordId > 0) {
            newWord.setId(wordId);
            repository.updateWord(newWord, ignored -> {
                message.setValue("已保存修改：" + wordText);
                done.setValue(true);
            });
        } else {
            repository.insertWordWithFirstReview(newWord, id -> {
                if (id != null && id > 0) {
                    message.setValue("已添加：" + wordText);
                    done.setValue(true);
                } else {
                    message.setValue("保存失败：" + wordText);
                }
            });
        }
    }

    /** 删除单词，关联的复习记录会级联删除。 */
    public void delete(Word word) {
        repository.deleteWord(word, ignored -> {
            message.setValue("已删除：" + word.getWord());
            done.setValue(true);
        });
    }
}
