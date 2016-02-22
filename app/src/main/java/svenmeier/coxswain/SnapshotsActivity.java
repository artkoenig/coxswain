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
package svenmeier.coxswain;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import propoid.db.Order;
import propoid.db.Reference;
import propoid.ui.list.MatchLookup;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.view.TimelineView;
import svenmeier.coxswain.view.Utils;
import svenmeier.coxswain.view.ValueView;


public class SnapshotsActivity extends Activity implements View.OnClickListener {

    private Gym gym;

    private Workout workout;

    private Preference<Long> windowPreference;

    private int highlight;

    private List<Snapshot> snapshots = new ArrayList<>();

    private Snapshot maxSnapshot;

    private TextView titleView;

    private TimelineView timelineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        windowPreference = Preference.getLong(this, R.string.preference_snapshots_window).fallback(2 * TimelineView.MINUTE);

        Reference<Workout> reference = Reference.from(getIntent());
        workout = gym.getWorkout(reference);
        if (workout == null) {
            finish();
        } else {
            setTitle(DateUtils.formatDateTime(this, workout.start.get(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.layout_snapshots);

        titleView = (TextView) findViewById(R.id.snapshots_title);
        updateTitle();

        timelineView = (TimelineView) findViewById(R.id.snapshots_timeline);
        timelineView.setOnClickListener(this);
        timelineView.setPeriods(new SnapshotPeriods());

        new SnapshotLookup().restartLoader(0, this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        timelineView.setWindow(windowPreference.get());
    }

    @Override
    protected void onStop() {
        windowPreference.set(timelineView.getWindow());

        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        highlight = (highlight + 1) % 3;

        timelineView.invalidate();

        updateTitle();
    }

    private void updateTitle() {
        switch (highlight) {
            case 0:
                titleView.setText(R.string.limit_speed);
                break;
            case 1:
                titleView.setText(R.string.limit_pulse);
                break;
            case 2:
                titleView.setText(R.string.limit_strokeRate);
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

    }

    private class SnapshotPeriods implements TimelineView.Periods {

        private SimpleDateFormat dateFormat;

        private NumberFormat numberFormat;

        private Paint paint = new Paint();

        private int border = Utils.dpToPx(SnapshotsActivity.this, 4);

        private float textSize = Utils.dpToPx(SnapshotsActivity.this, 20);

        private float strokeWidth = Utils.dpToPx(SnapshotsActivity.this, 3);

        private Path path = new Path();

        public SnapshotPeriods() {
            dateFormat = new SimpleDateFormat("H:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            numberFormat = NumberFormat.getNumberInstance();
        }

        @Override
        public long min() {
            return 0;
        }

        @Override
        public long max() {
            return workout.duration.get() * 1000;
        }

        @Override
        public long minWindow() {
            return TimelineView.MINUTE;
        }

        @Override
        public long maxWindow() {
            return TimelineView.HOUR;
        }


        @Override
        public TimelineView.Unit unit(long time, long window) {
            if (window > 10 * TimelineView.MINUTE) {
                return new TimelineView.MinuteUnit(time, 5);
            } else if (window > 4 * TimelineView.MINUTE) {
                return new TimelineView.MinuteUnit(time);
            } else if (window > 2 * TimelineView.MINUTE) {
                return new TimelineView.SecondUnit(time, 15);
            } else {
                return new TimelineView.SecondUnit(time, 10);
            }
        }

        @Override
        public void paint(Class<?> unit, long from, long to, Canvas canvas, RectF rect) {

            int start = (int)(from / 1000);
            int end = (int)(to / 1000);

            paintCurve(canvas, rect, start, end, 0);
            paintCurve(canvas, rect, start, end, 1);
            paintCurve(canvas, rect, start, end, 2);

            paintHeader(to, canvas, rect);
        }

        private void paintHeader(long to, Canvas canvas, RectF rect) {
            int index = (int)(to / 1000);

            if (index >= 0 && index < snapshots.size()) {
                Snapshot snapshot = snapshots.get(index);

                String what;
                switch (highlight) {
                    case 0:
                        what = numberFormat.format(snapshot.speed.get() / 100f);
                        break;
                    case 1:
                        what = numberFormat.format(snapshot.pulse.get());
                        break;
                    case 2:
                        what = numberFormat.format(snapshot.strokeRate.get());
                        break;
                    default:
                        throw new IndexOutOfBoundsException();
                }
                float whatWidth = paint.measureText(what);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xff3567ed);
                paint.setTextSize(textSize);
                canvas.drawText(what, rect.right - border - whatWidth, rect.top + border + textSize, paint);
            }

            String when = dateFormat.format(to);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff000000);
            paint.setTextSize(textSize);
            canvas.drawText(when, rect.left + border, rect.top + border + textSize, paint);
        }

        private void paintCurve(Canvas canvas, RectF rect, int start, int end, int index) {
            path.reset();
            for (int current = start; current <= end; current++) {
                if (current < 0 || current >= snapshots.size()) {
                    continue;
                }

                Snapshot snapshot = snapshots.get(current);

                int value;
                int max;
                switch (index) {
                    case 0:
                        value = snapshot.speed.get();
                        max = maxSnapshot.speed.get();
                        break;
                    case 1:
                        value = snapshot.pulse.get();
                        max = maxSnapshot.pulse.get();
                        break;
                    case 2:
                        value = snapshot.strokeRate.get();
                        max = maxSnapshot.strokeRate.get();
                        break;
                    default:
                        throw new IndexOutOfBoundsException();
                }

                paintLine(path, rect, value, max, current - start, end - start);
            }
            paint.setStyle(Paint.Style.STROKE);
            if (index == highlight) {
                paint.setColor(0x803567ed);
            } else {
                paint.setColor(0x403567ed);
            }
            paint.setStrokeWidth(strokeWidth);
            canvas.drawPath(path, paint);
        }

        private void paintLine(Path path, RectF rect, int value, int max, int index, int count) {

            float left = rect.left + border;
            float width = rect.width() - border - border;
            float x = left + border + (width * value / max);
            float y = rect.bottom - (rect.height() * index / count);

            if (path.isEmpty()) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
    }

    private class SnapshotLookup extends MatchLookup<Snapshot> {

        public SnapshotLookup() {
            super(gym.getSnapshots(workout));

            setOrder(Order.ascendingByInsert());
        }

        @Override
        protected void onLookup(List<Snapshot> lookup) {
            snapshots = new ArrayList<>(lookup);

            if (snapshots.isEmpty()) {
                // TODO
                for (int i = 0; i <= 27 * 60; i++) {
                    Snapshot object = new Snapshot();
                    object.speed.set((int)(Math.random() * 500));
                    object.strokeRate.set((int)(Math.random() * 500));
                    object.pulse.set((int)(Math.random() * 500));
                    snapshots.add(object);
                }
            }

            maxSnapshot = new Snapshot();
            for (Snapshot snapshot : snapshots) {
                maxSnapshot.speed.set(Math.max(maxSnapshot.speed.get(), snapshot.speed.get()));
                maxSnapshot.strokeRate.set(Math.max(maxSnapshot.strokeRate.get(), snapshot.strokeRate.get()));
                maxSnapshot.pulse.set(Math.max(maxSnapshot.pulse.get(), snapshot.pulse.get()));
            }

            timelineView.postInvalidate();
        }
    }

    public static Intent createIntent(Context context, Workout workout) {
        Intent intent = new Intent(context, SnapshotsActivity.class);

        intent.setData(new Reference<Workout>(workout).toUri());

        return intent;
    }
}