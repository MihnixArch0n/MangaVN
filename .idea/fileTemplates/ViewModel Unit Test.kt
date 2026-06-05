#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ${NAME} {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // TODO: init mocks + viewmodel
    }

    @Test
    fun `initial state is correct`() = runTest {
        // TODO: assert initial state
    }
}
