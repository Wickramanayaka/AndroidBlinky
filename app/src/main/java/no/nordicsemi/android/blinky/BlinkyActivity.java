/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.iotconnectsdk.IoTConnectSDK;
import com.iotconnectsdk.interfaces.IotSDKCallback;
import com.iotconnectsdk.webservices.responsebean.HubToSdkDataBean;
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;

@SuppressWarnings("ConstantConditions")
public class BlinkyActivity extends AppCompatActivity implements IotSDKCallback {
	public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

	private BlinkyViewModel viewModel;

	@BindView(R.id.tem_value) TextView temState;

	IoTConnectSDK iotConnect;
	String  cpId = "";
	String uniqueId = "3";
	String env = "Avnet";

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blinky);
		ButterKnife.bind(this);

		iotConnect = new IoTConnectSDK(BlinkyActivity.this,cpId, uniqueId,
				BlinkyActivity.this, env);

		final Intent intent = getIntent();
		final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
		final String deviceName = device.getName();
		final String deviceAddress = "";

		final MaterialToolbar toolbar = findViewById(R.id.toolbar);
		toolbar.setTitle(deviceName != null ? deviceName : getString(R.string.unknown_device));
		toolbar.setSubtitle(deviceAddress);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Configure the view model.
		viewModel = new ViewModelProvider(this).get(BlinkyViewModel.class);
		viewModel.connect(device);

		// Set up views.
		final LinearLayout progressContainer = findViewById(R.id.progress_container);
		final TextView connectionState = findViewById(R.id.connection_state);
		final View content = findViewById(R.id.device_container);
		final View notSupported = findViewById(R.id.not_supported);

		viewModel.getConnectionState().observe(this, state -> {
			switch (state.getState()) {
				case CONNECTING:
					progressContainer.setVisibility(View.VISIBLE);
					notSupported.setVisibility(View.GONE);
					connectionState.setText(R.string.state_connecting);
					break;
				case INITIALIZING:
					connectionState.setText(R.string.state_initializing);
					break;
				case READY:
					progressContainer.setVisibility(View.GONE);
					content.setVisibility(View.VISIBLE);
					onConnectionStateChanged(true);
					break;
				case DISCONNECTED:
					if (state instanceof ConnectionState.Disconnected) {
						final ConnectionState.Disconnected stateWithReason = (ConnectionState.Disconnected) state;
						if (stateWithReason.isNotSupported()) {
							progressContainer.setVisibility(View.GONE);
							notSupported.setVisibility(View.VISIBLE);
						}
					}
					// fallthrough
				case DISCONNECTING:
					onConnectionStateChanged(false);
					break;
			}
		});

		viewModel.getTemState().observe(this,
				pressed -> {
					temState.setText(pressed.toString());
					String sendTeledata = "[{\'data\': {\'Temperature\': \'"+pressed+"\'},\'uniqueId\':\'"+uniqueId+"\',\'time\' : \'"+getCurrentTime()+"\'}]";

					if (iotConnect != null) {
						iotConnect.sendData(sendTeledata);
						Log.e("json", "" + sendTeledata);
					}
					Log.e("Tem", String.valueOf(pressed));
		});
	}

	@OnClick(R.id.action_clear_cache)
	public void onTryAgainClicked() {
		viewModel.reconnect();
	}

	private void onConnectionStateChanged(final boolean connected) {

	}

	public static String getCurrentTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date());
	}

	private boolean checkValidation() {
		if (cpId.isEmpty()) {
			Toast.makeText(BlinkyActivity.this, "cpId can not be blank.", Toast.LENGTH_LONG).show();
			return false;
		} else if (uniqueId.isEmpty()) {
			Toast.makeText(BlinkyActivity.this, "uniqueId can not be blank.", Toast.LENGTH_LONG).show();
			return false;
		}else if (env.isEmpty()) {
			Toast.makeText(BlinkyActivity.this, "env can not be blank.", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	@Override
	public void onReceiveMsg(HubToSdkDataBean dataBean) {
		if (dataBean != null) {
			Log.e("onReceiveMsg", dataBean.getValue());

		}
	}

	@Override
	public void attributeData(List<SyncServiceResponse.DBeanXX.AttBean> attributesBeanList) {
		for(int i=0; i<attributesBeanList.size(); i++){
			Log.e("Attribute",attributesBeanList.get(i).getD().get(0).getLn());
		}
	}

	@Override
	public void onConnectionStateChange(boolean isConnected) {
		if (isConnected) {
			Log.e("onConnectionStateChange", "Device connected");
		} else {
			Log.e("onConnectionStateChange", "Device disconnected");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (iotConnect != null) {
			iotConnect.disconnectSDK();
		}
	}


}
