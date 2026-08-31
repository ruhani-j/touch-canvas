package com.ruhan.touchcanvas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CounterFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_counter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val clickerView = view.findViewById<TouchCanvasView>(R.id.clickerView)
        val btnClear = view.findViewById<MaterialButton>(R.id.btnClear)
        clickerView.mode = DrawMode.COUNTER
        btnClear.setOnClickListener { clickerView.clear() }
    }
}
