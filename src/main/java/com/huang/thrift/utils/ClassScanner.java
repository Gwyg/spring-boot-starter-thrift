package com.huang.thrift.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassScanner {

    public static List<Class<?>> findAnnotatedClasses(String packageName, Class<? extends Annotation> annotationClass) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            // 获取包路径
            String path = packageName.replace('.', '/');
            // 获取类加载器
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()){
                URL resource = resources.nextElement();
                File dir = new File(resource.getFile());
                if(dir.exists()){
                    scanDirectory(packageName, dir, classes, annotationClass);
                }
            }
        }catch (Exception e){
            throw new RuntimeException("Failed to scan classes in package: " + packageName, e);
        }
        return classes;
    }

    private static void scanDirectory(String packageName, File directory,List<Class<?>> classes, Class<? extends Annotation> annotationClass){
        File[] files = directory.listFiles();
        if(files == null) return;
        for (File file : files){
            if(file.isDirectory()){
                scanDirectory(packageName + "." + file.getName(), file, classes, annotationClass);
            }else if(file.getName().endsWith(".class")){
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if(clazz.isAnnotationPresent(annotationClass)){
                        classes.add(clazz);
                    }
                }catch (Exception e){
                    throw new RuntimeException("Failed to load class: " + className, e);
                }
            }
        }
    }

}
