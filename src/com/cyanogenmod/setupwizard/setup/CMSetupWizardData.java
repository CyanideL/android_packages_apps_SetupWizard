/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.cyanogenmod.setupwizard.setup;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.TelephonyIntents;
import com.cyanogenmod.setupwizard.ui.SetupWizardActivity;
import com.cyanogenmod.setupwizard.util.SetupWizardUtils;

import java.util.ArrayList;

public class CMSetupWizardData extends AbstractSetupData {

    private static final String TAG = CMSetupWizardData.class.getSimpleName();

    private boolean mTimeSet = false;
    private boolean mTimeZoneSet = false;

    public CMSetupWizardData(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewPageList() {
        ArrayList<Page> pages = new ArrayList<Page>();
        pages.add(new WelcomePage(mContext, this));
        pages.add(new WifiSetupPage(mContext, this));
        if (SetupWizardUtils.hasTelephony(mContext)) {
            pages.add(new SimCardMissingPage(mContext, this).setHidden(isSimInserted()));
        }
        if (SetupWizardUtils.isMultiSimDevice(mContext) && SetupWizardUtils.isOwner()) {
            pages.add(new ChooseDataSimPage(mContext, this).setHidden(!allSimsInserted()));
        }
        if (SetupWizardUtils.hasTelephony(mContext)) {
            pages.add(new MobileDataPage(mContext, this)
                    .setHidden(!isSimInserted() || SetupWizardUtils.isMobileDataEnabled(mContext)));
        }
        if (SetupWizardUtils.hasGMS(mContext)) {
            pages.add(new GmsAccountPage(mContext, this).setHidden(true));
        }
        if (SetupWizardUtils.isOwner()) {
            pages.add(new CyanogenServicesPage(mContext, this).setHidden(true));
            pages.add(new CyanogenSettingsPage(mContext, this));
            pages.add(new OtherSettingsPage(mContext, this));
            pages.add(new DateTimePage(mContext, this));
        }
        pages.add(new FinishPage(mContext, this));
        return new PageList(pages.toArray(new SetupPage[pages.size()]));
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            ChooseDataSimPage chooseDataSimPage =
                    (ChooseDataSimPage) getPage(ChooseDataSimPage.TAG);
            if (chooseDataSimPage != null) {
                chooseDataSimPage.setHidden(!allSimsInserted());
            }
            SimCardMissingPage simCardMissingPage =
                    (SimCardMissingPage) getPage(SimCardMissingPage.TAG);
            if (simCardMissingPage != null) {
                simCardMissingPage.setHidden(isSimInserted());
                if (isCurrentPage(simCardMissingPage)) {
                    onNextPage();
                }
            }
            showHideMobileDataPage();
        } else if (intent.getAction()
                .equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            showHideAccountPages();
        } else  if (intent.getAction()
                .equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
            showHideMobileDataPage();
            showHideAccountPages();
        } else if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED) ||
                intent.getAction().equals(TelephonyIntents.ACTION_NETWORK_SET_TIMEZONE)) {
            mTimeZoneSet = true;
            showHideDateTimePage();
        } else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
                intent.getAction().equals(TelephonyIntents.ACTION_NETWORK_SET_TIME)) {
            mTimeSet = true;
            showHideDateTimePage();
        }
    }

    private void showHideAccountPages() {
        boolean isConnected = SetupWizardUtils.isNetworkConnected(mContext);
        GmsAccountPage gmsAccountPage =
                (GmsAccountPage) getPage(GmsAccountPage.TAG);
        if (gmsAccountPage != null) {
            gmsAccountPage.setHidden(!isConnected);
        }
        CyanogenServicesPage cyanogenServicesPage =
                (CyanogenServicesPage) getPage(CyanogenServicesPage.TAG);
        if (cyanogenServicesPage != null) {
            cyanogenServicesPage.setHidden(!isConnected);
        }
    }

    private void showHideMobileDataPage() {
        MobileDataPage mobileDataPage =
                (MobileDataPage) getPage(MobileDataPage.TAG);
        if (mobileDataPage != null) {
            mobileDataPage.setHidden(!isSimInserted() ||
                    SetupWizardUtils.isMobileDataEnabled(mContext));
        }
    }

    private void showHideDateTimePage() {
        DateTimePage dateTimePage = (DateTimePage) getPage(DateTimePage.TAG);
        if (dateTimePage != null) {
            dateTimePage.setHidden(mTimeZoneSet & mTimeSet);
        }
    }

    public IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        if (SetupWizardUtils.hasTelephony(mContext)) {
            filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        }
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_NETWORK_SET_TIME);
        filter.addAction(TelephonyIntents.ACTION_NETWORK_SET_TIMEZONE);
        return filter;
    }

    // We only care that one sim is inserted
    private boolean isSimInserted() {
        TelephonyManager tm = TelephonyManager.from(mContext);
        int simSlotCount = tm.getSimCount();
        for (int i = 0; i < simSlotCount; i++) {
            int state = tm.getSimState(i);
            if (state != TelephonyManager.SIM_STATE_ABSENT
                    && state != TelephonyManager.SIM_STATE_UNKNOWN) {
                 return true;
            }
        }
        return false;
    }

    // We only care that each slot has a sim
    private boolean allSimsInserted() {
        TelephonyManager tm = TelephonyManager.from(mContext);
        int simSlotCount = tm.getSimCount();
        for (int i = 0; i < simSlotCount; i++) {
            int state = tm.getSimState(i);
            if (state == TelephonyManager.SIM_STATE_ABSENT) {
                return false;
            }
        }
        return true;
    }

}