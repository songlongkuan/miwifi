
## 小米红包WIFI试用

仅限学习参考，目前只适配了mac，实测MacOS 12.0.1 通过

> 小米红包WIFI的试用是通过mac地址来判断试用时间的，只需要修改电脑的mac地址即可无限试用

### 1、先生成多个mac地址
```shell
openssl rand -hex 6 | sed 's/\(..\)/\1:/g; s/.$//'
# 生成出来的mac地址不一定能用，先自行使用命令验证一下mac地址是否可行
# 具体可参考连接：https://zhuanlan.zhihu.com/p/33473860
```
### 2、修改配置文件 application.yaml

```yaml
wifi:
  #电脑的网卡名，一般是en0 ，可通过ifconfig命令找到wifi网卡的名字确认
  ether: en0
  #系统密码，因为修改mac地址需要密码
  password: xxxxxxx
  #wifi的信号
  ssid: '  小米共享WiFi_xxxx'
  # 通过命令生成出来的mac地址，会依据下面的mac地址以此修改本机mac地址
  mac:
    - xx:xx:xx:xx:xx:xx
```

### 3、最后编译运行项目即可
