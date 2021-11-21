package com.songlongkuan.miwifi.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: SongLongKuan
 * @date: 2021/11/21 7:56 上午
 */
@NoArgsConstructor
@Data
public class SnsInitEntity {
    private String deviceid;
    private String clientinfo;
    private String ssid;
    private Integer code;
}
