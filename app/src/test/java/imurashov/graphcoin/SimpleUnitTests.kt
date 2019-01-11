package imurashov.graphcoin

import imurashov.graphcoin.repository.network.Response
import imurashov.graphcoin.utils.Normalization
import org.junit.Test


class SimpleUnitTests {

    @Test
    fun normalization() {
        val values = listOf(Response.Value(1546560000, 3822.626666666667),
            Response.Value(1546646400, 3868.4875000000006),
            Response.Value(1546732800, 3920.4566666666665),
            Response.Value(1546819200, 4036.093333333333),
            Response.Value(1546905600, 4035.8549999999996),
            Response.Value(1546992000, 4034.138333333333))

        for (point in Normalization.normalizeValues("foo", values)) {
            println(point)
            assert(point.x in 0.0..1.0)
            assert(point.y in 0.0..1.0)
        }
    }
}
