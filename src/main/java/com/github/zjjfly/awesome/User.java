package com.github.zjjfly.awesome;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * @author zjjfly[https://github.com/zjjfly] on 2020/7/6
 */
public class User {

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    private String name;

    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public static void main(String[] args) {
        JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
        System.out.println(systemJavaCompiler.getStandardFileManager(new DiagnosticListener(){

            @Override
            public void report(Diagnostic diagnostic) {
                System.out.println(diagnostic.toString());
            }
        },null,null));
    }
}
