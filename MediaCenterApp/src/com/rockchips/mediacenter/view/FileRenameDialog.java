/**
 * 
 */
package com.rockchips.mediacenter.view;
import java.io.File;
import java.util.List;
import momo.cn.edu.fjnu.androidutils.data.CommonValues;
import momo.cn.edu.fjnu.androidutils.utils.ToastUtils;
import org.xutils.view.annotation.ViewInject;
import com.rockchips.mediacenter.bean.Device;
import com.rockchips.mediacenter.bean.FileInfo;
import com.rockchips.mediacenter.data.ConstData;
import com.rockchips.mediacenter.utils.DialogUtils;
import com.rockchips.mediacenter.utils.FileOpUtils;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import com.rockchips.mediacenter.R;
/**
 * @author GaoFei
 * 文件重命名对话框
 */
public class FileRenameDialog extends AppBaseDialog implements View.OnClickListener{
	
	public interface Callback{
		void onFinish(int errorCode);
	}
	
	private FileInfo mFileInfo;
	private Callback mCallback;
	private Context mContext;
	private Device mDevice;
	@ViewInject(R.id.edit_file_name)
	private EditText mEditFileNmae;
	@ViewInject(R.id.btn_ok)
	private Button mBtnOk;
	@ViewInject(R.id.btn_cancel)
	private Button mBtnCancel;
	public FileRenameDialog(Context context, Device device, FileInfo fileInfo, Callback callback) {
		super(context);
		mFileInfo = fileInfo;
		mCallback = callback;
		mContext = context;
		mDevice = device;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
	
	@Override
	public int getLayoutRes() {
		return R.layout.dialog_file_rename;
	}
	
	
	@Override
	public void initData() {
		String fileName = mFileInfo.getName();
		mEditFileNmae.setText(fileName);
		mEditFileNmae.setSelection(fileName.length());
	}


	
	
	@Override
	public void initEvent() {
		mBtnOk.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_ok:
			final String fileName = mEditFileNmae.getText().toString().trim();
			if(TextUtils.isEmpty(fileName)){
				ToastUtils.showToast(mContext.getString(R.string.enter_new_file_name));
				return;
			}
			if(fileName.equals(mFileInfo.getName())){
				ToastUtils.showToast(mContext.getString(R.string.enter_diff_name));
			}else{
				dismiss();
				DialogUtils.showLoadingDialog(mContext, false);
				//启动一个异步任务操作
				new AsyncTask<FileInfo, Integer, Integer>() {
					@Override
					protected Integer doInBackground(FileInfo... params) {
						File currentFile = new File(mFileInfo.getPath());
						File parentFile = currentFile.getParentFile();
						File destFile = new File(currentFile.getParentFile(), fileName);
						if(!parentFile.canWrite())
							parentFile.setWritable(true);
						if(!currentFile.canWrite())
							currentFile.setWritable(true);
						//获取重命名之前的所有文件路径
						List<String> allBeforePaths = FileOpUtils.getAllFilePaths(currentFile);
						boolean success = currentFile.renameTo(destFile);
						//获取重命名之后的所有文件路径
						List<String> afterPaths = FileOpUtils.getAllFilePaths(destFile);
						if(!success)
							return ConstData.FileOpErrorCode.RENAME_ERR;
						else{
							//更新媒体库文件
							if(allBeforePaths != null && allBeforePaths.size() > 0)
								MediaScannerConnection.scanFile(mContext, allBeforePaths.toArray(new String[0]), null, null);
							if(afterPaths != null && afterPaths.size() > 0)
								MediaScannerConnection.scanFile(mContext, afterPaths.toArray(new String[0]), null, null);
							//更新本地数据库文件
							//FileInfoService fileInfoService = new FileInfoService();
							//fileInfoService.deleteFileInfos(mFileInfo.getDeviceID(), currentFile.getPath());
							//发送重新触发扫描广播
							String deviceID = ConstData.devicePathIDs.get(mDevice.getLocalMountPath());
							if(deviceID != null){
								//更新本地数据库
								Intent broadIntent = new Intent(ConstData.BroadCastMsg.RESCAN_DEVICE);
								broadIntent.putExtra(ConstData.IntentKey.EXTRA_DEVICE_ID, deviceID);
								LocalBroadcastManager.getInstance(CommonValues.application).sendBroadcast(broadIntent);
							}
							return ConstData.FileOpErrorCode.NO_ERR;
						}
							
					}
					
					protected void onPostExecute(Integer result) {
						mCallback.onFinish(result);
					};
					
				}.execute(mFileInfo);
				
			}
			break;
		case R.id.btn_cancel:
			dismiss();
			break;
		default:
			break;
		}
	}

	
	public void setAllFileInfo(FileInfo fileInfo){
		mFileInfo = fileInfo;
		String fileName = mFileInfo.getName();
		mEditFileNmae.requestFocus();
		mEditFileNmae.setText(fileName);
		mEditFileNmae.setSelection(fileName.length());
	}
	
}
