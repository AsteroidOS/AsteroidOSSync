package github.vatsal.easyweather.retrofit.api;


import android.content.Context;
import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static ApiClient uniqInstance;
    private final String URL_LIVE = "https://api.openweathermap.org/data/2.5/";

    private WeatherInterface weatherInterface;

    public static synchronized ApiClient getInstance() {
        if (uniqInstance == null) {
            uniqInstance = new ApiClient();
        }
        return uniqInstance;
    }

    private void ApiClient(@NonNull final Context currContext) {
        try {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // set your desired log level
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor headerInterceptor = new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder();
                    builder.method(original.method(), original.body());

                    Request request = builder.build();

                    return chain.proceed(request);
                }
            };

            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(logging)
                    .build();
            String API_URL = URL_LIVE;

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();

            weatherInterface = retrofit.create(WeatherInterface.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WeatherInterface getApi(Context currContext) {
        if (uniqInstance == null) {
            getInstance();
        }
        uniqInstance.ApiClient(currContext);

        return weatherInterface;
    }
}
