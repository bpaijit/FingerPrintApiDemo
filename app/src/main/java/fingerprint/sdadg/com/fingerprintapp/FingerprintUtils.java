package fingerprint.sdadg.com.fingerprintapp;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;

import java.security.Signature;

/**
 * Created by Bryan on 11/8/2015.
 */
public class FingerprintUtils extends FingerprintManager.AuthenticationCallback {
    private final FingerprintManager mFingerprintManager;
    private IFingerprintUtilCallback mCallback;
    private CancellationSignal mCancellationSignal;

    //Constructor **********************************************************************************
    public FingerprintUtils(FingerprintManager manager, IFingerprintUtilCallback callback) {
        mFingerprintManager = manager;
        mCallback = callback;
    }

    //Action methods ********************************************************************************
    public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager.authenticate(cryptoObject, mCancellationSignal, 0, this, null);

    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            ;
            mCancellationSignal = null;
        }
    }

    //Callback events ***********************************************************************************
    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        mCallback.onError(String.valueOf(errString));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        Signature signature = result.getCryptoObject().getSignature();
        mCallback.onAuthenticated();
    }

    @Override
    public void onAuthenticationFailed() {
        mCallback.onError("Failed");
    }

    //Callback interface *********************************************************************************
    public interface IFingerprintUtilCallback {
        public void onAuthenticated();

        public void onError(String message);
    }
}
