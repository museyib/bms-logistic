package az.inci.bmslogistic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Objects;

public class DBHelper extends SQLiteOpenHelper
{
    public static final String TERMINAL_USER = "TERMINAL_USER";
    public static final String USER_ID = "USER_ID";
    public static final String USER_NAME = "USER_NAME";
    public static final String PASS_WORD = "PASS_WORD";
    public static final String COLLECT_FLAG = "COLLECT_FLAG";
    public static final String PICK_FLAG = "PICK_FLAG";
    public static final String CHECK_FLAG = "CHECK_FLAG";
    public static final String COUNT_FLAG = "COUNT_FLAG";
    public static final String LOCATION_FLAG = "LOCATION_FLAG";
    public static final String PACK_FLAG = "PACK_FLAG";
    public static final String DOC_FLAG = "DOC_FLAG";
    public static final String LOADING_FLAG = "LOADING_FLAG";

    private SQLiteDatabase db;

    public DBHelper(Context context)
    {
        super(context, Objects.requireNonNull(context.getExternalFilesDir("/"))
                .getPath() + "/" + AppConfig.DB_NAME, null, AppConfig.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        createUserTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onCreate(db);
    }

    public void open() throws SQLException
    {
        db = getWritableDatabase();
    }

    @Override
    public synchronized void close()
    {
        super.close();
    }

    private void createUserTable(SQLiteDatabase db)
    {
        StringBuilder sb = new StringBuilder();

        db.execSQL("DROP TABLE IF EXISTS " + TERMINAL_USER);

        db.execSQL(sb.append("CREATE TABLE ")
                .append(TERMINAL_USER).append("(")
                .append(USER_ID).append(" TEXT,")
                .append(USER_NAME).append(" TEXT,")
                .append(PASS_WORD).append(" TEXT,")
                .append(COLLECT_FLAG).append(" INTEGER,")
                .append(PICK_FLAG).append(" INTEGER,")
                .append(CHECK_FLAG).append(" INTEGER,")
                .append(COUNT_FLAG).append(" INTEGER,")
                .append(LOCATION_FLAG).append(" INTEGER,")
                .append(PACK_FLAG).append(" INTEGER,")
                .append(DOC_FLAG).append(" INTEGER,")
                .append(LOADING_FLAG).append(" INTEGER")
                .append(")")
                .toString());
    }

    public void addUser(User user)
    {
        db.delete(TERMINAL_USER, USER_ID + "=?", new String[]{user.getId()});

        ContentValues values = new ContentValues();
        values.put(USER_ID, user.getId());
        values.put(USER_NAME, user.getName());
        values.put(PASS_WORD, user.getPassword());
        values.put(COLLECT_FLAG, user.isCollectFlag() ? 1 : 0);
        values.put(PICK_FLAG, user.isPickFlag() ? 1 : 0);
        values.put(CHECK_FLAG, user.isCheckFlag() ? 1 : 0);
        values.put(COUNT_FLAG, user.isCountFlag() ? 1 : 0);
        values.put(LOCATION_FLAG, user.isLocationFlag() ? 1 : 0);
        values.put(PACK_FLAG, user.isPackFlag() ? 1 : 0);
        values.put(DOC_FLAG, user.isDocFlag() ? 1 : 0);
        values.put(LOADING_FLAG, user.isLoadingFlag() ? 1 : 0);

        db.insert(TERMINAL_USER, null, values);
    }

    public User getUser(String id)
    {
        String[] columns = new String[]{USER_ID, USER_NAME, PASS_WORD, COLLECT_FLAG,
                PICK_FLAG, CHECK_FLAG, COUNT_FLAG, LOCATION_FLAG, PACK_FLAG, DOC_FLAG, LOADING_FLAG};
        User user = null;

        try (Cursor cursor = db.query(TERMINAL_USER, columns,
                "USER_ID=?", new String[]{id.toUpperCase()}, null, null, null))
        {
            if (cursor.moveToNext())
            {
                user = new User();
                user.setId(cursor.getString(0));
                user.setName(cursor.getString(1));
                user.setPassword(cursor.getString(2));
                user.setCollectFlag(cursor.getInt(3) == 1);
                user.setPickFlag(cursor.getInt(4) == 1);
                user.setCheckFlag(cursor.getInt(5) == 1);
                user.setCountFlag(cursor.getInt(6) == 1);
                user.setLocationFlag(cursor.getInt(7) == 1);
                user.setPackFlag(cursor.getInt(8) == 1);
                user.setDocFlag(cursor.getInt(9) == 1);
                user.setLoadingFlag(cursor.getInt(10) == 1);
            }
        }
        return user;
    }
}
