package com.dn.spring.beans;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.spec.DESedeKeySpec;

public class DefaultBeanFactory implements BeanFactory, BeanDefinitionRegistry, Closeable {

    private final Log logger = LogFactory.getLog(getClass());

    private Map<String, BeanDefinition> beanDefintionMap = new ConcurrentHashMap<>(256);

    private Map<String, Object> beanMap = new ConcurrentHashMap<>(256);

    /**
     * 正在构建的bean
     */
    private ThreadLocal<Set<String>> buildingBeans = new ThreadLocal<>();

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionRegistException {
        Objects.requireNonNull(beanName, "注册bean需要给入beanName");
        Objects.requireNonNull(beanDefinition, "注册bean需要给入beanDefinition");

        // 校验给入的bean是否合法
        if (!beanDefinition.validate()) {
            throw new BeanDefinitionRegistException("名字为[" + beanName + "] 的bean定义不合法：" + beanDefinition);
        }

        if (this.containsBeanDefinition(beanName)) {
            throw new BeanDefinitionRegistException(
                    "名字为[" + beanName + "] 的bean定义已存在:" + this.getBeanDefinition(beanName));
        }

        this.beanDefintionMap.put(beanName, beanDefinition);
        logger.info("【" + beanName + "】 注册成功！");
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefintionMap.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {

        return this.beanDefintionMap.containsKey(beanName);
    }

    @Override
    public Object getBean(String name) throws Exception {
        return this.doGetBean(name);
    }

    protected Object doGetBean(String beanName) throws Exception {
        Objects.requireNonNull(beanName, "beanName不能为空");

        Object instance = beanMap.get(beanName);

        if (instance != null) {
            return instance;
        }

        BeanDefinition bd = this.getBeanDefinition(beanName);
        Objects.requireNonNull(bd, "不存在name为：" + beanName + "的beean 定义！");
        Objects.requireNonNull(bd, "beanDefinition不能为空");

        // 记录正在创建的Bean
        Set<String> ingBeans = this.buildingBeans.get();
        if (ingBeans == null) {
            ingBeans = new HashSet<>();
            this.buildingBeans.set(ingBeans);
        }

        // 检测循环依赖
        if (ingBeans.contains(beanName)) {
            throw new Exception(beanName + " 循环依赖！" + ingBeans);
        }

        // 记录正在创建的Bean
        ingBeans.add(beanName);

        /** 获取创建的bean */
        Class<?> type = bd.getBeanClass();
        if (type != null) {
            if (StringUtils.isBlank(bd.getFactoryMethodName())) {
                // 构造方法来构造对象
                instance = this.createInstanceByConstructor(bd);
            } else {
                // 静态工厂方法
                instance = this.createInstanceByStaticFactoryMethod(bd);
            }
        } else {
            // 工厂bean方式来构造对象
            instance = this.createInstanceByFactoryBean(bd);
        }

        // 创建好实例后，移除创建中记录
        ingBeans.remove(beanName);
        /** 属性依赖*/
        setPropertyDIValues(bd, instance);

        // 执行初始化方法
        this.doInit(bd, instance);

        if (bd.isSingleton()) {
            beanMap.put(beanName, instance);
        }

        return instance;
    }

    // 构造方法来构造对象
    private Object createInstanceByConstructor(BeanDefinition bd)
            throws InstantiationException, IllegalAccessException, Exception {
        try {
            /*判断构造函数*/
            Object[] args = getConstructorArgumentValues(bd);
            if (args == null) {
                return bd.getBeanClass().newInstance();
            } else {
                return determineConstructor(bd, args).newInstance(args);
            }

        } catch (SecurityException e1) {
            logger.error("创建bean的实例异常,beanDefinition：" + bd, e1);
            throw e1;
        }
    }

    /**
     * 获取构造器
     * @param bd
     * @param args
     * @return
     * @throws Exception
     */
    private Constructor<?> determineConstructor(BeanDefinition bd, Object[] args) throws Exception {
        Constructor<?> ct = null;

        if (null == args) {
            return bd.getBeanClass().getConstructor(null);
        }

        /** 对于原型bean，从第二次开始获取bean实例时，可直接获取第一次缓存的构造器 */
        ct = bd.getConstructor();
        if (null != ct) {
            return ct;
        }

        /** 根据参数类型获取精确匹配的构造方法 */
        Class<?> [] paramTypes = new Class[args.length];
        int j = 0;
        for (Object p :  args) {
            paramTypes[j++] = p.getClass();
        }
        try {
            ct = bd.getBeanClass().getConstructor(paramTypes);
        } catch (Exception e) {

        }

        /**
         * 没有精确参数类型匹配，则遍历匹配所有构造方法
         * 先判断参数数量，然后比对参数类型
         */
        if (null == ct) {
            outer:
            for (Constructor<?> ct0 : bd.getBeanClass().getConstructors()) {
                Class<?>[] paramterTypes = ct0.getParameterTypes();
                if (paramterTypes.length == args.length) {
                    for (int i = 0; i < paramterTypes.length; i++) {
                        if(!paramterTypes[i].isAssignableFrom(args[i].getClass())) {
                            continue outer;
                        }
                    }
                    ct = ct0;
                    break ;
                }
            }
        }

        if (null != ct) {
            /**
             * 缓存 构造器
             */
            if (bd.isPrototype()) {
                bd.setConstructor(ct);
            }
        } else {
            throw new RuntimeException("不存在对应的构造方法！" + bd);
        }

        return ct;
    }

    private Method determineFactoryMethod(BeanDefinition bd, Object[] args, Class<?> type) throws Exception {
        Method method = null;
        if (null == type) {
            type = bd.getBeanClass();
        }

        String methodName = bd.getFactoryMethodName();

        if (null == args) {
            return type.getMethod(methodName, null);
        }

        method = bd.getFactoryMethod();

        if (null != method) {
            return method;
        }

        /** 根据参数类型获取精确匹配的方法*/
        Class [] paramTypes = new Class[args.length];

        int j = 0;
        for (Object object : args) {
            paramTypes[j++] = object.getClass();
        }

        try {
            method = type.getMethod(methodName, paramTypes);
        } catch (Exception e) {

        }

        /** 如果没有找到，开始遍历查找*/
        if (null == method) {
            outer:
            for (Method method1 : type.getMethods()) {
                if (!method1.getName().equals(methodName)){
                    continue;
                }
                Class<?> [] paramterTypes = method1.getParameterTypes();

                if (paramterTypes.length == args.length) {
                    for (int i = 0; i < paramterTypes.length; i++) {
                        if (!paramterTypes[i].isAssignableFrom(args[i].getClass())) {
                            continue outer;
                        }
                    }
                    method = method1;
                    break outer;
                }
            }
        }

        if (null != method) {
            if (bd.isPrototype()) {
                bd.setFactoryMethod(method);
            }
        } else {
            throw new Exception("不存在对应的构造方法！" + bd);
        }
        return method;
    }

    private Object[] getConstructorArgumentValues(BeanDefinition bd) throws Exception {

        return this.getRealValues(bd.getConstructorArgumentValues());

    }

    /**
     * 设置属性关联
     * @param bd bean 定义信息
     * @param instance
     * @throws Exception
     */
    private void setPropertyDIValues(BeanDefinition bd, Object instance) throws Exception {
        /** 如果没有属性*/
        if (CollectionUtils.isEmpty(bd.getPropertyValues())) {
            return;
        }
        for (PropertyValue pv : bd.getPropertyValues()) {
            if(StringUtils.isBlank(pv.getName())) {
                continue;
            }
            Class<?> clazz = instance.getClass();

            Field field = clazz.getDeclaredField(pv.getName());

            field.setAccessible(Boolean.TRUE);

            Object rv = pv.getValue();
            Object v = getReferenceObject(rv);
            field.set(instance, v);
        }
    }

    private Object getReferenceObject(Object rv) throws Exception {
        Object v = null;
        if (rv == null) {
            v = null;
        } else if (rv instanceof BeanReference) {
            v = this.doGetBean(((BeanReference) rv).getBeanName());
        } else if (rv instanceof Object[]) {
            // TODO 处理集合中的bean引用
        } else if (rv instanceof Collection) {
            // TODO 处理集合中的bean引用
        } else if (rv instanceof Properties) {
            // TODO 处理properties中的bean引用
        } else if (rv instanceof Map) {
            // TODO 处理Map中的bean引用
        } else {
            v = rv;
        }
        return v;
    }

    private Object[] getRealValues(List<?> defs) throws Exception {
        if(CollectionUtils.isEmpty(defs)) {
            return null;
        }

        Object [] values = new Object[defs.size()];

        int i = 0;

        for (Object vt : defs) {
            Object v = getReferenceObject(vt);
            values[i++] = v;
        }
        return values;
    }

    // 静态工厂方法
    private Object createInstanceByStaticFactoryMethod(BeanDefinition bd) throws Exception {
        Class<?> type = bd.getBeanClass();
        Object[] realArgs = this.getRealValues(bd.getConstructorArgumentValues());
        Method m = this.determineFactoryMethod(bd, realArgs, null);
        return m.invoke(type, realArgs);
    }

    // 工厂bean方式来构造对象
    private Object createInstanceByFactoryBean(BeanDefinition bd) throws Exception {

        Object factoryBean = this.doGetBean(bd.getFactoryBeanName());
        Object [] realArgs = getRealValues((bd.getConstructorArgumentValues()));
        Method m = determineFactoryMethod(bd, realArgs, factoryBean.getClass());

        return m.invoke(factoryBean, realArgs);
    }

    /**
     * 执行初始化方法
     *
     * @param bd
     * @param instance
     * @throws Exception
     */
    private void doInit(BeanDefinition bd, Object instance) throws Exception {
        // 执行初始化方法
        if (StringUtils.isNotBlank(bd.getInitMethodName())) {
            Method m = instance.getClass().getMethod(bd.getInitMethodName(), null);
            m.invoke(instance, null);
        }
    }

    @Override
    public void close() throws IOException {
        // 执行单例实例的销毁方法
        for (Entry<String, BeanDefinition> e : this.beanDefintionMap.entrySet()) {
            String beanName = e.getKey();
            BeanDefinition bd = e.getValue();

            if (bd.isSingleton() && StringUtils.isNotBlank(bd.getDestroyMethodName())) {
                Object instance = this.beanMap.get(beanName);
                try {
                    Method m = instance.getClass().getMethod(bd.getDestroyMethodName(), null);
                    m.invoke(instance, null);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e1) {
                    logger.error("执行bean[" + beanName + "] " + bd + " 的 销毁方法异常！", e1);
                }
            }
        }
    }
}
