package com.dn.spring.samples;

public class ABean {

	private String name;

	private CBean cb;

	public ABean(String name, CBean cb) {
		super();
		this.name = name;
		this.cb = cb;
		System.out.println("调用了含有CBean参数的构造方法");
	}

	public void doSomthing() {
		System.out.println(System.currentTimeMillis() + " " + this);
	}

	public void init() {
		System.out.println("ABean.init() 执行了");
	}

	public void destroy() {
		System.out.println("ABean.destroy() 执行了");
	}
}
