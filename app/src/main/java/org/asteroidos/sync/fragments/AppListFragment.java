package org.asteroidos.sync.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ListView;

import org.asteroidos.sync.MainActivity;
import org.asteroidos.sync.R;
import org.asteroidos.sync.adapters.AppInfoAdapter;

public class AppListFragment extends Fragment {
    AppInfoAdapter adapter;
    ListView listView;
    View placeholder;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        adapter = new AppInfoAdapter(context, R.layout.app_list_item, MainActivity.appInfoList);
        adapter.restoreFilter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_list, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        listView = view.findViewById(R.id.listview);
        listView.setAdapter(adapter);

        placeholder = view.findViewById(R.id.no_notification_placeholder);
        adapter.getFilter().filter("", count ->
                placeholder.setVisibility(count == 0 ? View.VISIBLE : View.INVISIBLE));
    }
}
