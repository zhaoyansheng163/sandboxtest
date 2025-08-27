参考  https://www.cnblogs.com/ylty/p/17108963.html

1、sandbox-demo
maven package打包生成jar 
上传到服务器上  java -jar demo-0.0.1-SNAPSHOT.jar 启动
ava -jar demo-0.0.1-SNAPSHOT.jar --server.port=9000  指定端口

2、启动
# 这里假设 27377 是目标进程号（也就是报异常的原服务）
./sandbox.sh -p 27377

3、执行修复
./sandbox.sh -p 27377 -d 'exception-handler/repairExceptionVoid'


4、支持传参的
./sandbox.sh -p 21042 -d 'exception-repair/returnObject?class=com.example.demo.controller.SandboxController&method=errorObject&return=com.example.demo.dto.SandboxReturnType&returnString=%7B%22name%22%3A%22111%22%7D'


