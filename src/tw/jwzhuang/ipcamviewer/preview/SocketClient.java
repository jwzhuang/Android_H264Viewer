package tw.jwzhuang.ipcamviewer.preview;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;

import android.util.Log;

public class SocketClient {

	private String address = "";
	private int port = -1;
	private Socket client = null;
	private Boolean readStream = true;
	private BufferedInputStream in = null;
	private BufferedOutputStream out = null;
	private final String TAG = "SocketClient";
	private Preview activity = null;
	public SocketClient(Preview activity) {
		this.activity  = activity;
	}

	/**
	 * 設定伺服器IP位址
	 * @param adr String IP
	 * @param p	Integer Port
	 */
	public void setSrvAddress(String adr, int p) {
		this.address = adr;
		this.port = p;
	}

	/**
	 * 是否開啟
	 * @return Boolean
	 */
	public Boolean IsOpen() {
		return client.isConnected();
	}
	
	private String getSocketStr(String str)
			throws UnsupportedEncodingException {
		String groupStr = "";
		// String patternStr = "([\\S]+)";

		String patternStr = String.format("([^%s]+)", new String(new byte[1]));
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(str);
		while (matcher.find()) {
			for (int i = 0; i <= matcher.groupCount() - 1; i++) {
				if (groupStr.length() < matcher.group(i).length()) {
					groupStr = matcher.group(i);
				}
			}
		}
		return groupStr;
	}

	/**
	 * 取得伺服器傳送資料
	 * @throws IOException
	 */
	public void getMsg() throws IOException {
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					// android.os.Debug.waitForDebugger();
					Log.d("dddddd", "zzzz");
					while (readStream) {
						byte[] content = new byte[10240];
						in.read(content);
						// String recvStr = getSocketStr(new String(content,
						// "UTF8"));
						JSONObject jobj = new JSONObject(
								getSocketStr(new String(content, "UTF8")));
						if (jobj.optString("cmd") != null) {
							if (jobj.getString("cmd").equals("login")) {
								activity.setLoginState(jobj.getInt("state"));
							} else if (jobj.getString("cmd").equals("getparams")) {
								activity.setVideoParams(jobj.getString("sps"), jobj.getString("pps"), jobj.getInt("rate"));
							} else if (jobj.getString("cmd").equals("getstream")) {
								readStream = false;
								activity.setGetStreamState(jobj.getInt("state"), in);
							}
						}
					}
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}}).start();
	}
	
	/**
	 * 發送訊息至伺服器
	 * @param m String 訊息
	 */
	public void sentMsg(String m){
		sentMsg(m.getBytes());
	}

	/**
	 * 發送訊息至伺服器
	 * @param data byte array
	 */
	public void sentMsg(final byte[] data) {

		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					out.write(data);
					out.flush();
				} catch (Exception e) {
				}
			}}).start();
	}
	
	private void sentAlive(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
				JSONObject jobj = new JSONObject();
				jobj.put("cmd", "alive");
				while(readStream){
					sentMsg(jobj.toString());
					Thread.sleep(3000);
				}
				} catch (Exception e) {
				}
			}}).start();
	}

	/**
	 * 中斷與伺服器連線
	 * @throws IOException
	 */
	public void closeConnect() throws IOException {
		readStream = false;
		if (client != null) {
			client.close();
			client = null;
		}

		if (in != null) {
			in.close();
			in = null;
		}

		if (out != null) {
			out.close();
			out = null;
		}
	}

	/**
	 * 連接伺服器
	 * @throws IOException
	 */
	public void openConnect() throws IOException {
		if (client == null) {
			client = new Socket();
			InetSocketAddress isa = new InetSocketAddress(address, port);
			client.connect(isa, 10000);
		}

		if (out == null) {
			out = new BufferedOutputStream(client.getOutputStream());
		}

		if (in == null) {
			readStream = true;
			in = new BufferedInputStream(client.getInputStream());
		}
//		sentAlive();
	}
}
