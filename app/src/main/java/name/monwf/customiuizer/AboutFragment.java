package name.monwf.customiuizer;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AboutFragment extends SubFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		headLayoutId = R.layout.fragment_about_head;
		tailLayoutId = R.layout.fragment_about_tail;
	}

	@Override
	protected void fixStubLayout(View view, int postion) {
		if (postion == 2) {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
			lp.addRule(RelativeLayout.BELOW, android.R.id.list_container);
			view.setLayoutParams(lp);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//Add version name to support title
		View view = getView();
		if (view != null) try {
			TextView version = view.findViewById(R.id.about_version);
			String versionName = BuildConfig.VERSION_NAME;
			if (BuildConfig.BUILD_TYPE.equals("develop")) {
				SimpleDateFormat formatter = new SimpleDateFormat("yy.MM.dd", Locale.getDefault());
				Date buildDate = new Date(BuildConfig.BUILD_TIME);
				versionName = formatter.format(buildDate) + "-test";
			}
			version.setText(String.format(getResources().getString(R.string.about_version), versionName));
		} catch (Throwable e) {
			//Shouldn't happen...
			e.printStackTrace();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (getView() == null) return;
		getView().findViewById(R.id.miuizer_icon).setVisibility(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? View.GONE : View.VISIBLE);
		super.onConfigurationChanged(newConfig);
	}

}
