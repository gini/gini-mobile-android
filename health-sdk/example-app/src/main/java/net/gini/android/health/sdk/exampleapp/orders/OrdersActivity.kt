package net.gini.android.health.sdk.exampleapp.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.MainActivity
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.health.sdk.exampleapp.databinding.ActivityOrdersBinding
import net.gini.android.health.sdk.exampleapp.orders.data.model.OrderItem
import net.gini.android.health.sdk.exampleapp.util.SharedPreferencesUtil
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.internal.payment.utils.DisplayedScreen
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.slf4j.LoggerFactory

class OrdersActivity : AppCompatActivity() {

    private val viewModel: OrdersViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setActivityTitle(DisplayedScreen.Nothing)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ordersFlow.collect { orders ->
                        (binding.ordersList.adapter as OrdersAdapter).apply {
                            dataSet = orders
                            notifyDataSetChanged()
                        }
                        binding.noOrdersLabel.visibility =
                            if (orders.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.startIntegratedPaymentFlow.collect { paymentDetails ->
                        startIntegratedPaymentFlow(paymentDetails)
                    }
                }
                launch {
                    viewModel.giniHealth.displayedScreen.collect { screen ->
                        setActivityTitle(screen)
                    }
                }
                launch {
                    viewModel.errorsFlow.collect {
                        Toast.makeText(this@OrdersActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    viewModel.openBankState.collect { paymentState ->
                        when (paymentState) {
                            is GiniHealth.PaymentState.Success -> {
                                SharedPreferencesUtil.saveStringToSharedPreferences(
                                    SharedPreferencesUtil.PAYMENTREQUEST_KEY,
                                    paymentState.paymentRequest.id,
                                    this@OrdersActivity
                                )
                                supportFragmentManager.popBackStack()
                            }
                            is GiniHealth.PaymentState.Cancel -> {
                                supportFragmentManager.popBackStack()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        IntentCompat.getParcelableExtra(
            intent,
            MainActivity.PAYMENT_FLOW_CONFIGURATION,
            PaymentFlowConfiguration::class.java
        )?.let {
            viewModel.setIntegratedFlowConfiguration(it)
        }

        binding.ordersList.layoutManager = LinearLayoutManager(this)
        binding.ordersList.adapter = OrdersAdapter(emptyList()) { orderItem ->
            viewModel.setSelectedOrderItem(orderItem)
            showOrderDetailsFragment()
        }
        binding.ordersList.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                title = resources.getString(R.string.button_orders_list)
            }
            invalidateOptionsMenu()
        }
    }

    private fun setActivityTitle(screen: DisplayedScreen) {
        when (screen) {
            DisplayedScreen.MoreInformationFragment -> title = getString(net.gini.android.internal.payment.R.string.gps_more_information_fragment_title)
            DisplayedScreen.ReviewScreen -> title = getString(R.string.title_payment_review)
            DisplayedScreen.Nothing -> title = getString(R.string.title_activity_invoices)
            else -> Unit
        }
        invalidateOptionsMenu()
    }

    private fun showOrderDetailsFragment() {
        OrderDetailsFragment.newInstance().apply {
            add()
        }
    }

    private fun startIntegratedPaymentFlow(paymentDetails: PaymentDetails) {
        viewModel.getPaymentFragmentForPaymentDetails(paymentDetails, IntentCompat.getParcelableExtra(intent, MainActivity.PAYMENT_FLOW_CONFIGURATION, PaymentFlowConfiguration::class.java))
            .onSuccess { paymentFragment ->
                paymentFragment.apply{
                    add()
                }
            }
            .onFailure { error ->
                LOG.error("Error getting payment review fragment", )
                AlertDialog.Builder(this@OrdersActivity)
                    .setTitle(getString(R.string.could_not_start_payment_review))
                    .setMessage(error.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
    }

    private fun Fragment.add() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, this, this::class.java.simpleName)
            .addToBackStack(this::class.java.simpleName)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) {
            menuInflater.inflate(R.menu.orders_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.custom_order -> {
                viewModel.setSelectedOrderItem(null)
                showOrderDetailsFragment()
                true
            }

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OrdersActivity::class.java)
    }
}

class OrdersAdapter(
    var dataSet: List<OrderItem>,
    private val showOrderDetails: (OrderItem) -> Unit,
) :
    RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val recipient: TextView
        val purpose: TextView
        val amount: TextView

        init {
            recipient = view.findViewById(R.id.recipient)
            purpose = view.findViewById(R.id.purpose)
            amount = view.findViewById(R.id.amount)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_order, viewGroup, false)
        return ViewHolder(view).also { vh ->
            vh.itemView.setOnClickListener {
                showOrderDetails(dataSet[vh.adapterPosition])
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val orderItem = dataSet[position]

        viewHolder.recipient.text = orderItem.recipient ?: ""
        viewHolder.purpose.text = orderItem.purpose ?: ""
        viewHolder.amount.text = orderItem.amount ?: ""

    }

    override fun getItemCount() = dataSet.size
}
