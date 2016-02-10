package fingerprint.sdadg.com.fingerprintapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    Button btnMainFingerprint, btnMainAccounts;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        btnMainFingerprint = (Button) findViewById(R.id.btnMainFingerprint);
        btnMainFingerprint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, FingerprintActivity.class));
            }
        });

        btnMainAccounts = (Button) findViewById(R.id.btnMainAccounts);
        btnMainAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, AccountsActivity.class));
            }
        });
    }


}
