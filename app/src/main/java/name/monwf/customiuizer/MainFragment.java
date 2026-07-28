package name.monwf.customiuizer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import name.monwf.customiuizer.prefs.ListPreferenceEx;
import name.monwf.customiuizer.prefs.PreferenceEx;
import name.monwf.customiuizer.subs.CategorySelector;
import name.monwf.customiuizer.subs.Controls;
import name.monwf.customiuizer.subs.Launcher;
import name.monwf.customiuizer.subs.System;
import name.monwf.customiuizer.subs.Various;
import name.monwf.customiuizer.utils.AppHelper;
import name.monwf.customiuizer.utils.AppLocaleController;
import name.monwf.customiuizer.utils.Helpers;
import name.monwf.customiuizer.utils.ModData;
import name.monwf.customiuizer.utils.ModSearchAdapter;
import name.monwf.customiuizer.utils.SearchRoute;
import name.monwf.customiuizer.utils.SearchRouteResolver;
import name.monwf.customiuizer.utils.SearchStateMachine;

public class MainFragment extends PreferenceFragmentBase {

	private final CategorySelector catSelector = new CategorySelector();
	public System prefSystem = new System();
	public Launcher prefLauncher = new Launcher();
	public Controls prefControls = new Controls();
	public Various prefVarious = new Various();
	private Menu mActionMenu;
	private RecyclerView listView = null;
	private ListView resultView = null;
	private Handler mMainHandler;
	private Runnable mCheckActiveRunnable;
	private Runnable mHideKeyboardRunnable;
	boolean isSearchFocused = false;
	boolean isRestoringSearch = false;
	int inSearchView = SearchStateMachine.STATE_IDLE;
	String lastFilter;

	private final Runnable showUpdateNotification = new Runnable() {
		@Override
		public void run() {
			if (getView() != null) try {
				ImageView alert = getView().findViewById(R.id.update_alert);
				if (alert != null) alert.setVisibility(View.VISIBLE);
			} catch (Throwable e) {}
		}
	};

	private final Runnable hideUpdateNotification = new Runnable() {
		@Override
		public void run() {
			if (getView() != null) try {
				ImageView alert = getView().findViewById(R.id.update_alert);
				if (alert != null) alert.setVisibility(View.GONE);
			} catch (Throwable e) {}
		}
	};

	private boolean isFragmentReady(AppCompatActivity act) {
		return act != null && !act.isFinishing() && MainFragment.this.isAdded();
	}

	@Override
	@SuppressLint("MissingSuperCall")
	public void onCreate(Bundle savedInstanceState) {
		toolbarMenu = true;
		activeMenus = "all";
		if (savedInstanceState != null) {
			inSearchView = savedInstanceState.getInt("inSearchView", SearchStateMachine.STATE_IDLE);
			lastFilter = savedInstanceState.getString("lastFilter");
			isSearchFocused = savedInstanceState.getBoolean("isSearchFocused", false);
		}
		super.onCreate(savedInstanceState, R.xml.prefs_main);
		tailLayoutId = R.layout.prefs_main12;
		final AppCompatActivity act = (AppCompatActivity) getActivity();

		// Preventing launch delay
		new Thread(() -> {
			Helpers.getAllMods(act, savedInstanceState != null);
		}).start();

		checkModuleIsActive();
	}

	private void checkModuleIsActive() {
		if (mMainHandler == null) mMainHandler = new Handler(Looper.getMainLooper());
		if (mCheckActiveRunnable != null) mMainHandler.removeCallbacks(mCheckActiveRunnable);
		mCheckActiveRunnable = new Runnable() {
			@Override
			public void run() {
				final AppCompatActivity act = (AppCompatActivity) getActivity();
				if (isFragmentReady(act) && !AppHelper.moduleActive) {
					act.runOnUiThread(new Runnable() {
						public void run() {
							showXposedDialog(act);
						}
					});
				}
			}
		};
		mMainHandler.postDelayed(mCheckActiveRunnable, 800);
	}

	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		super.onCreatePreferences(savedInstanceState, rootKey);
		setPreferencesFromResource(R.xml.prefs_main, rootKey);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		mActionMenu = menu;
		MenuItem searchMenuItem = mActionMenu.findItem(R.id.search_btn);

		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionCollapse(MenuItem searchItem) {
				MenuItem item;
				for (int i = 0; i < mActionMenu.size(); i++) {
					item = mActionMenu.getItem(i);
					item.setVisible(item.getItemId() != R.id.edit_confirm && item.getItemId() != R.id.openinweb);
				}
				return true;
			}

