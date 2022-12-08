package net.gini.android.capture;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

public class Amount {

    private static Amount mAmount;

    @NonNull
    private final BigDecimal mBigDecimal;

    @NonNull
    private final AmountCurrency mCurrency;



    public Amount(@NonNull final BigDecimal decimal, @NonNull final AmountCurrency currency) {
        mBigDecimal = decimal;
        mCurrency = currency;
    }


    public String amountToPay() {
        return String.format("%.2f:%s", mBigDecimal.doubleValue(), mCurrency.name());
    }

    public static Amount EMPTY() {
        if (mAmount == null)
            mAmount = new Amount(BigDecimal.valueOf(0), AmountCurrency.EUR);

        return mAmount;
    }
}
