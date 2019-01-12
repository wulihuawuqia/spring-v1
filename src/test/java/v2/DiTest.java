/**
 * 版权所有: wulihua
 * 创建日期: 2019/1/12
 * 创建作者：wuqia
 * 文件名称：DiTest.java
 * 版本: 1.0
 * 修改记录:
 */
package v2;

import com.dn.spring.beans.BeanReference;
import com.dn.spring.beans.GenericBeanDefinition;
import com.dn.spring.beans.PreBuildBeanFactory;
import com.dn.spring.samples.ABean;
import com.dn.spring.samples.ABeanFactory;
import com.dn.spring.samples.CBean;
import com.dn.spring.samples.CCBean;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Description: 依赖注入测试.
 * @author wuqia
 * @since 2019/1/12
 */
public class DiTest {
    static PreBuildBeanFactory bf = new PreBuildBeanFactory();

    @Test
    public void testConstructorDI() throws Exception {

        GenericBeanDefinition bd = new GenericBeanDefinition();

        List<Object> args = new ArrayList<>();

        bd.setBeanClass(ABean.class);

        args.add("abean01");
        args.add(new BeanReference("cbean"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(CBean.class);
        args = new ArrayList<>();
        args.add("cbean01");
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("cbean", bd);

        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean");
        //CBean cBean = (CBean) bf.getBean("cbean");

        abean.doSomthing();
    }

    @Test
    public void testStaticFactoryMethodDI() throws Exception {

        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(ABeanFactory.class);
        bd.setFactoryMethodName("getABean");
        List<Object> args = new ArrayList<>();
        args.add("abean02");
        args.add(new BeanReference("cbean02"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean02", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(CBean.class);
        args = new ArrayList<>();
        args.add("cbean02");
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("cbean02", bd);

        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean02");

        abean.doSomthing();
    }

    @Test
    public void testFactoryMethodDI() throws Exception {

        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setFactoryBeanName("abeanFactory");
        bd.setFactoryMethodName("getABean2");
        List<Object> args = new ArrayList<>();
        args.add("abean03");
        args.add(new BeanReference("cbean02"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean03", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(ABeanFactory.class);
        bf.registerBeanDefinition("abeanFactory", bd);

        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean03");

        abean.doSomthing();
    }

    @Test
    public void testChildTypeDI() throws Exception {

        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(ABean.class);
        List<Object> args = new ArrayList<>();
        args.add("abean04");
        args.add(new BeanReference("ccbean01"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean04", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(CCBean.class);
        args = new ArrayList<>();
        args.add("Ccbean01");
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("ccbean01", bd);

        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean04");

        abean.doSomthing();
    }
}
