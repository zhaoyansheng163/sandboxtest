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
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@MetaInfServices(Module.class)
@Information(id = "spring-bean-invoker", version = "0.0.1", author = "your-email@example.com")
public class SpringBeanInvokerModule extends ParamSupported implements Module {
    private final Logger logger = LoggerFactory.getLogger(getClass());
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

        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);

        if (StringUtils.isBlank(methodName)) {
            printer.print("Error: methodName is required");
            return;
        }

        try {
            // 获取Bean（从缓存或直接获取）
            Object bean;
            String cacheKey = StringUtils.isNotBlank(beanName) ? beanName : className;

            if (beanCache.containsKey(cacheKey)) {
                bean = beanCache.get(cacheKey);
            } else {
                if (applicationContext == null) {
                    applicationContext = getApplicationContext();
                    if (applicationContext == null) {
                        printer.print("Error: Unable to get Spring ApplicationContext");
                        return;
                    }
                }

                if (StringUtils.isNotBlank(beanName)) {
                    bean = applicationContext.getBean(beanName);
                } else {
                    Class<?> clazz = Class.forName(className);
                    bean = applicationContext.getBean(clazz);
                }

                beanCache.put(cacheKey, bean);
            }

            // 获取方法（从缓存或通过反射）
            Method method;
            String methodCacheKey = cacheKey + "#" + methodName;

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
                    return;
                }

                methodCache.put(methodCacheKey, method);
            }

            // 解析参数
            Object[] args = null;
            if (StringUtils.isNotBlank(argsJson)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                args = objectMapper.readValue(argsJson, Object[].class);

                // 如果需要，可以在这里进行参数类型转换
                // 例如，将JSON数字转换为正确的Java类型
            }

            // 调用方法
            Object result;
            if (args != null && args.length > 0) {
                result = method.invoke(bean, args);
            } else {
                result = method.invoke(bean);
            }

            // 输出结果
            if (result != null) {
                printer.print("Method result: " + objectMapper.writeValueAsString(result));
            } else {
                printer.print("Method executed successfully, returned null");
            }

        } catch (Exception e) {
            printer.print("Error invoking method: " + e.getMessage());
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
                    return;
                }
            }

            String[] beanNames = applicationContext.getBeanDefinitionNames();
            printer.print("Spring Beans (" + beanNames.length + "):");

            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
                printer.print(beanName + " : " + bean.getClass().getName());
            }

        } catch (Exception e) {
            printer.print("Error listing beans: " + e.getMessage());
        }
    }
    @Http("/listBeans1")
    public void listBeans1(final Map<String, String> param, final PrintWriter writer) {
        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);
        printer.print("enter list beans1.....................");
        logger.info("enter list beans1");
        try {
            if (applicationContext == null) {
                applicationContext = getApplicationContext();
                if (applicationContext == null) {
                    printer.print("Error: Unable to get Spring ApplicationContext");
                    return;
                }
            }

            String[] beanNames = applicationContext.getBeanDefinitionNames();
            printer.print("Spring Beans (" + beanNames.length + "):");

            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
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
}