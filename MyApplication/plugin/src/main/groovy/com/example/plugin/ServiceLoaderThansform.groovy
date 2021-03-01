package com.example.plugin

import com.android.SdkConstants
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.util.concurrent.ForkJoinPool
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

public class ServiceLoaderThansform extends Transform {
    private Project project;
    private ServiceLoaderExtension extension;
    private Map<String,Map<String,String>> servicesMap;
    private volatile boolean modified = false;
    private boolean isDebug;

    public void setServicesMap(Map<String,Map<String,String>> serviceMapMap){
        servicesMap.putAll(serviceMapMap);
    }
    ServiceLoaderThansform(Project project, ServiceLoaderExtension extension) {
        this.project = project
        this.extension = extension
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        System.out.println("----------transform begin------------")
        long time = System.currentTimeMillis();
        ServiceLoaderPlugin serviceLoaderPlugin;
        if(project.plugins.hasPlugin("serviceloader")){
            serviceLoaderPlugin = project.plugins.findPlugin("serviceloader");
        }
        isDebug = (extension == null ? false : extension.enableLog)
        Map<String,Map<String,String>> declaredMap = null;
        if(serviceLoaderPlugin.servicesMap != null && !serviceLoaderPlugin.servicesMap.isEmpty()){
            declaredMap = new TreeMap<String,TreeMap<String,String>>();
            serviceLoaderPlugin.servicesMap.each {String key,Map<String,String> value->
            declaredMap.put(key,new TreeMap<String, String>(value))
            }
        }
       new ForkJoinPool().submit({
           inputs.parallelStream().each {input->
               new ForkJoinPool().submit({
                   input.directoryInputs.each {DirectoryInput dirInput->
                       def output = outputProvider.getContentLocation(dirInput.getName(),dirInput.contentTypes,dirInput.scopes, Format.DIRECTORY)
                       FileUtils.copyDirectory(dirInput.file,output)
                   }
               }).get()
              new ForkJoinPool().submit({
                  input.jarInputs.parallelStream().each {
                      JarInput jarInput->
                      def output = outputProvider.getContentLocation(jarInput.name,jarInput.contentTypes,jarInput.scopes,Format.JAR);
                      if(modified || (null == declaredMap || declaredMap.size() == 0)){
                           FileUtils.copyFile(jarInput.file,output)
                      }else{
                          String jarName = jarInput.name;
                          File dest = outputProvider.getContentLocation(jarName,jarInput.contentTypes,jarInput.scopes,Format.JAR)
                          if(!dest.exists()){
                              dest.parentFile.mkdirs()
                              dest.createNewFile();
                          }
                          if(true){//如果开启插件时候，则进行相应的操作
                              def jarFile = new JarFile(jarInput.file);
                              Enumeration<JarEntry> enumeration =  jarFile.entries();
                              JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(dest));
                              while (enumeration.hasMoreElements()){
                                  JarEntry jarEntry = (JarEntry)enumeration.nextElement();
                                  InputStream inputStream = jarFile.getInputStream(jarEntry);
                                  //在输出流中创建新的entry
                                  ZipEntry zipEntry = new ZipEntry(jarEntry.name)
                                  jarOutputStream.putNextEntry(zipEntry);
                                  if(!jarEntry.name.endsWith("com/example/serviceloader/ServiceLoader"+ SdkConstants.DOT_CLASS)){
                                      //如果非ServiceLoader.class文件直接copy
                                      jarOutputStream.write(IOUtils.toByteArray(inputStream))
                                      inputStream.close()
                                  }else{
                                      println("serviceloader class :${jarEntry.name}")
                                      ClassPool pool = ClassPool.getDefault();
                                      pool.insertClassPath(jarInput.file.absolutePath)
                                      //动态加载serviceloader
                                      CtClass ctClass = pool.get("com.example.serviceloader.ServiceLoader")
                                      //解冻
                                      ctClass.defrost();
                                      CtMethod ctMethod = ctClass.getDeclaredMethod("getServiceMap");
                                      StringBuilder bodyBuilder = new StringBuilder();
                                      //这块可以优化下
                                      bodyBuilder.append("{\n servicesMap = new java.util.HashMap();\n");
                                      bodyBuilder.append("java.util.Map implementMap;\n");
                                      declaredMap.each {String key,Map<String,String> value->
                                          bodyBuilder.append("implementMap = new java.util.HashMap();\n")
                                          value.each {Map.Entry<String,String> entry->
                                              bodyBuilder.append("implementMap.put(\"${entry.key}\",\"${entry.value}\");\n")
                                          }
                                          bodyBuilder.append("servicesMap.put(\"$key\",implementMap);\n")
                                      }
                                      bodyBuilder.append("}")
                                      if(isDebug){
                                          System.out.println("插入的代码为---${bodyBuilder.toString()}")
                                      }
                                      ctMethod.setBody(bodyBuilder.toString())
                                      byte[] classBytes = ctClass.toBytecode();
                                      ctClass.detach();//冻住类，禁止编辑类
                                      modified = true;
                                      jarOutputStream.write(classBytes);
                                      jarOutputStream.flush();
                                      inputStream.close();
                                  }
                                  jarOutputStream.closeEntry()
                              }
                              jarOutputStream.close();
                              jarFile.close();
                          }else{
                              FileUtils.copyFile(jarInput.getFile(),dest)
                          }
                      }
                  }
              }).get()
               System.out.println("----serviceloader thrasform cost time : ${(System.currentTimeMillis()- time) / 1000}s")
           }
       }).get()
        System.out.println("----------transform end------------")
    }

    @Override
    public String getName() {
        return "serviceLoader";
    }

    /**
     * 只处理jar类型的（就会走到transform里面）,class类型的就不会实现tansform了
     * @return
     */
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_JARS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }
}
