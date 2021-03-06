/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.view;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public abstract class AbstractValueFragment extends DialogFragment {

    private List<Tab> tabs = new ArrayList<>();

    private ViewPager pager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_tabs, container, false);

        pager = (ViewPager) view.findViewById(R.id.tabs_pager);
        pager.setAdapter(new MyFragmentAdapter());

        View titlesView = view.findViewById(R.id.tabs_titles);
        titlesView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSegment();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        for (Tab tab : tabs) {
            if (tab.segmentToIndex(getCallback().getSegment()) >= 0) {
                pager.setCurrentItem(tabs.indexOf(tab));
                break;
            }
        }
    }

    private void changeSegment() {
        Segment segment = getCallback().getSegment();

        Tab tab = tabs.get(pager.getCurrentItem());
        ListView listView = (ListView) pager.findViewWithTag(tab);
        tab.indexToSegment(segment, listView.getFirstVisiblePosition());

        getCallback().setSegment(segment);

        AbstractValueFragment.this.dismiss();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.requestWindowFeature(STYLE_NO_TITLE);

        return dialog;
    }

    protected void addTab(Tab tab) {
        this.tabs.add(tab);
    }

    private class MyFragmentAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public CharSequence getPageTitle(int tab) {
            return tabs.get(tab).getTitle();
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int tabIndex) {

            final Tab tab = tabs.get(tabIndex);

            ViewGroup root = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.layout_values, container, false);
            container.addView(root);

            ListView listView = (ListView) root.findViewById(R.id.values);
            listView.setTag(tab);
            listView.setOnScrollListener(new GridScroll());
            listView.setAdapter(new BaseAdapter() {

                @Override
                public int getCount() {
                    return tab.getCount();
                }

                @Override
                public Integer getItem(int index) {
                    return tab.getValue(index);
                }

                @Override
                public long getItemId(int index) {
                    return index;
                }

                @Override
                public View getView(int index, View view, ViewGroup parent) {
                    if (view == null) {
                        view = getActivity().getLayoutInflater().inflate(R.layout.layout_values_item, parent, false);
                    }

                    ValueView valueView = (ValueView) view.findViewById(R.id.values_value);
                    valueView.setPattern(tab.getPattern());
                    valueView.setValue(tab.getValue(index));
                    return view;
                }
            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    changeSegment();
                }
            });

            Segment segment = getCallback().getSegment();
            int index = tab.segmentToIndex(segment);
            if (index >= 0) {
                listView.setSelection(index);
            }

            return root;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }

    private Callback getCallback() {
        return Utils.getParent(this, Callback.class);
    }

    public interface Tab {
        CharSequence getTitle();

        int getCount();

        String getPattern();

        int getValue(int index);

        void indexToSegment(Segment segment, int index);

        int segmentToIndex(Segment segment);
    }

    public interface Callback {
        public Segment getSegment();

        public void setSegment(Segment segment);
    }
}