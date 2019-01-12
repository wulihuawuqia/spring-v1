package v1;

import com.dn.spring.beans.PreBuildBeanFactory;
import org.junit.AfterClass;
import org.junit.Test;

import com.dn.spring.beans.BeanDefinition;
import com.dn.spring.beans.DefaultBeanFactory;
import com.dn.spring.beans.GenericBeanDefinition;
import com.dn.spring.samples.ABean;
import com.dn.spring.samples.ABeanFactory;

import java.util.List;

public class DefaultBeanFactoryTest {

	static DefaultBeanFactory bf = new PreBuildBeanFactory();

	@Test
	public void testRegist() throws Exception {
		GenericBeanDefinition bd = new GenericBeanDefinition();

		bd.setBeanClass(ABean.class);
		bd.setScope(BeanDefinition.SCOPE_SINGLETION);
		// bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);

		bd.setInitMethodName("init");
		bd.setDestroyMethodName("destroy");

		bf.registerBeanDefinition("aBean", bd);

	}

	@Test
	public void testRegistStaticFactoryMethod() throws Exception {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(ABeanFactory.class);
		bd.setFactoryMethodName("getABean");
		bf.registerBeanDefinition("staticAbean", bd);
	}

	@Test
	public void testRegistFactoryMethod() throws Exception {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(ABeanFactory.class);
		String fbname = "factory";
		bf.registerBeanDefinition(fbname, bd);

		bd = new GenericBeanDefinition();
		bd.setFactoryBeanName(fbname);
		bd.setFactoryMethodName("getABean2");
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);

		bf.registerBeanDefinition("factoryAbean", bd);
	}

	@AfterClass
	public static void testGetBean() throws Exception {
		System.out.println("构造方法方式------------");
		for (int i = 0; i < 3; i++) {
			ABean ab = (ABean) bf.getBean("aBean");
			ab.doSomthing();
		}

		System.out.println("静态工厂方法方式------------");
		for (int i = 0; i < 3; i++) {
			ABean ab = (ABean) bf.getBean("staticAbean");
			ab.doSomthing();
		}

		System.out.println("工厂方法方式------------");
		for (int i = 0; i < 3; i++) {
			ABean ab = (ABean) bf.getBean("factoryAbean");
			ab.doSomthing();
		}

		bf.close();
	}
}
