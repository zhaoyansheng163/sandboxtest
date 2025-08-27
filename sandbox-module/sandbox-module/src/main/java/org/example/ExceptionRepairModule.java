package org.example;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatcher;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

@MetaInfServices(Module.class)
@Information(id = "exception-repair", version = "0.0.1", author = "xxx@mail.com")
public class ExceptionRepairModule extends ParamSupported implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Command("returnObject")
    public void returnObject(final Map<String, String> param, final PrintWriter writer) {
        final String cnPattern = getParameter(param, "class");
        final String mnPattern = getParameter(param, "method");
        final String rtPattern = getParameter(param, "return");
        final String rtString = getParameter(param, "returnString");

        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);

        final EventWatcher watcher = new EventWatchBuilder(moduleEventWatcher)
                .onClass(cnPattern)
                .onBehavior(mnPattern)
                .onWatch(new AdviceListener() {
                    /**
                     * 拦截指定方法，当这个方法抛出异常时将会被
                     * AdviceListener#afterThrowing()所拦截
                     */
                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {

                        Class clazz = Class.forName(rtPattern);
                        Object object;
                        if (StringUtils.isEmpty(rtString)) {
                            object = clazz.newInstance();
                            printer.print("repair exception, return empty object");
                        } else {
                            object = JSONObject.parseObject(rtString, clazz);
                            printer.print("repair exception, return object: " + object.toString());
                        }

                        ProcessController.returnImmediately(object);
                    }
                });

        try {
            printer.println(String.format(
                    "tracing on [%s#%s].\nPress CTRL_C abort it!",
                    cnPattern,
                    mnPattern
            ));
            printer.waitingForBroken();
        } finally {
            watcher.onUnWatched();
        }
    }

    @Command("returnEmptyList")
    public void returnList(final Map<String, String> param, final PrintWriter writer) {
        final String cnPattern = getParameter(param, "class");
        final String mnPattern = getParameter(param, "method");
        final String rtPattern = getParameter(param, "return");

        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);

        final EventWatcher watcher = new EventWatchBuilder(moduleEventWatcher)
                .onClass(cnPattern)
                .onBehavior(mnPattern)
                .onWatch(new AdviceListener() {
                    /**
                     * 拦截指定方法，当这个方法抛出异常时将会被
                     * AdviceListener#afterThrowing()所拦截
                     */
                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        ProcessController.returnImmediately(new ArrayList<>());
                    }
                });

        try {
            printer.println(String.format(
                    "tracing on [%s#%s].\nPress CTRL_C abort it!",
                    cnPattern,
                    mnPattern
            ));
            printer.waitingForBroken();
        } finally {
            watcher.onUnWatched();
        }
    }
}

