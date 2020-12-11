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

    public static final String LAST_LOGIN = "LAST_LOGIN";
    public static final String CONFIG = "CONFIG";
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
    public static final String NAME = "NAME";
    public static final String VALUE = "VALUE";

    private SQLiteDatabase db;

    DBHelper(Context context)
    {
        super(context, Objects.requireNonNull(context.getExternalFilesDir("/"))
                .getPath() + "/" + AppConfig.DB_NAME, null, AppConfig.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        createUserTable(db);
        createConfigTable(db);
        createLastLoginTable(db);
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

    void open() throws SQLException
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

    void addUser(User user)
    {
        db.delete(TERMINAL_USER, USER_ID + "=?", new String[]{user.getId()});

        ContentValues values = new ContentValues();
        values.put(USER_ID, user.getId());
        values.put(USER_NAME, user.getName());
        values.put(PASS_WORD, user.getPassword());
        values.put(COLLECT_FLAG, user.isCollect() ? 1 : 0);
        values.put(PICK_FLAG, user.isPick() ? 1 : 0);
        values.put(CHECK_FLAG, user.isCheck() ? 1 : 0);
        values.put(COUNT_FLAG, user.isCount() ? 1 : 0);
        values.put(LOCATION_FLAG, user.isLocation() ? 1 : 0);
        values.put(PACK_FLAG, user.isPack() ? 1 : 0);
        values.put(DOC_FLAG, user.isDoc() ? 1 : 0);
        values.put(LOADING_FLAG, user.isLoading() ? 1 : 0);

        db.insert(TERMINAL_USER, null, values);
    }

    User getUser(String id)
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

    private void createConfigTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS " + CONFIG);
        db.execSQL("CREATE TABLE CONFIG (NAME TEXT, VALUE TEXT)");
    }

    public String getParameter(String name)
    {
        String value = "";

        Cursor cursor = db.rawQuery("SELECT VALUE FROM CONFIG WHERE NAME=?", new String[]{name});
        if (cursor.moveToFirst())
        {
            value = cursor.getString(0);
        }

        cursor.close();

        return value;
    }

    public void updateParameter(String name, String value)
    {
        ContentValues values = new ContentValues();
        values.put(NAME, name);
        values.put(VALUE, value);
        try
        {
            db.delete(CONFIG, NAME + "=?", new String[]{name});
            db.insert(CONFIG, null, values);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

    }

    private void createLastLoginTable(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS " + LAST_LOGIN);
        db.execSQL("CREATE TABLE LAST_LOGIN (USER_ID TEXT, PASS_WORD TEXT)");
    }

    public void updateLastLogin(String userId, String password)
    {
        db.delete(LAST_LOGIN, USER_ID + "=?", new String[]{userId});

        ContentValues values = new ContentValues();
        values.put(USER_ID, userId);
        values.put(PASS_WORD, password);

        db.insert(LAST_LOGIN, null, values);
    }

    public String[] getLastLogin()
    {
        String[] result = new String[2];
        Cursor cursor = db.rawQuery("SELECT * FROM LAST_LOGIN", null);

        if (cursor.moveToLast())
        {
            result[0] = cursor.getString(0);
            result[1] = cursor.getString(1);
        }

        cursor.close();

        return result;
    }
}
