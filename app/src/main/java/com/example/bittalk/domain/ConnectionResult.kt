package com.example.bittalk.domain

sealed interface ConnectionResult{
    object ConnectionEstablished : ConnectionResult
    data class TransferSucceed(val message : BTMessage) : ConnectionResult
    data class Error(val message : String) : ConnectionResult
}