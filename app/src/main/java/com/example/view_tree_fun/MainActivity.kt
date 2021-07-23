package com.example.view_tree_fun

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment())
                .commit()
        }
    }
}

class MainFragment : Fragment(R.layout.main_fragment)

class MyCustomView : FrameLayout {
    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initialize(context)
    }

    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        initialize(context)
    }

    private var scope: CoroutineScope? = null
    private val viewModel by lazy {
        val factory = MyViewModel.Factory(findViewTreeSavedStateRegistryOwner()!!)
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!, factory)
            .get(MyViewModel::class.java)
    }
    private val textView: TextView by lazy { findViewById(R.id.delta) }

    private fun initialize(context: Context) {
        inflate(context, R.layout.my_custom_view, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        viewModel.tick
            .onEach { tick -> textView.text = tick.toString() }
            .launchIn(scope!!)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }
}

class MyViewModel : ViewModel() {
    val tick = flow {
        var counter = 0
        while (currentCoroutineContext().isActive) {
            emit(counter++)
            delay(1_000L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = 0
    )

    class Factory(
        owner: SavedStateRegistryOwner
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return MyViewModel() as T
        }
    }
}
