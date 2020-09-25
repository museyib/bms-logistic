package az.inci.bmslogistic;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class AppBaseActivity extends AppCompatActivity {

    protected static int SOUND_SUCCESS = R.raw.barcodebeep;
    protected static int SOUND_FAIL = R.raw.serror3;

    protected SoundPool soundPool;
    protected AudioManager audioManager;
    protected int sound;

    AlertDialog progressDialog;
    int mode;
    DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);
        dbHelper.open();

        soundPool = new SoundPool(10, 3, 5);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public void loadFooter() {
        TextView userId = findViewById(R.id.user_info_id);
        userId.setText(config().getUser().getId());
        userId.append(" - ");
        userId.append(config().getUser().getName());
    }

    public void showProgressDialog(boolean b) {
        View view = getLayoutInflater().inflate(R.layout.progress_dialog_layout,
                findViewById(android.R.id.content), false);
        if (progressDialog == null) {
            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        if (b) {
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
    }

    public String url(String... value) {
        StringBuilder sb = new StringBuilder();
        sb.append(config().getServerUrl());
        for (String s : value) {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public String addRequestParameters(String url, Map<String, String> requestParameters) {
        StringBuilder builder = new StringBuilder(url);
        builder.append("?");

        for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
            builder.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }

        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    public void loadUserInfo(User user, boolean newUser) {
        if (newUser) {
            dbHelper.addUser(user);
        }
        config().setUser(user);
    }

    public AppConfig config() {
        return ((App) getApplication()).getConfig();
    }

    protected void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showMessageDialog(String title, String message, int icon) {
        new android.app.AlertDialog.Builder(this)
                .setIcon(icon)
                .setTitle(title)
                .setMessage(message).show();
    }

    protected void playSound(int resourceId) {
        int volume = audioManager.getStreamMaxVolume(3);
        sound = soundPool.load(this, resourceId, 1);
        soundPool.setOnLoadCompleteListener((soundPool1, i, i1) -> {
            soundPool.play(sound, volume, volume, 1, 0, 1);
        });
    }
}
