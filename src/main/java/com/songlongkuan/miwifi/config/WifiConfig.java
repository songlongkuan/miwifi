package com.songlongkuan.miwifi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description:
 * @author: SongLongKuan
 * @date: 2021/11/21 7:22 上午
 */
@Data
@Component
@ConfigurationProperties(prefix = "wifi")
public class WifiConfig {

    private String ether;

    private List<String> mac;

    private String password;

    private String ssid;
}
