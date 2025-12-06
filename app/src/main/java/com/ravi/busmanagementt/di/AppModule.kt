package com.ravi.busmanagementt.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.ravi.busmanagementt.data.repository.AdminRepositoryImpl
import com.ravi.busmanagementt.data.repository.AuthRepositoryImpl
import com.ravi.busmanagementt.data.repository.FirestoreBusRepositoryImpl
import com.ravi.busmanagementt.data.repository.LocationRepositoryImpl
import com.ravi.busmanagementt.data.repository.UserRepositoryImpl
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.domain.repository.AuthRepository
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import com.ravi.busmanagementt.domain.repository.RealtimeLocationRepository
import com.ravi.busmanagementt.domain.repository.UserRepository
import com.ravi.busmanagementt.utils.NetworkConnectivityManager
import com.ravi.busmanagementt.utils.NetworkConnectivityManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule { // Use 'object' for @Provides methods

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseRealtimeDatabase(): FirebaseDatabase {
        val dbUrl = "https://bus-tacking-system-ca065-default-rtdb.asia-southeast1.firebasedatabase.app"
        // This is a perfect use case for @Provides because it needs custom logic (the URL)
        return Firebase.database(dbUrl)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore{
        return FirebaseFirestore.getInstance()
    }
}


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule { // Must be an 'abstract class' for @Binds methods

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): RealtimeLocationRepository

    @Binds
    @Singleton
    abstract fun bindBusRepository(
        busRepositoryImpl: FirestoreBusRepositoryImpl
    ): FirestoreBusRepository


    @Binds
    @Singleton
    abstract fun bindNetworkConnectivityManager(
        networkConnectivityManagerImpl: NetworkConnectivityManagerImpl
    ): NetworkConnectivityManager


    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository


    @Binds
    @Singleton
    abstract fun bindAdminRepository(
        adminRepositoryImpl: AdminRepositoryImpl
    ): AdminRepository

}
