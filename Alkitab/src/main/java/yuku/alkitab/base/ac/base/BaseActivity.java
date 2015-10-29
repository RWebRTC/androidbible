package yuku.alkitab.base.ac.base;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import yuku.afw.storage.Preferences;
import yuku.alkitab.base.storage.Prefkey;
import yuku.alkitab.base.util.ChangeLanguageHelper;
import yuku.alkitab.debug.R;

public abstract class BaseActivity extends AppCompatActivity {
	public static final String TAG = BaseActivity.class.getSimpleName();

	private boolean enableNonToolbarUpButton;

	private int lastKnownLocaleSerialNumber;

	@Override
	protected void onStart() {
		super.onStart();

		applyActionBarAndStatusBarColors();

		final int currentLocaleSerialNumber = ChangeLanguageHelper.getLocaleSerialCounter();
		if (lastKnownLocaleSerialNumber != currentLocaleSerialNumber) {
			Log.d(TAG, "Restarting activity " + getClass().getName() + " because of locale change " + lastKnownLocaleSerialNumber + " -> " + currentLocaleSerialNumber);
			lastKnownLocaleSerialNumber = currentLocaleSerialNumber;
			// restart activity
			final Intent originalIntent = getIntent();
			finish();
			startActivity(originalIntent);
		}
	}

	protected void applyActionBarAndStatusBarColors() {
		// action bar color and status bar color are set based on night mode
		final ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			if (Preferences.getBoolean(Prefkey.is_night_mode, false)) {
				actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primary_night_mode)));

				if (Build.VERSION.SDK_INT >= 21) {
					getWindow().setStatusBarColor(0xff000000);
				}
			} else {
				final TypedValue tv = new TypedValue();
				getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
				actionBar.setBackgroundDrawable(new ColorDrawable(tv.data));

				if (Build.VERSION.SDK_INT >= 21) {
					getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
				}
			}
		}
	}

	/**
	 * Call this from subclasses before super.onCreate() to enable up button.
	 */
	protected void enableNonToolbarUpButton() {
		this.enableNonToolbarUpButton = true;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		lastKnownLocaleSerialNumber = ChangeLanguageHelper.getLocaleSerialCounter();

		if (this.enableNonToolbarUpButton) {
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
		}
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		if (enableNonToolbarUpButton && item.getItemId() == android.R.id.home) {
			navigateUp();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

    protected void navigateUp() {
		final Intent upIntent = NavUtils.getParentActivityIntent(this);
		if (upIntent == null) { // not defined in manifest, let us finish() instead.
			finish();
			return;
		}

		if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
			TaskStackBuilder.create(this)
				.addNextIntentWithParentStack(upIntent)
				.startActivities();
		} else {
			NavUtils.navigateUpTo(this, upIntent);
		}
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		{ // reconfigure toolbar height (Need to have a toolbar with id toolbar)
			final View v = findViewById(R.id.toolbar);
			if (v instanceof Toolbar) {
				final Toolbar toolbar = (Toolbar) v;
				final ViewGroup.LayoutParams lp = toolbar.getLayoutParams();
				final TypedValue tv = new TypedValue();
				getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
				final int h = (int) tv.getDimension(getResources().getDisplayMetrics());
				lp.height = h;
				toolbar.setLayoutParams(lp);
				// Workaround for https://code.google.com/p/android/issues/detail?id=79813
				toolbar.setMinimumHeight(h);
			}
		}
	}
}
