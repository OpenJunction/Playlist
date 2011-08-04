package edu.stanford.junction.sample.partyware.playlist;

import edu.stanford.junction.sample.partyware.playlist.util.*;
import edu.stanford.junction.sample.partyware.playlist.helpers.*;

import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.ProgressDialog;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;
import android.util.Log;

import java.io.*;
import java.util.*;


public class UpdateProfileActivity extends RichActivity{

	public final static String LAUNCH_INTENT = "edu.stanford.junction.sample.partyware.playlist.UPDATE_PROFILE";

	public final static int REQUEST_CODE_PICK_FROM_LIBRARY = 0;
	public final static int REQUEST_CODE_TAKE_PICTURE = 1;

	private Uri mPortraitUri;
	private AutoCompleteTextView mNameText;
	private AutoCompleteTextView mEmailText;
	private ImageView mPortraitView;
	private ProgressDialog mUploadProgressDialog;
	private BroadcastReceiver mUriReceiver;
	private BroadcastReceiver mErrorReceiver;
	private BitmapManager mgr = new BitmapManager(1);

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.update_profile);

		Intent intent = getIntent();

		mNameText = (AutoCompleteTextView)findViewById(R.id.name_text);
		String name = intent.getStringExtra("name");
		if(name != null){
			mNameText.setText(intent.getStringExtra("name"));
		}
		mNameText.setAdapter(new ContactsAutoCompleteCursorAdapter.NameAdapter(this));

		mEmailText = (AutoCompleteTextView)findViewById(R.id.email_text);
		String email = intent.getStringExtra("email");
		if(email != null){
			mEmailText.setText(intent.getStringExtra("email"));
		}
		mEmailText.setAdapter(new ContactsAutoCompleteCursorAdapter.EmailAdapter(this));

		mPortraitView = (ImageView)findViewById(R.id.image);
		mPortraitView.setImageResource(R.drawable.ellipsis);
		String uri = intent.getStringExtra("image_url");
		if(uri != null){
			mPortraitUri = Uri.parse(uri);
			lazyLoadImage(mPortraitView, Uri.parse(uri));
		}


		Button button = (Button)findViewById(R.id.use_camera_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Helpers.takeSmallPicture(UpdateProfileActivity.this, 
											 REQUEST_CODE_TAKE_PICTURE);
				}
			});

		button = (Button)findViewById(R.id.pick_from_library_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					pickFromLibrary();
				}
			});

		button = (Button)findViewById(R.id.finished_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					confirm();
				}
			});

		button = (Button)findViewById(R.id.cancel_button);
		button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					cancel();
				}
			});
	}


	// XXX. Fix for Android bug where 'next' button doesn't pass 
	// focus down when an AutoCompleteTextView is focued.
	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event) {
		View ac1 = mNameText;
		View ac2 = mEmailText;
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (ac1.hasFocus()) {
				//sends focus to ac2 (user pressed "Next")
				ac2.requestFocus();
				return true;
			}
			else if (ac2.hasFocus()) {
				//closes soft keyboard (user pressed "Done")
				InputMethodManager inputManager = (InputMethodManager)
					getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(
					ac2.getWindowToken(), 
					InputMethodManager.HIDE_NOT_ALWAYS);
				return true;
			}
        }
        return false;

	} 

	protected void showPortrait(Uri uri){
		mgr.getBitmap(uri.toString(), new Handler(){
				public void handleMessage(Message msg){
					super.handleMessage(msg);
					Bitmap bm = (Bitmap)msg.obj;
					if(bm != null){
						mPortraitView.setImageBitmap(bm);
					}
				}
			});
	}

	protected void pickFromLibrary(){
		Intent i = new Intent(Intent.ACTION_PICK, 
							  android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		startActivityForResult(i, REQUEST_CODE_PICK_FROM_LIBRARY);
	}




	protected void startUpload(Uri localUri){

		mUriReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					mUploadProgressDialog.dismiss();
					mPortraitUri = Uri.parse(intent.getStringExtra("thumb_url"));
					showPortrait(mPortraitUri);
				}
			};
		IntentFilter intentFilter = new IntentFilter(ImgurUploadService.BROADCAST_FINISHED);
		registerReceiver(mUriReceiver, intentFilter); 

		mErrorReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					mUploadProgressDialog.dismiss();
					String error = intent.getStringExtra("error");
					showDialog(error);
				}
			};
		intentFilter = new IntentFilter(ImgurUploadService.BROADCAST_FAILED);
		registerReceiver(mErrorReceiver, intentFilter);

		Intent i = new Intent(this, ImgurUploadService.class);
		i.setAction(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_STREAM, localUri);
		startService(i);

		mUploadProgressDialog = ProgressDialog.show(this,"","Uploading. Please wait...",true);

	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_CODE_PICK_FROM_LIBRARY:
			if(resultCode == RESULT_OK){
				Uri localUri = data.getData();
				startUpload(localUri);
			}
			break;
		case REQUEST_CODE_TAKE_PICTURE:
			if(resultCode == RESULT_OK){
				Uri localUri;
				if (Misc.hasImageCaptureBug()) {
					File fi = new File("/sdcard/tmp");
					try {
						localUri = Uri.parse(
							android.provider.MediaStore.Images.Media.insertImage(
								getContentResolver(), 
								fi.getAbsolutePath(), null, null));
						if (!fi.delete()) {
							Log.i("AddPictureActivity", "Failed to delete " + fi);
						}
						startUpload(localUri);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} else {
					localUri = data.getData();
					startUpload(localUri);
                }
				
			}
			break;
		}
	}


    protected void confirm(){
		if(mPortraitUri == null){
			Toast.makeText(this, R.string.no_image_selected, 
						   Toast.LENGTH_SHORT).show();
		}
		else{
			// stop the image uploader service
			Intent i = new Intent(this, ImgurUploadService.class);
			stopService(i);

			String name = mNameText.getText().toString();
			String email = mEmailText.getText().toString();
			String imageUrl = mPortraitUri.toString();
			final JunctionApp app = (JunctionApp)getApplication();
			app.updateUser(name, email, imageUrl);

			finish();
		}
    }

    protected void cancel(){
		Intent intent = new Intent();
		setResult(RESULT_CANCELED, intent);
		finish();
	}


	public void onDestroy(){
		super.onDestroy();
		try{
			unregisterReceiver(mUriReceiver);
			unregisterReceiver(mErrorReceiver);
			Intent i = new Intent(this, ImgurUploadService.class);
			stopService(i);
		}
		catch(IllegalArgumentException e){}
	}

}



