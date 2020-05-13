package com.lwf.springServlet;

import com.lwf.annotation.LwfAutowired;
import com.lwf.annotation.LwfController;
import com.lwf.annotation.LwfRequestMapping;
import com.lwf.annotation.LwfRequestParam;
import com.lwf.annotation.LwfService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: liuwenfei14
 * @date: 2020-04-26 18:03
 */
public class LwfDispatcherServlet extends HttpServlet {
    private Set<String> scanClassNames = new HashSet<String>();
    private Map<String, Object> ioC = new HashMap<String, Object>();
    private Map<String, Object> handlerMapping = new HashMap<String, Object>();

    /**
     * web容器初始化的入口，因此从init出发需要触发ioc，并且需要进行maphandler映射。 1.使用classloader加载配置文件，读取到scanPackage
     * 2.扫描package加载class 3.分析加载的class，进行实例化instance，并进行handlermapping映射 4.instance后进行ioc依赖注入
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        Properties properties = new Properties();
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = classLoader.getResourceAsStream(config.getInitParameter("contextConfigLocation"));
            //1. 获取classloader，并加载web.xml中配置的配置文件的地址，从而获取配置信息，一般contextConfigLocation设置的是spring-config.xml
            properties.load(resourceAsStream);
            //2. 扫描设置的scanpackage路径，并生成类名set
            String scanPackage = properties.getProperty("scanPackage");
            doScaner(scanPackage);
            //3. 实例化要加载的类，并放入ioc容器
            initIoC();
            //4. Ioc中实例的类进行遍历，查询所有标注了指定annotation的类，从annotation中获取route url，并放入到handlerMapping中
            createHandlerMapping();
            //5. 对ioc中的类进行遍历，进行依赖注入
            processDI();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (resourceAsStream != null) {

                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processDI() {
        for (Object value : ioC.values()) {
            Class<?> clazz = value.getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (!declaredField.isAnnotationPresent(LwfAutowired.class)) {
                    continue;
                }
                Class<?> fieldType = declaredField.getType();
                LwfAutowired autowired = declaredField.getAnnotation(LwfAutowired.class);
                String nameValue = autowired.value();
                if (nameValue==null||nameValue.length()==0){
                    nameValue=getLowerCaseName( fieldType.getSimpleName());
                }
                declaredField.setAccessible(true); //设置私有属性的访问权限
                Object o = ioC.get(nameValue);
                try {
                    declaredField.set(value,o);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createHandlerMapping() {
        for (Object value : ioC.values()) {
            Class<?> clazz = value.getClass();
            if (clazz.isAnnotationPresent(LwfController.class)) {
                LwfRequestMapping lwfRequestMapping = clazz.getAnnotation(LwfRequestMapping.class);
                String rootRoute="/";
                if (lwfRequestMapping!=null){

                     rootRoute = lwfRequestMapping.value();
                }
                Method[] declaredMethods = clazz.getDeclaredMethods();
                for (Method declaredMethod : declaredMethods) {
                    LwfRequestMapping methodAnnotation = declaredMethod.getAnnotation(lwfRequestMapping.getClass());
                    if (methodAnnotation==null){
                        continue;
                    }else {
                        String methodRoute = methodAnnotation.value();
                        handlerMapping.put(("/"+rootRoute+"/"+methodRoute).replaceAll("/+","/"),declaredMethod);
                    }
                }
            }
        }
    }

    private void initIoC() {
        for (String scanClassName : scanClassNames) {
            try {
                Class<?> clazz = Class.forName(scanClassName);
                if (clazz.isAnnotationPresent(LwfController.class) || clazz.isAnnotationPresent(LwfService.class)) {

                    Object o = clazz.newInstance();
                    String simpleName = getLowerCaseName(clazz.getSimpleName());
                    ioC.put(simpleName, o);

                    if (clazz.isAnnotationPresent(LwfService.class)) {
                        Class<?>[] interfaces = clazz.getInterfaces();
                        for (Class<?> anInterface : interfaces) {
                            String lowerCaseName = getLowerCaseName(anInterface.getSimpleName());
                            if (ioC.containsKey(lowerCaseName)) {
                                System.out.println("this is already exists:" + lowerCaseName);
                            } else {
                                ioC.put(lowerCaseName, o);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private String getLowerCaseName(String name) {
        char[] chars = name.toCharArray();
        if (chars[0] < 'a') {
            chars[0] = (char) (chars[0] - 'A' + 'a');
        }
        return String.valueOf(chars);
    }

    private void doScaner(String scanPackage) {
        String path = scanPackage.replaceAll("\\.", "/");
        URL resource = this.getClass().getClassLoader().getResource("/" + path);
        File fileDirs = new File(resource.getFile());
        for (File file : fileDirs.listFiles()) {
            if (file.isDirectory()) {
                doScaner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                } else {
                    scanClassNames.add(scanPackage + "." + file.getName().replaceAll("\\.class$", ""));
                }
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 exception is :"+Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();

            resp.getWriter().write("500 exception is :"+Arrays.toString(e.getStackTrace()));
        }
    }
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Method method = (Method) this.handlerMapping.get(url);
        //第一个参数：方法所在的实例
        //第二个参数：调用时所需要的实参
        Map<String,String[]> params = req.getParameterMap();
        //获取方法的形参列表
        Class<?> [] parameterTypes = method.getParameterTypes();
        //保存请求的url参数列表
        Map<String,String[]> parameterMap = req.getParameterMap();
        //保存赋值参数的位置
        Object [] paramValues = new Object[parameterTypes.length];
        //按根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i ++){
            Class parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
                continue;
            }else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
                continue;
            }else if(parameterType == String.class){

                //提取方法中加了注解的参数
                Annotation[] [] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length ; j ++) {
                    for(Annotation a : pa[i]){
                        if(a instanceof LwfRequestParam){
                            String paramName = ((LwfRequestParam) a).value();
                            if(!"".equals(paramName.trim())){
                                String value = Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s",",");
                                paramValues[i] = value;
                            }
                        }
                    }
                }

            }
        }
        //投机取巧的方式
        //通过反射拿到method所在class，拿到class之后还是拿到class的名称
        //再调用toLowerFirstCase获得beanName
        String beanName = getLowerCaseName(method.getDeclaringClass().getSimpleName());
        method.invoke(ioC.get(beanName),new Object[]{req,resp,params.get("name")[0]});
    }
}
