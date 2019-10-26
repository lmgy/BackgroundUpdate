package com.lmgy.bgupdate;

/**
 * @author lmgy
 * @date 2019/10/26
 */
public interface ProgressListener {

    /**
     * @param section     已经下载或上传字节数
     * @param total        总字节数部分
     * @param done         是否完成
     */
    void onProgress(float section, float total, boolean done);

}
