package com.zhan_dui.animetaste;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;
import cn.sharesdk.framework.ShareSDK;
import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.avos.avoscloud.Parse;
import com.avos.avoscloud.ParseAnalytics;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.umeng.analytics.MobclickAgent;
import com.zhan_dui.data.ApiConnector;
import com.zhan_dui.modal.Advertise;
import com.zhan_dui.modal.Animation;
import com.zhan_dui.modal.Category;
import com.zhan_dui.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class LoadActivity extends ActionBarActivity {
	private Context mContext;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		Parse.initialize(this,
				"w43xht9daji0uut74pseeiibax8c2tnzxowmx9f81nvtpims",
				"86q8251hrodk6wnf4znistay1mva9rm1xikvp1s9mhp5n7od");
		ShareSDK.initSDK(mContext);
		ParseAnalytics.trackAppOpened(getIntent());
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide();
		}

		setContentView(R.layout.activity_load);
		MobclickAgent.onError(this);
		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
				"only_wifi", true)
				&& NetworkUtils.isWifi(mContext) == false) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
					.setTitle(R.string.only_wifi_title).setMessage(
							R.string.only_wifi_body);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.only_wifi_ok,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							init();
						}
					});
			builder.setNegativeButton(R.string.obly_wifi_cancel,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			builder.create().show();
		} else {
			init();
		}

	};

    private void init(){
        ApiConnector.instance().getInitData(20,5,2,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode,JSONObject response) {
                super.onSuccess(response);
                if(statusCode == 200 && response.has("data")){
                    new PrepareTask(response).execute();
                }else{
                    error();
                }
            }

            @Override
            public void onFailure(Throwable throwable, String s) {
                super.onFailure(throwable, s);
                Log.e("error",s);
                Toast.makeText(mContext,R.string.get_data_error,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class PrepareTask extends AsyncTask<Void,Void,Boolean>{
        private JSONObject mSetupResponse;
        private Intent mIntent;
        private boolean mResult = false;

        public PrepareTask(JSONObject setupObject){
            mSetupResponse = setupObject;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try{
                JSONObject list = mSetupResponse.getJSONObject("data").getJSONObject("list");
                JSONArray animations = list.getJSONArray("anime");
                JSONArray category = list.getJSONArray("category");
                JSONArray advert = list.getJSONArray("advert");
                JSONArray feature = list.getJSONArray("recommend");
                ArrayList<Animation> Animations = Animation.build(animations);
                ArrayList<Category> Categories = new ArrayList<Category>();
                ArrayList<Advertise> Advertises = new ArrayList<Advertise>();
                ArrayList<Animation> Recommends = new ArrayList<Animation>();
                new Delete().from(Animation.class).where("IsFavorite=?",0).execute();
                new Delete().from(Category.class).execute();
                new Delete().from(Advertise.class).execute();
                ActiveAndroid.beginTransaction();

                for(int i =0;i<Animations.size();i++){
                    if(null == new Select().from(Animation.class).where("IsFavorite=? and AnimationId=?",1,Animations.get(i).AnimationId).executeSingle())
                        Animations.get(i).save();
                    else{
                        Animations.get(i).setFav(true);
                    }
                }
                for(int i = 0; i < category.length();i++){
                    Category cat = Category.build(category.getJSONObject(i));
                    Categories.add(cat);
                    cat.save();
                }
                for(int i = 0; i < advert.length();i++){
                    Advertise ad = Advertise.build(advert.getJSONObject(i));
                    Advertises.add(ad);
                    ad.save();
                }
                for(int i = 0; i< feature.length();i++){
                    Recommends.add(Animation.build(feature.getJSONObject(i)));
                }
                ActiveAndroid.setTransactionSuccessful();
                mIntent = new Intent(LoadActivity.this,
                        StartActivity.class);
                mIntent.putParcelableArrayListExtra("Animations",Animations);
                mIntent.putParcelableArrayListExtra("Categories",Categories);
                mIntent.putParcelableArrayListExtra("Advertises",Advertises);
                mIntent.putParcelableArrayListExtra("Recommends",Recommends);
                mIntent.putExtra("Success",true);
                mResult = true;
            }catch(Exception e){
                e.printStackTrace();
                mResult = false;
            }finally {
                ActiveAndroid.endTransaction();
                return mResult;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(mResult){
                startActivity(mIntent);
            }else{
                error();
            }
        }
    }

    private void error(){
        Toast.makeText(mContext, R.string.get_data_error, Toast.LENGTH_SHORT)
                .show();
        finish();
    }

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(mContext);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(mContext);
	}

}