			@Override
			public boolean onMenuItemActionExpand(MenuItem searchItem) {
				MenuItem item = null;
				for (int i = 0; i < mActionMenu.size(); i++) {
					item = mActionMenu.getItem(i);
					item.setVisible(item.getItemId() == R.id.search_btn);
				}
				return true;
			}
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (isRestoringSearch || !SearchStateMachine.canFilter(inSearchView)) return false;
				inSearchView = SearchStateMachine.transitionOnQuery(inSearchView, newText);
				findMod(newText);
				return false;
			}
		});
		searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				isSearchFocused = hasFocus;
			}
		});
		if (SearchStateMachine.shouldClearOnReturn(inSearchView)) {
			resetSearchUi(searchMenuItem, searchView);
		} else if (inSearchView != SearchStateMachine.STATE_IDLE && !TextUtils.isEmpty(lastFilter)) {
			isRestoringSearch = true;
			if (!MenuItemCompat.isActionViewExpanded(searchMenuItem)) {
				MenuItemCompat.expandActionView(searchMenuItem);
			}
			if (!lastFilter.equals(searchView.getQuery().toString())) {
				searchView.setQuery(lastFilter, false);
				if (!isSearchFocused) searchView.clearFocus();
			}
			isRestoringSearch = false;
			if (resultView != null && listView != null) findMod(lastFilter);
		}
	}

	@Override
	protected void fixStubLayout(View view, int postion) {
		if (postion == 2) {
			ViewGroup.LayoutParams lp = view.getLayoutParams();
			lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
			view.setLayoutParams(lp);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.app_name);

		if (getView() == null) return;

		resultView = getView().findViewById(R.id.custom);
		resultView.setDivider(null);
		resultView.setDividerHeight(0);
		resultView.setAdapter(new ModSearchAdapter(getActivity()));
		resultView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ModData mod = (ModData)parent.getAdapter().getItem(position);
				if (openModCat(mod.cat.name(), mod.sub, mod.key)) {
					inSearchView = SearchStateMachine.STATE_NAVIGATED;
					isSearchFocused = false;
					Helpers.hideKeyboard((AppCompatActivity) getActivity(), getView());
				}
			}
		});
		resultView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				if (isSearchFocused) {
					isSearchFocused = false;
					if (mMainHandler == null) mMainHandler = new Handler(v.getContext().getMainLooper());
					if (mHideKeyboardRunnable != null) mMainHandler.removeCallbacks(mHideKeyboardRunnable);
					mHideKeyboardRunnable = () -> {
						Helpers.hideKeyboard((AppCompatActivity) getActivity(), getView());
					};
					mMainHandler.postDelayed(mHideKeyboardRunnable, getResources().getInteger(android.R.integer.config_shortAnimTime));
					resultView.requestFocus();
				}
				return false;
			}
		});

		listView = getListView();
		final Activity act = getActivity();

		if (SearchStateMachine.shouldClearOnReturn(inSearchView)) {
			resetSearchUi(null, null);
		} else if (inSearchView != SearchStateMachine.STATE_IDLE && !TextUtils.isEmpty(lastFilter)) {
			findMod(lastFilter);
		}

