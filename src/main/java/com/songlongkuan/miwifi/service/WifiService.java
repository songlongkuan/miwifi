package com.songlongkuan.miwifi.service;

import com.songlongkuan.miwifi.entity.AdApplyRentEntity;
import com.songlongkuan.miwifi.entity.SnsInitEntity;

/**
 * @description:
 * @author: SongLongKuan
 * @date: 2021/11/21 7:15 上午
 */
public interface WifiService {

    /**
     * 获取本地的mac地址
     *
     * @author: SongLongKuan
     * @date: 2021/11/21 7:25 上午
     * @return: {@link String}
     */
    String getLocalMacAddress();

    /**
     * 切换到指定的mac地址
     *
     * @param macAddress :
     * @author: SongLongKuan
     * @date: 2021/11/21 7:25 上午
     * @return: { void}
     */
    void switchMac(String macAddress);

    /**
     * 获取当前设备信息
     *
     * @author: SongLongKuan
     * @date: 2021/11/21 7:59 上午
     * @return: {@link SnsInitEntity}
     */
    SnsInitEntity getSnsInit();

    /**
     * 点击广告
     *
     * @param snsInitEntity: 当前客户端参数
     * @author: SongLongKuan
     * @date: 2021/11/21 8:09 上午
     * @return: {@link AdApplyRentEntity}
     */
    AdApplyRentEntity adApplyRent(SnsInitEntity snsInitEntity);

    /**
     * 重连wifi
     *
     * @author: SongLongKuan
     * @date: 2021/11/21 8:57 上午
     * @return: { void}
     */
    void resetConnectWifi();

    /**
     * 检查当前网络是否可用
     * @author: SongLongKuan
     * @date: 2021/11/21 5:31 下午
     * @return: {@link boolean}
     */
    boolean ping();
}
