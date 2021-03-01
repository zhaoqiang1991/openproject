package com.example.processsor;

import com.example.annotation.ServiceLoaderInterface;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

//加入这一行告诉注解框架解析，就不用在项目根目录下面写注解入口了，不然注解解释器不生效
@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.example.annotation.ServiceLoaderInterface"})
public class ServiceLoaderProcessor extends AbstractProcessor {
    private Map<String, Map<String, String>> servicesMap = new HashMap<>();
    private Filer filer;
    private Elements elementUtils;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //写入文件使用
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();

    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        log("process start");
        try {
            return processImpl(set,roundEnvironment);
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            log("process出现异常l"+writer.toString());
            return true;
        }
    }

    private boolean processImpl(Set<? extends TypeElement> aninations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()){
            //如果已经处理过注解，那么就生成文件
            generateConfigFiles();
        }else{
            //没有处理过注解，那么就处理注解
            processAnnotations(roundEnv);
        }
        return true;
    }

    private void processAnnotations(RoundEnvironment roundEnv) {
        //处理那种注解，因为项目中有很多的注解，比如@Override @NullPoint
        log("processAnnotations start");
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ServiceLoaderInterface.class);
        for (Element e : elements) {
            //只处理是在class，比如interface,filed,pacakge...都不处理
            if(e.getKind() != ElementKind.CLASS) continue;//继续找下一个
            TypeElement typeElement = (TypeElement) e;
            //返回类型元素的二进制名称, 比如:com.example.myapplication.serviceload.AppModuleInterface
            String className = processingEnv.getElementUtils().getBinaryName(typeElement).toString();
            //拿到注解
            ServiceLoaderInterface serviceLoaderInterface = typeElement.getAnnotation(ServiceLoaderInterface.class);
            //获取注解里面的key值
            String key = serviceLoaderInterface.key();
            //拿到注解的value,也就是class文件
            String interfaceName = null;
            try {
                Class<?> clazz = serviceLoaderInterface.interfaceClass();
                interfaceName = null;
                if(clazz != null && clazz == Void.class){
                    //返回java规范的类名称,比较getName和getSimpleName更加准确一些
                    interfaceName = clazz.getCanonicalName();
                }
            } catch (MirroredTypeException mte) {
                interfaceName = processMirroredTypeException(mte);
            }

            if(interfaceName == null || interfaceName.isEmpty()){
                interfaceName = serviceLoaderInterface.interfaceName();
            }

            if(interfaceName == null || interfaceName.isEmpty()){
                interfaceName = processBaseInterfaces(typeElement);
            }

            interfaceStore(key,interfaceName,className);

            String[] interfaceNames = null;
            Class<?>[] classes = new Class[0];
            try {
                classes = serviceLoaderInterface.interfaceClasses();
                interfaceNames = processClassArray(classes);
            } catch (MirroredTypesException mte) {
                interfaceNames = processMirroredTypesException(mte);
            }

            processInterfaceArray(serviceLoaderInterface.keys(),interfaceNames,className);
        }

        log("processAnnotations end");
    }

    private String[] processClassArray(Class<?>[] classes) {
        //写任何代码之前都是容错处理在最前面
        if(classes == null || classes.length == 0) return  null;
        String[] interfaceNames = new String[classes.length];
        for (int i = 0 ; i < interfaceNames.length ; i++){
            interfaceNames[i] = classes[i].getCanonicalName();
        }
        //返回这些interface名称
        return interfaceNames;
    }


    private String[] processMirroredTypesException(MirroredTypesException mte){
        List<? extends TypeMirror> typeMirrors = mte.getTypeMirrors();
        if(typeMirrors.isEmpty()) return null;
        String[] interfaceName = new String[typeMirrors.size()];
        for (int i = 0 ; i < typeMirrors.size() ; i++){
            interfaceName[i] = processExceptionTypeMirror(typeMirrors.get(i));
        }
        return interfaceName;
    }

    private String processMirroredTypeException(MirroredTypeException mte){
        //如果接口还没有被编译，也就是没有class文件，这种情况下直接获取class会抛出MirroredTypeException异常
        return processExceptionTypeMirror(mte.getTypeMirror());
    }
    private String processExceptionTypeMirror(TypeMirror typeMirror){
        DeclaredType classTypeMirror = (DeclaredType) typeMirror;
        TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
        String interfaceName = classTypeElement.getQualifiedName().toString();
        return interfaceName;
    }

    private void processInterfaceArray(String[] keys, String[] interfaceNames, String className) {
        if(keys == null || interfaceNames == null || keys.length == 0 || keys.length != interfaceNames.length){
            return;
        }
        for (int i = 0 ; i < keys.length ; i++){
            interfaceStore(keys[i],interfaceNames[i],className);
        }

    }

    private String processBaseInterfaces(TypeElement typeElement) {
        String interfaceName = null;
        List<? extends TypeMirror> interfaceList = typeElement.getInterfaces();
        if (interfaceList != null && interfaceList.size() > 0){
            TypeMirror typeMirror = interfaceList.get(0);
            //说明没有实现的接口，就不需要在遍历了，直接跳出循环
            if(typeMirror == null) return null;
            TypeElement interfaceElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
            if(null == interfaceElement) return null;
            interfaceName = processingEnv.getElementUtils().getBinaryName(interfaceElement).toString();
        }
        return interfaceName;
    }

    private void interfaceStore(String key, String interfaceName, String className) {
        if(key == null || interfaceName == null || className == null) return;
        Map<String, String> interfaceMap = servicesMap.get(interfaceName);//interfaceName就是接口名称,比如com.example.sample_baseinterface.ModuleInterface
        if(interfaceMap == null){
            interfaceMap = new HashMap<>();
            servicesMap.put(interfaceName,interfaceMap);
        }
        interfaceMap.put(key,className);//className就是实现接口(interfaceName这个接口)的类，interfaceMap存放是：这种结构数据app -> com.example.myapplication.serviceload.AppModuleInterface

        log("key = " +key);
        log("interfaceName = " +interfaceName);
        log("className = " +className);
        log("interfaceMap.size() = " +interfaceMap.size());

    }

    private void generateConfigFiles(){
        log("generateConfigFiles start");
        for(String interfaceName : servicesMap.keySet()){
            String resourceFile = "META-INF/services/serviceloader/" + interfaceName + "_auto";
            try {
                FileObject exitFile = filer.getResource(StandardLocation.CLASS_OUTPUT,"p",resourceFile);
                exitFile.delete();
            } catch (IOException e) {
                log("resource file did not already exit.");
            }
            Map<String,String> interfaceMap = servicesMap.get(interfaceName);
            if(interfaceMap == null) continue;
            FileObject fileObject = null;
            try {
                fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT,"", resourceFile);
                log("fileObject="+fileObject.toUri().toString());
                Writer writer = fileObject.openWriter();
                for (String key : interfaceMap.keySet()){
                    writer.write(key + " : " + interfaceMap.get(key));
                    writer.write("\n");
                }
                writer.flush();
                writer.close();

            } catch (IOException e) {
                log("generateConfigFiles exception" + e.getStackTrace());
                return;
            }

        }
        log("generateConfigFiles end");
    }

    private void log(String s) {
        System.out.println("-------"+s);
    }


}