//		PreferenceEx warning = findPreference("pref_key_warning");
//		if (warning != null) {
//			getPreferenceScreen().removePreference(warning);
//		}

		findPreference("pref_key_miuizer_launchericon").setOnPreferenceChangeListener(new CheckBoxPreference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PackageManager pm = act.getPackageManager();
				if ((Boolean)newValue)
					pm.setComponentEnabledSetting(new ComponentName(act, GateWayLauncher.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				else
					pm.setComponentEnabledSetting(new ComponentName(act, GateWayLauncher.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				return true;
			}
		});

		ListPreferenceEx locale = findPreference("pref_key_miuizer_locale");
		AppLocaleController.setupLocalePreference(locale, AppHelper.appPrefs);

	}

	void findMod(String filter) {
		if (isRestoringSearch || !SearchStateMachine.canFilter(inSearchView)) return;
		if (resultView == null || listView == null) return;
		lastFilter = filter;
		resultView.setVisibility(filter == null || filter.equals("") ? View.GONE : View.VISIBLE);
		listView.setEnabled(filter == null || filter.equals(""));
		ListAdapter adapter = resultView.getAdapter();
		if (adapter == null) return;
		((ModSearchAdapter)resultView.getAdapter()).getFilter().filter(filter == null ? "" : filter);
	}

	private void resetSearchUi(MenuItem searchMenuItem, SearchView searchView) {
		if (!SearchStateMachine.shouldClearOnReturn(inSearchView)) return;
		isRestoringSearch = true;
		try {
			if (searchMenuItem != null) MenuItemCompat.collapseActionView(searchMenuItem);
			if (searchView != null) {
				searchView.setQuery("", false);
				searchView.clearFocus();
			}
			if (resultView != null) resultView.setVisibility(View.GONE);
			if (listView != null) listView.setEnabled(true);
			isSearchFocused = false;
		} finally {
			isRestoringSearch = false;
		}
		inSearchView = SearchStateMachine.STATE_IDLE;
		lastFilter = null;
	}

	private boolean openModCat(String cat, String sub, String mod) {
		SearchRoute route = SearchRouteResolver.resolve(cat, sub, mod);
		if (route == null) return false;
		if (!isAdded()) return false;

		Bundle bundle = new Bundle();
		bundle.putString("cat", route.getCategory());
		if (route.getSub() != null) {
			bundle.putString("sub", route.getSub());
		}
		bundle.putString("mod", route.getKey());
		catSelector.setTargetFragment(this, 0);
		switch (route.getCategory()) {
			case "pref_key_system":
				if (route.isCategorySelector())
					openSubFragment(catSelector, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_mods, R.xml.prefs_system_cat);
				else
					openSubFragment(prefSystem, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.system_mods, R.xml.prefs_system);
				return true;
			case "pref_key_launcher":
				if (route.isCategorySelector())
					openSubFragment(catSelector, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.launcher_title, R.xml.prefs_launcher_cat);
				else
					openSubFragment(prefLauncher, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.launcher_title, R.xml.prefs_launcher);
				return true;
			case "pref_key_controls":
				if (route.isCategorySelector())
					openSubFragment(catSelector, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.controls_mods, R.xml.prefs_controls_cat);
				else
					openSubFragment(prefControls, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.controls_mods, R.xml.prefs_controls);
				return true;
			case "pref_key_various":
				openSubFragment(prefVarious, bundle, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.various_mods, R.xml.prefs_various);
				return true;
			default:
				return false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("inSearchView", inSearchView);
		outState.putString("lastFilter", lastFilter);
		outState.putBoolean("isSearchFocused", isSearchFocused);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mMainHandler != null) {
			if (mCheckActiveRunnable != null) mMainHandler.removeCallbacks(mCheckActiveRunnable);
			if (mHideKeyboardRunnable != null) mMainHandler.removeCallbacks(mHideKeyboardRunnable);
		}
	}

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		if (preference != null && preference.getKey() != null) {
			PreferenceCategory modsCat = findPreference("prefs_cat");
			if (modsCat != null && modsCat.findPreference(preference.getKey()) != null && openModCat(preference.getKey(), null, preference.getKey())) {
				return true;
			}
		}
		return super.onPreferenceTreeClick(preference);
	}
}
