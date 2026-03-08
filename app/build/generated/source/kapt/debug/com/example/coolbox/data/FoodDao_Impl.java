package com.example.coolbox.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.lifecycle.LiveData;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@SuppressWarnings({"unchecked", "deprecation"})
public final class FoodDao_Impl implements FoodDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FoodEntity> __insertionAdapterOfFoodEntity;

  private final EntityDeletionOrUpdateAdapter<FoodEntity> __deletionAdapterOfFoodEntity;

  private final EntityDeletionOrUpdateAdapter<FoodEntity> __updateAdapterOfFoodEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateCategory;

  private final SharedSQLiteStatement __preparedStmtOfSoftDelete;

  public FoodDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFoodEntity = new EntityInsertionAdapter<FoodEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `food_items` (`id`,`icon`,`name`,`fridgeName`,`inputDateMs`,`expiryDateMs`,`quantity`,`weightPerPortion`,`portions`,`category`,`unit`,`remark`,`lastModifiedMs`,`isDeleted`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, FoodEntity value) {
        if (value.getId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getId());
        }
        if (value.getIcon() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getIcon());
        }
        if (value.getName() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getName());
        }
        if (value.getFridgeName() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getFridgeName());
        }
        stmt.bindLong(5, value.getInputDateMs());
        stmt.bindLong(6, value.getExpiryDateMs());
        stmt.bindDouble(7, value.getQuantity());
        stmt.bindDouble(8, value.getWeightPerPortion());
        stmt.bindLong(9, value.getPortions());
        if (value.getCategory() == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.getCategory());
        }
        if (value.getUnit() == null) {
          stmt.bindNull(11);
        } else {
          stmt.bindString(11, value.getUnit());
        }
        if (value.getRemark() == null) {
          stmt.bindNull(12);
        } else {
          stmt.bindString(12, value.getRemark());
        }
        stmt.bindLong(13, value.getLastModifiedMs());
        final int _tmp = value.isDeleted() ? 1 : 0;
        stmt.bindLong(14, _tmp);
      }
    };
    this.__deletionAdapterOfFoodEntity = new EntityDeletionOrUpdateAdapter<FoodEntity>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `food_items` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, FoodEntity value) {
        if (value.getId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getId());
        }
      }
    };
    this.__updateAdapterOfFoodEntity = new EntityDeletionOrUpdateAdapter<FoodEntity>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `food_items` SET `id` = ?,`icon` = ?,`name` = ?,`fridgeName` = ?,`inputDateMs` = ?,`expiryDateMs` = ?,`quantity` = ?,`weightPerPortion` = ?,`portions` = ?,`category` = ?,`unit` = ?,`remark` = ?,`lastModifiedMs` = ?,`isDeleted` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, FoodEntity value) {
        if (value.getId() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getId());
        }
        if (value.getIcon() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getIcon());
        }
        if (value.getName() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getName());
        }
        if (value.getFridgeName() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getFridgeName());
        }
        stmt.bindLong(5, value.getInputDateMs());
        stmt.bindLong(6, value.getExpiryDateMs());
        stmt.bindDouble(7, value.getQuantity());
        stmt.bindDouble(8, value.getWeightPerPortion());
        stmt.bindLong(9, value.getPortions());
        if (value.getCategory() == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.getCategory());
        }
        if (value.getUnit() == null) {
          stmt.bindNull(11);
        } else {
          stmt.bindString(11, value.getUnit());
        }
        if (value.getRemark() == null) {
          stmt.bindNull(12);
        } else {
          stmt.bindString(12, value.getRemark());
        }
        stmt.bindLong(13, value.getLastModifiedMs());
        final int _tmp = value.isDeleted() ? 1 : 0;
        stmt.bindLong(14, _tmp);
        if (value.getId() == null) {
          stmt.bindNull(15);
        } else {
          stmt.bindString(15, value.getId());
        }
      }
    };
    this.__preparedStmtOfUpdateCategory = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE food_items SET category = ? WHERE category = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSoftDelete = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE food_items SET isDeleted = 1, lastModifiedMs = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertItem(final FoodEntity item, final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFoodEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object hardDelete(final FoodEntity item, final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfFoodEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object updateItem(final FoodEntity item, final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfFoodEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object updateCategory(final String oldCategory, final String newCategory,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateCategory.acquire();
        int _argIndex = 1;
        if (newCategory == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, newCategory);
        }
        _argIndex = 2;
        if (oldCategory == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, oldCategory);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfUpdateCategory.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object softDelete(final String id, final long timestamp,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDelete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        if (id == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, id);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfSoftDelete.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public LiveData<List<FoodEntity>> getAllItems() {
    final String _sql = "SELECT * FROM food_items WHERE isDeleted = 0 ORDER BY expiryDateMs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"food_items"}, false, new Callable<List<FoodEntity>>() {
      @Override
      public List<FoodEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFridgeName = CursorUtil.getColumnIndexOrThrow(_cursor, "fridgeName");
          final int _cursorIndexOfInputDateMs = CursorUtil.getColumnIndexOrThrow(_cursor, "inputDateMs");
          final int _cursorIndexOfExpiryDateMs = CursorUtil.getColumnIndexOrThrow(_cursor, "expiryDateMs");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfWeightPerPortion = CursorUtil.getColumnIndexOrThrow(_cursor, "weightPerPortion");
          final int _cursorIndexOfPortions = CursorUtil.getColumnIndexOrThrow(_cursor, "portions");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfRemark = CursorUtil.getColumnIndexOrThrow(_cursor, "remark");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final List<FoodEntity> _result = new ArrayList<FoodEntity>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final FoodEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpFridgeName;
            if (_cursor.isNull(_cursorIndexOfFridgeName)) {
              _tmpFridgeName = null;
            } else {
              _tmpFridgeName = _cursor.getString(_cursorIndexOfFridgeName);
            }
            final long _tmpInputDateMs;
            _tmpInputDateMs = _cursor.getLong(_cursorIndexOfInputDateMs);
            final long _tmpExpiryDateMs;
            _tmpExpiryDateMs = _cursor.getLong(_cursorIndexOfExpiryDateMs);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final double _tmpWeightPerPortion;
            _tmpWeightPerPortion = _cursor.getDouble(_cursorIndexOfWeightPerPortion);
            final int _tmpPortions;
            _tmpPortions = _cursor.getInt(_cursorIndexOfPortions);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpRemark;
            if (_cursor.isNull(_cursorIndexOfRemark)) {
              _tmpRemark = null;
            } else {
              _tmpRemark = _cursor.getString(_cursorIndexOfRemark);
            }
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            _item = new FoodEntity(_tmpId,_tmpIcon,_tmpName,_tmpFridgeName,_tmpInputDateMs,_tmpExpiryDateMs,_tmpQuantity,_tmpWeightPerPortion,_tmpPortions,_tmpCategory,_tmpUnit,_tmpRemark,_tmpLastModifiedMs,_tmpIsDeleted);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllIncludeDeleted(final Continuation<? super List<FoodEntity>> continuation) {
    final String _sql = "SELECT * FROM food_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FoodEntity>>() {
      @Override
      public List<FoodEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFridgeName = CursorUtil.getColumnIndexOrThrow(_cursor, "fridgeName");
          final int _cursorIndexOfInputDateMs = CursorUtil.getColumnIndexOrThrow(_cursor, "inputDateMs");
          final int _cursorIndexOfExpiryDateMs = CursorUtil.getColumnIndexOrThrow(_cursor, "expiryDateMs");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfWeightPerPortion = CursorUtil.getColumnIndexOrThrow(_cursor, "weightPerPortion");
          final int _cursorIndexOfPortions = CursorUtil.getColumnIndexOrThrow(_cursor, "portions");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfRemark = CursorUtil.getColumnIndexOrThrow(_cursor, "remark");
          final int _cursorIndexOfLastModifiedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModifiedMs");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final List<FoodEntity> _result = new ArrayList<FoodEntity>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final FoodEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpFridgeName;
            if (_cursor.isNull(_cursorIndexOfFridgeName)) {
              _tmpFridgeName = null;
            } else {
              _tmpFridgeName = _cursor.getString(_cursorIndexOfFridgeName);
            }
            final long _tmpInputDateMs;
            _tmpInputDateMs = _cursor.getLong(_cursorIndexOfInputDateMs);
            final long _tmpExpiryDateMs;
            _tmpExpiryDateMs = _cursor.getLong(_cursorIndexOfExpiryDateMs);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final double _tmpWeightPerPortion;
            _tmpWeightPerPortion = _cursor.getDouble(_cursorIndexOfWeightPerPortion);
            final int _tmpPortions;
            _tmpPortions = _cursor.getInt(_cursorIndexOfPortions);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpRemark;
            if (_cursor.isNull(_cursorIndexOfRemark)) {
              _tmpRemark = null;
            } else {
              _tmpRemark = _cursor.getString(_cursorIndexOfRemark);
            }
            final long _tmpLastModifiedMs;
            _tmpLastModifiedMs = _cursor.getLong(_cursorIndexOfLastModifiedMs);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            _item = new FoodEntity(_tmpId,_tmpIcon,_tmpName,_tmpFridgeName,_tmpInputDateMs,_tmpExpiryDateMs,_tmpQuantity,_tmpWeightPerPortion,_tmpPortions,_tmpCategory,_tmpUnit,_tmpRemark,_tmpLastModifiedMs,_tmpIsDeleted);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, continuation);
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
