package net.gini.android.internal.payment.moreinformation

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.databinding.GpsPaymentProviderIconHolderBinding
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import java.util.Locale

internal class PaymentProvidersIconsAdapter(var dataSet: List<PaymentProviderApp?>, var locale: Locale?) :
    RecyclerView.Adapter<PaymentProvidersIconsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = GpsPaymentProviderIconHolderBinding.inflate(
            parent.getLayoutInflaterWithGiniPaymentThemeAndLocale(locale),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.binding.root.context
        holder.binding.gpsPaymentProviderIcon.setImageDrawable(dataSet[position]?.icon)
        holder.binding.gpsPaymentProviderIcon.contentDescription = dataSet[position]?.paymentProvider?.name + " ${context.getString(
            R.string.gps_payment_provider_logo_content_description)}"
        holder.binding.root.isFocusable = true
        holder.binding.root.isFocusableInTouchMode = true
        holder.binding.root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    class ViewHolder(val binding: GpsPaymentProviderIconHolderBinding) :
        RecyclerView.ViewHolder(binding.root)
}
