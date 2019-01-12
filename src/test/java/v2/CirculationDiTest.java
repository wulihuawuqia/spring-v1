package v2;

import com.dn.spring.beans.BeanReference;
import com.dn.spring.beans.GenericBeanDefinition;
import com.dn.spring.beans.PreBuildBeanFactory;
import com.dn.spring.samples.DBean;
import com.dn.spring.samples.EBean;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CirculationDiTest {

	static PreBuildBeanFactory bf = new PreBuildBeanFactory();

	@Test
	public void testCirculationDI() throws Exception {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(DBean.class);
		List<Object> args = new ArrayList<>();
		args.add(new BeanReference("ebean"));
		bd.setConstructorArgumentValues(args);
		bf.registerBeanDefinition("dbean", bd);

		bd = new GenericBeanDefinition();
		bd.setBeanClass(EBean.class);
		args = new ArrayList<>();
		args.add(new BeanReference("dbean"));
		bd.setConstructorArgumentValues(args);
		bf.registerBeanDefinition("ebean", bd);

		bf.preInstantiateSingletons();
	}
}
