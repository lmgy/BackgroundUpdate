package com.lmgy.bgupdate;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

/**
 * @author lmgy
 * @date 2019/10/26
 */
public class NetWork {

    private static DownloadApi downloadApi;

    public static DownloadApi getApi(ProgressListener listener) {
        if (downloadApi == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://your.api.url/")
                    .client(addProgressResponseListener(listener))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
            downloadApi = retrofit.create(DownloadApi.class);
        }
        return downloadApi;
    }

    /**
     * 包装OkHttpClient，用于下载文件的回调
     *
     * @param progressListener 进度回调接口
     * @return 包装后的OkHttpClient
     */
    public static OkHttpClient addProgressResponseListener(final ProgressListener progressListener) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        //增加拦截器
        client.addInterceptor(chain -> {
            //拦截
            Response originalResponse = chain.proceed(chain.request());
            //包装响应体并返回
            return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
        });
        return client.build();
    }

}
