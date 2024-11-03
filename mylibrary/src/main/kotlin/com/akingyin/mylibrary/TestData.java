package com.akingyin.mylibrary;

/**
 * @author: aking <a href="mailto:akingyin@163.com">Contact me.</a>
 * @since: 2024/7/24 11:03
 * @version: 1.0
 */
public class TestData implements TestInterface {
    @Override
    public void test() {
        System.out.println("test");
    }

    @Override
    public String getName() {
        return "这是测试名111称";
    }
}
