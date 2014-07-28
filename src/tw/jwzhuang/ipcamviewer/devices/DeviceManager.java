package tw.jwzhuang.ipcamviewer.devices;

import java.util.ArrayList;
import java.util.Arrays;

import tw.jwzhuang.ipcamviewer.R;
import tw.jwzhuang.ipcamviewer.preview.MyApplication;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class DeviceManager extends ListActivity implements OnItemClickListener{

	private ArrayAdapter<String> mAdapter;
	private boolean mode_chose = false;
	private SQLiteHelper sh = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyApplication.getInstance().addActivity(this);
		
		if(getIntent().hasExtra("mode")){
			super.setTheme( android.R.style.Theme_Dialog );
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			mode_chose = true;
		}
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.devices);
		sh = new SQLiteHelper(this, DBStatements.DB, null, 1, DBStatements.Cameras_TABLE, DBStatements.Cameras_Fileds, DBStatements.Cameras_FieldType);
		mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,new ArrayList<String>(Arrays.asList(getDevices())));
		
        setListAdapter(mAdapter);
		ListView listView = getListView();
		SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return !mode_chose;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (final int position : reverseSortedPositions) {
                                	final String ip = Utils.getIPfromString(mAdapter.getItem(position));
                                	if(ip.isEmpty()){
                                		return;
                                	}
                                	AlertDialog.Builder altDlgBldr = new AlertDialog.Builder(DeviceManager.this);
                    				altDlgBldr.setMessage(String.format(getResources().getString(R.string.isdel), Utils.getCamerafromString(mAdapter.getItem(position))));
                    				altDlgBldr.setPositiveButton(
                    						getResources().getText(R.string.yes),
                    						new DialogInterface.OnClickListener() {
                    							@Override
                    							public void onClick(DialogInterface dialog, int which) {
                    								mAdapter.remove(mAdapter.getItem(position));
                    								deleteDevicebyIP(ip);
                    							}
                    						});

                    				altDlgBldr.setNegativeButton(getResources().getText(R.string.no),
                    						null).show();
                                    
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        });
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        listView.setOnItemClickListener(this);
	}

	@Override
	protected void onResume(){
		super.onResume();
		mAdapter.clear();
		mAdapter.addAll(new ArrayList<String>(Arrays.asList(getDevices())));
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDestroy() {
		sh.close();
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, CaptureActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String[] getDevices(){
		Cursor cs = sh.select(DBStatements.Cameras_TABLE[0], new String[]{"ip"});
		String[] items = new String[cs.getCount()];
		cs.moveToFirst();
		for(int i = 1 ;i <= cs.getCount() ; i++){
			items[i - 1] = String.format("Camera%d - %s", i,cs.getString(0));
			cs.moveToNext();
		}
		cs.close();
		return items;
	}
	
	private void deleteDevicebyIP(String ip){
		sh.delete(DBStatements.Cameras_TABLE[0], "ip = ?", new String[]{ip});
	}
	
	private String getPwdbyIP(String ip){
		Cursor cs = sh.select(DBStatements.Cameras_TABLE[0], new String[]{"pwd"},"ip = ?",new String[]{ip});
		cs.moveToFirst();
		String pwd = cs.getString(0);
		cs.close();
		return pwd;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if(!mode_chose){
			return;
		}
		String ip = Utils.getIPfromString(mAdapter.getItem(position));
		String pwd = getPwdbyIP(ip);
		
		Intent intent = new Intent();       //創建一個Intent，聯繫Activity之用	
		Bundle bundleBack = new Bundle();   //創建一個Bundle，傳值之用
		bundleBack.putString("ip", ip);
		bundleBack.putString("pwd", pwd);
		intent.putExtras(bundleBack);       
		setResult(RESULT_OK, intent);
		finish();
	}

}
