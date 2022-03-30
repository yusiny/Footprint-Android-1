package com.footprint.footprint.ui.signin

import android.content.Intent
import android.os.Handler
import androidx.lifecycle.Observer
import com.footprint.footprint.R
import com.footprint.footprint.data.dto.Login
import com.footprint.footprint.data.remote.badge.BadgeInfo
import com.footprint.footprint.data.remote.badge.BadgeService
import com.footprint.footprint.databinding.ActivitySplashBinding
import com.footprint.footprint.ui.BaseActivity
import com.footprint.footprint.ui.main.MainActivity
import com.footprint.footprint.ui.onboarding.OnBoardingActivity
import com.footprint.footprint.utils.*
import com.footprint.footprint.viewmodel.SplashViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import org.koin.androidx.viewmodel.ext.android.viewModel


class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate), MonthBadgeView {

    private val splashVm: SplashViewModel by viewModel()

    override fun initAfterBinding() {
        //온보딩 화면 O/X => 1.5
        val handler = Handler()
        handler.postDelayed({
            //1. 온보딩 실행 여부 spf에서 받아오기
            if (!getOnboarding()) {
                //2. false -> 온보딩 실행해야 함 -> OnboardingActivity
                startNextActivity(OnBoardingActivity::class.java)
                finish()
            } else {
                autoLogin()
            }
        }, 1500)

        observe()
    }


    private fun autoLogin() {
        if (getJwt() != null) { // O -> 자동로그인 API 호출
            //AuthService.autoLogin(this)
            saveJwt("12")
            splashVm.autoLogin()
        } else {  // X -> 로그인 액티비티
            startNextActivity(SigninActivity::class.java)
            finish()
        }
    }

    /*뱃지 API*/
    override fun onMonthBadgeSuccess(isBadgeExist: Boolean, monthBadge: BadgeInfo?) {
        val intent = Intent(this, MainActivity::class.java)
        if (isBadgeExist)
            intent.putExtra("badge", Gson().toJson(monthBadge))
        startActivity(intent)
        LogUtils.d("SPLASH(BADGE)/API-SUCCESS", monthBadge.toString())
    }

    override fun onMonthBadgeFailure(code: Int, message: String) {
        LogUtils.d("SPLASH(BADGE)/API-FAILURE", code.toString() + message)
    }

    /*액티비티 이동*/
    //Main Activity
    private fun startMainActivity() {
        startNextActivity(MainActivity::class.java)
        finish()
    }

    //SignIn Activity
    private fun startSignInActivity() {
        startNextActivity(SigninActivity::class.java)
        finish()
    }

    private fun observe(){
        splashVm.mutableErrorType.observe(this, androidx.lifecycle.Observer {
            when (it) {
                ErrorType.JWT -> { //JWT 관련 에러 발생 시, jwt 지우고 로그인 액티비티로 이동
                    removeJwt()
                    startSignInActivity()
                }
                ErrorType.NETWORK -> {
                    Snackbar.make(binding.root, getString(R.string.error_network), Snackbar.LENGTH_INDEFINITE).setAction(
                        R.string.action_retry) {
                    }.show()
                }
                else -> Snackbar.make(binding.root, getString(R.string.error_api_fail), Snackbar.LENGTH_INDEFINITE).setAction(
                    R.string.action_retry) {
                }.show()
            }
        })

        splashVm.thisLogin.observe(this, Observer {
            when(it.status){
                "ACTIVE" -> {   // 가입된 회원
                    if (it.checkMonthChanged) { // 첫 접속 -> 뱃지 API 호출
                        BadgeService.getMonthBadge(this)
                    } else { // -> 메인 액티비티
                        startMainActivity()
                    }
                }
                "ONGOING" -> { // 가입이 완료되지 않은 회원 -> 로그인 액티비티
                    startSignInActivity()
                }
            }
        })
    }

}
