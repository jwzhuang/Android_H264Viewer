package tw.jwzhuang.ipcamviewer.preview;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import tw.jwzhuang.ipcamviewer.R;
import tw.jwzhuang.ipcamviewer.devices.DeviceManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

public class Preview extends Activity {
	
	private final String TAG = this.getClass().getSimpleName();
	private SocketClient client = null;
	private int loginState = -1;
	private final byte[] Head = new byte[]{0x00, 0x00, 0x00, 0x01};
	private byte[] SPS = null;
	private byte[] PPS = null;
	private int videoRate = -1;
	private int canGetStream = -1;
	private MjpegView image = null;
	private String ip = "";
	private String pwd = "";
	private FrameLayout layout = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyApplication.getInstance().addActivity(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewer_layout);
		layout = (FrameLayout)findViewById(R.id.flayout);
	}
	
	@Override
	public void onPause() {
		disconnect();
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chose, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, DeviceManager.class); //創建一個Intent，用來呼叫其他Activity
			Bundle bundle = new Bundle();                  //創建一個Bundle，用來放置參數
			bundle.putInt("mode", 1);                      //範例，放置int
			intent.putExtras(bundle);  
			startActivityForResult(intent,1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			
//			image = (MjpegView) findViewById(R.id.mjpegView);
			image = new MjpegView(this);
			image.setDisplayMode(MjpegView.SIZE_FULLSCREEN);
			image.showFps(true);
			layout.addView(image, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			
	    	Bundle bundleResult = data.getExtras();
	    	ip = bundleResult.getString("ip");
	    	pwd = bundleResult.getString("pwd");
	    	new Thread() {
				public void run() {
					connectSrv();
				}
			}.start();
	    }
	}
	
	private void disconnect(){
		loginState = -1;
		canGetStream = -1;
		this.SPS = null;
		this.PPS = null;
		this.videoRate = -1;
		
		if(image != null){
			image.stopPlayback();
		}
		
		if (client != null) {
			try {
				client.closeConnect();
				client = null;
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
	
	/**
	 * 連接派送伺服器
	 */
	private void connectSrv() {
		if (client == null) {
			client = new SocketClient(this);
			client.setSrvAddress(ip, 47226);
			try {
				client.openConnect();
				while (!client.IsOpen()) {
					Log.i(TAG, "wait open");
				}
				client.getMsg();
				authenticate();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	public void authenticate() throws JSONException {
		JSONObject jobj = new JSONObject();
		jobj.put("cmd", "login");
		jobj.put("pwd", pwd);
		client.sentMsg(jobj.toString());
	}
	
	public void setLoginState(int state) throws JSONException{
		this.loginState  = state;
		if(state == 0 && videoRate == -1){
			JSONObject jobj = new JSONObject();
			jobj.put("cmd", "getparams");
			client.sentMsg(jobj.toString());
		}
	}
	
	public void setVideoParams(String sps, String pps, int rate) throws JSONException{
		this.SPS = Base64.decode(sps, Base64.DEFAULT);
		this.PPS = Base64.decode(pps, Base64.DEFAULT);
		this.videoRate = rate;
		if(canGetStream > -1){
			return;
		}
		JSONObject jobj = new JSONObject();
		jobj.put("cmd", "getstream");
		client.sentMsg(jobj.toString());
	}
	
	public void setGetStreamState(int state, InputStream in) {
		this.canGetStream  = state;
		if(state == 0){
			image.setSource(in);
			image.startPlayback();
		}
	}
}
