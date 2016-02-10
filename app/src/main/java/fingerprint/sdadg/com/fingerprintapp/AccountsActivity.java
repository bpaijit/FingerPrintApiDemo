package fingerprint.sdadg.com.fingerprintapp;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AccountsActivity extends Activity {

    private static final int REQUEST_GET_ACCOUNTS = 1;

    private static final String KEY_NAME = "my_key";

    private static final String KEY_EMAIL = "key_email";

    private static final String KEY_TOKEN = "key_token";

    private static final String SHARED_PREF_NAME = "Fingerprint App Preferences6";

    private static final String SECRET_BYTE_ARRAY = "abcdef1234567890";

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 2;

    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    private SharedPreferences mPreferences;
    private KeyguardManager mKeyguardManager;
    private TextView tvUsername, tvOldTokenValue, tvNewTokenValue;

    String mEmailAddress = "";

    private String iv = "fedcba9876543210";
    private IvParameterSpec ivspec;
    private SecretKeySpec keyspec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        ivspec = new IvParameterSpec(iv.getBytes());
        keyspec = new SecretKeySpec(SECRET_BYTE_ARRAY.getBytes(), "AES");

        tvUsername = (TextView) findViewById(R.id.tvUsername);
        /*tvOldTokenValue = (TextView) findViewById(R.id.tvOldTokenValue);
        tvNewTokenValue = (TextView) findViewById(R.id.tvNewTokenValue);*/

        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        mPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        mEmailAddress = mPreferences.getString(KEY_EMAIL, "");

        if (mEmailAddress.contentEquals("")) {
            getAccount();
        } else {
            createKey();
            tryEncrypt();
        }
    }

    private void getAccount() {
        startActivityForResult(AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"}, false, null, null, null, null), REQUEST_GET_ACCOUNTS);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_GET_ACCOUNTS: {
                if (resultCode == RESULT_OK) {
                    mEmailAddress = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString(KEY_EMAIL, mEmailAddress);
                    editor.apply();

                    createKey();
                    tryEncrypt();
                }
                break;
            }
            case REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS: {
                createKey();
                tryEncrypt();

                break;
            }
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with device credentials within the last X seconds.
     */
    private void createKey() {
        // Generate a key to decrypt payment credentials, tokens, etc.
        // This will most likely be a registration step for the user when they are setting up your app.
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                            // Require that the user has unlocked in the last 30 seconds
                    .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e) {
            throw new RuntimeException("Failed to create a symmetric key", e);
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via device credentials.
     */
    private boolean tryEncrypt() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_NONE);

            // Try encrypting something, it will only work if the user authenticated within
            // the last AUTHENTICATION_DURATION_SECONDS seconds.
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] returnedToken = cipher.doFinal(SECRET_BYTE_ARRAY.getBytes());

            //Compare the token to the saved one
            String savedToken = mPreferences.getString(KEY_TOKEN, "");

            if (savedToken != null && !savedToken.contentEquals("")) {
                cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
                byte[] decryptedToken = cipher.doFinal(savedToken.getBytes());

                //tvOldTokenValue.setText((decryptedToken.toString()));

                if (!decryptedToken.equals(SECRET_BYTE_ARRAY)) {
                    Toast.makeText(this, "Success", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
                }
            }

            //tvNewTokenValue.setText(returnedToken.toString());

            //Store the token
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(KEY_TOKEN, SECRET_BYTE_ARRAY);
            editor.apply();

            // If the user has recently authenticated, you will reach here.
            showUsername();
            return true;
        } catch (UserNotAuthenticatedException e) {
            // User is not authenticated, let's authenticate with device credentials.
            showAuthenticationScreen();
            return false;
        } catch (KeyPermanentlyInvalidatedException e) {
            // This happens if the lock screen has been disabled or reset after the key was
            // generated after the key was generated.
            Toast.makeText(this, "Keys are invalidated after created. Retry the purchase\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return false;
        } catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showAuthenticationScreen() {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    private void showUsername() {
        tvUsername.setText(mEmailAddress);
    }

}
