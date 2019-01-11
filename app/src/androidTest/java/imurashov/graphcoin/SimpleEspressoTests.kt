package imurashov.graphcoin

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import imurashov.graphcoin.presentation.view.MainActivity
import org.hamcrest.Matchers.not
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.net.wifi.WifiManager
import androidx.test.filters.LargeTest


/**
 * Instrumented start, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SimpleEspressoTests {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.deleteDatabase("database")
            context.getSharedPreferences("imurashov.graphcoin.preferences", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }

    @get:Rule
    var activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Test
    fun start() {
        testStartup()
        testFetchingData()
        testOffline()
        testCachedData()
        testReloadedData()
    }

    // App name in toolbar switches to 1 month
    private fun testStartup() {
        onView(withText(R.string.app_name)).check(matches(isDisplayed()))
        Thread.sleep(3000)
        onView(withText(R.string.month)).check(matches(isDisplayed()))
    }

    // Fetch data from server, check price and description visibility
    private fun testFetchingData() {
        setPeriod(R.string.half_year)
        Thread.sleep(3000)
        onView(withText(R.string.half_year)).check(matches(isDisplayed()))
        onView(withId(R.id.price)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches((not(isDisplayed()))))
        onView(withId(R.id.priceProgress)).check(matches((not(isDisplayed()))))
    }

    // Disable wi-fi, check all TextViews and ProgressBars are hidden
    private fun testOffline() {
        setWiFiEnabled(false)
        setPeriod(R.string.year)
        onView(withText(R.string.year)).check(matches(isDisplayed()))
        onView(withId(R.id.price)).check(matches(not(isDisplayed())))
        onView(withId(R.id.description)).check(matches((not(isDisplayed()))))
        onView(withId(R.id.priceProgress)).check(matches((not(isDisplayed()))))
        onView(withId(R.id.surfaceProgress)).check(matches((not(isDisplayed()))))
    }

    // Check description field is visible
    private fun testCachedData() {
        setPeriod(R.string.month)
        onView(withText(R.string.month)).check(matches(isDisplayed()))
        onView(withId(R.id.price)).check(matches(isDisplayed()))
        onView(withId(R.id.description)).check(matches((isDisplayed())))
        onView(withId(R.id.priceProgress)).check(matches((not(isDisplayed()))))
    }

    // Check description field is hidden, price is visible
    private fun testReloadedData() {
        setWiFiEnabled(true)
        testFetchingData()
    }

    // Change period in options menu
    private fun setPeriod(periodNameId: Int) {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        Thread.sleep(1000)
        onView(withText(periodNameId)).perform(click())
        Thread.sleep(1000)
    }

    // Set wi-fi state
    private fun setWiFiEnabled(enabled: Boolean) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val wifi = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifi.isWifiEnabled = enabled
    }
}
