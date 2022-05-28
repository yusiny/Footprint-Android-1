package com.footprint.footprint.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.footprint.footprint.data.dto.Result
import com.footprint.footprint.data.dto.Login
import com.footprint.footprint.data.dto.VersionDto
import com.footprint.footprint.domain.usecase.AutoLoginUseCase
import com.footprint.footprint.domain.usecase.GetVersionUseCase
import com.footprint.footprint.utils.ErrorType
import com.footprint.footprint.utils.LogUtils
import kotlinx.coroutines.launch

class SplashViewModel(private val autoLoginUseCase: AutoLoginUseCase, private val getVersionUseCase: GetVersionUseCase): BaseViewModel() {
    private val _thisLogin: MutableLiveData<Login> = MutableLiveData()
    val thisLogin: LiveData<Login> get() = _thisLogin

    private val _thisVersion: MutableLiveData<VersionDto> = MutableLiveData()
    val thisVersion: LiveData<VersionDto> get() = _thisVersion

    fun autoLogin(){
        viewModelScope.launch {
            when(val response = autoLoginUseCase.invoke()) {
                is Result.Success -> _thisLogin.value = response.value
                is Result.NetworkError -> mutableErrorType.postValue(ErrorType.NETWORK)
                is Result.GenericError -> {
                    LogUtils.d("responseCode", response.toString())
                    when(response.code){
                        2001, 2002, 2003, 2004 -> mutableErrorType.postValue(ErrorType.JWT)
                        else -> mutableErrorType.postValue(ErrorType.UNKNOWN)
                    }
                }
            }
        }
    }

    fun getVersion(version: String){
        viewModelScope.launch {
            when(val response = getVersionUseCase.invoke(version)){
                is Result.Success -> _thisVersion.value = response.value
                is Result.NetworkError -> mutableErrorType.postValue(ErrorType.NETWORK)
                is Result.GenericError -> mutableErrorType.postValue(ErrorType.UNKNOWN)
            }
        }
    }
}