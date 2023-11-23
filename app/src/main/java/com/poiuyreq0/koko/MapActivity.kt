package com.poiuyreq0.koko

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import com.poiuyreq0.koko.databinding.ActivityMapBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding

    private val sofaLength: Int = 12

    private lateinit var sofaArray: Array<ImageView>

    private lateinit var positions: List<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sofaArray = arrayOf(binding.sofa1, binding.sofa2, binding.sofa3,
            binding.sofa4, binding.sofa5, binding.sofa6,
            binding.sofa7, binding.sofa8, binding.sofa9,
            binding.sofa10, binding.sofa11, binding.sofa12)

        val name = intent.getStringExtra("name") ?: ""
        binding.nameTextView.text = name

        positions = findPositions(name)

        val button: AppCompatButton = binding.mapRefreshButton
        button.setOnClickListener {
            positions = findPositions(name)
        }
    }

    fun updateNumber(avgArray: List<Long>) {

        for (i in avgArray.indices) {
            if (avgArray[i] > 0) {
                sofaArray[i].setImageResource(R.drawable.sofa_custom)
                Log.d("if", "idx $i")
            } else {
                sofaArray[i].setImageResource(R.drawable.sofa)
                Log.d("else", "idx $i")
            }

            when (i) {
                in 0..1 -> binding.num1.text = (avgArray[0]+avgArray[1]).toString()
                in 2..3 -> binding.num2.text = (avgArray[2]+avgArray[3]).toString()
                4 -> binding.num3.text = avgArray[4].toString()
                5 -> binding.num4.text = avgArray[5].toString()
                6 -> binding.num5.text = avgArray[6].toString()
                7 -> binding.num6.text = avgArray[7].toString()
                in 8..9 -> binding.num7.text = (avgArray[8]+avgArray[9]).toString()
                in 10..11 -> binding.num8.text = (avgArray[10]+avgArray[11]).toString()
            }
        }
    }

    private fun findPositions(name: String): List<Long> {
        var positions: List<Long> = emptyList()

        val baseUrl = "http://ec2-3-34-131-7.ap-northeast-2.compute.amazonaws.com/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val cafesAPI = retrofit.create(CafesAPI::class.java)

        val call = cafesAPI.positionsApi(name)

        call.enqueue(object: Callback<List<Long>> {
            override fun onResponse(
                call: Call<List<Long>>,
                response: Response<List<Long>>
            ) {
                positions = response.body() ?: emptyList()

                updateNumber(positions)
            }

            override fun onFailure(call: Call<List<Long>>, t: Throwable) {
                Log.e("positions", "onFailure", t)
            }
        })

        return positions
    }
}