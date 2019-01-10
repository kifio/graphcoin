package imurashov.graphcoin.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import imurashov.graphcoin.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, MainFragment(), MainFragment::class.simpleName)
                .commit()
        }
    }
}
