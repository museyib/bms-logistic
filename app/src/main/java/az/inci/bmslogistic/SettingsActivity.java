package az.inci.bmslogistic;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppBaseActivity
{

    EditText serverUrlEdit;
    EditText connectionTimeoutEdit;
    Button update;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        loadComponents();
    }

    private void loadComponents()
    {
        serverUrlEdit = findViewById(R.id.server_url);
        connectionTimeoutEdit = findViewById(R.id.connection_timeout);
        update = findViewById(R.id.update);

        serverUrlEdit.setText(config().getServerUrl());
        connectionTimeoutEdit.setText(String.valueOf(config().getConnectionTimeout()));

        update.setOnClickListener(v -> updateParameters());
    }

    private void updateParameters()
    {
        String serverUrl = serverUrlEdit.getText().toString();
        String connectionTimeout = connectionTimeoutEdit.getText().toString();

        if (!serverUrl.isEmpty())
        {
            config().setServerUrl(serverUrl);
            dbHelper.updateParameter("serverUrl", serverUrl);
        }

        if (!connectionTimeout.isEmpty())
        {
            config().setConnectionTimeout(Integer.parseInt(connectionTimeout));
            dbHelper.updateParameter("connectionTimeout", connectionTimeout);
        }

        showToastMessage("Parametrlər yeniləndi");
        loadComponents();
    }
}