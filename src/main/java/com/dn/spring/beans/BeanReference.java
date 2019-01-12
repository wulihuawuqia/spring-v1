/**
 * 版权所有: wulihua
 * 创建日期: 2019/1/12
 * 创建作者：wuqia
 * 文件名称：BeanReference.java
 * 版本: 1.0
 * 修改记录:
 */
package com.dn.spring.beans;

/**
 * Description: bean 指向.
 * @author wuqia
 * @since 2019/1/12
 */
public class BeanReference {

    private String beanName;

    public BeanReference(String beanName) {
        super();
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
