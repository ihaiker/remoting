# Remoting

    Remoting是一个针对使用TCP业务的脚手架。

    由于很多项目中都会用TCP或者PRC这时候我们都会选择Netty，每次都会把一些相同的逻辑在写一遍耗费时间亦容易出错。为了不重复造轮子此项目产生了。

    注：此项目整体代码借鉴了RocketMQ remoting模块。

## 功能点

- [x] 同步发送命令
- [x] 异步发送命令
- [x] 发送单向命令
- [x] 客户端信息上报
- [x] 连接Auth验证
- [x] SSL
- [x] 命令HOOK
- [x] HA客户端

## 默认命令组成及编码

![命令组成及编码](https://github.con/ihaiker/remoting/blob/master/RemotingCommand.png)

## 实例
- [同步、异步、单向消息](https://github.con/ihaiker/remoting/blob/master/netty/src/test/java/la/renzhen/remoting/NettyTest.java)
- [客户端信息上报，连接Auth验证](https://github.con/ihaiker/remoting/blob/master/netty/src/test/java/la/renzhen/remoting/TestAuthNetty.java)
- [SSL 单向认证](https://github.con/ihaiker/remoting/blob/master/netty/src/test/java/la/renzhen/remoting/SecurityOneWayTest.java)
- [SSL 双向认证](https://github.con/ihaiker/remoting/blob/master/netty/src/test/java/la/renzhen/remoting/SecurityTwoWayTest.java)