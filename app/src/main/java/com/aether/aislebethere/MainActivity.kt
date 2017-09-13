package com.aether.aislebethere

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Item selectors
        val newItemEditText: EditText = findViewById(R.id.newItemEditText)
        newItemEditText.setOnKeyListener(object: View.OnKeyListener{
            override fun onKey(v: View?, code: Int, ev: KeyEvent?) : Boolean {
                if (ev?.action == KeyEvent.ACTION_UP && (ev.keyCode == KeyEvent.KEYCODE_ENTER || code == EditorInfo.IME_ACTION_DONE)) {
                    Toast.makeText(this@MainActivity, "typed", Toast.LENGTH_SHORT).show()
                }
                return false
            }
        })
    }

}
