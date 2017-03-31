package org.lathanh.play.rxandroid.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * Simply hosts a single Fragment; initially the {@link MainMenuFragment}
 * (which is a chooser for individual demo fragments, which replaces the
 * content of this activity upon being chosen).
 *
 * @author Robert LaThanh
 * @since 2017-02-23
 */
public class DemoActivity extends FragmentActivity {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .replace(android.R.id.content, new MainMenuFragment(),
                   MainMenuFragment.class.getName())
          .commit();
    }
  }

  public void switchToFragment(Fragment fragment) {
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragment, fragment.getClass().getName())
        .addToBackStack(fragment.getClass().getName())
        .commit();
  }
}
