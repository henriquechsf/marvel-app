package daniel.lop.io.marvelappstarter.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import daniel.lop.io.marvelappstarter.data.local.MarvelDatabase
import daniel.lop.io.marvelappstarter.data.remote.ServiceApi
import daniel.lop.io.marvelappstarter.util.Constants.APIKEY
import daniel.lop.io.marvelappstarter.util.Constants.BASE_URL
import daniel.lop.io.marvelappstarter.util.Constants.DATABASE_NAME
import daniel.lop.io.marvelappstarter.util.Constants.HASH
import daniel.lop.io.marvelappstarter.util.Constants.PRIVATE_KEY
import daniel.lop.io.marvelappstarter.util.Constants.PUBLIC_KEY
import daniel.lop.io.marvelappstarter.util.Constants.TS
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {

    // config ROOM database
    @Singleton
    @Provides
    fun provideMarvelDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context,
        MarvelDatabase::class.java,
        DATABASE_NAME
    ).build()

    @Singleton
    @Provides
    fun provideMarvelDao(database: MarvelDatabase) = database.marvelDao()

    @Singleton // cria uma única instancia
    @Provides // prove para a aplicação essa instancia
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient().newBuilder()
            .addInterceptor { chain ->
                val currentTimestamp = System.currentTimeMillis()

                val newUrl = chain.request().url
                    .newBuilder()
                    .addQueryParameter(TS, currentTimestamp.toString())
                    .addQueryParameter(APIKEY, PUBLIC_KEY)
                    .addQueryParameter(HASH,
                    provideToMd5Hash(currentTimestamp.toString() + PRIVATE_KEY + PUBLIC_KEY))
                    .build()

                val newRequest = chain.request()
                    .newBuilder()
                    .url(newUrl)
                    .build()

                chain.proceed(request = newRequest)
            }
            .addInterceptor(logging)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @Singleton
    @Provides
    fun provideServiceApi(retrofit: Retrofit): ServiceApi {
        return retrofit.create(ServiceApi::class.java)
    }

    @Singleton
    @Provides
    fun provideToMd5Hash(encrypted: String): String {
        var pass = encrypted
        var encryptedString: String? = null
        val md5: MessageDigest
        try {
            md5 = MessageDigest.getInstance("MD5")
            md5.update(pass.toByteArray(), 0, pass.length)
            pass = BigInteger(1, md5.digest()).toString(16)
            while (pass.length < 32) {
                pass = "0$pass"
            }
            encryptedString = pass
        } catch (e1: NoSuchAlgorithmException) {
            e1.printStackTrace()
        }
        Timber.d("hash -> $encryptedString")
        return encryptedString ?: ""
    }
}