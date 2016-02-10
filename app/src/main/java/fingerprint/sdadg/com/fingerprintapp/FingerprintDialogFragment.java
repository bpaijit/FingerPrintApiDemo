package fingerprint.sdadg.com.fingerprintapp;

import android.app.DialogFragment;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Bryan on 11/8/2015.
 */
public class FingerprintDialogFragment extends DialogFragment {
    private FingerprintUtils mFingerprintUtils;
    private FingerprintManager.CryptoObject mCryptoObject;

    public void setCallback(FingerprintUtils.IFingerprintUtilCallback callback) {
        mCallback = callback;
    }

    private FingerprintUtils.IFingerprintUtilCallback mCallback;

    public FingerprintDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_NoActionBar);
        mFingerprintUtils = new FingerprintUtils(getActivity().getSystemService(FingerprintManager.class), mCallback);
        mFingerprintUtils.startListening(mCryptoObject);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fingerprint, container);
        return view;
    }

    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }
}
