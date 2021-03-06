package com.rockchips.mediacenter.service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import com.rockchips.mediacenter.utils.PlatformUtils;

import android.util.Log;

public class HttpUtils
{
    private static final String TAG = "HttpUtils";

    public static HttpDetails getHttpResourceDetails(URL url, int connTimeout, int readTimeout)
    {
        try
        {
        	if(PlatformUtils.getSDKVersion() <= 19){
        		HttpClient client = new DefaultHttpClient();
    			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000); 
    			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
    			Log.i(TAG, "getHttpResourceDetails->httpGet->url:" + url.toString());
    			HttpGet httpGet = new HttpGet(url.toString());
    			HttpResponse response = client.execute(httpGet);
    			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
    				HttpDetails httpDetails = new HttpDetails();
                    httpDetails.contentLength = (int)response.getEntity().getContentLength();
                    httpDetails.acceptRanges = null;
                    httpDetails.responseCode = HttpStatus.SC_OK;
                    client = null;
                    return httpDetails;
    			}
               client = null;
        	}

            URLConnection conn2 = url.openConnection();

            // 首次连接获取长度===========================================
            conn2.setConnectTimeout(connTimeout);
            conn2.setReadTimeout(readTimeout);
            conn2.connect();

            // Accept-Ranges 这个字段说明Web服务器是否支持Range(是否支持断点续传功能),如果支持,则返回Accept-Ranges: bytes
            String ar = conn2.getHeaderField("Accept-Ranges");
            int k = conn2.getContentLength();
            ((HttpURLConnection) conn2).disconnect();

            // 再次连接设置断点续传起始位置用于判断是否支持断点续传====================
            conn2 = url.openConnection();
            conn2.setConnectTimeout(connTimeout);
            conn2.setReadTimeout(readTimeout);
            // 设置断点续传开始到结束的位置-----zzm
            conn2.setRequestProperty("Range", "bytes=" + 0 + "-" + 1);
            conn2.connect();
            // 获取返回码-----zzm（206代表支持断点续传）
            int responseCode = ((HttpURLConnection) conn2).getResponseCode();
            ((HttpURLConnection) conn2).disconnect();

            Log.d(TAG, "url:" + url.toString() + " Length:" + k + " Accept-Ranges:" + ar + " responseCode:" + responseCode);

            HttpDetails det = new HttpDetails();
            det.contentLength = k;
            det.acceptRanges = ar;
            det.responseCode = responseCode;

            return det;

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * 
     * {@link Description} the info of url pointing to file when to download <功能详细描述>
     * 
     * @author t00181037
     * @version [版本号, 2013-6-13]
     * @see [相关类/方法]
     * @since [产品/模块版本]
     */
    public static class HttpDetails
    {
        public int contentLength;

        public String acceptRanges;

        public int responseCode;
    }
}
