/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
*/

package org.cerberus.client.profile.battery.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.cerberus.client.profile.battery.util.SystemInfo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

public class UMLoggerService extends Service{
  private static final String TAG = "UMLoggerService";

  private static final int NOTIFICATION_ID = 1;
  private static final int NOTIFICATION_ID_LETTER = 2;

  private Thread estimatorThread;
  private PowerEstimator powerEstimator;

  private Notification notification;

  private NotificationManager notificationManager;
  private TelephonyManager phoneManager;

  @Override
  public IBinder onBind(Intent intent) {
//    return binder;
    return null;
  }
  
  @Override
  public void onCreate() {
    powerEstimator = new PowerEstimator(this);
    
    /* Register to receive phone state messages. */
    phoneManager = (TelephonyManager)this.getSystemService(TELEPHONY_SERVICE);
    phoneManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE |
                        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                        PhoneStateListener.LISTEN_SERVICE_STATE |
                        PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
    
    /* Register to receive airplane mode and battery low messages. */
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    filter.addAction(Intent.ACTION_BATTERY_LOW);
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
    filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
    filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
    registerReceiver(broadcastIntentReceiver, filter);

    notificationManager = (NotificationManager)getSystemService(
                                                  NOTIFICATION_SERVICE); 
  }
  
  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
//android.os.Debug.startMethodTracing("pt.trace");

    if(intent.getBooleanExtra("stop", false)) {
      stopSelf();
      return;
    } else if(estimatorThread != null) {
      return;
    }
    estimatorThread = new Thread(powerEstimator);
    estimatorThread.start();
  }
  
  @Override
  public void onDestroy() {
//android.os.Debug.stopMethodTracing();
    if(estimatorThread != null) {
      estimatorThread.interrupt();
      while(estimatorThread.isAlive()) {
        try {
          estimatorThread.join();
        } catch(InterruptedException e) {
        }
      }
    }
    unregisterReceiver(broadcastIntentReceiver);

    /* See comments in showNotification() for why we are using reflection here.
     */
    boolean foregroundSet = false;
    try {
      Method stopForeground = getClass().getMethod("stopForeground",
                                   boolean.class);
      stopForeground.invoke(this, true);
      foregroundSet = true;
    } catch (InvocationTargetException e) {
    } catch (IllegalAccessException e) {
    } catch(NoSuchMethodException e) {
    }
    if(!foregroundSet) {
//      setForeground(false);
      notificationManager.cancel(NOTIFICATION_ID);
    }
    phoneManager = null;
    super.onDestroy();
  };
  
   


  
  
  BroadcastReceiver broadcastIntentReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      if(intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        Bundle extra = intent.getExtras();
        try {
          if ((Boolean)extra.get("state")) {
            powerEstimator.writeToLog("airplane-mode on\n");
          } else {
            powerEstimator.writeToLog("airplane-mode off\n");
          }  
        } catch(ClassCastException e) {
          // Some people apparently are having this problem.  I'm not really
          // sure why this should happen.
          Log.w(TAG, "Couldn't determine airplane mode state");
        }
      } else if(intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
        powerEstimator.writeToLog("battery low\n");
      } else if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
        powerEstimator.writeToLog("battery-change " +
            intent.getIntExtra("plugged", -1) + " " +
            intent.getIntExtra("level", -1) + "/" +
            intent.getIntExtra("scale", -1) + " " +
            intent.getIntExtra("voltage", -1) +
            intent.getIntExtra("temperature", -1) + "\n");
        powerEstimator.plug(
            intent.getIntExtra("plugged", -1) != 0);
      } else if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) ||
                intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
        // A package has either been removed or its metadata has changed and we
        // need to clear the cache of metadata for that app.
        SystemInfo.getInstance().voidUidCache(
            intent.getIntExtra(Intent.EXTRA_UID, -1));
      }
    };
  };

  PhoneStateListener phoneListener = new PhoneStateListener() {
    public void onServiceStateChanged(ServiceState serviceState) {
      switch(serviceState.getState()) {
        case ServiceState.STATE_EMERGENCY_ONLY:
          powerEstimator.writeToLog("phone-service emergency-only\n");
          break;
        case ServiceState.STATE_IN_SERVICE:
          powerEstimator.writeToLog("phone-service in-service\n");
          switch(phoneManager.getNetworkType()) {
            case(TelephonyManager.NETWORK_TYPE_EDGE):
              powerEstimator.writeToLog("phone-network edge\n");
              break;
            case(TelephonyManager.NETWORK_TYPE_GPRS):
              powerEstimator.writeToLog("phone-network GPRS\n");
              break;
            case 8:
              powerEstimator.writeToLog("phone-network HSDPA\n");
              break;
            case(TelephonyManager.NETWORK_TYPE_UMTS):
              powerEstimator.writeToLog("phone-network UMTS\n");
              break;
            default:
              powerEstimator.writeToLog("phone-network " +
                                        phoneManager.getNetworkType() + "\n");
          }
          break;
        case ServiceState.STATE_OUT_OF_SERVICE:
          powerEstimator.writeToLog("phone-service out-of-service\n");
          break;
        case ServiceState.STATE_POWER_OFF:
          powerEstimator.writeToLog("phone-service power-off\n");
          break;
      }
    }

    public void onCallStateChanged(int state, String incomingNumber) {
      switch(state) {
        case TelephonyManager.CALL_STATE_IDLE:
          powerEstimator.writeToLog("phone-call idle\n");
          break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
          powerEstimator.writeToLog("phone-call off-hook\n");
          break;
        case TelephonyManager.CALL_STATE_RINGING:
          powerEstimator.writeToLog("phone-call ringing\n");
          break;
      }
    }

    public void onDataConnectionStateChanged(int state) {
      switch(state) {
        case TelephonyManager.DATA_DISCONNECTED:
          powerEstimator.writeToLog("data disconnected\n");
          break;
        case TelephonyManager.DATA_CONNECTING:
          powerEstimator.writeToLog("data connecting\n");
          break;
        case TelephonyManager.DATA_CONNECTED:
          powerEstimator.writeToLog("data connected\n");
          break;
        case TelephonyManager.DATA_SUSPENDED:
          powerEstimator.writeToLog("data suspended\n");  
          break;
      }
    }

    public void onSignalStrengthChanged(int asu) {
      powerEstimator.writeToLog("signal " + asu + "\n");
    }
  };
}
