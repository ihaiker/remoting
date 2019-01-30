#!/usr/bin/env bash

echo <<EOF
参数说明：
    -alias 产生别名
    -validity：指定创建的证书有效期多少天
    -keyalg ：指定密钥的算法
    -dname ：指定证书拥有者信息  CN=commonName OU=organizationUnit O=organizationName
                              L=localityName S=stateName C=country
    -keysize 指定密钥长度
    -storepass 指定密钥库的密码
    -keypass 指定别名条目的密码
    -keystore 指定密钥库的名称(产生的各类信息将不在.keystore文件中)
    -export 将别名指定的证书导出到文件 keytool -export -alias caroot -file caroot.crt
    -file 参数指定导出到文件的文件名
EOF
echo "remove jks folder"
rm -rf jks
echo "mkdir jks folder"
mkdir jks

cd jks

export ALIAS=remoting
export KEY_PASSWORD=remoting
export DNAME="CN=Haiker, OU=Developer,O=Remoting, L=Beijing, S=Beijing, C=CH"

echo "------- server -----------"

echo "server 1.创建server端KeyStore文件serverKeys.jks，用于虚构的通信者 Client的证书 ："
keytool -genkeypair -alias $ALIAS -keysize 1024 -validity 3650 -keyalg RSA -dname "$DNAME" -keypass $KEY_PASSWORD -storepass $KEY_PASSWORD -keystore serverKeys.jks


echo "server 2.生成服务端 Alice公钥证书,并将client.cer文件发给netty客户端使用者"
keytool -export -alias $ALIAS -keystore serverKeys.jks -file server.cer -storepass $KEY_PASSWORD


echo "------- client -----------"

echo "client 1.创建Client端KeyStore文件clientKeys.jks，用于虚构的通信者 Client的证书 ："
keytool -genkeypair -alias $ALIAS -keysize 1024 -validity 3650 -keyalg RSA -dname "$DNAME" -keypass $KEY_PASSWORD -storepass $KEY_PASSWORD -keystore clientKeys.jks

echo "client 2.导出客户端 Alice公钥证书,并将client.cer文件发给netty服务使用者"
keytool -export -alias $ALIAS -keystore clientKeys.jks -file client.cer -storepass $KEY_PASSWORD


echo "server 3.创建客户端KeyStore文件serverTurst.jks并导入服务端公钥证书(从netty客户端机器那拿到client.cer文件 )"
keytool -import -noprompt -alias $ALIAS -keystore serverTrust.jks -file client.cer -keypass $KEY_PASSWORD -storepass $KEY_PASSWORD


echo "client 3.创建客户端KeyStore文件clientTurst.jks并导入服务端公钥证书(从netty服务端机器那拿到server.cer文件 )"
keytool -import -noprompt -alias $ALIAS -keystore clientTrust.jks -file server.cer -keypass $KEY_PASSWORD -storepass $KEY_PASSWORD

