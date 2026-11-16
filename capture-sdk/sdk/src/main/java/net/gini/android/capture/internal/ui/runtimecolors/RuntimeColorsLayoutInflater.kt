package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import java.lang.reflect.Constructor

/**
 * A [LayoutInflater] that applies the [GiniColorResolver] colors to every view it inflates.
 *
 * Its factory runs before the factories it inherited (AppCompat, Material, fragments): it lets
 * them create the view, or creates it the same way [LayoutInflater] does, and then styles it with
 * [RuntimeColorsViewStyler]. When the view can't be created here, it returns `null` and
 * [LayoutInflater] continues with its normal path.
 */
internal class RuntimeColorsLayoutInflater private constructor(
    original: LayoutInflater,
    newContext: Context,
    private val resolver: GiniColorResolver,
    wrapFactories: Boolean,
) : LayoutInflater(original, newContext) {

    constructor(original: LayoutInflater, newContext: Context, resolver: GiniColorResolver) :
        this(original, newContext, resolver, wrapFactories = true)

    init {
        // A clone already inherits the styling factory from its original.
        if (wrapFactories) {
            val inherited = factory2 ?: factory?.let { FactoryAdapter(it) }
            setFactory2(StylingFactory(inherited))
        }
    }

    override fun cloneInContext(newContext: Context): LayoutInflater =
        RuntimeColorsLayoutInflater(this, newContext, resolver, wrapFactories = false)

    /** Same lookup as the platform's phone layout inflater for tags without a package. */
    override fun onCreateView(name: String, attrs: AttributeSet): View {
        for (prefix in WIDGET_PREFIXES) {
            try {
                createView(name, prefix, attrs)?.let { return it }
            } catch (ignored: ClassNotFoundException) {
                // Try the next package.
            }
        }
        return super.onCreateView(name, attrs)
    }

    private inner class StylingFactory(private val inherited: Factory2?) : Factory2 {

        override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
            val view = inherited?.onCreateView(parent, name, context, attrs)
                ?: createViewOrNull(name, context, attrs)
                ?: return null
            RuntimeColorsViewStyler.apply(view, attrs, resolver)
            return view
        }

        override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
            onCreateView(null, name, context, attrs)
    }

    private class FactoryAdapter(private val factory: Factory) : Factory2 {
        override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
            factory.onCreateView(name, context, attrs)

        override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
            factory.onCreateView(name, context, attrs)
    }

    private fun createViewOrNull(name: String, context: Context, attrs: AttributeSet): View? {
        val classNames = when {
            name in NOT_VIEW_TAGS -> emptyList()
            '.' in name -> listOf(name)
            else -> (WIDGET_PREFIXES + VIEW_PREFIX).map { it + name }
        }
        val view = classNames.firstNotNullOfOrNull { constructorFor(it, context) }
            ?.let { newInstanceOrNull(it, context, attrs) }
        if (view is ViewStub) view.layoutInflater = cloneInContext(context)
        return view
    }

    // On failure LayoutInflater retries with its own path and reports the error as usual.
    private fun newInstanceOrNull(constructor: Constructor<out View>, context: Context, attrs: AttributeSet): View? =
        try {
            constructor.newInstance(context, attrs)
        } catch (ignored: ReflectiveOperationException) {
            null
        }

    private fun constructorFor(className: String, context: Context): Constructor<out View>? =
        synchronized(CONSTRUCTORS) {
            if (CONSTRUCTORS.containsKey(className)) return CONSTRUCTORS[className]
            val constructor = try {
                Class.forName(className, false, context.classLoader)
                    .asSubclass(View::class.java)
                    .getConstructor(Context::class.java, AttributeSet::class.java)
            } catch (ignored: ClassNotFoundException) {
                null
            } catch (ignored: NoSuchMethodException) {
                null
            } catch (ignored: ClassCastException) {
                null
            }
            CONSTRUCTORS[className] = constructor
            constructor
        }

    private companion object {
        val WIDGET_PREFIXES = listOf("android.widget.", "android.webkit.", "android.app.")
        const val VIEW_PREFIX = "android.view."
        val NOT_VIEW_TAGS = setOf("fragment", "blink")
        val CONSTRUCTORS = HashMap<String, Constructor<out View>?>()
    }
}
