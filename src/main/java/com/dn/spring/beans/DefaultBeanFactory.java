package com.dn.spring.beans;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultBeanFactory implements BeanFactory, BeanDefinitionRegistry, Closeable {

    private final Log logger = LogFactory.getLog(getClass());

    private Map<String, BeanDefinition> beanDefintionMap = new ConcurrentHashMap<>(256);

    private Map<String, Object> beanMap = new ConcurrentHashMap<>(256);

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
        Objects.requireNonNull(bd, "beanDefinition不能为空");

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

        // 执行初始化方法
        this.doInit(bd, instance);

        if (bd.isSingleton()) {
            beanMap.put(beanName, instance);
        }

        return instance;
    }

    // 构造方法来构造对象
    private Object createInstanceByConstructor(BeanDefinition bd)
            throws InstantiationException, IllegalAccessException {
        try {
            return bd.getBeanClass().newInstance();
        } catch (SecurityException e1) {
            logger.error("创建bean的实例异常,beanDefinition：" + bd, e1);
            throw e1;
        }
    }

    // 静态工厂方法
    private Object createInstanceByStaticFactoryMethod(BeanDefinition bd) throws Exception {
        Class<?> type = bd.getBeanClass();
        Method m = type.getMethod(bd.getFactoryMethodName(), null);
        return m.invoke(type, null);
    }

    // 工厂bean方式来构造对象
    private Object createInstanceByFactoryBean(BeanDefinition bd) throws Exception {

        Object factoryBean = this.doGetBean(bd.getFactoryBeanName());

        Method m = factoryBean.getClass().getMethod(bd.getFactoryMethodName(), null);

        return m.invoke(factoryBean, null);
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
