package org.superboot.pub.utils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.superboot.base.SuperBootCode;
import org.superboot.base.SuperBootException;
import org.superboot.config.RequestMappingHandlerConfig;
import org.superboot.entity.base.BaseApi;
import org.superboot.pub.Pub_Tools;
import org.superboot.repository.sql.base.BaseApiRepository;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b> 通用数据库操作工具类 </b>
 * <p>
 * 功能描述:
 * </p>
 *
 * @author jesion
 * @date 2017/9/17
 * @time 22:34
 * @Path org.superboot.pub.utils.Pub_DBUtils
 */
@Component
public class Pub_DBUtils {

    @Autowired  //业务服务类
    private BaseApiRepository sysApiRepository;

    @Autowired
    private RequestMappingHandlerConfig requestMappingHandlerConfig;

    @Autowired
    private Pub_Tools pubTools;

    /**
     * 自动添加服务器的接口API到数据库
     */
    public void addApiToDB() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = requestMappingHandlerConfig.requestMappingHandlerMapping();
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        Set<RequestMappingInfo> mappings = map.keySet();
        for (RequestMappingInfo info : mappings) {
            HandlerMethod method = map.get(info);
            String moduleStr = method.toString();
            if (0 < moduleStr.split("\\(").length) {
                moduleStr = moduleStr.split("\\(")[0];
            }

            if (2 < moduleStr.split(" ").length) {
                moduleStr = moduleStr.split(" ")[2];
            }
            int i = moduleStr.lastIndexOf(".");
            moduleStr = moduleStr.substring(0, i);
            String urlparm = info.getPatternsCondition().toString();
            String url = urlparm.substring(1, urlparm.length() - 1);
            String methodName = method.getMethod().getName();

            //获取API信息
            BaseApi list = (BaseApi) sysApiRepository.findByUrlAndMethodName(url, methodName);
            if (null == list) {
                //只有项目用到的资源才需要添加
                if (moduleStr.startsWith("org.superboot.controller")) {
                    BaseApi api = new BaseApi();
                    api.setUrl(url);
                    api.setMethodPath(moduleStr);
                    //获取字段的注解信息，此处用到的swagger配置的注解信息
                    try {
                        api.setMethodName(methodName);
                        //获取对象信息
                        Object o = Class.forName(moduleStr).newInstance();
                        //映射模块路径
                        RequestMapping requestMapping = o.getClass().getAnnotation(RequestMapping.class);
                        if (null != requestMapping) {
                            api.setModulePath(StringUtils.join(requestMapping.value(), ","));
                        }
                        //设置模块名称
                        Api api_annotation = o.getClass().getAnnotation(Api.class);
                        if (null != api_annotation) {
                            api.setModuleName(StringUtils.join(api_annotation.tags(), ","));
                        }
                        //根据API注解设置API名称及备注信息
                        Annotation operation_annotation = pubTools.getMothodAnnotationByAnnotation(o, ApiOperation.class, methodName);
                        if (null != operation_annotation) {
                            api.setApiName(((ApiOperation) operation_annotation).value());
                            api.setRemark(((ApiOperation) operation_annotation).notes());
                        }
                        sysApiRepository.save(api);
                    } catch (InstantiationException e) {
                        throw new SuperBootException(SuperBootCode.EXCEPTION,e);
                    } catch (IllegalAccessException e) {
                        throw new SuperBootException(SuperBootCode.EXCEPTION,e);
                    } catch (ClassNotFoundException e) {
                        throw new SuperBootException(SuperBootCode.EXCEPTION,e);
                    }
                }
            }
        }
    }
}
