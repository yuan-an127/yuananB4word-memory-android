package com.zsy.wordmemoryapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * 应用数据库：Word（单词） + ReviewRecord（复习记录），并给出迁移的基础配置。
 */
@Database(entities = {Word.class, ReviewRecord.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DB_NAME = "word_db";
    private static volatile AppDatabase INSTANCE;

    public abstract WordDao wordDao();

    /**
     * v1 -> v2：Word 表补充“难度 / 记忆状态”字段（v1 只有 id/word/meaning），
     * 同时新增复习记录表，为艾宾浩斯复习计划做准备。
     *
     * <p>说明：v1 版本中 Word 还没有声明为 Entity，实际不会生成 word_table，
     * 这里在迁移里做兜底建表，保证升级路径可用。</p>
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_table` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`word` TEXT NOT NULL, "
                    + "`meaning` TEXT NOT NULL, "
                    + "`difficulty` INTEGER NOT NULL DEFAULT 1, "
                    + "`memory_state` INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_word_table_word` ON `word_table` (`word`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `review_record_table` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`word_id` INTEGER NOT NULL, "
                    + "`review_stage` INTEGER NOT NULL, "
                    + "`next_review_time` INTEGER NOT NULL, "
                    + "`review_result` INTEGER NOT NULL, "
                    + "`reviewed_at` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`word_id`) REFERENCES `word_table`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_record_table_word_id` "
                    + "ON `review_record_table` (`word_id`)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DB_NAME)
                            // 迁移基础配置：已声明的迁移走 migrate，未声明的版本兜底重建
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
