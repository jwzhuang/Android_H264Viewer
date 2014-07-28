package tw.jwzhuang.ipcamviewer.h264;

import android.util.Log;

public class Decoder {
	
	static {
		System.loadLibrary("H264Decoder");
		Log.i("H264Decoder", "Load Library");
	}
	public native int Init();

	public native int free();

	public native int decodeFrame(byte[] in, int insize, byte[] out);

	
}
