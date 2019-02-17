#!/usr/bin/env bash

DIR=`pwd`

export ALIAS="remoting"
export DNAME="C=CN,L=Beijing,OU=Remoting,O=Remoting,CN=*"
export DNAME_CLIENT="C=CN,L=Beijing,OU=RemotingClient,O=RemotingClient,CN=*"
export CA_SUBJ="/C=CN/L=Beijing/O=Remoting/OU=Remoting/CN=*"

export SERVER_KEYSTORE="server.jks"
export SERVER_TRUSTSTORE="serverTrust.jks"
export SERVER_SIGN="server.cer"

export CLIENT_KEYSTORE="client.jks"
export CLIENT_TRUSTSTORE="clientTrust.jks"
export CLIENT_SIGN="client.cer"

export KEY_PASSWORD="remoting"

export OPENSSL_CONFIG="/usr/local/etc/openssl/openssl.cnf"
#export OPENSSL_CONFIG="/etc/ssl/openssl.cnf"

clean(){
    echo "clean jks"
    rm -rf $DIR/jks/*
}

help(){
cat <<EOF
参数说明：
    -alias    : 产生别名
    -validity : 指定创建的证书有效期多少天
    -keyalg   : 指定密钥的算法
    -dname    : 指定证书拥有者信息
                    CN = Common Name   域名        默认：*
                    OU = Organizational Unit        默认：Developer
                    O  = Organization  组织和名称   默认：Remoting
                    L  = Locality      地区        默认：Beijing
                    C  = Country       单位的两字母国家代码 默认：CN
    -keysize  : 指定密钥长度  2048
    -storepass: 指定密钥库的密码    默认：remoting
    -keypass    指定别名条目的密码  默认：remoting
    -keystore : 指定密钥库的名称    默认：server.jks,client.jks
    -export   : 将别名指定的证书导出到文件 keytool -export -alias caroot -file caroot.crt
    -file     : 参数指定导出到文件的文件名
EOF

    echo "Usage:"
    echo "./`basename $0` help"
    echo "./`basename $0` clean"
    echo "./`basename $0` oneway"
    echo "./`basename $0` twoway"
}

oneway(){
    echo "-*- 单向认证秘钥及签名证书生成 -*-"

    echo "（1）生成Netty服务器公钥、私钥和证书仓库：$SERVER_KEYSTORE"
    keytool -genkeypair -alias $ALIAS -keysize 2048 -validity 3650 -keyalg RSA \
        -dname "$DNAME" -keypass $KEY_PASSWORD -storepass $KEY_PASSWORD -keystore  $SERVER_KEYSTORE

    echo "（2）导出Netty服务端签名证书：$SERVER_SIGN"
    keytool -export -alias $ALIAS -keystore $SERVER_KEYSTORE -storepass $KEY_PASSWORD -file $SERVER_SIGN

    echo "（3）生成Netty客户端的公钥、私钥和证书仓库：$CLIENT_KEYSTORE"
    keytool -genkey -alias $ALIAS -keysize 2048 -validity 3650 -keyalg RSA \
        -dname $DNAME_CLIENT -keypass $KEY_PASSWORD -storepass $KEY_PASSWORD -keystore $CLIENT_KEYSTORE

    echo "（4）将Netty服务端的证书($SERVER_SIGN)导入到客户端的证书仓库($CLIENT_TRUSTSTORE)中："
    keytool -import -noprompt -trustcacerts -alias $ALIAS -file $SERVER_SIGN -storepass $KEY_PASSWORD -keypass $KEY_PASSWORD -keystore $CLIENT_TRUSTSTORE
}

twoway(){
    oneway
    echo "-*- 双向认证秘钥及签名证书的生成 -*-"
    echo "（5）、从($CLIENT_KEYSTORE)导出Netty的客户端的自签名证书：$CLIENT_SIGN"
    keytool -export -alias $ALIAS -keystore $CLIENT_KEYSTORE -storepass $KEY_PASSWORD -file $CLIENT_SIGN

    echo "（6）、将客户端的自签名证书($CLIENT_SIGN)导入到服务器的证书仓库中($SERVER_TRUSTSTORE)："
    keytool -import -noprompt -trustcacerts -alias $ALIAS -file $CLIENT_SIGN -storepass $KEY_PASSWORD -keypass $KEY_PASSWORD -keystore $SERVER_TRUSTSTORE
}

case "$1" in
  twoway)
    mkdir jks
    cd jks
    twoway
    ;;
  oneway)
    mkdir jks
    cd jks
    oneway
    ;;
  clean)
    clean
    ;;
  help)
    help
  ;;
  *)
    clean
    mkdir jks
    cd jks
    twoway
esac

