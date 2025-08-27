package org.example;

import java.util.Map;

public class ParamSupported {
    /**
     * 从参数Map中获取指定参数的值
     * @param param 参数Map
     * @param name 参数名
     * @return 参数值，如果不存在则返回null
     */
    protected String getParameter(final Map<String, String> param, final String name) {
        if (param == null || name == null) {
            return null;
        }
        return param.get(name);
    }
}
