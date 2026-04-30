package com.reveny.virtualinject.ui.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.reveny.virtualinject.R;
import com.reveny.virtualinject.ui.fragment.HomeFragment;

public class MainActivity extends BaseActivity {

    private HomeFragment homeFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeFragment = new HomeFragment();
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.container, homeFragment, "home")
            .commit();
    }

    /** Push a new fragment onto the back stack (clone setup, manage, progress) */
    public void pushFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit();
    }

    /** Pop everything back to HomeFragment, then switch tab if needed */
    public void goHomeAndShowCloned(boolean showClonedTab) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (showClonedTab && homeFragment != null) {
            homeFragment.refreshClonedDots();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
