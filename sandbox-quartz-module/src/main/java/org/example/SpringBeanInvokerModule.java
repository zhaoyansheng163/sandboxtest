package org.example;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatcher;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@MetaInfServices(Module.class)
@Information(id = "spring-bean-invoker", version = "0.0.1", author = "your-email@example.com")
public class SpringBeanInvokerModule extends ParamSupported implements Module {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static DefaultListableBeanFactory beanFactory;
    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存ApplicationContext，避免每次都通过反射获取
    private ApplicationContext applicationContext;

    // 缓存Bean和方法，提高性能
    private final Map<String, Object> beanCache = new ConcurrentHashMap<>();
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    @Command("getBean")
    public void getBean(final Map<String, String> param, final PrintWriter writer) {
        final String beanName = getParameter(param, "beanName");
        final String className = getParameter(param, "className");

        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);

        if (StringUtils.isBlank(beanName) && StringUtils.isBlank(className)) {
            printer.print("Error: Either beanName or className must be provided");
            return;
        }

        try {
            // 获取ApplicationContext
            if (applicationContext == null) {
                applicationContext = getApplicationContext();
                if (applicationContext == null) {
                    printer.print("Error: Unable to get Spring ApplicationContext");
                    return;
                }
            }

            // 获取Bean
            Object bean;
            if (StringUtils.isNotBlank(beanName)) {
                bean = applicationContext.getBean(beanName);
            } else {
                Class<?> clazz = Class.forName(className);
                bean = applicationContext.getBean(clazz);
            }

            // 缓存Bean
            String cacheKey = StringUtils.isNotBlank(beanName) ? beanName : className;
            beanCache.put(cacheKey, bean);

            printer.print("Successfully got bean: " + bean.getClass().getName());

        } catch (Exception e) {
            printer.print("Error getting bean: " + e.getMessage());
        }
    }

    @Command("invokeMethod")
    public void invokeMethod(final Map<String, String> param, final PrintWriter writer) {
        final String beanName = getParameter(param, "beanName");
        final String className = getParameter(param, "className");
        final String methodName = getParameter(param, "methodName");
        final String argsJson = getParameter(param, "args");
        logger.info("enter invokeMethod {}  {}  {}  {}",beanName,className,methodName,argsJson);

        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);

        if (StringUtils.isBlank(methodName)) {
            printer.print("Error: methodName is required");
            logger.info("Error: methodName is required");

            return;
        }

        try {
            // 获取Bean（从缓存或直接获取）
            Object bean;
            String cacheKey = StringUtils.isNotBlank(beanName) ? beanName : className;
            logger.info("cacheKey:{}",cacheKey);
            if (beanCache.containsKey(cacheKey)) {
                logger.info("------111bean：");
                bean = beanCache.get(cacheKey);
                logger.info("bean:{}",bean);

            } else {
                logger.info("------22222bean：");
                //Class<?> aClass = Class.forName(cacheKey);
                //logger.info("------22222bean：{}",aClass);
                bean = getBeanFactory().getBean(cacheKey);
                logger.info("------bean：{}",bean);

//                Object bean1 = getBeanFactory().getBean(aClass);
//                logger.info("------bean1：{}",bean1);

                beanCache.put(cacheKey, bean);
            }

            // 获取方法（从缓存或通过反射）
            Method method;
            String methodCacheKey = cacheKey + "#" + methodName;
            logger.info("获取方法：{}" ,methodCacheKey);
            if (methodCache.containsKey(methodCacheKey)) {
                method = methodCache.get(methodCacheKey);
            } else {
                // 查找方法
                Method[] methods = bean.getClass().getMethods();
                method = Arrays.stream(methods)
                        .filter(m -> m.getName().equals(methodName))
                        .findFirst()
                        .orElse(null);

                if (method == null) {
                    printer.print("Error: Method not found: " + methodName);
                    logger.info("Error: Method not found: {}",methodName);
                    return;
                }

                methodCache.put(methodCacheKey, method);
            }

            // 解析参数
//            Object[] args = null;
//            if (StringUtils.isNotBlank(argsJson)) {
//                Class<?>[] parameterTypes = method.getParameterTypes();
//                args = objectMapper.readValue(argsJson, Object[].class);
//
//                // 如果需要，可以在这里进行参数类型转换
//                // 例如，将JSON数字转换为正确的Java类型
//            }
            logger.info("准备调用");
            // 调用方法
            Object result;
//            if (args != null && args.length > 0) {
//                result = method.invoke(bean, args);
//            } else {
//                result = method.invoke(bean);
//            }
            result = method.invoke(bean);

            // 输出结果
            if (result != null) {
                printer.print("Method result: " + objectMapper.writeValueAsString(result));
                logger.info("Method result: {}",result);
            } else {
                printer.print("Method executed successfully, returned null");
                logger.info("Method executed successfully, returned null");
            }

        } catch (Exception e) {
            printer.print("Error invoking method: " + e.getMessage());
            logger.info("Error invoking method: {}" , e.getMessage());
        }
    }

    @Command("listBeans")
    public void listBeans(final Map<String, String> param, final PrintWriter writer) {
        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);
        printer.print("enter list beans.....................");
        logger.info("enter list beans");

        try {
            if (applicationContext == null) {
                applicationContext = getApplicationContext();
                if (applicationContext == null) {
                    printer.print("Error: Unable to get Spring ApplicationContext");
                    logger.info("Error: Unable to get Spring ApplicationContext");
                    return;
                }
            }

            String[] beanNames = applicationContext.getBeanDefinitionNames();
            printer.print("Spring Beans (" + beanNames.length + "):");
            logger.info("beans:{}",beanNames);

            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
                logger.info("bean111:{}",bean);
                printer.print(beanName + " : " + bean.getClass().getName());
            }

        } catch (Exception e) {
            printer.print("Error listing beans: " + e.getMessage());
        }
    }

    /**
     * 获取Spring ApplicationContext
     */
    private ApplicationContext getApplicationContext() {
        try {
            // 尝试通过Spring的特定类获取ApplicationContext
            Class<?> contextClass = Class.forName("org.springframework.web.context.support.WebApplicationContextUtils");
            Method method = contextClass.getMethod("getWebApplicationContext", javax.servlet.ServletContext.class);

            // 获取ServletContext
            Class<?> servletContextClass = Class.forName("org.springframework.web.context.ContextLoader");
            Method getCurrentWebApplicationContextMethod = servletContextClass.getMethod("getCurrentWebApplicationContext");
            Object webApplicationContext = getCurrentWebApplicationContextMethod.invoke(null);

            if (webApplicationContext != null) {
                Method getServletContextMethod = webApplicationContext.getClass().getMethod("getServletContext");
                Object servletContext = getServletContextMethod.invoke(webApplicationContext);

                return (ApplicationContext) method.invoke(null, servletContext);
            }

            // 非Web环境备选方案
            if (webApplicationContext == null) {
                try {
                    // 尝试通过Spring Boot的SpringApplication获取
                    Class<?> springApplicationClass = Class.forName("org.springframework.boot.SpringApplication");
                    java.lang.reflect.Method getApplicationContextMethod =
                            springApplicationClass.getMethod("getApplicationContext");
                    return (ApplicationContext) getApplicationContextMethod.invoke(null);
                } catch (Exception e) {
                    // 记录日志但不要抛出异常
                    System.err.println("Failed to get ApplicationContext: " + e.getMessage());
                }
            }


        } catch (Exception e) {
            // 尝试其他方式获取ApplicationContext
            try {
                // 尝试通过Spring Boot的SpringApplication获取
                Class<?> springApplicationClass = Class.forName("org.springframework.boot.SpringApplication");
                Method getApplicationContextMethod = springApplicationClass.getMethod("getApplicationContext");
                return (ApplicationContext) getApplicationContextMethod.invoke(null);
            } catch (Exception ex) {
                // 如果以上方法都失败，尝试拦截Spring相关方法获取
                return getApplicationContextByIntercept();
            }
        }

        return null;
    }

    /**
     * 通过拦截Spring相关方法获取ApplicationContext
     */
    private ApplicationContext getApplicationContextByIntercept() {
        final Object[] contextHolder = new Object[1];

        try {
            // 拦截Spring的ApplicationContextAware.setApplicationContext方法
            EventWatcher watcher = new EventWatchBuilder(moduleEventWatcher)
                    .onClass("org.springframework.context.ApplicationContextAware")
                    .onBehavior("setApplicationContext")
                    .onWatch(new AdviceListener() {
                        @Override
                        protected void before(Advice advice) throws Throwable {
                            contextHolder[0] = advice.getParameterArray()[0];
                            ProcessController.returnImmediately(null);
                        }
                    });

            // 等待一段时间获取ApplicationContext
            Thread.sleep(1000);
            watcher.onUnWatched();

            return (ApplicationContext) contextHolder[0];
        } catch (Exception e) {
            return null;
        }
    }

    public DefaultListableBeanFactory getBeanFactory() {
        if (beanFactory != null)
            return beanFactory;
        try {
            logger.info("...........................Mock-Server:");
            Class<DefaultListableBeanFactory> defaultListableBeanFactoryClass = DefaultListableBeanFactory.class;
            Field serializableFactories = defaultListableBeanFactoryClass.getDeclaredField("serializableFactories");
            serializableFactories.setAccessible(true);
            Map<String, Reference<DefaultListableBeanFactory>> o = (Map<String, Reference<DefaultListableBeanFactory>>)serializableFactories.get((Object)null);
            Set<Map.Entry<String, Reference<DefaultListableBeanFactory>>> entries = o.entrySet();
            Iterator<Map.Entry<String, Reference<DefaultListableBeanFactory>>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Reference<DefaultListableBeanFactory>> next = iterator.next();
                Reference<DefaultListableBeanFactory> value = next.getValue();
                DefaultListableBeanFactory defaultListableBeanFactory = value.get();
                assert defaultListableBeanFactory != null;
                beanFactory = defaultListableBeanFactory;
            }
        } catch (Exception|NoClassDefFoundError e) {
            logger.info("...........................Mock-Server  err",e);
        }
        logger.info("...........................Mock-Server  {}",beanFactory);
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = beanFactory.getBean(beanName);
            logger.info("Bean name: " + beanName + ", Bean object: " + bean);
        }

        return beanFactory;
    }

    public static String getBeanName(String className) {
        String[] parts = className.split("\\.");
        String simpleClassName = parts[parts.length - 1];
        String firstLetter = simpleClassName.substring(0, 1).toLowerCase();
        String restOfName = simpleClassName.substring(1);
        String result = firstLetter + restOfName;
        return result;
    }
}