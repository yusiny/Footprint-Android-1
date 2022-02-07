package com.footprint.footprint.ui.signin

import android.content.Intent
import android.os.Handler
import android.util.Log
import com.footprint.footprint.R
import com.footprint.footprint.data.remote.auth.AuthService
import com.footprint.footprint.data.remote.auth.Login
import com.footprint.footprint.data.remote.badge.BadgeService
import com.footprint.footprint.data.remote.badge.MonthBadge
import com.footprint.footprint.databinding.ActivitySplashBinding
import com.footprint.footprint.ui.BaseActivity
import com.footprint.footprint.ui.main.MainActivity
import com.footprint.footprint.ui.onboarding.OnBoardingActivity
import com.footprint.footprint.utils.getJwt
import com.footprint.footprint.utils.getLoginStatus
import com.footprint.footprint.utils.getOnboarding
import com.kakao.sdk.user.UserApiClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate), SplashView, BadgeView {
    lateinit var mGoogleSignInClient: GoogleSignInClient
    private var isGoogleLogin = false
    private var isKakaoLogin = false

    override fun initAfterBinding() {
        //온보딩 화면 O/X => 3초 후 실행
        val handler = Handler()
        handler.postDelayed({
            //1. 온보딩 실행 여부 spf에서 받아오기
            var onboardingStatus = getOnboarding(this)
            Log.d("ONBOARDING", onboardingStatus.toString())

            if (!onboardingStatus) {
                //2. false -> 온보딩 실행해야 함 -> OnboardingActivity
                startNextActivity(OnBoardingActivity::class.java)
                finish()
            } else {
                autoLogin()
            }
        }, 3000)
    }


    private fun autoLogin(){
        Log.d("SPLASH", getJwt().toString())
        if(getJwt() != null){ // O -> 자동로그인 API 호출
            AuthService.autoLogin(this)
        }else{  // X -> 로그인 액티비티
            startNextActivity(SigninActivity::class.java)
            finish()
        }
    }


    /*자동 로그인 API*/
    override fun onAutoLoginSuccess(result: Login) {
        if(result.status == "ACTIVE"){
            if(result.checkMonthChanged){ // 바뀜 -> 뱃지 API 호출
                BadgeService.getMonthBadge(this)
            }else{ // -> 메인 액티비티
                startNextActivity(MainActivity::class.java)
                finish()
            }
        }

        Log.d("SPLASH/API-SUCCESS", "status: $result.status jwt: $result.jwtId")
    }

    override fun onAutoLoginFailure(code: Int, message: String) {
        Log.d("SPLASH/API-FAILURE", "code: $code message: $message")
    }

    /*뱃지 API*/
    override fun onMonthBadgeSuccess(isBadgeExist: Boolean, monthBadge: MonthBadge?) {
        val intent = Intent(this, MainActivity::class.java)
        if(isBadgeExist)
            intent.putExtra("badge", monthBadge)
        startActivity(intent)
        Log.d("SPLASH(BADGE)/API-SUCCESS", monthBadge.toString())
    }

    override fun onMonthBadgeFailure(code: Int, message: String) {
        Log.d("SPLASH(BADGE)/API-FAILURE", code.toString() + message)
    }

}
