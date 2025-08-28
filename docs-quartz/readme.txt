查看版本信息
http://192.168.50.201:32930/sandbox/default/module/http/sandbox-info/version

查看加载了哪些资源
http://192.168.50.201:32930/sandbox/default/module/http/sandbox-module-mgr/list

sandbox日志  /root/logs/sandbox
在 cfg/sandbox-logback.xml中配置的


./sandbox.sh -p 28438 -P 37710 -n mocktest -d spring-bean-invoker/listBeans
这样启动可以设置sandbox的端口 和启用的模块

http://192.168.50.201:42759/sandbox/default/module/http/spring-bean-invoker/listBeans  可以进入方法并打印日志
http://192.168.50.201:42759/sandbox/default/module/http/spring-bean-invoker/invokeMethod?beanName=com.yangxj.quartz.controller.JobController
http://192.168.50.201:40367/sandbox/default/module/http/spring-bean-invoker/invokeMethod?beanName=MyJob&className=com.yangxj.quartz.job.MyJob&methodName=executeInternal&args=1

参考
https://zhuanlan.zhihu.com/p/462148154

参考  
https://www.cnblogs.com/moonpool/articles/14510443.html  
http访问