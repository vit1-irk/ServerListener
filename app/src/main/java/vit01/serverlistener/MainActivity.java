package vit01.serverlistener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {
    SharedPreferences prefManager;
    Spinner exchange_type_spinner;
    EditText address, update_interval, custom_intent;
    Switch job_enable;
    SharedPreferences.Editor editor;
    Intent alarmIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefManager = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefManager.edit();
        alarmIntent = new Intent(this, AlarmService.class);
        loadForms();
        loadSettings();
    }

    private void loadForms() {
        exchange_type_spinner = (Spinner) findViewById(R.id.spinner);
        address = (EditText) findViewById(R.id.editText);
        update_interval = (EditText) findViewById(R.id.editText2);
        custom_intent = (EditText) findViewById(R.id.editText3);
        job_enable = (Switch) findViewById(R.id.switch1);

        Button params_save = (Button) findViewById(R.id.button);
        if (params_save != null) {
            params_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveSettings();
                }
            });
        }

        Button customAction = (Button) findViewById(R.id.button2);
        if (customAction != null) {
            customAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent executeIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    executeIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
                    executeIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.new_action));
                    startActivityForResult(executeIntent, 14); // magic number for home screen shortcut
                }
            });
        }
    }

    private void loadSettings() {
        exchange_type_spinner.setSelection(prefManager.getInt("data_exchange_type", 0));
        address.setText(prefManager.getString("server_adress", "http://example.com/myscript?id={ts_id}"));
        int update_data_interval = prefManager.getInt("update_data_interval", 10);
        update_interval.setText(String.valueOf(update_data_interval));
        custom_intent.setText(prefManager.getString("custom_intent_action", "null"));
        job_enable.setChecked(prefManager.getBoolean("job_enable", false));
        job_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) job_set_enabled(true);
                else job_set_enabled(false);
            }
        });
    }

    public void job_set_enabled(boolean enabled) {
        editor.putBoolean("job_enable", enabled);
        editor.commit();
        startService(alarmIntent);
    }

    private void saveSettings() {
        editor.putInt("data_exchange_type", exchange_type_spinner.getSelectedItemPosition());
        editor.putString("server_adress", address.getText().toString());
        editor.putInt("update_data_interval", parseInt(update_interval.getText().toString()));
        editor.putString("custom_intent_action", custom_intent.getText().toString());
        editor.commit();

        startService(alarmIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_clean_cache:
                editor.putString("xc_data", "null");
                editor.putString("ts_id", "0");
                editor.commit();
                Toast.makeText(MainActivity.this,
                        getString(R.string.cache_clear_done),
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_help:
                startActivity(new Intent(this, HelpActivity.class));
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 14:
                    startActivityForResult(data, 15);
                    break;
                case 15:
                    Intent i = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    saveShortcutIntent(i);
                    break;
            }
        } else {
            Toast.makeText(MainActivity.this,
                    getString(R.string.action_none), Toast.LENGTH_SHORT).show();
        }
    }

    public void saveShortcutIntent(Intent intent) {
        String uri = intent.toUri(0);
        custom_intent.setText(uri);
    }
}
