package com.songlongkuan.miwifi.corn;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.songlongkuan.miwifi.config.WifiConfig;
import com.songlongkuan.miwifi.entity.AdApplyRentEntity;
import com.songlongkuan.miwifi.entity.SnsInitEntity;
import com.songlongkuan.miwifi.service.WifiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @description:
 * @author: SongLongKuan
 * @date: 2021/11/21 8:15 上午
 */
@Service
@Slf4j
public class CornService {
    @Autowired
    private WifiService wifiService;
    @Autowired
    private WifiConfig wifiConfig;

    /**
     * mac 地址对应的设备id、clientinfo 等
     */
    private final Map<String, SnsInitEntity> mac$SnsInitEntity = new ConcurrentHashMap<>();

    @Scheduled(initialDelay = 3000, fixedDelay = (1000 * 60 * 3))
    public void scheduling() {
        try {
            String localMacAddress = wifiService.getLocalMacAddress();
            //把客户端信息缓存起来
            SnsInitEntity snsInitEntity = mac$SnsInitEntity.computeIfAbsent(localMacAddress, mac -> wifiService.getSnsInit());
            AdApplyRentEntity adApplyRentEntity = wifiService.adApplyRent(snsInitEntity);
            if (Objects.isNull(adApplyRentEntity)) {
                log.error("处理失败");
                return;
            }
            if (!Objects.equals(adApplyRentEntity.getCode(), "0")) {
                String chooseMac = this.choose(wifiConfig.getMac(), localMacAddress);
                //如果该mac地址已经有缓存的client信息  则直接用该mac的信息点击一下广告
                if (mac$SnsInitEntity.containsKey(chooseMac)) {
                    SnsInitEntity chooseMacSnsInitEntity = mac$SnsInitEntity.get(chooseMac);
                    wifiService.adApplyRent(chooseMacSnsInitEntity);
                }
                //修改wifi的mac地址
                wifiService.switchMac(chooseMac);
                //重连wifi
                wifiService.resetConnectWifi();
                if (mac$SnsInitEntity.containsKey(chooseMac)) {
                    log.info("顺滑切换mac地址 chooseMac={}", chooseMac);
                    return;
                }
                //等三秒
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
                //已经切换好了mac地址，并重连了WIFI，再调用一次方法  点击广告
                this.scheduling();
            } else {
                log.info("试用WIFI成功");
            }
        } catch (IORuntimeException ioRuntimeException) {
            log.debug("发起http请求失败", ioRuntimeException);
            log.info("发起http请求失败 可能没连上wifi，等一会，休息一下");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
            this.scheduling();
        }
    }


    @Scheduled(initialDelay = 3000L, fixedDelay = 3000L)
    public void ping() {
        boolean ping = wifiService.ping();
        if (!ping) {
            this.scheduling();
        }
    }


    /**
     * 选择下一个mac地址
     *
     * @param macList         : mac地址列表
     * @param localMacAddress : 当前的mac地址
     * @author: SongLongKuan
     * @date: 2021/11/21 11:19 上午
     * @return: {@link String}
     */
    private String choose(List<String> macList, String localMacAddress) {
        List<String> mac = wifiConfig.getMac();
        //查找当前的mac地址是第几位，如果找不到的话，从第一个开始获取
        int currMacIndex = -1;
        for (int i = 0; i < mac.size(); i++) {
            if (Objects.equals(mac.get(i), localMacAddress)) {
                currMacIndex = i;
                break;
            }
        }
        //选择下一个mac地址
        int index = (currMacIndex + 1) % macList.size();
        String chooseMac = macList.get(index);
        log.info("需要切换mac地址，当前地址为:{} 将要切换的地址为:{}", localMacAddress, chooseMac);
        return chooseMac;
    }


    @PreDestroy
    public void destory() {
        log.info("destory mac$SnsInitEntity={}", JSONUtil.toJsonStr(mac$SnsInitEntity));
        if (CollectionUtil.isEmpty(mac$SnsInitEntity)) {
            return;
        }
        String property = System.getProperty("user.dir");
        File cacheFile = new File(property + File.separator + "cache$clientinfo.json");
        log.info("cacheFile={}", cacheFile.getAbsolutePath());
        try (FileOutputStream fileOutputStream = new FileOutputStream(cacheFile)) {
            String jsonStr = JSONUtil.toJsonStr(mac$SnsInitEntity);
            fileOutputStream.write(jsonStr.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void init() {
        String property = System.getProperty("user.dir");
        File file = new File(property + File.separator + "cache$clientinfo.json");
        if (!file.exists()) {
            return;
        }
        FileReader fileReader = new FileReader(file, "UTF-8");
        String jsonStr = fileReader.readString();
        if (!StringUtils.hasText(jsonStr)) {
            return;
        }
        TypeReference typeReference = new TypeReference<Map<String, SnsInitEntity>>() {
        };
        Map<String, SnsInitEntity> initEntityMap = JSONUtil.toBean(jsonStr, (Type) typeReference, false);
        if (CollectionUtil.isEmpty(initEntityMap)) {
            return;
        }
        mac$SnsInitEntity.putAll(initEntityMap);
        log.info("初始化缓存成功: {}", JSONUtil.toJsonStr(mac$SnsInitEntity));
    }

}
