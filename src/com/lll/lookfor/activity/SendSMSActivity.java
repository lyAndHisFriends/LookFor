package com.lll.lookfor.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

import com.lll.lookfor.R;

public class SendSMSActivity extends Activity implements OnClickListener {

	private Button sensmsButton, verificationButton, countryButton;
	private TextView countryTextView, textView2;
	private EditText phonEditText, verEditText;
	// 填写从短信SDK应用后台注册得到的APPKEY
	private static String APPKEY = "518ec8a6aa30";
	// 填写从短信SDK应用后台注册得到的APPSECRET
	private static String APPSECRET = "23f82461e51ac52d992af8117fe5d331";
	public String phString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_sendsms);
		sensmsButton = (Button) findViewById(R.id.button1);
		verificationButton = (Button) findViewById(R.id.button2);
		countryButton = (Button) findViewById(R.id.button3);
		countryTextView = (TextView) findViewById(R.id.textView1);
		textView2 = (TextView) findViewById(R.id.textView2);
		phonEditText = (EditText) findViewById(R.id.editText1);
		verEditText = (EditText) findViewById(R.id.editText2);
		sensmsButton.setOnClickListener(this);
		verificationButton.setOnClickListener(this);
		countryButton.setOnClickListener(this);

		SMSSDK.initSDK(this, APPKEY, APPSECRET);
		EventHandler eh = new EventHandler() {

			@Override
			public void afterEvent(int event, int result, Object data) {

				Message msg = new Message();
				msg.arg1 = event;
				msg.arg2 = result;
				msg.obj = data;
				handler.sendMessage(msg);
			}

		};
		SMSSDK.registerEventHandler(eh);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.button1:// 获取验证码
			if (!TextUtils.isEmpty(phonEditText.getText().toString())) {
				SMSSDK.getVerificationCode("86", phonEditText.getText()
						.toString());
				phString = phonEditText.getText().toString();
			} else {
				Toast.makeText(this, "电话不能为空", 1).show();
			}

			break;
		case R.id.button2:// 校验验证码
			if (!TextUtils.isEmpty(verEditText.getText().toString())) {
				SMSSDK.submitVerificationCode("86", phString, verEditText
						.getText().toString());
			} else {
				Toast.makeText(this, "验证码不能为空", 1).show();
			}
			break;
		case R.id.button3:// 国家列表
			SMSSDK.getSupportedCountries();
			break;
		default:
			break;
		}
	}

	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			int event = msg.arg1;
			int result = msg.arg2;
			Object data = msg.obj;
			Log.e("event", "event=" + event);
			if (result == SMSSDK.RESULT_COMPLETE) {
				// 短信注册成功后，返回MainActivity,然后提示新好友
				if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {// 提交验证码成功
					Toast.makeText(getApplicationContext(), "提交验证码成功",
							Toast.LENGTH_SHORT).show();
					textView2.setText("提交验证码成功");
				} else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
					Toast.makeText(getApplicationContext(), "验证码已经发送",
							Toast.LENGTH_SHORT).show();
					textView2.setText("验证码已经发送");
				} else if (event == SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES) {// 返回支持发送验证码的国家列表
					Toast.makeText(getApplicationContext(), "获取国家列表成功",
							Toast.LENGTH_SHORT).show();
					countryTextView.setText(data.toString());

				}
			} else {
				((Throwable) data).printStackTrace();
			}

		}

	};

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		SMSSDK.unregisterAllEventHandler();
	}

}
