package tw.jwzhuang.ipcamviewer;

import tw.jwzhuang.ipcamviewer.devices.DeviceManager;
import tw.jwzhuang.ipcamviewer.preview.MyApplication;
import tw.jwzhuang.ipcamviewer.preview.Preview;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyApplication.getInstance().addActivity(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	@Override
	public void onBackPressed() {
		MyApplication.getInstance().onTerminate();
		super.onBackPressed();
	}

	public void click_preview(View view){
		startActivity(new Intent(this, Preview.class));
	}
	
	public void click_devices(View view){
		startActivity(new Intent(this, DeviceManager.class));
	}
}
