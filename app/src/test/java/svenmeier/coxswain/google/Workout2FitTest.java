package svenmeier.coxswain.google;

import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link Workout2Fit}.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = svenmeier.coxswain.BuildConfig.class, sdk = 18)
public class Workout2FitTest {

	private static final long Mon_Jun_15_2015 = 1434326400000l;

	@Test
	public void snapshots() throws IOException {
		Workout workout = new Workout();
		workout.start.set(Mon_Jun_15_2015 + (60 * 1000));
		workout.duration.set(2);
		workout.distance.set(6);
		workout.strokes.set(2);
		workout.energy.set(3);

		List<Snapshot> snapshots = new ArrayList<>();

		Snapshot snapshot = new Snapshot();
		snapshot.speed.set(4_50);
		snapshot.pulse.set(80);
		snapshot.strokeRate.set(25);
		snapshot.distance.set(2);
		snapshot.strokes.set(0);
		snapshots.add(snapshot);

		snapshot = new Snapshot();
		snapshot.speed.set(4_51);
		snapshot.pulse.set(81);
		snapshot.strokeRate.set(26);
		snapshot.distance.set(4);
		snapshot.strokes.set(1);
		snapshots.add(snapshot);

		snapshot = new Snapshot();
		snapshot.speed.set(4_52);
		snapshot.pulse.set(82);
		snapshot.strokeRate.set(27);
		snapshot.distance.set(6);
		snapshot.strokes.set(2);
		snapshots.add(snapshot);

		Workout2Fit workout2Fit = new Workout2Fit();
		Session session = workout2Fit.session(workout);
		for (DataSet dataSet : workout2Fit.dataSets(workout, snapshots)) {

		}
	}
}