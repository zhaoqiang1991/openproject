package com.example.sample_baseinterface;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

public interface ModuleInterface {
    boolean isHandleCate(Context context, String searchword, Bundle bundle);

    Fragment getFragment(Context context,String searchword, Bundle bundle);

    boolean isHandle(boolean handle);
}
