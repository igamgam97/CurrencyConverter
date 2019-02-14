package com.example.gamgam.currencyconverter

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import com.example.gamgam.currencyconverter.api.CurrencyConverterApi
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    lateinit var coeffFromFirstToSecond: BigDecimal
    lateinit var coeffFromSecondToFirst: BigDecimal
    /* state = 0 if focus on first inputEditText and we changed FirstSpinner
       state = 1 if focus on second inputEditText and we changed FirstSpinner
       state = 2 if focus on first inputEditText and we changed SecondSpinner
       state = 3 if focus on second inputEditText and we changed SecondSpinner
    */
    var state: Int = 0
    var isConnectedToInternet = true
    private lateinit var currencyConverterApi: CurrencyConverterApi
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //block ui until get listCurrencies (because all elements depend from Currencies)
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        currencyConverterApi = CurrencyConverterApi.create(this)
        loadCurrencies()
        initTextInputEditTexts()
        initSpinners()


    }
    //load currencies and fill spinner
    private fun loadCurrencies() {

        currencyConverterApi.getCurrencies()
                .flatMap { it -> Observable.fromArray(it.results.keys) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    run {
                        val newList =ArrayList(list).sorted()
                        second_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, newList)
                        first_spinner.adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, newList)
                        progressBar.visibility = ProgressBar.INVISIBLE
                        //unblock ui
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        first_input_edit_text.requestFocus()
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    }
                }, {
                    Toast.makeText(this, getString(R.string.user_has_lost_internet_connection_and_dont_cashe_message), Toast.LENGTH_SHORT).show()
                }
                )
    }



    private fun initSpinners() {

        first_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val firstCurrency = parent.getItemAtPosition(second_spinner.selectedItemId.toInt()).toString()
                val secondCurrency = parent.getItemAtPosition(position).toString()
                state = if (first_input_layout.hasFocus()) {
                    0
                } else {
                    1
                }
                getCoeffConversion(secondCurrency, firstCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        second_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val secondCurrency = parent.getItemAtPosition(first_spinner.selectedItemId.toInt()).toString()
                val firstCurrency = parent.getItemAtPosition(position).toString()
                state = if (first_input_layout.hasFocus()) {
                    2
                } else {
                    3
                }
                getCoeffConversion(secondCurrency, firstCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun initTextInputEditTexts() {
        //first_input_edit_text.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(10, 2))
        RxTextView.textChanges(first_input_edit_text)
                .filter { first_input_layout.hasFocus() }
                .subscribe { inputText -> second_input_edit_text.setText(convertToOtherCurrency(inputText, coeffFromFirstToSecond)) }
        RxTextView.textChanges(second_input_edit_text)
                .filter { second_input_layout.hasFocus() }
                .subscribe { inputText -> first_input_edit_text.setText(convertToOtherCurrency(inputText, coeffFromSecondToFirst)) }
    }

    private fun changeNoFocusInputEditText(state: Int) {
        when (state) {
            0, 2 -> second_input_edit_text.setText(convertToOtherCurrency(first_input_edit_text.text!!, coeffFromFirstToSecond))
            1, 3 -> first_input_edit_text.setText(convertToOtherCurrency(second_input_edit_text.text!!, coeffFromSecondToFirst))
        }
    }


    // get conversion coefficients and change the field out of focus
    private fun getCoeffConversion(secondCurrency: String, firstCurrency: String) {
        if (secondCurrency != firstCurrency) {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBar.visibility = ProgressBar.VISIBLE
            val secondCurrencyfirstCurrency = """${secondCurrency}_$firstCurrency"""
            val firstCurrencysecondCurrency = """${firstCurrency}_$secondCurrency"""
            val partOfQuerry = """$secondCurrencyfirstCurrency,$firstCurrencysecondCurrency"""
            currencyConverterApi.getExchangeRates(partOfQuerry)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({
                            coeffFromFirstToSecond = it[secondCurrencyfirstCurrency]!!.toBigDecimal()
                            coeffFromSecondToFirst = it[firstCurrencysecondCurrency]!!.toBigDecimal()
                            changeNoFocusInputEditText(state)
                            progressBar.visibility = ProgressBar.INVISIBLE
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        }, {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        Toast.makeText(this, getString(R.string.user_has_lost_internet_and_dont_have_this_coeff), Toast.LENGTH_SHORT).show()}
                    )
        } else {
            coeffFromFirstToSecond = BigDecimal(1.0)
            coeffFromSecondToFirst = BigDecimal(1.0)
            changeNoFocusInputEditText(state)
        }
    }

    private fun convertToOtherCurrency(inputEditText: CharSequence, coeff: BigDecimal): String {
        return when {
            inputEditText.isEmpty() -> ""
            inputEditText.toString() == "." -> "0.0"
            else -> {
                // convert from format 0.9988595 -> 0.99
                val newValueCurrencyValue = (inputEditText.toString().toBigDecimal() * coeff).setScale(2, RoundingMode.DOWN)
                newValueCurrencyValue.toPlainString()
            }
        }
    }


    private fun setupInternetConnectionObserver(): Disposable {
        return ReactiveNetwork.observeInternetConnectivity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { isConnected: Boolean? ->
                            isConnected?.let {
                                if (!isConnected)
                                    Toast.makeText(this, getString(R.string.user_has_lost_internet_connection_message), Toast.LENGTH_SHORT).show()
                                else if (isConnected && !isConnectedToInternet) {
                                    Toast.makeText(this, getString(R.string.user_find_internet_connection_message), Toast.LENGTH_SHORT).show()
                                    loadCurrencies()
                                }
                                isConnectedToInternet = isConnected
                            }
                        },
                        { t: Throwable? ->
                            Log.v("ReactiveNetwork", t?.message)
                        }
                )
    }

    override fun onStart() {
        super.onStart()
        compositeDisposable.add(setupInternetConnectionObserver())
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
