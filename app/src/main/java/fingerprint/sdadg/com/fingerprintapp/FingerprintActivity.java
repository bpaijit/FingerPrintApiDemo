package fingerprint.sdadg.com.fingerprintapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class FingerprintActivity extends Activity implements FingerprintUtils.IFingerprintUtilCallback {

    private static final int PERMISSION_REQUEST_CODE_FINGERPRINT = 1;
    private final String KEY_NAME = "demoKey";

    private KeyStore mKeyStore;

    private KeyGenerator mKeyGenerator;

    private Cipher mCipher;

    private FingerprintDialogFragment mDialogFragment;

    public FingerprintActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");

            mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        //New runtime permission request dialog
        requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT}, PERMISSION_REQUEST_CODE_FINGERPRINT);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case (PERMISSION_REQUEST_CODE_FINGERPRINT): {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (createKey()) {
                        if (initCipher()) {
                            mDialogFragment = new FingerprintDialogFragment();
                            mDialogFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                            mDialogFragment.setCallback(this);
                            mDialogFragment.show(getFragmentManager(), "Fingerprint dialog");
                        }
                    }
                }

                break;
            }
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @return {@code true} if key is created successful, {@code false} otherwise such as when no
     * fingerprints are registered.
     */
    private boolean createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);

            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                    // Require the user to authenticate with a fingerprint to authorize every use
                                    // of the key
                            .setUserAuthenticationRequired(true)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .build()
            );

            mKeyGenerator.generateKey();

            return true;
        } catch (IllegalStateException exState) {
            Toast.makeText(this, "No fingerprints registered.", Toast.LENGTH_SHORT).show();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Initialize the {@link Cipher} instance with the created key in the {@link #createKey()}
     * method.
     *
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onAuthenticated() {
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        mDialogFragment.dismiss();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, "Failed:  " + message, Toast.LENGTH_SHORT).show();
        mDialogFragment.dismiss();
    }
}
