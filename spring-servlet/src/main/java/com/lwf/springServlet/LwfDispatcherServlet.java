package com.lwf.springServlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    }

    private void createHandlerMapping() {
        for (Object value : ioC.values()) {

        }
    }

    private void initIoC() {
        for (String scanClassName : scanClassNames) {
            try {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(scanClassName);

                Object o = clazz.newInstance();
                String simpleName = clazz.getSimpleName();
                ioC.put(getLowerCaseName(simpleName),o);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    private String getLowerCaseName(String name){
        char[] chars = name.toCharArray();
        if (chars[0]<'a'){
            chars[0]=(char)(chars[0]-'A'+'a');
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
                    scanClassNames.add(scanPackage + "." + file.getName().replaceAll("\\.class$", ""))
                }
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }
}
