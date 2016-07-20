package com.labs.okey.oneride;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Created by Oleg on 19-Jul-16.
 */
@RunWith(AndroidJUnit4.class)
public class AddPassengersTest extends ActivityInstrumentationTestCase2<DriverRoleActivity> {

    public AddPassengersTest(Class<DriverRoleActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Injecting the Instrumentation instance is required
        // for your test to run with AndroidJUnitRunner.
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
