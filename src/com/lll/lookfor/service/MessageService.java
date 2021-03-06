package com.lll.lookfor.service;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.lll.lookfor.BaseApplication;
import com.lll.lookfor.HttpInterface;
import com.lll.lookfor.model.FriendBean;
import com.lll.lookfor.network.HooHttpResponse;
import com.lll.lookfor.network.OnHttpResponseListener;
import com.lll.lookfor.network.ResponseHandler;
import com.lll.lookfor.utils.Log;
import com.lll.lookfor.utils.SharePreferenceUtil;

public class MessageService extends Service {
	private final String TAG = "MessageService";
	private Timer timer;
	private SharePreferenceUtil share;

	public void onCreate() {
		super.onCreate();
		this.share = BaseApplication.getInstance().getSharePreferenceUtil();// 获取sharereferenceUtil

		// 开启时间任务，每十秒请求一次
		timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				Log.v(TAG, "Service TimerTask for every 10 seconds");
				mHandler.sendEmptyMessage(0);
			}
		}, 0, 10 * 1000);
		Log.i(TAG, "startService onCreate");
	}

	@SuppressLint("HandlerLeak")
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			// process incoming messages here
			switch (msg.what) {
			case 0:
				getMessageList();
				break;
			default:
				break;
			}
		}
	};

	public void onDestroy() {
		if (timer != null) {
			timer.cancel();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	// ------------------------------------------网络请求及响应事件-----------------------------------------------

	/**
	 * 获取数据
	 */
	private void getMessageList() {
		ResponseHandler<FriendBean> handler = new ResponseHandler<FriendBean>(
				FriendBean.class);
		handler.setOnHttpResponseListener(new OnGetHomeMessageListener());
		if (share.getIsLogin()) {// 当用户登录后才进行查询
//			Log.w(TAG, "消息轮询中:" + HttpInterface.FRIEND_LIST);
//			HttpUtil.get(HttpInterface.FRIEND_LIST, handler);
		}
	}

	/** 请求站内信相应事件 */
	private class OnGetHomeMessageListener implements OnHttpResponseListener {

		@SuppressWarnings("rawtypes")
		@Override
		public void onSuccess(HooHttpResponse response) {
			int rc = response.getHeader().getRc();
			String rm = response.getHeader().getRm();
			if (rc == 0) {
				FriendBean friendList = (FriendBean) response.getBody();
				if (friendList != null && friendList.getItems().size() > 0) {
					BaseApplication.getInstance().getAll_friends().clear();
					BaseApplication.getInstance().getStatus_friends().clear();

					// 添加全部好友集合
					BaseApplication.getInstance().getAll_friends()
							.addAll(friendList.getItems());
					// 添加可见好友集合
					for (int i = 0; i < friendList.getItems().size(); i++) {
						FriendBean uBean = friendList.getItems().get(i);
						if (uBean.getStatus() == 1) {
							BaseApplication.getInstance().getStatus_friends()
									.add(uBean);
						}
					}
				}
				Log.e(TAG, "获取好友列表成功:"
						+ BaseApplication.getInstance().getAll_friends()
								.toString());
				Log.e(TAG, "获取可见好友列表成功:"
						+ BaseApplication.getInstance().getStatus_friends()
								.toString());
			} else {
				Log.e(TAG, "获取新消息失败:" + "RC=" + rc + "RM=" + rm);
			}

		}

		@Override
		public void onError(int statusCode, Throwable error, String content) {
			Log.e(TAG, "获取新消息失败" + error);
		}

		@Override
		public void onStart() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onEnd() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProgress(int bytesWritten, int totalSize) {
			// TODO Auto-generated method stub

		}

	}
}
