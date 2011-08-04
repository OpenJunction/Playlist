package edu.stanford.junction.sample.partyware.playlist;

import edu.stanford.junction.sample.partyware.playlist.helpers.RichActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;

public class PartyActivity extends RichActivity {

	public final static int REQUEST_CODE_SCAN_URL = 0;

	private TextView mPartyName;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.party);

		mPartyName = (TextView)findViewById(R.id.party_name);

		Button button = (Button)findViewById(R.id.join_party_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					scanURL();
				}
			});

		button = (Button)findViewById(R.id.join_default_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					JunctionApp app = (JunctionApp)getApplication();
					app.connectToSession(null);
				}
			});

		button = (Button)findViewById(R.id.update_profile_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					updateProfile();
				}
			});

	
		listenForAnyPropChange();
		refresh();
	}

	protected void updateProfile(){
		JunctionApp app = (JunctionApp)getApplication();
		Intent intent = new Intent(UpdateProfileActivity.LAUNCH_INTENT);
		intent.putExtra("name", app.getUserName());
		intent.putExtra("email", app.getUserEmail());
		intent.putExtra("image_url", app.getUserImageUrl());
		startActivity(intent);
	}

	protected void onAnyPropChange(){
		refresh();
	}


	private void refresh(){
		JunctionApp app = (JunctionApp)getApplication();
		PartyProp prop = app.getProp();
		mPartyName.setText(prop.getName());
	}

	protected void scanURL(){
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
		startActivityForResult(intent, REQUEST_CODE_SCAN_URL);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch(requestCode) {
		case REQUEST_CODE_SCAN_URL:
			if(resultCode == RESULT_OK){
				String url = intent.getStringExtra("SCAN_RESULT");
				toastShort("Connecting to party at: " + url);
				JunctionApp app = (JunctionApp)getApplication();
				app.connectToSession(Uri.parse(url));
			}
			break;
		}
	}

	public void onDestroy(){
		super.onDestroy();
	}

}



