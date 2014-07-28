/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.laomo.zxing.decode;

import java.util.Collection;

import tw.jwzhuang.ipcamviewer.R;
import tw.jwzhuang.ipcamviewer.devices.CaptureActivity;
import tw.jwzhuang.ipcamviewer.devices.DBStatements;
import tw.jwzhuang.ipcamviewer.devices.SQLiteHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.laomo.zxing.camera.CameraManager;
import com.laomo.zxing.view.ViewfinderResultPointCallback;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {

	private static final String TAG = CaptureActivityHandler.class
			.getSimpleName();

	private final CaptureActivity activity;
	private final DecodeThread decodeThread;
	private State state;
	private final CameraManager cameraManager;
	public final short ASKADD = 0x1;
	public final short RESCAN = 0x2;
	public final short ADDOTH = 0x3;
	private SQLiteHelper sh = null;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	public CaptureActivityHandler(CaptureActivity activity,
			Collection<BarcodeFormat> decodeFormats, String characterSet,
			CameraManager cameraManager) {
		this.activity = activity;
		decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
				new ViewfinderResultPointCallback(activity.getViewfinderView()));
		decodeThread.start();
		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		this.cameraManager = cameraManager;
		cameraManager.startPreview();
		restartPreviewAndDecode();
		
		sh = new SQLiteHelper(activity, DBStatements.DB,null,1,DBStatements.Cameras_TABLE, DBStatements.Cameras_Fileds, DBStatements.Cameras_FieldType);
	}

	@Override
	public void handleMessage(Message message) {
		Bundle bundle = null;
		AlertDialog.Builder altDlgBldr = null;
		switch (message.what) {
		case R.id.auto_focus:
			// Log.d(TAG, "Got auto-focus message");
			// When one auto focus pass finishes, start another. This is the
			// closest thing to
			// continuous AF. It does seem to hunt a bit, but I'm not sure what
			// else to do.
			if (state == State.PREVIEW) {
				cameraManager.requestAutoFocus(this, R.id.auto_focus);
			}
			break;
		case R.id.restart_preview:
			Log.d(TAG, "Got restart preview message");
			restartPreviewAndDecode();
			break;
		case R.id.decode_succeeded:
			Log.d(TAG, "Got decode succeeded message");
			state = State.SUCCESS;
			bundle = message.getData();
			Bitmap barcode = bundle == null ? null : (Bitmap) bundle
					.getParcelable(DecodeThread.BARCODE_BITMAP);
			activity.handleDecode((Result) message.obj, barcode);
			break;
		case R.id.decode_failed:
			// We're decoding as fast as possible, so when one decode fails,
			// start another.
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(),
					R.id.decode);
			break;
		case R.id.return_scan_result:
			Log.d(TAG, "Got return scan result message");
			activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
			activity.finish();
			break;
		case R.id.launch_product_query:
			Log.d(TAG, "Got product query message");
			String url = (String) message.obj;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			activity.startActivity(intent);
			break;
		case ASKADD:
			
			
			
			bundle = message.getData();
			final String pwd = bundle.getString("pwd");
			final String ip = bundle.getString("ip");
			
			Cursor cs = sh.select(DBStatements.Cameras_TABLE[0], new String[]{"_id"}, "ip = ? and pwd = ?", new String[]{ip,pwd}, null, null, null);
			if(cs.getCount() == 0){
				altDlgBldr = new AlertDialog.Builder(activity);
				altDlgBldr.setMessage(activity.getResources().getText(R.string.isadd));
				altDlgBldr.setPositiveButton(
						activity.getResources().getText(R.string.yes),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								sh.insert(DBStatements.Cameras_TABLE[0], new String[]{"ip" ,"pwd"},  new String[]{ip, pwd});
								Toast.makeText(activity, R.string.addsuc, 
								   Toast.LENGTH_SHORT).show();
								activity.restartPreviewAfterDelay(2L);
							}
						});

				altDlgBldr.setNegativeButton(activity.getResources().getText(R.string.rescan),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								activity.restartPreviewAfterDelay(0L);
							}

						}).show();
			}else{
				activity.restartPreviewAfterDelay(0L);
			}
			cs.close();
			
			break;
		case RESCAN:
			bundle = message.getData();
			
			break;
		}
		
	}

	public void quitSynchronously() {
		sh.close();
		state = State.DONE;
		cameraManager.stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			// Wait at most half a second; should be enough time, and onPause()
			// will timeout quickly
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(),
					R.id.decode);
			cameraManager.requestAutoFocus(this, R.id.auto_focus);
			activity.drawViewfinder();
		}
	}

}
