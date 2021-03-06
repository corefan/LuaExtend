package com.harry.sdk;

import android.app.Application;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

import com.adjust.sdk.plugin.AndroidIdUtil;
import com.harry.engine.AndroidUtils;
import com.harry.engine.MainActivity;
import com.lilith.sdk.LilithSDK;
import com.lilith.sdk.SDKObserver;
import com.lilith.sdk.SDKRemoteCallback;
import com.lilith.sdk.base.model.SkuItem;
import com.lilith.sdk.base.model.User;
import com.lilith.sdk.base.model.UserInfo;
import com.lilith.sdk.base.observer.UILessSDKObserver;
import com.lilith.sdk.common.constant.LoginType;
import com.lilith.sdk.common.constant.PayType;
import com.lilith.sdk.common.util.AppUtils;
import com.lilith.sdk.common.util.DeviceUtils;
import com.lilith.sdk.special.uiless.LilithUILess;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.lilith.sdk.common.util.AppUtils.getConfigValue;

/**
 * Created by 55555 on 2017/12/6.
 */

public class LiLithSDKUtils {

    public static LiLithSDKUtils getInstance()
    {
        if (minstance == null)
            minstance = new LiLithSDKUtils();
        return minstance;
    }

    private static LilithUILess SDKUILess_instance = null;
    public static LilithUILess SDKUILess()
    {
        if (SDKUILess_instance == null)
            SDKUILess_instance = LilithSDK.getInstance(LilithUILess.class);
        return SDKUILess_instance;
    }


    private String m_curAppUid = "";
    public String getAppID()
    {
        String a = (String)getConfigValue(AndroidUtils.gameActivity.getApplicationContext(), "lilith_sdk_app_id", String.class, null);
        return a;
    }
    public String getGameID()
    {
        String a = (String)getConfigValue(AndroidUtils.gameActivity.getApplicationContext(),"lilith_sdk_game_id", String.class, null);
        return a;
    }
    public String getAdjustToken()
    {
        String a = (String)getConfigValue(AndroidUtils.gameActivity.getApplicationContext(),"lilith_sdk_adjust_app_token", String.class, null);
        return a;
    }
    private String mObserver_CustomerServerUnRead = "";
    protected class CCustomerServiceListener implements com.lilith.sdk.CustomerServiceInterface.CustomerServiceListener
    {

        @Override
        public void onReceiveNotification(int i) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onReceiveNotificationCustomerServer");
                jsonObject.put("unread", "" + i);

                mObserver_CustomerServerUnRead = jsonObject.toString();

