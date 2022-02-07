package com.footprint.footprint.data.remote.users

import android.util.Log
import com.footprint.footprint.ui.main.home.HomeDayView
import com.footprint.footprint.ui.main.home.HomeMonthView
import retrofit2.*
import com.footprint.footprint.ui.main.home.HomeView
import com.footprint.footprint.utils.GlobalApplication.Companion.retrofit


object UserService {
    private val userService = retrofit.create(UserRetrofitInterface::class.java)
    private lateinit var homeView: HomeView

    fun setHomeView(homeView: HomeView){
        this.homeView = homeView
    }

    /*유저 정보 API*/
    fun getUser(){
        userService.getUser().enqueue(object : Callback<UserResponse>{
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                val body = response.body()

                Log.d("USER/API-SUCCESS", body.toString())
                when(body!!.code){
                    1000 ->{
                        val result = body.result
                        homeView.onUserSuccess(result!!)
                    }
                    else -> homeView.onUserFailure(body.code, body.message)
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                homeView.onUserFailure(213, t.message.toString())
                Log.d("USER/API-FAILURE", t.message.toString())
            }
        })
    }

    /*일별 정보 API*/
    fun getToday(homeDayView: HomeDayView){
        userService.getToday().enqueue(object : Callback<TodayResponse>{
            override fun onResponse(call: Call<TodayResponse>, response: Response<TodayResponse>) {
                val body = response.body()

                Log.d("TODAY/API-SUCCESS", body.toString())
                when(body!!.code){
                    1000 ->{
                        val result = body.result
                        homeDayView.onTodaySuccess(result!!)
                    }
                    else -> homeDayView.onTodayFailure(body.code, body.message)
                }
            }

            override fun onFailure(call: Call<TodayResponse>, t: Throwable) {
                homeDayView.onTodayFailure(213, t.message.toString())
                Log.d("TODAY/API-FAILURE", t.message.toString())
            }
        })
    }


    /*월별 정보 API*/
    fun getTMonth(homeMonthView: HomeMonthView){
        userService.getTMonth().enqueue(object : Callback<TMonthResponse>{
            override fun onResponse(
                call: Call<TMonthResponse>,
                response: Response<TMonthResponse>
            ) {
                val body = response.body()

                Log.d("TMONTH/API-SUCCESS", body.toString())
                when(body!!.code){
                    1000 ->{
                        val result = body.result
                        homeMonthView.onTMonthSuccess(result!!)
                    }
                    else -> homeMonthView.onTMonthFailure(body.code, body.message)
                }
            }

            override fun onFailure(call: Call<TMonthResponse>, t: Throwable) {
                Log.d("TMONTH/API-FAILURE", t.message.toString())
                homeMonthView.onTMonthFailure(213, t.message.toString())
            }

        })
    }

}