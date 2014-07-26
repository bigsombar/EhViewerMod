/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.ui;

import android.content.res.Configuration;
import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;
import com.hippo.ehviewer.cache.ImageCache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

public class AbstractSlidingActivity extends SlidingActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Ui.adjustOrientation(this, true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Ui.updateTranslucent(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Ui.adjustOrientation(this, true);
    }

    @Override
    public void onStart() {
      super.onStart();

      if (Config.getAllowAnalyics())
          EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
      super.onStop();

      if (Config.getAllowAnalyics())
          EasyTracker.getInstance(this).activityStop(this);
      ImageCache.getInstance(this).flush();
    }
}
