package cn.muxin.springframework.beans.factory.xml;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.muxin.springframework.beans.BeansException;
import cn.muxin.springframework.beans.PropertyValue;
import cn.muxin.springframework.beans.factory.config.BeanDefinition;
import cn.muxin.springframework.beans.factory.config.BeanReference;
import cn.muxin.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import cn.muxin.springframework.beans.factory.support.BeanDefinitionRegistry;
import cn.muxin.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import cn.muxin.springframework.core.io.Resource;
import cn.muxin.springframework.core.io.ResourceLoader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Bean definition reader for XML bean definitions.
 *
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {
        super(registry, resourceLoader);
    }

    @Override
    public void loadBeanDefinitions(Resource resource) throws BeansException {
        try {
            try (InputStream inputStream = resource.getInputStream()) {
                doLoadBeanDefinitions(inputStream);
            }
        } catch (IOException | ClassNotFoundException | DocumentException e) {
            throw new BeansException("IOException parsing XML document from " + resource, e);
        }
    }

    @Override
    public void loadBeanDefinitions(Resource... resources) throws BeansException {
        for (Resource resource : resources) {
            loadBeanDefinitions(resource);
        }
    }

    @Override
    public void loadBeanDefinitions(String location) throws BeansException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        loadBeanDefinitions(resource);
    }

    @Override
    public void loadBeanDefinitions(String... locations) throws BeansException {
        for (String location : locations) {
            loadBeanDefinitions(location);
        }
    }

    protected void doLoadBeanDefinitions(InputStream inputStream) throws ClassNotFoundException, DocumentException {
//        Document doc = XmlUtil.readXML(inputStream);
//        Element root = doc.getDocumentElement();
//        NodeList childNodes = root.getChildNodes();
        SAXReader reader = new SAXReader();
        Document document = reader.read(inputStream);
        Element root = document.getRootElement();

        // ?????? context:component-scan ??????????????????????????????????????????????????????????????? BeanDefinition
        Element componentScan = root.element("component-scan");
        if (null != componentScan) {
            String scanPath = componentScan.attributeValue("base-package");
            if (StrUtil.isEmpty(scanPath)) {
                throw new BeansException("The value of base-package attribute can not be empty or null");
            }
            scanPackage(scanPath);
        }

        List<Element> beanList = root.elements("bean");

        for (Element bean : beanList) {
//            // ????????????
//            if (!(childNodes.item(i) instanceof Element)) {
//                continue;
//            }
//            // ????????????
//            if (!"bean".equals(childNodes.item(i).getNodeName())) {
//                continue;
//            }
            
            // ????????????
//            Element bean = (Element) childNodes.item(i);
//            String id = bean.getAttribute("id");
//            String name = bean.getAttribute("name");
//            String className = bean.getAttribute("class");
//            String initMethod = bean.getAttribute("init-method");
//            String destroyMethodName = bean.getAttribute("destroy-method");
//            String beanScope = bean.getAttribute("scope");
            String id = bean.attributeValue("id");
            String name = bean.attributeValue("name");
            String className = bean.attributeValue("class");
            String initMethod = bean.attributeValue("init-method");
            String destroyMethodName = bean.attributeValue("destroy-method");
            String beanScope = bean.attributeValue("scope");

            // ?????? Class??????????????????????????????
            Class<?> clazz = Class.forName(className);
            // ????????? id > name
            String beanName = StrUtil.isNotEmpty(id) ? id : name;
            if (StrUtil.isEmpty(beanName)) {
                beanName = StrUtil.lowerFirst(clazz.getSimpleName());
            }

            // ??????Bean
            BeanDefinition beanDefinition = new BeanDefinition(clazz);
            beanDefinition.setInitMethodName(initMethod);
            beanDefinition.setDestroyMethodName(destroyMethodName);

            //???????????????
            if (StrUtil.isNotEmpty(beanScope)) {
                beanDefinition.setScope(beanScope);
            }
            List<Element> propertyList = bean.elements("property");
            // ?????????????????????
//            for (int j = 0; j < bean.getChildNodes().getLength(); j++) {
//                if (!(bean.getChildNodes().item(j) instanceof Element)) {
//                    continue;
//                }
//                if (!"property".equals(bean.getChildNodes().item(j).getNodeName())) {
//                    continue;
//                }
//                // ???????????????property
//                Element property = (Element) bean.getChildNodes().item(j);
//                String attrName = property.getAttribute("name");
//                String attrValue = property.getAttribute("value");
//                String attrRef = property.getAttribute("ref");
//                // ??????????????????????????????????????????
//                Object value = StrUtil.isNotEmpty(attrRef) ? new BeanReference(attrRef) : attrValue;
//                // ??????????????????
//                PropertyValue propertyValue = new PropertyValue(attrName, value);
//                beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
//            }
            // ?????????????????????
            for (Element property : propertyList) {
                // ???????????????property
                String attrName = property.attributeValue("name");
                String attrValue = property.attributeValue("value");
                String attrRef = property.attributeValue("ref");
                // ??????????????????????????????????????????
                Object value = StrUtil.isNotEmpty(attrRef) ? new BeanReference(attrRef) : attrValue;
                // ??????????????????
                PropertyValue propertyValue = new PropertyValue(attrName, value);
                beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
            }
            if (getRegistry().containsBeanDefinition(beanName)) {
                throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
            }
            // ?????? BeanDefinition
            getRegistry().registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private void scanPackage(String scanPath) {
        String[] basePackages = StrUtil.splitToArray(scanPath, ',');
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(getRegistry());
        scanner.doScan(basePackages);
    }

}
