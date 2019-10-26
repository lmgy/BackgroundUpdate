package com.lmgy.bgupdate;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;

/**
 * @author lmgy
 * @date 2019/10/26
 */
public interface DownloadApi {

    @GET
    Observable<ResponseBody> downloadFile(@Url String fileUrl);

}
