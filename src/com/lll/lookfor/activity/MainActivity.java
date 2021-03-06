package com.lll.lookfor.activity;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.lll.lookfor.BaseApplication;
import com.lll.lookfor.HttpInterface;
import com.lll.lookfor.R;
import com.lll.lookfor.adapter.DrawerListAdapter;
import com.lll.lookfor.adapter.VisiableFriendAdapter;
import com.lll.lookfor.crossbutton.CrossButtonFragment;
import com.lll.lookfor.model.DrawerItem;
import com.lll.lookfor.model.FriendBean;
import com.lll.lookfor.model.LbsBean;
import com.lll.lookfor.network.HooHttpResponse;
import com.lll.lookfor.network.OnHttpResponseListener;
import com.lll.lookfor.network.ResponseHandler;
import com.lll.lookfor.ui.InfoWindow_View;
import com.lll.lookfor.ui.Overlay_View;
import com.lll.lookfor.utils.HttpUtil;
import com.lll.lookfor.utils.Log;
import com.lll.lookfor.utils.SharePreferenceUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class MainActivity extends Activity implements OnClickListener {
	private final String TAG = "MainActivity";
	// 左侧侧滑菜单栏
	private ListView mDrawerList;
	private ArrayList<DrawerItem> list;
	private DrawerListAdapter adapter;
	private ImageView left_drawer_img;
	private DrawerLayout mDrawerLayout;

	// 在线好友按钮
	private Button btn_home_right;
	// 判断好友列表是否展开状态
	private boolean isFriendListShow;

	// 定位相关
	private LocationClient mLocClient;
	private MyLocationListenner myListener;
	boolean isFirstLoc = true;// 是否首次定位
	// 地图相关
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	// 自定义图标
	private InfoWindow mInfoWindow;
	private ArrayList<Marker> markerList;
	private ArrayList<BitmapDescriptor> bitmapList;
	// Imageloader配置
	private DisplayImageOptions overly_options;
	private DisplayImageOptions head_options;

	private Button btn_home_recovery;// 定位按钮
	private LatLng ll_recovery;// 当前经纬度
	private LinearLayout ll_userinfo;// 底部用户信息
	private TextView tv_userinfo_name;// 用户名称
	private Button btn_userinfo_close;// 关闭图标
	private TextView tv_userinfo_position;// 用户地址
	private TextView tv_userinfo_time;// 用户最后登陆时间
	private Button btn_userinfo_tohere;// 去TA那按钮
	private Button btn_userinfo_hide;// 用户信息隐藏部分信息按钮
	private LinearLayout ll_userinfo_bottom;// 用户信息部分隐藏信息
	private TextView tv_click;// 点击登录
	private TextView tv_nickname;// 昵称
	private ImageView img_portrait;// 头像
	private RelativeLayout rl_drawer;

	private final static String MY_ACTION = "MYACTION";
	protected MyReceiver myReceiver;
	private SharePreferenceUtil sharePfUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_drawer);
		conHandler = fListHandler;
		// 创建底部导航按钮碎片
		if (savedInstanceState == null) {
			getFragmentManager()
					.beginTransaction()
					.add(R.id.cross_button_container, new CrossButtonFragment())
					.commit();
		}

		this.markerList = new ArrayList<Marker>();
		this.bitmapList = new ArrayList<BitmapDescriptor>();
		this.sharePfUtil = BaseApplication.getInstance()
				.getSharePreferenceUtil();
		this.head_options = new DisplayImageOptions.Builder()
				.showImageOnFail(R.drawable.left_login_up)
				.showImageOnLoading(R.drawable.left_login_up)
				.showImageForEmptyUri(R.drawable.left_login_up)
				.bitmapConfig(Bitmap.Config.RGB_565).cacheInMemory(true)
				.cacheOnDisk(true).displayer(new RoundedBitmapDisplayer(360))
				.build();
		this.overly_options = new DisplayImageOptions.Builder()
				.bitmapConfig(Bitmap.Config.RGB_565).cacheInMemory(true)
				.cacheOnDisk(true).displayer(new RoundedBitmapDisplayer(360))
				.build();

		initView();
		initMyLocation();
		setHeadUserInfo();
		myReceiver = new MyReceiver();
	}

	/**
	 * 初始化视图控件
	 */
	private void initView() {
		rl_drawer = (RelativeLayout) findViewById(R.id.rl_left_drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		left_drawer_img = (ImageView) findViewById(R.id.left_drawer_img);
		left_drawer_img.setOnClickListener(this);
		btn_home_right = (Button) findViewById(R.id.btn_home_right);
		btn_home_right.setOnClickListener(this);
		mDrawerList = (ListView) findViewById(R.id.lv_left_drawer);

		// ListView头部
		View listHead = LayoutInflater.from(this).inflate(
				R.layout.drawer_list_head, null);
		tv_click = (TextView) listHead.findViewById(R.id.tv_head_click);
		tv_nickname = (TextView) listHead.findViewById(R.id.tv_head_nickname);
		img_portrait = (ImageView) listHead
				.findViewById(R.id.img_head_portrait);

		mDrawerList.addHeaderView(listHead);
		LinearLayout setting = (LinearLayout) findViewById(R.id.ll_footer);
		setting.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						SettingActivity.class);
				startActivity(intent);
			}
		});

		this.list = new ArrayList<DrawerItem>();

		list.add(new DrawerItem(R.drawable.left_quanta,
				getString(R.string.my_circles)));
		list.add(new DrawerItem(R.drawable.left_message_default,
				getString(R.string.my_message)));
		list.add(new DrawerItem(R.drawable.left_friends,
				getString(R.string.my_friend)));
		list.add(new DrawerItem(R.drawable.left_application,
				getString(R.string.my_request)));
		adapter = new DrawerListAdapter(this, list);
		mDrawerList.setAdapter(adapter);

		// 左边侧滑栏点击事件处理
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == 0) {
					if (!sharePfUtil.getIsLogin()) {// 登录状态判断
						Intent intent = new Intent(MainActivity.this,
								LoginActivity.class);
						startActivity(intent);
					} else {
						Intent intent = new Intent(MainActivity.this,
								ModifyDataActivity.class);
						startActivity(intent);
					}
				} else if (position == 1) {
					Intent intent = new Intent(MainActivity.this,
							MyCirclesActivity.class);
					startActivity(intent);
				} else if (position == 2) {
					Intent intent = new Intent(MainActivity.this,
							MyMessageActivity.class);
					startActivity(intent);
				} else if (position == 3) {
					Intent intent = new Intent(MainActivity.this,
							FriendListActivity.class);
					startActivity(intent);
				} else if (position == 4) {
					Intent intent = new Intent(MainActivity.this,
							MyRequesActivity.class);
					startActivity(intent);
				}
			}
		});

		// 地图初始化
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();

		// 隐藏缩放控件
		mMapView.showZoomControls(false);
		// 隐藏比例尺控件
		mMapView.showScaleControl(false);
		// 隐藏指南针
		mBaiduMap.getUiSettings().setCompassEnabled(false);
		// 删除百度地图logo
		mMapView.removeViewAt(1);
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		// 关闭俯视功能
		mBaiduMap.getUiSettings().setOverlookingGesturesEnabled(false);

		// 默认百度地图显示500米
		MapStatusUpdate u = MapStatusUpdateFactory.zoomTo(15);
		mBaiduMap.animateMapStatus(u);

		if ("mapmode".equals(sharePfUtil.getMapExhibition())) {
			mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
		} else {
			mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
		}

		mBaiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {
			public boolean onMarkerClick(final Marker marker) {
				selectorOverlay(marker);
				return true;
			}
		});

		// 地图点击事件
		mBaiduMap.setOnMapClickListener(new OnMapClickListener() {
			public void onMapClick(LatLng point) {
				Log.e("MainActivity", "单击地图");
				mBaiduMap.hideInfoWindow();
				ll_userinfo.setVisibility(View.GONE);
				addOverlay(BaseApplication.getInstance().getAll_friends());
			}

			public boolean onMapPoiClick(MapPoi poi) {
				return false;
			}
		});

		btn_home_recovery = (Button) findViewById(R.id.btn_home_recovery);
		btn_home_recovery.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapStatusUpdate u = MapStatusUpdateFactory
						.newLatLng(ll_recovery);
				mBaiduMap.animateMapStatus(u);
			}
		});

		ll_userinfo = (LinearLayout) findViewById(R.id.ll_userinfo);
		tv_userinfo_name = (TextView) findViewById(R.id.userinfo_name);
		btn_userinfo_close = (Button) findViewById(R.id.userinfo_close);
		tv_userinfo_position = (TextView) findViewById(R.id.userinfo_position);
		tv_userinfo_time = (TextView) findViewById(R.id.userinfo_time);
		btn_userinfo_tohere = (Button) findViewById(R.id.userinfo_tohere);
		btn_userinfo_hide = (Button) findViewById(R.id.userinfo_hide);
		ll_userinfo_bottom = (LinearLayout) findViewById(R.id.userinfo_bottom);
	}

	/**
	 * 设置头部用户信息
	 */
	private void setHeadUserInfo() {
		if (sharePfUtil.getIsLogin()) {
			tv_click.setVisibility(View.GONE);
			tv_nickname.setVisibility(View.VISIBLE);
			tv_nickname.setText(sharePfUtil.getNickname());
			ImageLoader.getInstance().displayImage(
					sharePfUtil.getPortraitPic(), img_portrait, head_options);
		} else {
			tv_click.setVisibility(View.VISIBLE);
			tv_nickname.setVisibility(View.GONE);
		}
	}

	/**
	 * 设置用户信息，从底部弹出
	 */
	private void setBottomUserInfo(LbsBean userBean) {
		tv_userinfo_name.setText(userBean.getNickName());// 用户昵称
		tv_userinfo_position.setText(userBean.getLocation());// 用户地址
		tv_userinfo_time.setText(userBean.getUpdateTime());// 用户最后登陆时间

		ll_userinfo.setVisibility(View.VISIBLE);
		ll_userinfo_bottom.setVisibility(View.GONE);

		btn_userinfo_close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 隐藏用户信息界面
				ll_userinfo.setVisibility(View.GONE);
			}
		});

		btn_userinfo_hide.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (ll_userinfo_bottom.getVisibility() == View.GONE) {
					ll_userinfo_bottom.setVisibility(View.VISIBLE);
					btn_userinfo_hide
							.setBackgroundResource(R.drawable.selector_home_arrowup);
				} else {
					ll_userinfo_bottom.setVisibility(View.GONE);
					btn_userinfo_hide
							.setBackgroundResource(R.drawable.selector_home_arrowdown);
				}
			}
		});
	}

	/**
	 * 初始化定位相关代码
	 */
	private void initMyLocation() {
		// 定位初始化
		mLocClient = new LocationClient(this);
		myListener = new MyLocationListenner();
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开gps
		option.setCoorType("bd09ll"); // 设置坐标类型
		option.setScanSpan(1000);
		option.setLocationNotify(false);
		option.setAddrType("all");
		mLocClient.setLocOption(option);
	}

	/**
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			if (location == null || mMapView == null)
				return;

			ll_recovery = new LatLng(location.getLatitude(),
					location.getLongitude());
			if (!TextUtils.isEmpty(location.getCity())) {
				BaseApplication.getInstance().getSharePreferenceUtil()
						.setCity(location.getCity());
			}
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
				mBaiduMap.animateMapStatus(u);

				addOverlay(BaseApplication.getInstance().getAll_friends());
			}
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	/**
	 * 增加覆盖物
	 */
	public void addOverlay(ArrayList<FriendBean> infos) {
		mBaiduMap.clear();
		for (final FriendBean info : infos) {
			final Overlay_View item = new Overlay_View(MainActivity.this);

			ImageLoader.getInstance().displayImage(info.getPortrait(),
					item.getItem_img(), overly_options,
					new SimpleImageLoadingListener() {
						public void onLoadingComplete(String imageUri,
								android.view.View view,
								android.graphics.Bitmap loadedImage) {
							item.getItem_text().setText(info.getNickName());
							BitmapDescriptor bdA = BitmapDescriptorFactory
									.fromView(item.getView());
							bitmapList.add(bdA);
							// 位置
							LatLng latLng = new LatLng(info.getLatitude(), info
									.getLongitude());
							OverlayOptions overlayOptions = new MarkerOptions()
									.position(latLng).icon(bdA);
							Marker marker = (Marker) (mBaiduMap
									.addOverlay(overlayOptions));
							// 将实体传递
							Bundle bundle = new Bundle();
							bundle.putSerializable("info", info);
							marker.setExtraInfo(bundle);
							markerList.add(marker);
						};
					});

		}
	}

	/**
	 * 选中覆盖物
	 */
	private void selectorOverlay(Marker selector) {
		for (int i = 0; i < markerList.size(); i++) {
			final Marker position = markerList.get(i);
			if (position == selector) {
				final Overlay_View item = new Overlay_View(MainActivity.this);
				final FriendBean info = (FriendBean) position.getExtraInfo()
						.get("info");
				ImageLoader.getInstance().displayImage(info.getPortrait(),
						item.getItem_img(), overly_options,
						new SimpleImageLoadingListener() {
							public void onLoadingComplete(String imageUri,
									android.view.View view,
									android.graphics.Bitmap loadedImage) {
								item.getItem_text().setVisibility(View.GONE);
								item.getItem_bg().setBackgroundResource(
										R.drawable.icon_user_selected);
								BitmapDescriptor bdA = BitmapDescriptorFactory
										.fromView(item.getView());
								bitmapList.add(bdA);
								position.setIcon(bdA);
							};
						});

				InfoWindow_View infoWindow_View = new InfoWindow_View(
						MainActivity.this);
				infoWindow_View.getHere().setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(MainActivity.this,
										RoutePlanActivity.class);
								intent.putExtra("en", info);
								startActivity(intent);
							}
						});
				infoWindow_View.getQuanta().setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {

							}
						});
				LatLng ll = position.getPosition();
				mInfoWindow = new InfoWindow(infoWindow_View.getView(), ll,
						-130);
				mBaiduMap.showInfoWindow(mInfoWindow);
				getUserData();
			} else {
				final Overlay_View item = new Overlay_View(MainActivity.this);
				Marker otherMarker = markerList.get(i);
				final FriendBean info = (FriendBean) otherMarker.getExtraInfo()
						.get("info");
				ImageLoader.getInstance().displayImage(info.getPortrait(),
						item.getItem_img(), overly_options,
						new SimpleImageLoadingListener() {
							public void onLoadingComplete(String imageUri,
									android.view.View view,
									android.graphics.Bitmap loadedImage) {
								item.getItem_text().setText(info.getNickName());
								item.getItem_bg().setBackgroundResource(
										R.drawable.icon_user);
								BitmapDescriptor bdA = BitmapDescriptorFactory
										.fromView(item.getView());
								bitmapList.add(bdA);
								position.setIcon(bdA);
							};
						});
			}

		}
	}

	@Override
	protected void onStart() {
		// 开启图层定位
		mBaiduMap.setMyLocationEnabled(true);
		if (!mLocClient.isStarted()) {
			mLocClient.start();
		}
		super.onStart();
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(MY_ACTION);
		filter.addAction(BaseApplication.BRODCAST_ISLOGIN);
		filter.addAction(BaseApplication.BRODCAST_MAPMODE);
		registerReceiver(myReceiver, filter);
		mMapView.onResume();

		if (sharePfUtil.getIsLogin()) {
			getFriendList();
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// 退出时销毁定位
		mLocClient.stop();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;

		// 回收 bitmap 资源
		if (bitmapList != null && bitmapList.size() > 0) {
			for (int i = 0; i < bitmapList.size(); i++) {
				BitmapDescriptor bitmap = bitmapList.get(i);
				bitmap.recycle();
			}
		}
		unregisterReceiver(myReceiver);
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.left_drawer_img:
			if (mDrawerLayout.isDrawerOpen(rl_drawer)) {
				mDrawerLayout.closeDrawer(rl_drawer);
			} else {
				mDrawerLayout.openDrawer(rl_drawer);
			}
			break;
		case R.id.btn_home_right:
			if (!isFriendListShow) {
				// showFriendList();
				fListHandler.sendEmptyMessage(1);// 列表显示
				fListHandler.sendEmptyMessageDelayed(2, 3000);// 3秒后隐藏
			} else {
				// hideFriendList();
				fListHandler.sendEmptyMessage(0);// 列表隐藏
				fListHandler.removeMessages(2);// 取消自动隐藏
			}

			break;
		default:
			break;
		}

	}

	/** 显示可见好友列表 */
	private void showFriendList() {
		RelativeLayout friend_list_container = (RelativeLayout) findViewById(R.id.friend_list_container);

		if (FriendListFragment.getInstance() == null) {
			getFragmentManager()
					.beginTransaction()
					.replace(R.id.friend_list_container,
							FriendListFragment.newInstance()).commit();
		} else {
			getFragmentManager().beginTransaction()
					.show(FriendListFragment.getInstance()).commit();
		}
		// 添加一个简单动画，避免添加View太生硬
		Animation animation = AnimationUtils.loadAnimation(MainActivity.this,
				android.R.anim.fade_in);
		friend_list_container.startAnimation(animation);
		friend_list_container.setVisibility(View.VISIBLE);
		isFriendListShow = true;
	}

	/** 隐藏可见好友列表 */
	private void hideFriendList() {
		RelativeLayout friend_list_container = (RelativeLayout) findViewById(R.id.friend_list_container);
		// 添加一个简单动画，避免添加View太生硬
		Animation animation = AnimationUtils.loadAnimation(MainActivity.this,
				android.R.anim.fade_out);
		animation.setAnimationListener(new AnimationListener() {// 设置一下动画监听
					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {// 当渐隐动画播放完后，再销毁Fragment
						isFriendListShow = false;
						getFragmentManager().beginTransaction()
								.remove(FriendListFragment.getInstance())
								.commit();// 务必销毁之，这个Fragment每次都要重新来过
					}
				});
		friend_list_container.startAnimation(animation);
		friend_list_container.setVisibility(View.GONE);
	}

	/** 静态Handler，用于Fragment通讯 */
	public static Handler conHandler;

	@SuppressLint("HandlerLeak")
	private Handler fListHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 0:
				hideFriendList();
				break;
			case 1:
				showFriendList();
				break;
			case 2:
				hideFriendList();
				break;
			}
		}

	};

	/** 好友列表Fragment */
	public static class FriendListFragment extends Fragment {

		ViewPager viewPager;// 列表的ViewPager

		private static FriendListFragment instance = null;

		public FriendListFragment() {
		}

		public static FriendListFragment newInstance() {
			instance = new FriendListFragment();
			return instance;
		}

		public static Fragment getInstance() {
			return instance;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_friend_list,
					container, false);
			viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
			return rootView;
		}

		@Override
		public void onStart() {
			super.onStart();
			// 获取可见好友列表
			BaseApplication application = (BaseApplication) (getActivity()
					.getApplication());
			ArrayList<FriendBean> visibleFriendList = application
					.getStatus_friends();
			// Log.w("liuy", "可见人数：" + visibleFriendList.size());
			createList(visibleFriendList);
		}

		@Override
		public void onDestroy() {
			instance = null;// 务必销毁之，这个Fragment每次都要重新来过
			super.onDestroy();
		}

		private void createList(ArrayList<FriendBean> beanList) {
			// 清空原有队列
			List<View> pages = new ArrayList<View>();
			if (beanList != null && beanList.size() > 0) {
				for (int i = 0; i < beanList.size(); i++) {
					final FriendBean ubean = beanList.get(i);
					View view = getActivity().getLayoutInflater().inflate(
							R.layout.item_visiable_friend, null);

					// 添加头像
					String portrait_url = ubean.getPortrait();
					ImageView iv_portrait = (ImageView) view
							.findViewById(R.id.iv_portrait);
					if (portrait_url != null && portrait_url.length() > 10) {
						ImageLoader.getInstance().displayImage(portrait_url,
								iv_portrait);
					}

					// 添加名称
					final String name = ubean.getNickName();
					TextView tv_name = (TextView) view
							.findViewById(R.id.tv_name);
					tv_name.setText(name);
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {// item点击事件，TODO
							Intent intent = new Intent();
							intent.setAction(MY_ACTION);
							intent.putExtra("select", ubean);
							getActivity().sendBroadcast(intent);
						}
					});
					pages.add(view);// 最关键的步骤，把创建好的View添加到ViewPager队列中
				}
			}

			// 创建ViewPager适配器
			VisiableFriendAdapter viewPageradapter = new VisiableFriendAdapter(
					pages, getActivity());
			viewPager.setAdapter(viewPageradapter);
			viewPager.setLeft(200);// 这俩不知道是干啥的
			viewPager.setRight(200);// 这俩不知道是干啥的
			viewPager.setOffscreenPageLimit(5);// 一次性加载的View数量，作用应该是和ListView的滚动加载差不多
			viewPager.setPageMargin(0);// 两个View的间隔距离
			viewPager.setOnPageChangeListener(new OnPageChangeListener() {// 监听viewPager的切换，设置列表的自动隐藏

						@Override
						public void onPageSelected(int arg0) {// page移动，改变并且固定位置时回调
							conHandler.removeMessages(2);
							conHandler.sendEmptyMessageDelayed(2, 3000);
						}

						@Override
						public void onPageScrolled(int arg0, float arg1,
								int arg2) {// 拖动时一直回调
						}

						@Override
						public void onPageScrollStateChanged(int arg0) {// 拖动，靠边，状态改变时
						}
					});
		}

	}

	/**
	 * 头部显示好友列表的选中广播事件处理
	 */
	public class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String flag = intent.getAction();
			Log.d(TAG, "接收到广播：" + flag);
			if (BaseApplication.BRODCAST_ISLOGIN.equals(flag)) {
				setHeadUserInfo();
			} else if (MY_ACTION.equals(flag)) {
				FriendBean selectBean = (FriendBean) intent
						.getSerializableExtra("select");
				for (int i = 0; i < markerList.size(); i++) {
					Marker position = markerList.get(i);
					FriendBean info = (FriendBean) position.getExtraInfo().get(
							"info");
					if (selectBean.getFriendId().equals(info.getFriendId())) {
						LatLng ll = new LatLng(info.getLatitude(),
								info.getLongitude());
						MapStatusUpdate u = MapStatusUpdateFactory
								.newLatLng(ll);
						mBaiduMap.animateMapStatus(u);
						selectorOverlay(position);
					}
				}
			} else if (BaseApplication.BRODCAST_MAPMODE.endsWith(flag)) {
				String mapexhibition = intent.getStringExtra("mapexhibition");
				if (mapexhibition.equals("mapmode")) {
					mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
				} else {
					mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
				}
			}
		}
	}

	/**
	 * 获取数据
	 */
	private void getUserData() {
		ResponseHandler<LbsBean> handler = new ResponseHandler<LbsBean>(
				LbsBean.class);
		handler.setOnHttpResponseListener(new OnGetHomeMessageListener());

		// 请求
		HttpUtil.get(HttpInterface.LBS_LIST, handler);
	}

	/** 请求站内信相应事件 */
	private class OnGetHomeMessageListener implements OnHttpResponseListener {

		@SuppressWarnings("rawtypes")
		@Override
		public void onSuccess(HooHttpResponse response) {
			int rc = response.getHeader().getRc();
			String rm = response.getHeader().getRm();
			if (rc == 0) {
				LbsBean lbsListData = (LbsBean) response.getBody();
				setBottomUserInfo(lbsListData.getItems().get(0));
			} else {
				Log.e(TAG, "获取用户信息失败:" + "RC=" + rc + "RM=" + rm);
			}

		}

		@Override
		public void onError(int statusCode, Throwable error, String content) {
			Log.e(TAG, "获取用户信息失败:" + error);
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

	private long exitTime = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), "再按一次退出程序",
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				finish();
				// System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 获取好友列表
	 * 
	 */
	public void getFriendList() {
		ResponseHandler<FriendBean> handler = new ResponseHandler<FriendBean>(
				FriendBean.class);
		handler.setOnHttpResponseListener(new OnHttpResponseListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void onSuccess(HooHttpResponse response) {
				int rc = response.getHeader().getRc();
				String rm = response.getHeader().getRm();
				if (rc == 0) {
					FriendBean friendList = (FriendBean) response.getBody();
					if (friendList != null && friendList.getItems().size() > 0) {
						BaseApplication.getInstance().getAll_friends().clear();
						BaseApplication.getInstance().getStatus_friends()
								.clear();

						// 添加全部好友集合
						BaseApplication.getInstance().getAll_friends()
								.addAll(friendList.getItems());
						// 添加可见好友集合
						for (int i = 0; i < friendList.getItems().size(); i++) {
							FriendBean uBean = friendList.getItems().get(i);
							if (uBean.getStatus() == 1) {
								BaseApplication.getInstance()
										.getStatus_friends().add(uBean);
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
					Log.e(TAG, "获取好友列表失败:" + "RC=" + rc + "RM=" + rm);
				}

			}

			// 网络请求失败，气泡显示错误信息
			@Override
			public void onError(int statusCode, Throwable error, String content) {
				// TODO Auto-generated method stub
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
		});
		Log.e("获取好友列表", "请求URL：" + HttpInterface.FRIEND_LIST);
		HttpUtil.get(HttpInterface.FRIEND_LIST, handler);
	}
}
