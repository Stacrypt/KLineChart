package com.github.fujianlian.klinechartdemo

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.github.fujianlian.klinechart.DataHelper
import com.github.fujianlian.klinechart.KLineChartAdapter
import com.github.fujianlian.klinechart.KLineEntity
import com.github.fujianlian.klinechart.draw.Status
import com.github.fujianlian.klinechart.formatter.DateFormatter
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var datas: List<KLineEntity>

    private val adapter by lazy { KLineChartAdapter() }

    private val subTexts: ArrayList<TextView> by lazy { arrayListOf(macdText, kdjText, rsiText, wrText) }
    // Main chart index subscript
    private var mainIndex = 0
    // Sub-picture index subscript
    private var subIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        kLineChartView.adapter = adapter
        kLineChartView.dateTimeFormatter = DateFormatter()
        kLineChartView.setGridRows(4)
        kLineChartView.setGridColumns(4)
        initData()
        initListener()
    }

    private fun initData() {
        kLineChartView.justShowLoading()
        datas = DataRequest.getALL(this@MainActivity).subList(0, 500)
        DataHelper.calculate(datas)
        runOnUiThread {
            adapter.addFooterData(datas)
            adapter.notifyDataSetChanged()
            kLineChartView.startAnimation()
            kLineChartView.refreshEnd()
        }
    }

    private fun initListener() {
        maText.setOnClickListener {
            if (mainIndex != 0) {
                kLineChartView.hideSelectData()
                mainIndex = 0
                maText.setTextColor(Color.parseColor("#eeb350"))
                bollText.setTextColor(Color.WHITE)
                kLineChartView.changeMainDrawType(Status.MA)
            }
        }
        bollText.setOnClickListener {
            if (mainIndex != 1) {
                kLineChartView.hideSelectData()
                mainIndex = 1
                bollText.setTextColor(Color.parseColor("#eeb350"))
                maText.setTextColor(Color.WHITE)
                kLineChartView.changeMainDrawType(Status.BOLL)
            }
        }
        mainHide.setOnClickListener {
            if (mainIndex != -1) {
                kLineChartView.hideSelectData()
                mainIndex = -1
                bollText.setTextColor(Color.WHITE)
                maText.setTextColor(Color.WHITE)
                kLineChartView.changeMainDrawType(Status.NONE)
            }
        }
        for ((index, text) in subTexts.withIndex()) {
            text.setOnClickListener {
                if (subIndex != index) {
                    kLineChartView.hideSelectData()
                    if (subIndex != -1) {
                        subTexts[subIndex].setTextColor(Color.WHITE)
                    }
                    subIndex = index
                    text.setTextColor(Color.parseColor("#eeb350"))
                    kLineChartView.setChildDraw(subIndex)
                }
            }
        }
        subHide.setOnClickListener {
            if (subIndex != -1) {
                kLineChartView.hideSelectData()
                subTexts[subIndex].setTextColor(Color.WHITE)
                subIndex = -1
                kLineChartView.hideChildDraw()
            }
        }
        fenText.setOnClickListener {
            kLineChartView.hideSelectData()
            fenText.setTextColor(Color.parseColor("#eeb350"))
            kText.setTextColor(Color.WHITE)
            kLineChartView.setMainDrawLine(true)
        }
        kText.setOnClickListener {
            kLineChartView.hideSelectData()
            kText.setTextColor(Color.parseColor("#eeb350"))
            fenText.setTextColor(Color.WHITE)
            kLineChartView.setMainDrawLine(false)
        }
    }
}
