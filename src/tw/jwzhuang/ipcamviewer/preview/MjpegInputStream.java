package tw.jwzhuang.ipcamviewer.preview;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import tw.jwzhuang.ipcamviewer.devices.Utils;
import tw.jwzhuang.ipcamviewer.h264.Decoder;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.util.Log;

public class MjpegInputStream extends DataInputStream {
	private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
	private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
	private final static int HEADER_MAX_LENGTH = 100;
	private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;
	private int mContentLength = -1;
	public static String sessionID = "";
	private Decoder h264Decoder = null;
	private byte[] mPixel = new byte[320*240*2];
	private Bitmap VideoBit = Bitmap.createBitmap(320 ,240, Config.RGB_565);
	private ByteBuffer buffers = ByteBuffer.wrap(mPixel);

	public MjpegInputStream(InputStream in, Decoder h264Decoder) {
		super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
		this.h264Decoder = h264Decoder;
	}
	
	private int getEndOfSeqeunce(DataInputStream in, byte[] sequence)
			throws IOException {
		int seqIndex = 0;
		byte c;
		for (int i = 0; i < FRAME_MAX_LENGTH; i++) {
			c = (byte) in.readUnsignedByte();
			if (c == sequence[seqIndex]) {
				seqIndex++;
				if (seqIndex == sequence.length)
					return i + 1;
			} else
				seqIndex = 0;
		}
		return -1;
	}

	private int getStartOfSequence(DataInputStream in, byte[] sequence)
			throws IOException {
		int end = getEndOfSeqeunce(in, sequence);
		return (end < 0) ? (-1) : (end - sequence.length);
	}

	public Bitmap readFrame() throws IOException {
		byte[] buffer = readMjpegByte();
		
		int len = h264Decoder.decodeFrame(buffer, buffer.length, mPixel);
		if(len == -1){
			return null;
		}
		
		VideoBit.copyPixelsFromBuffer(buffers);
		buffers.position(0);
		return Utils.RotateBitmap(VideoBit, 90);
	}
	
	public byte[] readMjpegByte() throws IOException {
		mark(FRAME_MAX_LENGTH);
		int headerLen = getStartOfSequence(this, SOI_MARKER);
		reset();
		byte[] header = new byte[headerLen];
		readFully(header);
		mContentLength = getEndOfSeqeunce(this, EOF_MARKER);
		reset();
		byte[] frameData = new byte[mContentLength];
		skipBytes(headerLen);
		readFully(frameData);
		return ArrayUtils.subarray(frameData, 2, mContentLength-2);
	}

	public void shutdown() throws IOException {
		close();
	}

}
