# ZZ(Zero Trust, Zero Governance)

一个基于ipv6的手机端(Android)与Web技术的点对点聊天软件

## 基本功能目标

1. 建立不依赖于特定公司的点对点用户系统
2. 能轻松通过短信，二维码，字符串分享的方式轻松与对方建立点对点的（加密/安全的）聊天
3. 基本的好友，群组，文件传输功能
4. 其它必要的即时通讯软件功能，如文字，语音，视频，实时通话，实时视频等。
5. 作为私密聊天需要的备用软件

## API 说明

### API协议的格式

API 非即时交互全部采用JSON格式传递，全部基于HTTP协议。

>对于语音通话或者视频通话采用其它实时交互协议


### API 业务逻辑

API 分两大块：

1. 建立连接与更新连接信息
2. 获取消息

#### 建立连接与更新连接信息

##### 建立连接

API连接建立过程：
1. 服务器发布访问地址与公钥
2. 客户端发起连接请求，通过服务器公钥加密发送自己的公钥，ip地址与端口，以及服务器的会话ID
3. 服务器接收并保存客户端公钥，IP地址和端口,和会话ID
4. 服务器生成会话ID, 通过客户端的公钥加密生成新的加密字符串，并发送给客户端。
5. 连接建立完成

> 未来会考虑再增加对称加密算法加密以防止量子计算的攻击


##### 更新连接

由于服务器的IP变化是一个常态化的事情，通过更新服务器IP可以更好的保存以前的会话记录，防止过快的失联。

API服务器建立过程：
1. 变更端发起更新请求，通过服务器的公钥加密会话ID，ip地址与端口
3. 服务器接收并保存，会话结束

#### 建立消息通讯

1. 一方发起消息通讯请求
2. 另一方接收消息通讯请求
3. 开启消息的推送与接收功能



