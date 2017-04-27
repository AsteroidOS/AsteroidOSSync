package org.asteroidos.sync.fragments;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import org.asteroidos.sync.MainActivity;
import org.asteroidos.sync.R;

import dk.jens.backup.adapters.AppInfoAdapter;

public class AppListFragment extends Fragment {

    AppInfoAdapter adapter;
    ListView listView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        adapter = new AppInfoAdapter(context, R.layout.app_list_item, MainActivity.appInfoList);
        adapter.restoreFilter();
        Toast toast = Toast.makeText(context, R.string.explain_app_prefs, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_list, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        listView = (ListView) view.findViewById(R.id.listview);
        listView.setAdapter(adapter);
    }

}
