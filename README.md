# oplus_exsystemserver-analyz
oplus-services.jar analyze

设备：OPPO K7，IQOO_Z1x Android12

结论：从OPPO和vivo的Framework实现基本是将自家的自定义服务在运行和编译都做了解耦处理。

OPPO：

oplus-frameworks.jar依赖frameworks.jar
oplus-services.jar依赖services.jar
oplus-services.jar和services.jar都依赖oplus-frameworks.jar
       SystemServer中通过SystemServerExtPlugin反射拿到ISystemServerExt实例，执行自定义服务启动和注册工作；ISystemServerExt的实现在oplus-services.jar中；

 

vivo：

vivo-frameworks.jar依赖frameworks.jar
vivo-services.jar依赖services.jar
vivo-services.jar和services.jar都依赖vivo-frameworks.jar
       SystemServer中通过IVivoSystemServer实例执行自定义服务启动和注册工作，其中IVivoSystemServer的实现在vivo-services.jar
