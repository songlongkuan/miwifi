package com.songlongkuan.miwifi.service.impl;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.json.JSONUtil;
import com.songlongkuan.miwifi.config.WifiConfig;
import com.songlongkuan.miwifi.entity.AdApplyRentEntity;
import com.songlongkuan.miwifi.entity.SnsInitEntity;
import com.songlongkuan.miwifi.service.WifiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;

/**
 * @description:
 * @author: SongLongKuan
 * @date: 2021/11/21 7:16 上午
 */
@Slf4j
@Service
public class WifiServiceImpl implements WifiService {
    private final String CONSTANT_ETHER = "ether";

    /**
     * okhttp
     */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .build();
    @Autowired
    private WifiConfig wifiConfig;

    @Override
    public String getLocalMacAddress() {
        String ifconfigEn0 = shell("ifconfig " + wifiConfig.getEther());
        if (!StringUtils.hasText(ifconfigEn0)) {
            log.error("获取本地的mac地址失败");
            return null;
        }
        String[] split = ifconfigEn0.split("\n");
        for (String s : split) {
            if (s.trim().startsWith(CONSTANT_ETHER)) {
                return s.substring(CONSTANT_ETHER.length() + 1).trim();
            }
        }
        log.error("没有找到本地的mac地址");
        return null;
    }

    @Override
    public void switchMac(String macAddress) {
        shell("/System/Library/PrivateFrameworks/Apple80211.framework/Resources/airport -z");
        shell(String.format("ifconfig %s ether %s", wifiConfig.getEther(), macAddress));
        shell("networksetup -detectnewhardware");
    }

    @Override
    public SnsInitEntity getSnsInit() {
        String jsonResponse = requestGet("http://guest.miwifi.com:8999/cgi-bin/luci/api/misns/sns_init");
        if (!StringUtils.hasText(jsonResponse)) {
            log.error("请求失败，返回值为空");
            return null;
        }
        //清理jsonp
        jsonResponse = jsonResponse.replace("jsonpHandler(", "");
        jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 2);
        log.info("jsonResponse={}", jsonResponse);
        return JSONUtil.toBean(jsonResponse, SnsInitEntity.class);
    }

    @Override
    public AdApplyRentEntity adApplyRent(SnsInitEntity snsInitEntity) {
        String httpUrl = "http://api.miwifi.com/wifirent/api/ad_apply_rent?callback=jsonpCallback&router_id=%s&client_info=%s&_=" + System.currentTimeMillis();
        String jsonResponse = requestGet(String.format(httpUrl, snsInitEntity.getDeviceid(), snsInitEntity.getClientinfo()));
        if (!StringUtils.hasText(jsonResponse)) {
            log.error("请求失败，返回值为空");
            return null;
        }
        //清理jsonp
        jsonResponse = jsonResponse.replace("jsonpCallback(", "");
        if (jsonResponse.endsWith(");")) {
            jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 2);
        }
        log.info("jsonResponse={}", jsonResponse);
        return JSONUtil.toBean(jsonResponse, AdApplyRentEntity.class);
    }

    @Override
    public void resetConnectWifi() {
        shell(String.format("networksetup -setairportnetwork %s '%s'", wifiConfig.getEther(), wifiConfig.getSsid()));
    }

    @Override
    public boolean ping() {
        try {
            String requestGet = requestGet("http://baidu.com");
            if (!StringUtils.hasText(requestGet)) {
                return false;
            }
            return !requestGet.contains("小米共享WiFi");
        } catch (Exception ignored) {
        }
        return false;
    }


    /**
     * 执行shell命令，并且获得返回值
     *
     * @param shell :  shell命令
     * @author: SongLongKuan
     * @date: 2021/11/21 6:57 上午
     * @return: {@link String}
     */
    private String shell(String shell) {
        if (!StringUtils.hasText(shell)) {
            return null;
        }
        Process process = null;
        try {
            String shellCmd = String.format("echo %s| sudo -S %s", wifiConfig.getPassword(), shell);
            String[] cmd = {"/bin/bash", "-c", shellCmd};
            log.info("exece shell:{}", shellCmd);
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(cmd);
            String errorMessage = readInputStream(process.getErrorStream());
            if (StringUtils.hasText(errorMessage) && StringUtils.hasText(errorMessage.replaceAll("Password:", ""))) {
                log.error("shell fail message: {}", errorMessage);
            }
            InputStream inputStream = process.getInputStream();
            return readInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (Objects.nonNull(process)) {
                process.destroy();
            }
            log.info("exece shell end.");
        }
        return null;
    }

    private String readInputStream(InputStream inputStream) {
        try {
            if (Objects.isNull(inputStream)) {
                return null;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len = -1;
            byte[] byteCache = new byte[1024];
            while ((len = inputStream.read(byteCache)) != -1) {
                byteArrayOutputStream.write(byteCache, 0, len);
            }
            return byteArrayOutputStream.toString("UTF-8");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }


    private String requestGet(String url) {
        Response execute = null;
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            execute = okHttpClient.newCall(request).execute();
            return execute.body().string();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            if (Objects.nonNull(execute)) {
                execute.close();
            }
        }
    }
}
