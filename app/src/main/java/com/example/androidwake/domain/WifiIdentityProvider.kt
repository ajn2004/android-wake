package com.example.androidwake.domain

interface WifiIdentityProvider {
    fun getCurrentIdentity(): NetworkIdentity?
}