                Runnable tmp = new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtils.sendMessageToLua(mObserver_CustomerServerUnRead);
                    }
                };
                AndroidUtils.AddUnityThread(tmp);
            } catch (Exception e) {
            }
        }
    }
    private String mObserver_RateResult = "";
    protected class CRateActionListener implements com.lilith.sdk.CustomerServiceInterface.RateActionListener
    {

        @Override
        public void onAction(int action,Bundle extra) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onRateAction");
                jsonObject.put("action", "" + action);

                mObserver_RateResult = jsonObject.toString();

                Runnable tmp = new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtils.sendMessageToLua(mObserver_RateResult);
                    }
                };
                AndroidUtils.AddUnityThread(tmp);
            } catch (Exception e) {
            }
        }
    }
    private List<SkuItem> m_SkuItems = null;
    public String getCurrency(String itemID)
    {
        List<SkuItem> list = m_SkuItems;
        if(m_SkuItems != null)
        {
            int sz = list.size();
            for (int i = 0 ; i < sz; i ++){
                SkuItem a = list.get(i);
                if(itemID.compareTo(a.getSku()) == 0)
                {
                    StringBuffer ab = new StringBuffer();
                    String Pric = a.getPrice();
                    for(int j = 0; j < Pric.length(); j ++)
                    {
                        char c = Pric.charAt(j);
                        if( c != '.' && (c < '0' || c > '9'))
                        {
                            ab.append(c);
                        }
                    }
                    return ab.toString();
                }
            }
        }
        return "USD";
    }
    public float getRevenue(String itemID)
    {
        List<SkuItem> list = m_SkuItems;
        if(m_SkuItems != null)
        {
            int sz = list.size();
            for (int i = 0 ; i < sz; i ++){
                SkuItem a = list.get(i);
                if(itemID.compareTo(a.getSku()) == 0)
                {
                    StringBuffer ab = new StringBuffer();
                    String Pric = a.getPrice();
                    for(int j = 0; j < Pric.length(); j ++)
                    {
                        char c = Pric.charAt(j);
                        if( c == '.' || (c >= '0' && c <= '9'))
                        {
                            ab.append(c);
                        }
                    }
                    return Float.parseFloat(ab.toString());
                }
            }
        }
        return 0;
    }
    protected
    class SdkRemoteCallBack extends com.lilith.sdk.SDKRemoteCallback
    {
        @Override
        public void onCallback(boolean sucess , int errorCode, Bundle params) {
            if (params != null && params.containsKey("skus")) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("MSG_ID", "SDK_onQuerySkuItemDetails");
                    jsonObject.put("sucess", "" + sucess);
                    jsonObject.put("errorCode", errorCode);
                    if (sucess) {
                        JSONArray itemsA = new JSONArray();
                        List<SkuItem> list = (List<SkuItem>) params.getSerializable("skus");
                        m_SkuItems = list;
                        int sz = list.size();
                        for (int i = 0 ; i < sz; i ++){
                            SkuItem a = list.get(i);
                            JSONObject itdata = new JSONObject();
                            itdata.put("Sku",a.getSku());
                            itdata.put("Type",a.getType());
                            itdata.put("Price",a.getPrice());
                            itdata.put("Title",a.getTitle());
                            itdata.put("Description",a.getDescription());
                            itemsA.put(itdata);
                        }
                        jsonObject.put("items",itemsA);
                    }
                    mObserver_QueryItemString = jsonObject.toString();
                    Runnable tmp = new Runnable() {
                        @Override
                        public void run() {
                            AndroidUtils.sendMessageToLua(mObserver_QueryItemString);
                        }
                    };
                    AndroidUtils.AddUnityThread(tmp);
                } catch (Exception e) {
                }
            }
        }
    }
    SdkRemoteCallBack remoteCallBack = new SdkRemoteCallBack();
    CCustomerServiceListener customerservicelistener = new CCustomerServiceListener();
    CRateActionListener rateactionlistener = new CRateActionListener();
    public void ShowRate(String url)
    {
        LiLithSDKUtils.SDKUILess().showRate(AndroidUtils.gameActivity,url,rateactionlistener);
    }
    public void ShowFAQS(Bundle extra)
    {
        LiLithSDKUtils.SDKUILess().showFAQs(AndroidUtils.gameActivity,extra);
    }
    public void ShowConversation(Bundle extra)
    {
        LiLithSDKUtils.SDKUILess().showConversation(AndroidUtils.gameActivity,extra);
    }
    public void Init()
    {
        LiLithSDKUtils.SDKUILess().setCustomerServiceListener(customerservicelistener);
    }
    public void CallSDKFunction(String jsoncmd)
    {
        try {
            JSONObject jsonObject = new JSONObject(jsoncmd);
            String functionName = jsonObject.getString("Function");
            if (functionName.compareTo("Login") == 0) {
                LiLithSDKUtils.SDKUILess().startLogin(AndroidUtils.gameActivity);
            } else if (functionName.compareTo("SwitchAccount") == 0) {
                LiLithSDKUtils.SDKUILess().switchOrLinkAccount(AndroidUtils.gameActivity);
            } else if (functionName.compareTo("clearAutoLogin") == 0){
                LiLithSDKUtils.SDKUILess().clearAutoLogin();
            } else if (functionName.compareTo("bindLogin") == 0)
            {
                int type = Integer.parseInt((String)jsonObject.get("type"));
                LiLithSDKUtils.SDKUILess().bindLogin(AndroidUtils.gameActivity,LoginType.parseValue(type,-1));
            } else if (functionName.compareTo("startPay") == 0)
            {
                String payItemID = (String)jsonObject.get("payItemID");
                String payContext = (String)jsonObject.get("payContext");
                LiLithSDKUtils.SDKUILess().startPay(AndroidUtils.gameActivity,payItemID,payContext);
                LiLithSDKUtils.SDKUILess().reportWithRevenue("PayStart",LiLithSDKUtils.getInstance().getAdjustToken(),LiLithSDKUtils.getInstance().getCurrency(payItemID),LiLithSDKUtils.getInstance().getRevenue(payItemID),payItemID);
            }else if (functionName.compareTo("querySkuItemDetails") == 0)
            {
                JSONArray jsa = jsonObject.getJSONArray("items");
                String[] items = new String[jsa.length()];
                for(int i = 0; i < jsa.length(); i ++)
                {
                    items[i] = jsa.getString(i);
                }
                LiLithSDKUtils.SDKUILess().querySkuItemDetails(items,remoteCallBack);
            }
            else if (functionName.compareTo("queryCurrentUser") == 0)
            {
                User cuser = LiLithSDKUtils.SDKUILess().queryCurrentUser();
                UserInfo cuserinfo = LiLithSDKUtils.SDKUILess().queryCurrentUserInfo();
                JSONObject jsonO = new JSONObject();
                try {
                    jsonO.put("MSG_ID", "SDK_onQueryCurrentUser");
                    jsonO.put("appUid", "" + cuser.getAppUid());
                    jsonO.put("appToken", cuser.getAppToken());
                    jsonO.put("loginType", "" + cuser.getLoginType().getLoginType());
                    String assocaitep = cuserinfo.getAssociatePhone();
                    String assocaiteemail = cuserinfo.getAssociateEmail();
                    boolean isnewreg = cuserinfo.isNewReg();
                    Set t = cuserinfo.getBoundLoginTypes();
                    Object[] lt =t.toArray();
                    String loginTypeStr="";
                    for(int i = 0; i < lt.length; i ++)
                    {
                        LoginType lty = (LoginType) lt[i];
                        loginTypeStr += "" + lty.getLoginType();
                        if(i != lt.length - 1)
                            loginTypeStr += "_";
                    }
                    String userName = cuser.getName();
                    if(assocaitep != null)
                        jsonO.put("PhoneNumber", "" + assocaitep);
                    if(assocaiteemail != null)
                        jsonO.put("Email", "" + assocaiteemail);
                    jsonO.put("isNewReg", "" + isnewreg);
                    if (lt.length > 0)
                        jsonO.put("LoginTypes", "" + loginTypeStr);
                }catch(Exception e){}
                mObserver_QueryCurrentUserString = jsonO.toString();
                Runnable tmp = new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtils.sendMessageToLua(mObserver_QueryCurrentUserString);
                    }
                };
                AndroidUtils.AddUnityThread(tmp);
            }
            else if (functionName.compareTo("showFQAs") == 0)
            {
                Iterator iter = jsonObject.keys();
                Bundle bd = new Bundle();
                while(iter.hasNext())
                {
                    String key = (String)iter.next();
                    if (key.compareTo("tags") == 0)
                    {
                        JSONArray jsa = jsonObject.getJSONArray("tags");
                        ArrayList<String> llst = new ArrayList<String>();
                        for(int i = 0; i < jsa.length(); i ++)
                        {
                            llst.add(jsa.getString(i));
                        }
                        bd.putStringArrayList("tags",llst);
                    }else if (key.compareTo("Function") != 0)
                    {
                        bd.putString(key,jsonObject.getString(key));
                    }
                }
                Log.d("showFQAs",bd.toString());
                LiLithSDKUtils.getInstance().ShowFAQS(bd);
            }
            else if (functionName.compareTo("showConversation") == 0)
            {
                Iterator iter = jsonObject.keys();
                Bundle bd = new Bundle();
                while(iter.hasNext())
                {
                    String key = (String)iter.next();

                    if (key.compareTo("Function") != 0)
                    {
                        bd.putString(key,jsonObject.getString(key));
                    }
                    else if (key.compareTo("tags") != 0)
                    {
                        JSONArray jsa = jsonObject.getJSONArray("tags");
                        ArrayList<String> llst = new ArrayList<String>();
                        for(int i = 0; i < jsa.length(); i ++)
                        {
                            llst.add(jsa.getString(i));
                        }
                        bd.putStringArrayList("tags",llst);
                    }
                }
                LiLithSDKUtils.getInstance().ShowConversation(bd);
            }
            else if (functionName.compareTo("showRate") == 0)
            {
                String url = (String)jsonObject.get("url");
                LiLithSDKUtils.getInstance().ShowRate(url);
            }
            else {
                int i = 0;
                i = 3;
                Log.d("dsdfsf","dddsdf" + i);
            }
        }catch(Exception e){ }
    }
    public UILessSDKObserver getSDKObserver()
    {
        return mObserver;
    }
    private static LiLithSDKUtils minstance = null;
    private String mObserver_LoginFailedString = "";
    private String mObserver_LoginFinishString = "";
    private String mObserver_QueryCurrentUserString = "";
    private String mObserver_BindString = "";
    private String mObserver_PayString = "";
    private String mObserver_QueryItemString = "";
    private String mObserver_switechAccountString = "";
    private UILessSDKObserver mObserver = new UILessSDKObserver() {
        @Override
        public void onSwitchAccountFinish(long var1, String var3, LoginType var4) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onSwitchAccountFinish");
                jsonObject.put("appUid", "" + var1);
                jsonObject.put("appToken", var3);
                jsonObject.put("loginType", "" + var4.getLoginType());
            }catch(Exception e){}
            mObserver_switechAccountString = jsonObject.toString();
            Runnable tmp = new Runnable() {
                @Override
                public void run() {
                    AndroidUtils.sendMessageToLua(mObserver_switechAccountString);
                }
            };
            AndroidUtils.AddUnityThread(tmp);
        }
        @Override
        public void onSwitchAccountFailed(LoginType var1, int var2) {
        }
        @Override
        public void onUpdate(int i, Object[] objects) {
            super.onUpdate(i, objects);
        }

        @Override
        public void onLoginFinish(long l, String s, LoginType loginType) {
            super.onLoginFinish(l, s, loginType);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onLoginFinish");
                jsonObject.put("appUid", "" + l);
                if (m_curAppUid.length() > 1 && m_curAppUid.compareTo("" + l) != 0)
                {
                    jsonObject.put("AccountSwitch", "true");
                }
                m_curAppUid = "" + l;
                jsonObject.put("appToken", s);
                jsonObject.put("loginType", "" + loginType.getLoginType());

                jsonObject.put("GameID", "" + LiLithSDKUtils.getInstance().getGameID());
                jsonObject.put("AppID", "" + LiLithSDKUtils.getInstance().getAppID());
                String tmp = DeviceUtils.getDeviceId(AndroidUtils.gameActivity);
                jsonObject.put("DeviceID", tmp == null?"":tmp);
                tmp = DeviceUtils.getAndroidId(AndroidUtils.gameActivity);
                jsonObject.put("AndroidID", tmp == null?"":tmp);
                tmp = AndroidUtils.GetAdvertisementID();
                jsonObject.put("GoogleAdid", tmp == null?"":tmp);
                tmp = DeviceUtils.getSerialNumber(AndroidUtils.gameActivity);
                jsonObject.put("SerialNumber", tmp == null?"":tmp);
                tmp = DeviceUtils.getMacAddress(AndroidUtils.gameActivity);
                jsonObject.put("MacAddress", tmp == null?"":tmp);
                tmp = DeviceUtils.getIMSI(AndroidUtils.gameActivity);
                jsonObject.put("IMSI", tmp == null?"":tmp);
                tmp = DeviceUtils.getDeviceModel();
                jsonObject.put("DeviceModel", tmp == null?"":tmp);
                tmp = DeviceUtils.getOSVersion();
                jsonObject.put("OSVersion", tmp == null?"":tmp);
                tmp = DeviceUtils.getCPUModel();
                jsonObject.put("CPUModel", tmp == null?"":tmp);
                jsonObject.put("VersionCode", "" + AppUtils.getVersionCode(AndroidUtils.gameActivity));
                tmp = AppUtils.getVersionName(AndroidUtils.gameActivity);
                jsonObject.put("VersionName", tmp == null?"":tmp);
                tmp = AppUtils.getAppName(AndroidUtils.gameActivity);
                jsonObject.put("AppName", tmp == null?"":tmp);
                tmp = AppUtils.getChannelID(AndroidUtils.gameActivity);
                jsonObject.put("ChannelID", tmp == null?"":tmp);
                jsonObject.put("IDFA", "");
                jsonObject.put("OSType", "android");
                tmp = AndroidUtils.GetBundleIdentifier();
                jsonObject.put("Package", tmp == null?"":tmp);
                tmp = AppUtils.getAppLocale(AndroidUtils.gameActivity).getLanguage();
                jsonObject.put("Local", tmp == null?"en":tmp);
            }catch(Exception e){}
            mObserver_LoginFinishString = jsonObject.toString();
            Runnable tmp = new Runnable() {
                @Override
                public void run() {
                    AndroidUtils.sendMessageToLua(mObserver_LoginFinishString);
                }
            };
            AndroidUtils.AddUnityThread(tmp);
            CallSDKFunction("{\"Function\":\"queryCurrentUser\"}");
        }

        @Override
        public void onLoginFailed(LoginType loginType, int i) {
            super.onLoginFailed(loginType, i);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onLoginFailed");
                jsonObject.put("errorCode", "" + i);
                jsonObject.put("loginType", "" + loginType.getLoginType());


                jsonObject.put("GameID", "" + LiLithSDKUtils.getInstance().getGameID());
                jsonObject.put("AppID", "" + LiLithSDKUtils.getInstance().getAppID());
                String tmp = DeviceUtils.getDeviceId(AndroidUtils.gameActivity);
                jsonObject.put("DeviceID", tmp == null?"":tmp);
                tmp = DeviceUtils.getAndroidId(AndroidUtils.gameActivity);
                jsonObject.put("AndroidID", tmp == null?"":tmp);
                tmp = AndroidUtils.GetAdvertisementID();
                jsonObject.put("GoogleAdid", tmp == null?"":tmp);
                tmp = DeviceUtils.getSerialNumber(AndroidUtils.gameActivity);
                jsonObject.put("SerialNumber", tmp == null?"":tmp);
                tmp = DeviceUtils.getMacAddress(AndroidUtils.gameActivity);
                jsonObject.put("MacAddress", tmp == null?"":tmp);
                tmp = DeviceUtils.getIMSI(AndroidUtils.gameActivity);
                jsonObject.put("IMSI", tmp == null?"":tmp);
                tmp = DeviceUtils.getDeviceModel();
                jsonObject.put("DeviceModel", tmp == null?"":tmp);
                tmp = DeviceUtils.getOSVersion();
                jsonObject.put("OSVersion", tmp == null?"":tmp);
                tmp = DeviceUtils.getCPUModel();
                jsonObject.put("CPUModel", tmp == null?"":tmp);
                jsonObject.put("VersionCode", "" + AppUtils.getVersionCode(AndroidUtils.gameActivity));
                tmp = AppUtils.getVersionName(AndroidUtils.gameActivity);
                jsonObject.put("VersionName", tmp == null?"":tmp);
                tmp = AppUtils.getAppName(AndroidUtils.gameActivity);
                jsonObject.put("AppName", tmp == null?"":tmp);
                tmp = AppUtils.getChannelID(AndroidUtils.gameActivity);
                jsonObject.put("ChannelID", tmp == null?"":tmp);
                jsonObject.put("IDFA", "");
                jsonObject.put("OSType", "Android");
                tmp = AndroidUtils.GetBundleIdentifier();
                jsonObject.put("Package", tmp == null?"":tmp);
                tmp = AppUtils.getAppLocale(AndroidUtils.gameActivity).getLanguage();
                jsonObject.put("Local", tmp == null?"en":tmp);
            }catch(Exception e){}
            mObserver_LoginFailedString = jsonObject.toString();
            Runnable tmp = new Runnable() {
                @Override
                public void run() {
                    AndroidUtils.sendMessageToLua(mObserver_LoginFailedString);
                }
            };
            AndroidUtils.AddUnityThread(tmp);
        }

        @Override
        protected void onBindFinish(boolean b, long l, String s, LoginType loginType) {
            super.onBindFinish(b, l, s, loginType);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onBindFinish");
                jsonObject.put("bSuccess", "" + b);
                jsonObject.put("appUid", "" + l);
                if (m_curAppUid.length() > 1 && m_curAppUid.compareTo("" + l) != 0)
                {
                    jsonObject.put("AccountSwitch", "true");
                }
                m_curAppUid = "" + l;
                jsonObject.put("appToken", s);
                jsonObject.put("loginType", "" + loginType.getLoginType());
            }catch(Exception e){}
            mObserver_BindString = jsonObject.toString();
            Runnable tmp = new Runnable() {
                @Override
                public void run() {
                    AndroidUtils.sendMessageToLua(mObserver_BindString);
                }
            };
            AndroidUtils.AddUnityThread(tmp);
        }

        @Override
        public void onPayFinish(boolean b, int i, String s, PayType payType) {
            super.onPayFinish(b, i, s, payType);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("MSG_ID", "SDK_onPayFinish");
                jsonObject.put("bSuccess", "" + b);
                jsonObject.put("price", "" + i);
                jsonObject.put("itemID", s);
                jsonObject.put("payType", "" + payType.getPayType());
                if(b)
                {
                    LiLithSDKUtils.SDKUILess().reportWithRevenue("PaySucess",LiLithSDKUtils.getInstance().getAdjustToken(),LiLithSDKUtils.getInstance().getCurrency(s),LiLithSDKUtils.getInstance().getRevenue(s),s);
                }else
                {
                    LiLithSDKUtils.SDKUILess().reportWithRevenue("PayFailed",LiLithSDKUtils.getInstance().getAdjustToken(),LiLithSDKUtils.getInstance().getCurrency(s),LiLithSDKUtils.getInstance().getRevenue(s),s);
                }
            }catch(Exception e){}
            mObserver_PayString = jsonObject.toString();
            Runnable tmp = new Runnable() {
                @Override
                public void run() {
                    AndroidUtils.sendMessageToLua(mObserver_PayString);
                }
            };
            AndroidUtils.AddUnityThread(tmp);
        }

        @Override
        protected void onRegionNotified(String s) {
            super.onRegionNotified(s);
        }

        @Override
        protected void onIdentified() {
            super.onIdentified();
        }

        @Override
        protected void onUserForgetAccount() {
            super.onUserForgetAccount();
        }

        @Override
        protected void onEarningHalfed(long l, long l1) {
            super.onEarningHalfed(l, l1);
        }

        @Override
        protected void onEarningCleared(long l, long l1) {
            super.onEarningCleared(l, l1);
        }

        @Override
        protected void onEarningReset(long l, long l1) {
            super.onEarningReset(l, l1);
        }

        @Override
        protected void onWebPayDBFinish(boolean b, int i, int i1) {
            super.onWebPayDBFinish(b, i, i1);
        }

        @Override
        protected void onGoogleGamesConnectFailed(boolean b, int i, String s) {
            super.onGoogleGamesConnectFailed(b, i, s);
        }

        @Override
        protected void onGoogleGameConnectSucceed(boolean b) {
            super.onGoogleGameConnectSucceed(b);
        }
    };
}
