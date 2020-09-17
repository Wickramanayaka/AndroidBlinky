package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

@SuppressWarnings("ConstantConditions")
public abstract class BlinkyTemDataCallback implements ProfileDataCallback, BlinkyTemCallback
{
    private static final int STATE_RELEASED = 0x00;
    private static final int STATE_PRESSED = 0x01;

    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        if (data.size() != 1) {
            onInvalidDataReceived(device, data);
            return;
        }
        final int state = data.getIntValue(Data.FORMAT_UINT8, 0);
        if (state >= 0) {
            onButtonStateChanged(device,state);
        } else {
            onInvalidDataReceived(device, data);
        }

    }
}
